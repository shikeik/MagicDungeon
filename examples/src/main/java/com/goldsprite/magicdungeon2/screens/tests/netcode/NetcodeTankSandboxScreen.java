package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.LocalMemoryTransport;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.Transport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;

/**
 * Netcode 坦克纯内存沙盒 -- 同进程分屏（左=Server，右=Client）。
 * <p>
 * 核心游戏逻辑全部委托给 {@link TankGameLogic}，
 * 与 {@link NetcodeTankOnlineScreen} 共用同一套规则，
 * 确保内存场景可作为快速验证的等价替身。
 */
public class NetcodeTankSandboxScreen extends GScreen {

    private NeonBatch neon;
    private BitmapFont font;

    // ============== 网络层基础设施 ==============
    private NetworkManager serverManager;
    private NetworkManager clientManager;
    private Transport serverTransport;
    private Transport clientTransport;

    // ============== 游戏系统（与 Online 共享） ==============
    private TankGameLogic gameLogic;
    private BulletSystem bulletSystem;
    private TankGameMap gameMap;

    // ============== Server 端坦克映射（与 Online 结构一致） ==============
    private final Map<Integer, TankBehaviour> serverTanks = new HashMap<>();

    // ============== Client 端引用（用于渲染右半屏） ==============
    private TankBehaviour clientP1;
    private TankBehaviour clientP2;

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);

        // -- 游戏系统初始化（与 Online 结构一致） --
        float halfW = Gdx.graphics.getWidth() / 2f;
        float screenH = Gdx.graphics.getHeight();
        gameMap = new TankGameMap(halfW, screenH);
        bulletSystem = new BulletSystem();
        gameLogic = new TankGameLogic(bulletSystem, gameMap);

        // -- 网络层初始化 --
        serverManager = new NetworkManager();
        clientManager = new NetworkManager();

        LocalMemoryTransport memServer = new LocalMemoryTransport(true);
        LocalMemoryTransport memClient = new LocalMemoryTransport(false);
        serverTransport = memServer;
        clientTransport = memClient;
        serverManager.setTransport(serverTransport);
        clientManager.setTransport(clientTransport);
        memServer.connectToPeer(memClient);

        serverManager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());
        clientManager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());

        // -- 使用 TankSpawnSystem 统一出生逻辑（与 Online 一致） --
        TankBehaviour serverP1 = TankSpawnSystem.spawnTankForOwner(serverManager, 1, serverTanks);
        TankBehaviour serverP2 = TankSpawnSystem.spawnTankForOwner(serverManager, 2, serverTanks);

        // Client 端由 SpawnPacket 自动派生（内存传输是同步的，无需等待）
        long nid1 = serverP1.getNetworkObject().getNetworkId();
        long nid2 = serverP2.getNetworkObject().getNetworkId();
        clientP1 = (TankBehaviour) clientManager.getNetworkObject((int) nid1).getBehaviours().get(0);
        clientP2 = (TankBehaviour) clientManager.getNetworkObject((int) nid2).getBehaviours().get(0);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // -- 读取输入 -> 写入 pendingMoveX/Y & pendingFire --
        readP1Input();
        readP2Input();

        // -- Server 端统一驱动（移动/开火/死亡/子弹/碰撞） --
        gameLogic.serverTick(delta, serverTanks);

        // -- 网络同步：与真实联机场景一致，使用 tick(delta) 累加器节奏 --
        serverManager.tick(delta);

        // -- Client 端驱动：平滑调和 + 子弹 --
        TankGameLogic.clientReconcileTick(delta, clientManager);
        gameLogic.clientBulletTick(delta, clientManager);

        // ========== 渲染 ==========
        float halfW = Gdx.graphics.getWidth() / 2f;
        neon.begin();

        // 分割线
        neon.drawLine(halfW, 0, halfW, Gdx.graphics.getHeight(), 2, Color.WHITE);

        // 左半屏：Server 视角
        TankBehaviour sp1 = serverTanks.get(1);
        TankBehaviour sp2 = serverTanks.get(2);
        drawWorld(sp1, sp2, bulletSystem.getServerBullets(), 0);

        // 右半屏：Client 视角
        drawWorld(clientP1, clientP2, bulletSystem.getClientBullets(), halfW);

        // HUD
        font.setColor(Color.YELLOW);
        font.draw(neon, "Transport: LocalMemory", 10, Gdx.graphics.getHeight() - 10);
        font.draw(neon, "SERVER", halfW * 0.4f, Gdx.graphics.getHeight() - 30);
        font.draw(neon, "CLIENT", halfW + halfW * 0.4f, Gdx.graphics.getHeight() - 30);

        neon.end();
    }

    // -- 输入读取：写入 pendingMoveX/Y + pendingFire，由 serverTick 统一消费 --

    /** P1（Host）: 通过 InputManager 统一读取（键盘WASD + 手柄 + 虚拟摇杆），J/ATTACK 开火 */
    private void readP1Input() {
        TankBehaviour tank = serverTanks.get(1);
        if (tank == null || tank.isDead.getValue()) return;

        InputManager input = InputManager.getInstance();
        Vector2 axis = input.getAxis(InputManager.AXIS_LEFT);
        Vector2 dir = TankGameLogic.normalizeDir(axis.x, axis.y);
        tank.pendingMoveX = dir.x;
        tank.pendingMoveY = dir.y;

        if (input.isJustPressed(InputAction.ATTACK)) {
            tank.pendingFire = true;
        }
    }

    /** P2（模拟 Client）: 方向键移动，Enter 开火 */
    private void readP2Input() {
        TankBehaviour tank = serverTanks.get(2);
        if (tank == null || tank.isDead.getValue()) return;

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1f;
        Vector2 dir = TankGameLogic.normalizeDir(dx, dy);
        tank.pendingMoveX = dir.x;
        tank.pendingMoveY = dir.y;

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            tank.pendingFire = true;
        }
    }

    // -- 渲染辅助 --

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
