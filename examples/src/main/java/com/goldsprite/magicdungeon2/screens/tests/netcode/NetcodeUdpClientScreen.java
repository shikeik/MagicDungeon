package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.UdpSocketTransport;
import com.goldsprite.gdengine.screens.GScreen;

/**
 * 跨进程 UDP 坦克沙盒 —— 客户端屏幕。
 * <p>
 * 用法：先启动 {@link NetcodeUdpServerScreen}（Server），再启动本屏幕。
 * Client 负责：
 * <ul>
 *   <li>连接到 Server 的 UDP 端口</li>
 *   <li>通过 NetworkManager.tick() 接收状态同步</li>
 *   <li>通过 ClientRpc 接收子弹生成事件并本地模拟运动</li>
 *   <li>在整个屏幕渲染同步后的世界（不分屏）</li>
 * </ul>
 * 注意：当前 Client 为只读观察端，所有输入由 Server 处理。
 */
public class NetcodeUdpClientScreen extends GScreen {

    /** 服务端地址（同机回环测试） */
    static final String SERVER_IP = "127.0.0.1";
    static final int SERVER_PORT = NetcodeUdpServerScreen.PORT;

    // ── 状态机 ───────
    private enum State { CONNECTING, PLAYING }
    private State state = State.CONNECTING;

    // ── 渲染 ─────────
    private NeonBatch neon;
    private BitmapFont font;

    // ── 网络层 ────────
    private NetworkManager manager;
    private UdpSocketTransport transport;

    // ── 游戏对象（从 Server 同步而来） ──
    private TankBehaviour p1;
    private TankBehaviour p2;
    private final List<Bullet> clientBullets = new ArrayList<>();

    // ══════════════ 生命周期 ══════════════

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);

        manager = new NetworkManager();
        transport = new UdpSocketTransport(false);
        manager.setTransport(transport);

        // 必须在 connect 之前注册，否则 UDP 收包线程收到 SpawnPacket 时工厂尚未就绪
        manager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());

        transport.connect(SERVER_IP, SERVER_PORT);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // 无论何种状态，每帧都执行 tick 以处理传入的网络数据
        manager.tick();

        switch (state) {
            case CONNECTING:
                // 等待 Server 的 SpawnPacket 抵达（异步 UDP，需轮询）
                if (manager.getNetworkObjectCount() >= 2) {
                    onSpawnReceived();
                }
                renderConnecting();
                break;

            case PLAYING:
                updateClientBullets(delta);
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

    // ══════════════ Spawn 接收 ══════════════

    /** 两个坦克的 SpawnPacket 均已收到，提取 TankBehaviour 引用 */
    private void onSpawnReceived() {
        // 遍历所有已收到的网络对象，按 networkId 升序取前两个作为 P1、P2
        List<NetworkObject> ordered = new ArrayList<>(manager.getAllNetworkObjects());
        ordered.sort((a, b) -> Integer.compare((int) a.getNetworkId(), (int) b.getNetworkId()));

        p1 = (TankBehaviour) ordered.get(0).getBehaviours().get(0);
        p2 = (TankBehaviour) ordered.get(1).getBehaviours().get(0);

        state = State.PLAYING;
        System.out.println("[UdpClient] 收到 Spawn，进入游戏状态");
    }

    // ══════════════ 客户端子弹模拟 ══════════════

    /** 收集由 ClientRpc 触发的本地子弹，模拟运动 + 越界/碰撞移除 */
    private void updateClientBullets(float delta) {
        clientBullets.clear();

        collectAndSimulate(p1, delta);
        collectAndSimulate(p2, delta);
    }

    private void collectAndSimulate(TankBehaviour tank, float delta) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        Iterator<Bullet> iter = tank.localBullets.iterator();
        while (iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;

            // 越界移除
            if (b.x < 0 || b.x > screenW || b.y < 0 || b.y > screenH) {
                iter.remove();
                continue;
            }
            // 碰撞移除（与服务端一致的半径判定）
            if (b.ownerId != 1 && !p1.isDead.getValue()
                && Math.hypot(b.x - p1.x.getValue(), b.y - p1.y.getValue()) < 20) {
                iter.remove();
                continue;
            }
            if (b.ownerId != 2 && !p2.isDead.getValue()
                && Math.hypot(b.x - p2.x.getValue(), b.y - p2.y.getValue()) < 20) {
                iter.remove();
                continue;
            }
            clientBullets.add(b);
        }
    }

    // ══════════════ 渲染 ══════════════

    private void renderConnecting() {
        neon.begin();
        font.setColor(Color.YELLOW);
        float cx = Gdx.graphics.getWidth() / 2f - 120;
        float cy = Gdx.graphics.getHeight() / 2f;
        font.draw(neon, "UDP 客户端 — 等待服务端 Spawn...", cx, cy + 20);
        font.draw(neon, "连接: " + SERVER_IP + ":" + SERVER_PORT, cx, cy - 10);
        font.draw(neon, "已收到实体: " + manager.getNetworkObjectCount() + " / 2", cx, cy - 40);
        neon.end();
    }

    private void renderWorld() {
        neon.begin();

        // 绘制坦克 & 子弹（全屏，offsetX = 0）
        TankSandboxUtils.drawTank(neon, font, p1, 0);
        TankSandboxUtils.drawTank(neon, font, p2, 0);
        TankSandboxUtils.drawBullets(neon, clientBullets, 0);

        // HUD
        font.setColor(Color.YELLOW);
        font.draw(neon, "UDP Client -> " + SERVER_IP + ":" + SERVER_PORT, 10, Gdx.graphics.getHeight() - 10);
        font.draw(neon, "只读观察模式（输入由 Server 处理）", 10, Gdx.graphics.getHeight() - 30);

        neon.end();
    }
}
