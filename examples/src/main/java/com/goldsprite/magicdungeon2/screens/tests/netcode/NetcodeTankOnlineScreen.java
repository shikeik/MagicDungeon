package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.ReliableUdpTransport;
import com.goldsprite.gdengine.netcode.UdpSocketTransport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;

/**
 * Netcode 坦克联机对战 —— 统一屏幕（Server / Client 合一）。
 * <p>
 * 替代原 NetcodeUdpServerScreen + NetcodeUdpClientScreen。
 * 启动时显示配置面板，选择「创建房间」或「加入房间」，输入 IP / 端口后进入游戏。
 * <ul>
 *   <li>Server 端：处理所有游戏逻辑 + tick 同步</li>
 *   <li>Client 端：上报输入 (ServerRpc) + 渲染同步状态</li>
 *   <li>支持多客户端动态 Spawn，坦克数量无上限</li>
 * </ul>
 */
public class NetcodeTankOnlineScreen extends GScreen {

    // ── 外部预配置 (由大厅屏幕设置，跳过 CONFIG 阶段) ──
    private static String preConfigIp = null;
    private static int preConfigPort = -1;
    private static Boolean preConfigIsServer = null;
    private static String preConfigRoomName = null;

    /**
     * 从大厅屏幕调用：预配置为房主模式，进入后直接启动 Server。
     * @param port UDP 监听端口
     */
    public static void preConfigureAsHost(int port) {
        preConfigIsServer = true;
        preConfigPort = port;
        preConfigIp = null;
    }

    /**
     * 从大厅屏幕调用：预配置为客户端模式，进入后直接连接。
     * @param ip   房主公网 IP
     * @param port 房主 UDP 端口
     */
    public static void preConfigureAsClient(String ip, int port) {
        preConfigIsServer = false;
        preConfigIp = ip;
        preConfigPort = port;
    }

    /** 从大厅屏幕调用: 携带房间元信息 */
    public static void preConfigureRoomInfo(String roomName) {
        preConfigRoomName = roomName;
    }

    /** 清除预配置 */
    private static void clearPreConfig() {
        preConfigIp = null;
        preConfigPort = -1;
        preConfigIsServer = null;
        preConfigRoomName = null;
    }

    // ── 配置面板参数 ─────
    /** 当前选择的角色：true=Server, false=Client */
    private boolean isServerRole = true;
    /** 配置中的 IP 地址（Client 用） */
    private String configIp = "127.0.0.1";
    /** 配置中的端口 */
    private int configPort = 19100;
    /** 从大厅传入的房间名称 */
    private String roomName = null;
    /** IP 输入缓冲 */
    private StringBuilder ipInput = new StringBuilder("127.0.0.1");
    /** 端口输入缓冲 */
    private StringBuilder portInput = new StringBuilder("19100");
    /** 当前编辑焦点: 0=无, 1=IP, 2=端口 */
    private int editFocus = 0;

    // ── 状态机 ───────
    private enum State { CONFIG, WAITING, PLAYING }
    private State state = State.CONFIG;
    /** 配置阶段的错误提示（端口占用等） */
    private String configError = null;

    // ── 渲染器（解耦提取，管理 NeonBatch + 字体 + 所有绘制方法）──
    private TankGameRenderer renderer;

    // ── 网络层 ────────
    private NetworkManager manager;
    private ReliableUdpTransport transport;
    private UdpSocketTransport rawTransport;

    // ── 游戏对象 (Server 端维护) ──────
    /** Server 端: clientId → TankBehaviour 映射 */
    private final Map<Integer, TankBehaviour> clientTanks = new HashMap<>();

    // ── 子弹系统（解耦提取，可独立测试）──
    private final BulletSystem bulletSystem = new BulletSystem();

    // ── 地图 ──
    /** 地图数据与碰撞逻辑（解耦提取，可独立测试） */
    private final TankGameMap gameMap = new TankGameMap(2000f, 1500f);

    // ── 核心逻辑（与 Sandbox 共享，避免重复维护） ──
    private final TankGameLogic gameLogic = new TankGameLogic(bulletSystem, gameMap);

    // ── 虚拟触控控件（Android 摇杆 + 攻击按钮） ──
    private TankVirtualControls virtualControls;

    // ── 抽屉面板（房间成员详情） ──
    /** 抽屉是否展开 */
    private boolean drawerExpanded = false;
    /** 抽屉动画进度 0(收起)~1(展开) */
    private float drawerAnimProgress = 0f;
    /** 抽屉动画时长(秒) */
    private static final float DRAWER_ANIM_DURATION = 0.25f;

    // ── 心跳 / 断线检测 ──
    /** 连接是否已断开（心跳超时 / Server 主动断线） */
    private boolean connectionLost = false;
    /** 连接断开后的计时器(秒) */
    private float connectionLostTimer = 0f;
    /** 心跳超时阈值(秒) — 超过此时间未收到数据视为断线 */
    private static final float HEARTBEAT_TIMEOUT_SEC = 5f;

	@Override protected void initViewport() {
		uiViewportScale = 0.7f;
		super.initViewport();
	}


	// ══════════════ 生命周期 ══════════════

    @Override
    public void create() {
        renderer = new TankGameRenderer();

        // 创建虚拟触控控件（Android 默认显示，PC 默认隐藏）
        virtualControls = new TankVirtualControls(new ExtendViewport(
            uiViewport.getWorldWidth(), uiViewport.getWorldHeight()));
        // 立即同步视口尺寸，确保触摸坐标映射正确
        virtualControls.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        if (imp != null) imp.addProcessor(virtualControls.getStage());

        // 检查是否有来自大厅的预配置，如果有则跳过 CONFIG 阶段直接启动网络
        if (preConfigIsServer != null) {
            isServerRole = preConfigIsServer;
            configPort = preConfigPort > 0 ? preConfigPort : 19100;
            if (preConfigIp != null) {
                configIp = preConfigIp;
                ipInput = new StringBuilder(preConfigIp);
            }
            portInput = new StringBuilder(String.valueOf(configPort));
            roomName = preConfigRoomName;
            clearPreConfig();
            startNetwork();
        }

        // 初始化游戏世界相机到地图中心
        worldCamera.position.set(
            gameMap.getMapWidth() / 2f,
            gameMap.getMapHeight() / 2f, 0);
        worldCamera.update();
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // 更新虚拟触控控件
        if (virtualControls != null) virtualControls.update(delta);

        // 应用 UI 视口，确保 resize 后坐标系正确
        uiViewport.apply();

        switch (state) {
            case CONFIG:
                updateConfig();
                renderer.renderConfig(uiViewport, isServerRole, ipInput, portInput, editFocus, configError);
                break;

            case WAITING:
                updateWaiting(delta);
                renderer.renderWaiting(uiViewport, isServerRole, configIp, configPort,
                    clientTanks.size(), manager != null ? manager.getNetworkObjectCount() : 0);
                break;

            case PLAYING:
                updatePlaying(delta);
                renderer.renderPlaying(worldCamera, uiViewport, isServerRole, configIp, configPort, roomName,
                    clientTanks, manager, transport, bulletSystem, gameMap,
                    drawerAnimProgress, connectionLost, connectionLostTimer);
                break;
        }

        // 渲染虚拟触控控件（在所有 HUD 之上）
        if (virtualControls != null) virtualControls.render();
    }

    /** ESC/返回时会调用 hide()（不调用 dispose），在此清理网络资源 */
    @Override
    public void hide() {
        super.hide();
        shutdownNetwork();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (virtualControls != null) virtualControls.resize(width, height);
    }

    @Override
    public void dispose() {
        super.dispose();
        shutdownNetwork();
        if (renderer != null) { renderer.dispose(); renderer = null; }
        if (virtualControls != null) { virtualControls.dispose(); virtualControls = null; }
    }

    /** 安全关闭网络连接并重置游戏状态 */
    private void shutdownNetwork() {
        if (transport != null) {
            transport.disconnect();
            transport = null;
        }
        rawTransport = null;
        manager = null;
        clientTanks.clear();
        bulletSystem.clear();
        gameMap.clear();
        connectionLost = false;
        connectionLostTimer = 0f;
        state = State.CONFIG;
    }

    // ══════════════ CONFIG 阶段 ══════════════

    private void updateConfig() {
        // Tab 切换角色
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            isServerRole = !isServerRole;
        }

        // 点击切换编辑焦点
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) editFocus = 1;
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) editFocus = 2;

        // 处理文本输入
        if (editFocus > 0) {
            StringBuilder target = editFocus == 1 ? ipInput : portInput;
            // 退格
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && target.length() > 0) {
                target.deleteCharAt(target.length() - 1);
            }
            // 数字和点
            for (int key = Input.Keys.NUM_0; key <= Input.Keys.NUM_9; key++) {
                if (Gdx.input.isKeyJustPressed(key)) {
                    target.append((char) ('0' + (key - Input.Keys.NUM_0)));
                }
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD) && editFocus == 1) {
                ipInput.append('.');
            }
        }

        // Enter 确认开始
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            configIp = ipInput.toString();
            try {
                configPort = Integer.parseInt(portInput.toString());
            } catch (NumberFormatException e) {
                configPort = 19100;
            }
            startNetwork();
        }
    }

    private void startNetwork() {
        manager = new NetworkManager();
        // 设置可配置的网络同步频率（默认 60Hz，可按需调整为 20/30/60/128）
        manager.setTickRate(60);
        rawTransport = new UdpSocketTransport(isServerRole);
        transport = new ReliableUdpTransport(rawTransport);
        manager.setTransport(transport);
        manager.registerPrefab(TankSandboxUtils.TANK_PREFAB_ID, TankSandboxUtils.createTankFactory());

        try {
            if (isServerRole) {
                // Server 模式：设置连接/断开监听
                manager.setConnectionListener(new com.goldsprite.gdengine.netcode.NetworkConnectionListener() {
                    @Override
                    public void onClientConnected(int clientId) {
                        Gdx.app.postRunnable(() -> onNewClientConnected(clientId));
                    }

                    @Override
                    public void onClientDisconnected(int clientId) {
                        // tick() 中已在主线程执行，直接清理游戏层映射
                        onClientDisconnectedHandler(clientId);
                    }
                });
                transport.startServer(configPort);

                // 生成地图墙体（Server 权威种子，后续通过 rpcSyncMapSeed 下发给客户端）
                int mapSeed = roomName != null ? roomName.hashCode() : (int) System.currentTimeMillis();
                gameMap.generateWalls(mapSeed);

                // Server 自身也产一辆坦克（Host 模式: ownerClientId = -1）
                spawnTankForOwner(-1);

                state = State.WAITING;
                configError = null;
                DLog.logT("Netcode", "[Online] Server 启动，监听端口: " + configPort);
            } else {
                // Client 模式
                transport.connect(configIp, configPort);
                // 地图种子由 Server 通过 rpcSyncMapSeed 权威下发，客户端不再自行猜测
                state = State.WAITING;
                configError = null;
                DLog.logT("Netcode", "[Online] Client 连接中: " + configIp + ":" + configPort);
            }
        } catch (Exception e) {
            // 端口占用或其他网络错误，优雅回退到配置界面
            configError = e.getMessage();
            DLog.logErr("[Online] 网络启动失败: " + e.getMessage());
            // 清理已初始化的资源
            if (transport != null) {
                try { transport.disconnect(); } catch (Exception ignored) {}
                transport = null;
            }
            manager = null;
        }
    }

    // ══════════════ WAITING 阶段 ══════════════

    private void updateWaiting(float delta) {
        if (isServerRole) {
            // Server: 始终可以直接进入 PLAYING（自己已有一辆坦克）
            if (clientTanks.size() >= 1) {
                state = State.PLAYING;
            }
        } else {
            // Client: 等待至少收到一个实体的 SpawnPacket
            if (manager.getNetworkObjectCount() >= 1) {
                state = State.PLAYING;
                DLog.logT("Netcode", "[Online] Client 收到 Spawn，进入游戏");
            }
        }
    }

    // ══════════════ PLAYING 阶段 ══════════════

    private void updatePlaying(float delta) {
        // 抽屉开关: 按 I 键切换
        if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
            drawerExpanded = !drawerExpanded;
        }
        // 抽屉动画插值
        float target = drawerExpanded ? 1f : 0f;
        if (drawerAnimProgress != target) {
            float speed = 1f / DRAWER_ANIM_DURATION * delta;
            if (drawerExpanded) {
                drawerAnimProgress = Math.min(1f, drawerAnimProgress + speed);
            } else {
                drawerAnimProgress = Math.max(0f, drawerAnimProgress - speed);
            }
        }

        if (isServerRole) {
            updateServerLogic(delta);
        } else {
            // 检查并应用 Server 下发的地图种子
            checkMapSeedSync();
            updateClientLogic(delta);
        }

        // 游戏相机平滑跟随本地玩家坦克
        updateCameraFollow(delta);
    }

    // ── Server 逻辑 ──

    private void updateServerLogic(float delta) {
        // ── 服务器端心跳超时检测 ──
        if (transport != null) {
            transport.checkHeartbeatTimeouts((long)(HEARTBEAT_TIMEOUT_SEC * 1000));
        }

        // Host 坦克（ownerClientId = -1）：读键盘 → 写 pendingMove/pendingFire
        TankBehaviour hostTank = clientTanks.get(-1);
        if (hostTank != null) {
            readHostInput(hostTank);
        }

        // 统一驱动：移动/开火/死亡/子弹/碰撞（与 Sandbox 共用同一套规则）
        gameLogic.serverTick(delta, clientTanks);

        // 同步（使用累加器模式，根据 tick rate 自动控制发送频率）
        manager.tick(delta);
        // 可靠层超时重传检查
        transport.tickReliable();
    }

    /**
     * Host 坦克输入: 通过 InputManager 统一读取（键盘WASD + 手柄 + 虚拟摇杆）。
     * 仅写入 pending*，由 serverTick 统一消费。
     */
    private void readHostInput(TankBehaviour tank) {
        if (tank.isDead.getValue()) return;

        InputManager input = InputManager.getInstance();
        Vector2 axis = input.getAxis(InputManager.AXIS_LEFT);
        Vector2 dir = TankGameLogic.normalizeDir(axis.x, axis.y);
        tank.pendingMoveX = dir.x;
        tank.pendingMoveY = dir.y;

        if (input.isJustPressed(InputAction.ATTACK)) {
            tank.pendingFire = true;
        }
    }

    // ── Client 逻辑 ──

    private void updateClientLogic(float delta) {
        // ── 驱动所有网络变量的调和 / 客户端预测漂移修正 ──
        TankGameLogic.clientReconcileTick(delta, manager);

        // 检测本地输入并通过 ServerRpc 上报
        TankBehaviour myTank = findLocalPlayerTank();
        if (myTank != null && !myTank.isDead.getValue()) {
            // 本地玩家 x/y 启用客户端预测模式（仅首次调用生效，后续幂等）
            myTank.x.enableClientPrediction();
            myTank.y.enableClientPrediction();

            InputManager input = InputManager.getInstance();
            Vector2 axis = input.getAxis(InputManager.AXIS_LEFT);
            Vector2 dir = TankGameLogic.normalizeDir(axis.x, axis.y);

            // 始终发送方向（包括 0,0 停止）—— 服务端不清零 pendingMove，
            // 必须显式发送停止指令，否则坦克会永远滑行
            myTank.sendServerRpc("rpcMoveInput", dir.x, dir.y);

            if (dir.x != 0 || dir.y != 0) {
                // ── 客户端预测: 本地立即移动 + 本地碰撞，消除 RTT 延迟感 ──
                float speed = TankGameLogic.MOVE_SPEED * delta;
                float newX = myTank.x.getValue() + dir.x * speed;
                float newY = myTank.y.getValue() + dir.y * speed;

                // 本地也做边界 + 墙体碰撞（与服务端 serverTickCollision 同逻辑）
                Vector2 clamped = gameMap.clampToBoundary(newX, newY, TankGameLogic.TANK_HALF_SIZE);
                Vector2 pushed = gameMap.pushOutOfWalls(clamped.x, clamped.y, TankGameLogic.TANK_HALF_SIZE);

                myTank.x.setLocal(pushed.x);
                myTank.y.setLocal(pushed.y);
                myTank.rot.setLocal(dir.angleDeg());
            }
            if (input.isJustPressed(InputAction.ATTACK)) {
                myTank.sendServerRpc("rpcFireInput");
            }
        }

        // 模拟 Client 端子弹运动（与 Sandbox 共用 TankGameLogic）
        gameLogic.clientBulletTick(delta, manager);

        // 可靠层超时重传检查
        transport.tickReliable();
    }

    /** 客户端: 检查是否收到 Server 的地图种子 RPC，收到则重新生成地图 */
    private void checkMapSeedSync() {
        if (manager == null) return;
        for (NetworkObject obj : manager.getAllNetworkObjects()) {
            for (com.goldsprite.gdengine.netcode.NetworkBehaviour b : obj.getBehaviours()) {
                if (b instanceof TankBehaviour) {
                    TankBehaviour tb = (TankBehaviour) b;
                    if (tb.mapSeedReceived) {
                        gameMap.generateWalls(tb.receivedMapSeed);
                        tb.mapSeedReceived = false;
                        DLog.logT("Netcode", "[Online] Client 收到地图种子: " + tb.receivedMapSeed + "，已重新生成地图");
                        return;
                    }
                }
            }
        }
    }

    /** 从 manager 中找到 isLocalPlayer=true 的坦克 */
    private TankBehaviour findLocalPlayerTank() {
        for (NetworkObject obj : manager.getAllNetworkObjects()) {
            if (obj.isLocalPlayer && !obj.getBehaviours().isEmpty()) {
                return (TankBehaviour) obj.getBehaviours().get(0);
            }
        }
        return null;
    }

    // ══════════════ 连接事件 ══════════════

    /** Server 端: 新 Client 连入时，先补发已有实体+状态，再 Spawn 新坦克 */
    private void onNewClientConnected(int clientId) {
        // 1. 先向新客户端补发所有已存在的 NetworkObject 的 SpawnPacket
        manager.sendExistingSpawnsToClient(clientId);
        // 2. 补发已有实体的全量状态快照（位置、颜色、HP 等）
        manager.sendFullStateToClient(clientId);
        // 3. 为新客户端 Spawn 自己的坦克（会广播 SpawnPacket 给所有客户端）
        spawnTankForOwner(clientId);
        // 4. 再次发送全量状态（确保新坦克的初始数据也同步给该客户端）
        manager.sendFullStateToClient(clientId);
        // 5. 向所有客户端广播地图种子（确保新客户端获得正确的墙体布局）
        TankBehaviour hostTank = clientTanks.get(-1);
        if (hostTank != null) {
            hostTank.sendClientRpc("rpcSyncMapSeed", (int) gameMap.getMapSeed());
        }
        DLog.logT("Netcode", "[Online] Client #" + clientId + " 已连接，Spawn 坦克，已广播地图种子");
    }

    /** Server 端: Client 断开时，清理游戏层映射（已在主线程 tick() 中调用） */
    private void onClientDisconnectedHandler(int clientId) {
        clientTanks.remove(clientId);
        DLog.logT("Netcode", "[Online] Client #" + clientId + " 已断开，移除坦克。剩余坦克: " + clientTanks.size());
    }

    /** 为指定 ownerClientId Spawn 一辆坦克（委托给 TankSpawnSystem） */
    private void spawnTankForOwner(int ownerClientId) {
        TankSpawnSystem.spawnTankForOwner(manager, ownerClientId, clientTanks);
    }

    // ══════════════ 相机跟随 ══════════════

    /** 游戏相机平滑跟随本地玩家坦克 */
    private void updateCameraFollow(float delta) {
        TankBehaviour followTank = isServerRole ? clientTanks.get(-1) : findLocalPlayerTank();
        if (followTank != null) {
            float tx = followTank.x.getValue();
            float ty = followTank.y.getValue();
            float lerp = Math.min(1f, 5f * delta);
            worldCamera.position.x += (tx - worldCamera.position.x) * lerp;
            worldCamera.position.y += (ty - worldCamera.position.y) * lerp;
        }
        worldCamera.update();
    }

}
