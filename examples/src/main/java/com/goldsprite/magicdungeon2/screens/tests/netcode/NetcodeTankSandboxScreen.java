package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.LocalMemoryTransport;
import com.goldsprite.gdengine.netcode.NetworkBehaviour;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.NetworkVariable;
import com.goldsprite.gdengine.screens.GScreen;

public class NetcodeTankSandboxScreen extends GScreen {

    private NeonBatch neon;
    private BitmapFont font;

    // ============== 网络层基础设施 ==============
    private NetworkManager serverManager;
    private NetworkManager clientManager;
    private LocalMemoryTransport serverTransport;
    private LocalMemoryTransport clientTransport;

    // 实例
    private TankBehaviour serverP1;
    private TankBehaviour serverP2;
    private TankBehaviour clientP1;
    private TankBehaviour clientP2;

    public static class Bullet {
        public float x, y, vx, vy;
        public int ownerId;
        public Color color;
    }

    private List<Bullet> serverBullets = new ArrayList<>();
    private List<Bullet> clientBullets = new ArrayList<>();

    // ================= 具体的游戏逻辑行为 =================
    public static class TankBehaviour extends NetworkBehaviour {
        public NetworkVariable<Float> x = new NetworkVariable<>(0f);
        public NetworkVariable<Float> y = new NetworkVariable<>(0f);
        public NetworkVariable<Float> rot = new NetworkVariable<>(90f);
        public NetworkVariable<Color> color = new NetworkVariable<>(Color.RED);
        public NetworkVariable<Integer> hp = new NetworkVariable<>(4);
        public NetworkVariable<Boolean> isDead = new NetworkVariable<>(false);
        public NetworkVariable<Float> respawnTimer = new NetworkVariable<>(0f);
    }

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();
        font = new BitmapFont();

        serverManager = new NetworkManager();
        clientManager = new NetworkManager();
        serverTransport = new LocalMemoryTransport(true);
        clientTransport = new LocalMemoryTransport(false);

        serverTransport.setManager(serverManager);
        clientTransport.setManager(clientManager);
        serverManager.setTransport(serverTransport);
        clientManager.setTransport(clientTransport);
        serverTransport.connectToPeer(clientTransport);

        // Server P1
        NetworkObject sObj1 = new NetworkObject(1);
        serverP1 = new TankBehaviour();
        serverP1.x.setValue(200f); serverP1.y.setValue(300f);
        serverP1.color.setValue(Color.ORANGE);
        sObj1.addComponent(serverP1);
        serverManager.spawn(sObj1);

        // Server P2
        NetworkObject sObj2 = new NetworkObject(2);
        serverP2 = new TankBehaviour();
        serverP2.x.setValue(200f); serverP2.y.setValue(100f);
        serverP2.color.setValue(Color.CYAN);
        sObj2.addComponent(serverP2);
        serverManager.spawn(sObj2);

        // Client P1 Ghost
        NetworkObject cObj1 = new NetworkObject(1);
        clientP1 = new TankBehaviour();
        cObj1.addComponent(clientP1);
        registerEntityInClientManager(clientManager, cObj1);

        // Client P2 Ghost
        NetworkObject cObj2 = new NetworkObject(2);
        clientP2 = new TankBehaviour();
        cObj2.addComponent(clientP2);
        registerEntityInClientManager(clientManager, cObj2);
    }

    private void registerEntityInClientManager(NetworkManager manager, NetworkObject obj) {
        try {
            java.lang.reflect.Field field = com.goldsprite.gdengine.netcode.NetworkManager.class.getDeclaredField("networkObjects");
            field.setAccessible(true);
            java.util.Map<Integer, NetworkObject> map = (java.util.Map<Integer, NetworkObject>) field.get(manager);
            map.put((int)obj.getNetworkId(), obj);

            java.lang.reflect.Field behaField = com.goldsprite.gdengine.netcode.NetworkObject.class.getDeclaredField("behaviours");
            behaField.setAccessible(true);
            java.util.List<com.goldsprite.gdengine.netcode.NetworkBehaviour> behas = (java.util.List<com.goldsprite.gdengine.netcode.NetworkBehaviour>) behaField.get(obj);

            for(com.goldsprite.gdengine.netcode.NetworkBehaviour b : behas) {
                b.internalAttach(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnBullet(TankBehaviour tank, int ownerId) {
        Bullet b = new Bullet();
        float rad = tank.rot.getValue() * MathUtils.degreesToRadians;
        b.x = tank.x.getValue() + MathUtils.cos(rad) * 20;
        b.y = tank.y.getValue() + MathUtils.sin(rad) * 20;
        b.vx = MathUtils.cos(rad) * 400;
        b.vy = MathUtils.sin(rad) * 400;
        b.ownerId = ownerId;
        b.color = tank.color.getValue(); // Let bullet color match the tank color for clarity
        serverBullets.add(b);
    }

    private void hitTank(TankBehaviour tank) {
        int hp = tank.hp.getValue() - 1;
        tank.hp.setValue(hp);
        if(hp <= 0) {
            tank.isDead.setValue(true);
            tank.respawnTimer.setValue(3f); // 3 seconds respawn
            tank.color.setValue(Color.GRAY);
        }
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
        
        clientBullets.clear();
        for (Bullet b : serverBullets) {
            Bullet cb = new Bullet();
            cb.x = b.x; cb.y = b.y; cb.color = b.color;
            clientBullets.add(cb);
        }

        // ====== Rendering ======
        float halfW = Gdx.graphics.getWidth() / 2f;
        neon.begin(); // BEGIN BATCH

        // 分割线
        neon.drawLine(halfW, 0, halfW, Gdx.graphics.getHeight(), 2, Color.WHITE);

        // 左半屏：代表 Server 端的世界线 (offsetX=0)
        drawWorld(serverP1, serverP2, serverBullets, 0);
        // 右半屏：代表 Client 端的世界线 (offsetX=halfW)
        drawWorld(clientP1, clientP2, clientBullets, halfW);

        neon.end(); // END BATCH
    }

    private void drawWorld(TankBehaviour p1, TankBehaviour p2, List<Bullet> bullets, float offsetX) {
        drawTank(p1, offsetX);
        drawTank(p2, offsetX);
        for(Bullet b : bullets) {
            // Draw bullet
            neon.drawRect(b.x + offsetX - 4, b.y - 4, 8, 8, 0, 0, b.color, true);
        }
    }

    private void drawTank(TankBehaviour t, float offsetX) {
        float cx = t.x.getValue() + offsetX;
        float cy = t.y.getValue();
        float rot = t.rot.getValue();

        if (t.isDead.getValue()) {
            // Overlay text countdown
            font.setColor(Color.WHITE);
            font.draw(neon, String.format("Respawn: %.1f s", t.respawnTimer.getValue()), cx - 40, cy);
            return;
        }

        // 车体
        neon.drawRect(cx - 15, cy - 15, 30, 30, rot, 0, t.color.getValue(), true);

        // 炮管
        float rad = rot * MathUtils.degreesToRadians;
        float dirX = MathUtils.cos(rad);
        float dirY = MathUtils.sin(rad);
        float barrelLen = 25f;
        float bcx = cx + dirX * (barrelLen / 2f + 5); 
        float bcy = cy + dirY * (barrelLen / 2f + 5);
        neon.drawRect(bcx - barrelLen/2f, bcy - 4f, barrelLen, 8f, rot, 0, Color.WHITE, true);

        // HP 槽
        int hp = t.hp.getValue();
        for(int i=0; i<4; i++) {
            Color hpColor = i < hp ? Color.GREEN : Color.RED;
            neon.drawRect(cx - 15 + i * 8, cy - 25, 6, 6, 0, 0, hpColor, true);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (neon != null) neon.dispose();
        if (font != null) font.dispose();
        serverTransport.disconnect();
    }
}