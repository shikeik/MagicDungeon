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
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.UdpSocketTransport;
import com.goldsprite.gdengine.screens.GScreen;

/**
 * 跨进程 UDP 坦克沙盒 —— 服务端屏幕。
 * <p>
 * 用法：先启动本屏幕（Server），再启动 {@link NetcodeUdpClientScreen}（Client）。
 * Server 负责：
 * <ul>
 *   <li>监听 UDP 端口，等待 Client 连接</li>
 *   <li>处理所有游戏逻辑（双人输入、子弹物理、碰撞检测）</li>
 *   <li>通过 NetworkManager.tick() 将状态同步给 Client</li>
 *   <li>在整个屏幕渲染当前世界（不分屏）</li>
 * </ul>
 * 键位：P1 = WASD + J 开火 | P2 = 方向键 + Enter 开火
 */
public class NetcodeUdpServerScreen extends GScreen {

    /** 服务端监听端口（双端须一致） */
    static final int PORT = 19100;

    // ── 状态机 ───────
    private enum State { WAITING_FOR_CLIENT, PLAYING }
    private State state = State.WAITING_FOR_CLIENT;

    // ── 渲染 ─────────
    private NeonBatch neon;
    private BitmapFont font;

    // ── 网络层 ────────
    private NetworkManager manager;
    private UdpSocketTransport transport;

    // ── 游戏对象 ──────
    private TankBehaviour p1;
    private TankBehaviour p2;
    private final List<Bullet> bullets = new ArrayList<>();

    // ══════════════ 生命周期 ══════════════

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);

        manager = new NetworkManager();
        transport = new UdpSocketTransport(true);
        manager.setTransport(transport);
        transport.startServer(PORT);

        // 注册坦克预制体工厂
        manager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        switch (state) {
            case WAITING_FOR_CLIENT:
                // 等待客户端握手
                if (transport.getClientCount() > 0) {
                    onClientConnected();
                }
                renderWaiting();
                break;

            case PLAYING:
                updateGameLogic(delta);
                updateBullets(delta);
                manager.tick();
                renderWorld();
                break;
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (neon != null) neon.dispose();
        if (font != null) font.dispose();
        if (transport != null) transport.disconnect();
    }

    // ══════════════ 客户端连接 ══════════════

    /** 客户端握手完成后触发一次 */
    private void onClientConnected() {
        // Spawn 两辆坦克（自动通过 SpawnPacket 广播给 Client）
        NetworkObject obj1 = manager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID);
        p1 = (TankBehaviour) obj1.getBehaviours().get(0);
        p1.x.setValue(200f);
        p1.y.setValue(300f);
        p1.color.setValue(Color.ORANGE);

        NetworkObject obj2 = manager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID);
        p2 = (TankBehaviour) obj2.getBehaviours().get(0);
        p2.x.setValue(200f);
        p2.y.setValue(100f);
        p2.color.setValue(Color.CYAN);

        state = State.PLAYING;
        System.out.println("[UdpServer] 客户端已连接，Spawn 完成，进入游戏状态");
    }

    // ══════════════ 游戏逻辑 ══════════════

    private void updateGameLogic(float delta) {
        float speed = 200 * delta;

        // ── P1（WASD + J）──
        updateTankInput(p1, 1, delta, speed,
            Input.Keys.W, Input.Keys.S, Input.Keys.A, Input.Keys.D, Input.Keys.J,
            200f, 300f, Color.ORANGE);

        // ── P2（方向键 + Enter）──
        updateTankInput(p2, 2, delta, speed,
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT, Input.Keys.ENTER,
            200f, 100f, Color.CYAN);
    }

    /** 通用坦克输入处理（移动 / 开火 / 死亡倒计时） */
    private void updateTankInput(TankBehaviour tank, int ownerId, float delta, float speed,
                                  int keyUp, int keyDown, int keyLeft, int keyRight, int keyFire,
                                  float spawnX, float spawnY, Color spawnColor) {
        if (!tank.isDead.getValue()) {
            float dx = 0, dy = 0;
            if (Gdx.input.isKeyPressed(keyUp)) dy += speed;
            if (Gdx.input.isKeyPressed(keyDown)) dy -= speed;
            if (Gdx.input.isKeyPressed(keyLeft)) dx -= speed;
            if (Gdx.input.isKeyPressed(keyRight)) dx += speed;

            if (dx != 0 || dy != 0) {
                tank.x.setValue(tank.x.getValue() + dx);
                tank.y.setValue(tank.y.getValue() + dy);
                tank.rot.setValue(new Vector2(dx, dy).angleDeg());
            }
            if (Gdx.input.isKeyJustPressed(keyFire)) {
                TankSandboxUtils.spawnBullet(tank, ownerId, bullets);
            }
        } else {
            // 死亡倒计时
            float timer = tank.respawnTimer.getValue() - delta;
            tank.respawnTimer.setValue(Math.max(0, timer));
            if (timer <= 0) {
                tank.isDead.setValue(false);
                tank.hp.setValue(4);
                tank.x.setValue(spawnX);
                tank.y.setValue(spawnY);
                tank.color.setValue(spawnColor);
            }
        }
    }

    /** 服务端子弹物理 + 碰撞检测 */
    private void updateBullets(float delta) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        Iterator<Bullet> iter = bullets.iterator();
        while (iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;

            // 越界移除
            if (b.x < 0 || b.x > screenW || b.y < 0 || b.y > screenH) {
                iter.remove();
                continue;
            }
            // 碰撞检测
            if (b.ownerId != 1 && !p1.isDead.getValue()
                && Math.hypot(b.x - p1.x.getValue(), b.y - p1.y.getValue()) < 20) {
                TankSandboxUtils.hitTank(p1);
                iter.remove();
                continue;
            }
            if (b.ownerId != 2 && !p2.isDead.getValue()
                && Math.hypot(b.x - p2.x.getValue(), b.y - p2.y.getValue()) < 20) {
                TankSandboxUtils.hitTank(p2);
                iter.remove();
                continue;
            }
        }
    }

    // ══════════════ 渲染 ══════════════

    private void renderWaiting() {
        neon.begin();
        font.setColor(Color.YELLOW);
        float cx = Gdx.graphics.getWidth() / 2f - 120;
        float cy = Gdx.graphics.getHeight() / 2f;
        font.draw(neon, "UDP 服务端 — 等待客户端连接...", cx, cy + 20);
        font.draw(neon, "监听端口: " + PORT, cx, cy - 10);
        font.draw(neon, "请启动 [Netcode UDP客户端] 屏幕", cx, cy - 40);
        neon.end();
    }

    private void renderWorld() {
        neon.begin();

        // 绘制坦克 & 子弹（全屏，offsetX = 0）
        TankSandboxUtils.drawTank(neon, font, p1, 0);
        TankSandboxUtils.drawTank(neon, font, p2, 0);
        TankSandboxUtils.drawBullets(neon, bullets, 0);

        // HUD
        font.setColor(Color.YELLOW);
        font.draw(neon, "UDP Server (Port: " + PORT + ")  客户端数: " + transport.getClientCount(),
            10, Gdx.graphics.getHeight() - 10);
        font.draw(neon, "P1: WASD + J   P2: 方向键 + Enter", 10, Gdx.graphics.getHeight() - 30);

        neon.end();
    }
}
