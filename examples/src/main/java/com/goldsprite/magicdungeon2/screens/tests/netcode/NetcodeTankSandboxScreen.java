package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.LocalMemoryTransport;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.Transport;
import com.goldsprite.gdengine.screens.GScreen;

public class NetcodeTankSandboxScreen extends GScreen {

    private NeonBatch neon;
    private BitmapFont font;

    // ============== 网络层基础设施 ==============
    private NetworkManager serverManager;
    private NetworkManager clientManager;
    private Transport serverTransport;
    private Transport clientTransport;

    // 实例
    private TankBehaviour serverP1;
    private TankBehaviour serverP2;
    private TankBehaviour clientP1;
    private TankBehaviour clientP2;

    private List<Bullet> serverBullets = new ArrayList<>();
    private List<Bullet> clientBullets = new ArrayList<>();

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);

        serverManager = new NetworkManager();
        clientManager = new NetworkManager();

        // 进程内内存传输层（同步、确定性）
        LocalMemoryTransport memServer = new LocalMemoryTransport(true);
        LocalMemoryTransport memClient = new LocalMemoryTransport(false);
        serverTransport = memServer;
        clientTransport = memClient;
        serverManager.setTransport(serverTransport);
        clientManager.setTransport(clientTransport);
        memServer.connectToPeer(memClient);

        // 双端注册坦克预制体工厂
        serverManager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());
        clientManager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());

        // Server Spawn P1（自动广播到 Client）
        NetworkObject sObj1 = serverManager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID);
        serverP1 = (TankBehaviour) sObj1.getBehaviours().get(0);
        serverP1.x.setValue(200f); serverP1.y.setValue(300f);
        serverP1.color.setValue(Color.ORANGE);

        // Server Spawn P2（自动广播到 Client）
        NetworkObject sObj2 = serverManager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID);
        serverP2 = (TankBehaviour) sObj2.getBehaviours().get(0);
        serverP2.x.setValue(200f); serverP2.y.setValue(100f);
        serverP2.color.setValue(Color.CYAN);

        // Client 端已由 SpawnPacket 自动派生（内存传输是同步的，无需等待）
        int id1 = (int) sObj1.getNetworkId();
        int id2 = (int) sObj2.getNetworkId();
        clientP1 = (TankBehaviour) clientManager.getNetworkObject(id1).getBehaviours().get(0);
        clientP2 = (TankBehaviour) clientManager.getNetworkObject(id2).getBehaviours().get(0);
    }

    private void spawnBullet(TankBehaviour tank, int ownerId) {
        TankSandboxUtils.spawnBullet(tank, ownerId, serverBullets);
    }

    private void hitTank(TankBehaviour tank) {
        TankSandboxUtils.hitTank(tank);
    }

    // syncTank function removed. Let the true NetBuffer transport handle it!

    public void render(float delta) {
        super.render(delta);
        float moveSpeed = 200 * delta;

        // 【Server Logic】 模拟处理本地主机P1与远端发来的客户端P2输入
        // P1 Host Logic
        if (!serverP1.isDead.getValue()) {
            float dx1 = 0, dy1 = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.W)) dy1 += moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.S)) dy1 -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.A)) dx1 -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.D)) dx1 += moveSpeed;
            
            if (dx1 != 0 || dy1 != 0) {
                serverP1.x.setValue(serverP1.x.getValue() + dx1);
                serverP1.y.setValue(serverP1.y.getValue() + dy1);
                serverP1.rot.setValue(new Vector2(dx1, dy1).angleDeg());
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
                spawnBullet(serverP1, 1);
            }
        } else {
            float timer = serverP1.respawnTimer.getValue() - delta;
            serverP1.respawnTimer.setValue(Math.max(0, timer));
            if (timer <= 0) {
                serverP1.isDead.setValue(false);
                serverP1.hp.setValue(4);
                serverP1.x.setValue(200f); serverP1.y.setValue(300f);
                serverP1.color.setValue(Color.ORANGE);
            }
        }

        // P2 Client Logic (Processed on Server logically)
        if (!serverP2.isDead.getValue()) {
            float dx2 = 0, dy2 = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy2 += moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy2 -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx2 -= moveSpeed;
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx2 += moveSpeed;

            if (dx2 != 0 || dy2 != 0) {
                serverP2.x.setValue(serverP2.x.getValue() + dx2);
                serverP2.y.setValue(serverP2.y.getValue() + dy2);
                serverP2.rot.setValue(new Vector2(dx2, dy2).angleDeg());
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                spawnBullet(serverP2, 2);
            }
        } else {
            float timer = serverP2.respawnTimer.getValue() - delta;
            serverP2.respawnTimer.setValue(Math.max(0, timer));
            if (timer <= 0) {
                serverP2.isDead.setValue(false);
                serverP2.hp.setValue(4);
                serverP2.x.setValue(200f); serverP2.y.setValue(100f);
                serverP2.color.setValue(Color.CYAN);
            }
        }

        // Server Bullets Logic
        Iterator<Bullet> iter = serverBullets.iterator();
        while(iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;
            
            // Out of bounds check (just a simple box logic based on left half screen)
            float halfW = Gdx.graphics.getWidth() / 2f;
            if(b.x < 0 || b.x > halfW || b.y < 0 || b.y > Gdx.graphics.getHeight()) {
                iter.remove();
                continue;
            }
            
            // Hit check
            if(b.ownerId != 1 && !serverP1.isDead.getValue() && Math.hypot(b.x - serverP1.x.getValue(), b.y - serverP1.y.getValue()) < 20) {
                hitTank(serverP1);
                iter.remove();
                continue;
            }
            if(b.ownerId != 2 && !serverP2.isDead.getValue() && Math.hypot(b.x - serverP2.x.getValue(), b.y - serverP2.y.getValue()) < 20) {
                hitTank(serverP2);
                iter.remove();
                continue;
            }
        }

        // 核心网络循环：在每一帧必须被调用执行数据交换
        serverManager.tick();
        clientManager.tick();

        // 【Client Logic】由前面 tick() 执行的 NetBuffer 反序列化代替了手工打桩！！
        // 客户端子弹现在由 ClientRpc 触发生成，独立模拟运动
        clientBullets.clear();
        updateAndCollectClientBullets(clientP1, delta);
        updateAndCollectClientBullets(clientP2, delta);

        // ====== Rendering ======
        float halfW = Gdx.graphics.getWidth() / 2f;
        neon.begin(); // BEGIN BATCH

        // 分割线
        neon.drawLine(halfW, 0, halfW, Gdx.graphics.getHeight(), 2, Color.WHITE);

        // 左半屏：代表 Server 端的世界线 (offsetX=0)
        drawWorld(serverP1, serverP2, serverBullets, 0);
        // 右半屏：代表 Client 端的世界线 (offsetX=halfW)
        drawWorld(clientP1, clientP2, clientBullets, halfW);

        // HUD：显示传输层模式
        font.setColor(Color.YELLOW);
        font.draw(neon, "Transport: LocalMemory", 10, Gdx.graphics.getHeight() - 10);
        font.draw(neon, "SERVER", halfW * 0.4f, Gdx.graphics.getHeight() - 30);
        font.draw(neon, "CLIENT", halfW + halfW * 0.4f, Gdx.graphics.getHeight() - 30);

        neon.end(); // END BATCH
    }

    private void updateAndCollectClientBullets(TankBehaviour tank, float delta) {
        float halfW = Gdx.graphics.getWidth() / 2f;
        Iterator<Bullet> iter = tank.localBullets.iterator();
        while (iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;
            // 越界检测
            if (b.x < 0 || b.x > halfW || b.y < 0 || b.y > Gdx.graphics.getHeight()) {
                iter.remove();
                continue;
            }
            // 碰撞检测（与服务端逻辑一致）
            if (b.ownerId != 1 && !clientP1.isDead.getValue()
                && Math.hypot(b.x - clientP1.x.getValue(), b.y - clientP1.y.getValue()) < 20) {
                iter.remove();
                continue;
            }
            if (b.ownerId != 2 && !clientP2.isDead.getValue()
                && Math.hypot(b.x - clientP2.x.getValue(), b.y - clientP2.y.getValue()) < 20) {
                iter.remove();
                continue;
            }
            clientBullets.add(b);
        }
    }

    private void drawWorld(TankBehaviour p1, TankBehaviour p2, List<Bullet> bullets, float offsetX) {
        TankSandboxUtils.drawTank(neon, font, p1, offsetX);
        TankSandboxUtils.drawTank(neon, font, p2, offsetX);
        TankSandboxUtils.drawBullets(neon, bullets, offsetX);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (neon != null) neon.dispose();
        if (font != null) font.dispose();
        if (serverTransport != null) serverTransport.disconnect();
        if (clientTransport != null) clientTransport.disconnect();
    }
}