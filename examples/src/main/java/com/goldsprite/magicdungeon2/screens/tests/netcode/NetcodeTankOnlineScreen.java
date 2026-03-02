package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.UdpSocketTransport;
import com.goldsprite.gdengine.screens.GScreen;

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

    // ── 配置面板参数 ─────
    /** 当前选择的角色：true=Server, false=Client */
    private boolean isServerRole = true;
    /** 配置中的 IP 地址（Client 用） */
    private String configIp = "127.0.0.1";
    /** 配置中的端口 */
    private int configPort = 19100;
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

    // ── 渲染 ─────────
    private NeonBatch neon;
    private BitmapFont font;
    private BitmapFont titleFont;

    // ── 网络层 ────────
    private NetworkManager manager;
    private UdpSocketTransport transport;

    // ── 游戏对象 (Server 端维护) ──────
    /** Server 端: clientId → TankBehaviour 映射 */
    private final Map<Integer, TankBehaviour> clientTanks = new HashMap<>();
    /** Server 端: 所有活跃子弹（Server 权威） */
    private final List<Bullet> serverBullets = new ArrayList<>();
    /** Server 端: 子弹自增ID */
    private int nextBulletId = 1;

    // ── 游戏对象 (Client 端维护) ──────
    /** Client 端: 从 ClientRpc 收集的子弹 */
    private final List<Bullet> clientBullets = new ArrayList<>();

    // ── 可配的出生颜色列表 ──
    private static final Color[] SPAWN_COLORS = {
        Color.ORANGE, Color.CYAN, Color.LIME, Color.MAGENTA, Color.GOLD, Color.SKY
    };

    // ── 抽屉面板（房间成员详情） ──
    /** 抽屉是否展开 */
    private boolean drawerExpanded = false;
    /** 抽屉动画进度 0(收起)~1(展开) */
    private float drawerAnimProgress = 0f;
    /** 抽屉宽度 */
    private static final float DRAWER_WIDTH = 220f;
    /** 抽屉动画时长(秒) */
    private static final float DRAWER_ANIM_DURATION = 0.25f;

	@Override protected void initViewport() {
		uiViewportScale = 0.7f;
		super.initViewport();
	}


	// ══════════════ 生命周期 ══════════════

    @Override
    public void create() {
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);
        titleFont = FontUtils.generate(18, 2);
    }

    @Override
    public void render(float delta) {
        super.render(delta);

        // 应用 UI 视口，确保 resize 后坐标系正确
        uiViewport.apply();

        switch (state) {
            case CONFIG:
                updateConfig();
                renderConfig();
                break;

            case WAITING:
                updateWaiting(delta);
                renderWaiting();
                break;

            case PLAYING:
                updatePlaying(delta);
                renderPlaying();
                break;
        }
    }

    /** ESC/返回时会调用 hide()（不调用 dispose），在此清理网络资源 */
    @Override
    public void hide() {
        super.hide();
        shutdownNetwork();
    }

    @Override
    public void dispose() {
        super.dispose();
        shutdownNetwork();
        if (neon != null) { neon.dispose(); neon = null; }
        if (font != null) { font.dispose(); font = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
    }

    /** 安全关闭网络连接并重置游戏状态 */
    private void shutdownNetwork() {
        if (transport != null) {
            transport.disconnect();
            transport = null;
        }
        manager = null;
        clientTanks.clear();
        serverBullets.clear();
        clientBullets.clear();
        nextBulletId = 1;
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
        transport = new UdpSocketTransport(isServerRole);
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

                // Server 自身也产一辆坦克（Host 模式: ownerClientId = -1）
                spawnTankForOwner(-1);

                state = State.WAITING;
                configError = null;
                DLog.logT("Netcode", "[Online] Server 启动，监听端口: " + configPort);
            } else {
                // Client 模式
                transport.connect(configIp, configPort);
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
            updateClientLogic(delta);
        }
    }

    // ── Server 逻辑 ──

    private void updateServerLogic(float delta) {
        float speed = 200 * delta;

        // Host 的坦克（ownerClientId = -1）由本地键盘控制: WASD + J
        TankBehaviour hostTank = clientTanks.get(-1);
        if (hostTank != null) {
            updateHostTankInput(hostTank, delta, speed);
        }

        // 处理所有远程坦克的 pendingMove + pendingFire
        for (Map.Entry<Integer, TankBehaviour> entry : clientTanks.entrySet()) {
            TankBehaviour tank = entry.getValue();
            int ownerId = entry.getKey();

            // Server 权威驱动远程坦克移动（消费 pendingMove）
            if (ownerId >= 0 && !tank.isDead.getValue()) {
                float mx = tank.pendingMoveX;
                float my = tank.pendingMoveY;
                if (mx != 0 || my != 0) {
                    tank.x.setValue(tank.x.getValue() + mx * speed);
                    tank.y.setValue(tank.y.getValue() + my * speed);
                    tank.rot.setValue(new Vector2(mx, my).angleDeg());
                }
                // 消费后清零，确保松键后停止移动
                tank.pendingMoveX = 0;
                tank.pendingMoveY = 0;
            }

            if (tank.pendingFire && !tank.isDead.getValue()) {
                TankSandboxUtils.spawnBullet(tank, ownerId, serverBullets, nextBulletId++);
                tank.pendingFire = false;
            }
            // 死亡倒计时
            if (tank.isDead.getValue()) {
                float timer = tank.respawnTimer.getValue() - delta;
                tank.respawnTimer.setValue(Math.max(0, timer));
                if (timer <= 0) {
                    respawnTank(tank, ownerId);
                }
            }
        }

        // 子弹物理 + 碰撞
        updateServerBullets(delta);

        // 同步
        manager.tick();
    }

    /** Host 坦克由本地键盘驱动: WASD 移动, J 开火 */
    private void updateHostTankInput(TankBehaviour tank, float delta, float speed) {
        if (tank.isDead.getValue()) return;

        float dx = 0, dy = 0;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += speed;

        if (dx != 0 || dy != 0) {
            tank.x.setValue(tank.x.getValue() + dx);
            tank.y.setValue(tank.y.getValue() + dy);
            tank.rot.setValue(new Vector2(dx, dy).angleDeg());
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.J)) {
            TankSandboxUtils.spawnBullet(tank, -1, serverBullets, nextBulletId++);
        }
    }

    /** Server 子弹物理 + 泛化碰撞检测（支持 N 辆坦克） */
    private void updateServerBullets(float delta) {
        float screenW = uiViewport.getWorldWidth();
        float screenH = uiViewport.getWorldHeight();
        Iterator<Bullet> iter = serverBullets.iterator();
        while (iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;

            // 越界移除
            if (b.x < 0 || b.x > screenW || b.y < 0 || b.y > screenH) {
                iter.remove();
                continue;
            }

            // 泛化碰撞: 遍历所有坦克
            boolean hit = false;
            for (Map.Entry<Integer, TankBehaviour> entry : clientTanks.entrySet()) {
                int tankOwnerId = entry.getKey();
                TankBehaviour tank = entry.getValue();
                if (b.ownerId == tankOwnerId) continue; // 不打自己
                if (tank.isDead.getValue()) continue;
                double dist = Math.hypot(b.x - tank.x.getValue(), b.y - tank.y.getValue());
                if (dist < 20) {
                    TankSandboxUtils.hitTank(tank);
                    // 通知所有客户端销毁该子弹（从射手坦克发送，因为子弹在射手的 localBullets 中）
                    TankBehaviour shooter = clientTanks.get(b.ownerId);
                    if (shooter != null) {
                        shooter.sendClientRpc("rpcDestroyBullet", b.bulletId);
                    }
                    hit = true;
                    break;
                }
            }
            if (hit) {
                iter.remove();
            }
        }
    }

    // ── Client 逻辑 ──

    private void updateClientLogic(float delta) {
        // 检测本地输入并通过 ServerRpc 上报
        TankBehaviour myTank = findLocalPlayerTank();
        if (myTank != null && !myTank.isDead.getValue()) {
            float dx = 0, dy = 0;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) dy += 1;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx -= 1;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx += 1;

            if (dx != 0 || dy != 0) {
                myTank.sendServerRpc("rpcMoveInput", dx, dy);
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.J) || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                myTank.sendServerRpc("rpcFireInput");
            }
        }

        // 模拟 Client 端子弹运动
        updateClientBullets(delta);
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

    /** 收集由 ClientRpc 触发的子弹，模拟运动 */
    private void updateClientBullets(float delta) {
        clientBullets.clear();
        float screenW = uiViewport.getWorldWidth();
        float screenH = uiViewport.getWorldHeight();

        for (NetworkObject obj : manager.getAllNetworkObjects()) {
            if (obj.getBehaviours().isEmpty()) continue;
            TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);
            Iterator<Bullet> iter = tank.localBullets.iterator();
            while (iter.hasNext()) {
                Bullet b = iter.next();
                b.x += b.vx * delta;
                b.y += b.vy * delta;
                if (b.x < 0 || b.x > screenW || b.y < 0 || b.y > screenH) {
                    iter.remove();
                    continue;
                }
                clientBullets.add(b);
            }
        }
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
        DLog.logT("Netcode", "[Online] Client #" + clientId + " 已连接，Spawn 坦克");
    }

    /** Server 端: Client 断开时，清理游戏层映射（已在主线程 tick() 中调用） */
    private void onClientDisconnectedHandler(int clientId) {
        clientTanks.remove(clientId);
        DLog.logT("Netcode", "[Online] Client #" + clientId + " 已断开，移除坦克。剩余坦克: " + clientTanks.size());
    }

    /** 为指定 ownerClientId Spawn 一辆坦克 */
    private void spawnTankForOwner(int ownerClientId) {
        NetworkObject obj = manager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID, ownerClientId);
        TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);

        // 按序号分配出生点和颜色
        int index = clientTanks.size();
        float spawnX = 150f + (index % 4) * 150f;
        float spawnY = 150f + (index / 4) * 150f;
        Color spawnColor = SPAWN_COLORS[index % SPAWN_COLORS.length];

        tank.x.setValue(spawnX);
        tank.y.setValue(spawnY);
        tank.color.setValue(spawnColor);
        // 设置玩家名称标签
        if (ownerClientId == -1) {
            tank.playerName.setValue("Host");
        } else {
            tank.playerName.setValue("Player#" + ownerClientId);
        }

        clientTanks.put(ownerClientId, tank);
    }

    /** 复活坦克 */
    private void respawnTank(TankBehaviour tank, int ownerClientId) {
        tank.isDead.setValue(false);
        tank.hp.setValue(4);
        // 回到出生点
        int index = new ArrayList<>(clientTanks.keySet()).indexOf(ownerClientId);
        if (index < 0) index = 0;
        tank.x.setValue(150f + (index % 4) * 150f);
        tank.y.setValue(150f + (index / 4) * 150f);
        tank.color.setValue(SPAWN_COLORS[index % SPAWN_COLORS.length]);
    }

    // ══════════════ 渲染 ══════════════

    private void renderConfig() {
        neon.setProjectionMatrix(uiViewport.getCamera().combined);
        neon.begin();
        float cx = uiViewport.getWorldWidth() / 2f - 160;
        float cy = uiViewport.getWorldHeight() / 2f + 80;

        titleFont.setColor(Color.WHITE);
        titleFont.draw(neon, "Netcode 坦克联机对战", cx + 30, cy + 20);

        font.setColor(Color.GRAY);
        font.draw(neon, "[Tab] 切换角色    [1] 编辑IP    [2] 编辑端口    [Enter] 开始", cx - 30, cy - 10);

        // 错误提示
        if (configError != null) {
            font.setColor(Color.RED);
            font.draw(neon, "✖ " + configError, cx - 30, cy - 30);
        }

        // 角色选择
        float y1 = cy - 50;
        font.setColor(isServerRole ? Color.YELLOW : Color.GRAY);
        font.draw(neon, (isServerRole ? ">> " : "   ") + "创建房间 (Server)", cx, y1);
        font.setColor(!isServerRole ? Color.YELLOW : Color.GRAY);
        font.draw(neon, (!isServerRole ? ">> " : "   ") + "加入房间 (Client)", cx, y1 - 25);

        // IP 输入
        float y2 = y1 - 70;
        font.setColor(editFocus == 1 ? Color.GREEN : Color.WHITE);
        font.draw(neon, "[1] 服务器IP: " + ipInput + (editFocus == 1 ? "_" : ""), cx, y2);

        // 端口输入
        font.setColor(editFocus == 2 ? Color.GREEN : Color.WHITE);
        font.draw(neon, "[2] 端口:     " + portInput + (editFocus == 2 ? "_" : ""), cx, y2 - 25);

        // 提示
        font.setColor(Color.LIGHT_GRAY);
        if (isServerRole) {
            font.draw(neon, "Server 模式: 端口被其他客户端用来连接你", cx, y2 - 65);
            font.draw(neon, "操作: WASD 移动 + J 开火", cx, y2 - 85);
        } else {
            font.draw(neon, "Client 模式: 输入 Server 的 IP 和端口", cx, y2 - 65);
            font.draw(neon, "操作: WASD/方向键 移动 + J/Enter 开火", cx, y2 - 85);
        }

        neon.end();
    }

    private void renderWaiting() {
        neon.setProjectionMatrix(uiViewport.getCamera().combined);
        neon.begin();
        font.setColor(Color.YELLOW);
        float cx = uiViewport.getWorldWidth() / 2f - 120;
        float cy = uiViewport.getWorldHeight() / 2f;

        if (isServerRole) {
            font.draw(neon, "Server 等待客户端连接...", cx, cy + 20);
            font.draw(neon, "监听端口: " + configPort, cx, cy - 10);
            font.draw(neon, "已连接坦克: " + clientTanks.size(), cx, cy - 30);
        } else {
            font.draw(neon, "连接中: " + configIp + ":" + configPort, cx, cy + 20);
            font.draw(neon, "等待 Spawn 数据...", cx, cy - 10);
            font.draw(neon, "已收到实体: " + manager.getNetworkObjectCount(), cx, cy - 30);
        }
        neon.end();
    }

    private void renderPlaying() {
        neon.setProjectionMatrix(uiViewport.getCamera().combined);
        neon.begin();

        if (isServerRole) {
            // ── Server 渲染 ──
            for (TankBehaviour tank : clientTanks.values()) {
                TankSandboxUtils.drawTank(neon, font, tank, 0);
            }
            TankSandboxUtils.drawBullets(neon, serverBullets, 0);

            font.setColor(Color.YELLOW);
            font.draw(neon, "Server (Port:" + configPort + ")  坦克数:" + clientTanks.size()
                + "  远程客户端:" + transport.getActiveClientCount(),
                10, uiViewport.getWorldHeight() - 10);
            font.draw(neon, "Host: WASD + J  |  远程客户端通过 ServerRpc 操控",
                10, uiViewport.getWorldHeight() - 30);
        } else {
            // ── Client 渲染 ──
            for (NetworkObject obj : manager.getAllNetworkObjects()) {
                if (!obj.getBehaviours().isEmpty()) {
                    TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);
                    TankSandboxUtils.drawTank(neon, font, tank, 0);
                }
            }
            TankSandboxUtils.drawBullets(neon, clientBullets, 0);

            TankBehaviour myTank = findLocalPlayerTank();
            font.setColor(Color.YELLOW);
            font.draw(neon, "Client -> " + configIp + ":" + configPort
                + "  实体数:" + manager.getNetworkObjectCount()
                + "  myId:" + manager.getLocalClientId(),
                10, uiViewport.getWorldHeight() - 10);
            font.draw(neon, myTank != null ? "WASD/方向键 移动 + J/Enter 开火" : "等待分配坦克...",
                10, uiViewport.getWorldHeight() - 30);
        }

        // 绘制抽屉面板（两端通用）
        renderDrawerPanel();

        neon.end();
    }

    // ══════════════ 抽屉面板 ══════════════

    /**
     * 绘制右侧抽屉式房间成员详情面板。
     * 参考旧项目 DrawerPanel 的展开/收起概念，以即时模式（NeonBatch）实现。
     * 按 I 键切换展开/收起，带平滑动画。
     */
    private void renderDrawerPanel() {
        if (drawerAnimProgress <= 0.001f) {
            // 完全收起时只画一个小标签提示
            font.setColor(Color.GRAY);
            font.draw(neon, "[I] 成员面板", uiViewport.getWorldWidth() - 105, uiViewport.getWorldHeight() / 2f + 8);
            return;
        }

        float screenW = uiViewport.getWorldWidth();
        float screenH = uiViewport.getWorldHeight();

        // 使用 pow2Out 缓动（类似旧项目的 Interpolation.pow2Out）
        float eased = 1f - (1f - drawerAnimProgress) * (1f - drawerAnimProgress);
        float visibleW = DRAWER_WIDTH * eased;

        // 面板区域：从右边缘向左滑出
        float panelX = screenW - visibleW;
        float panelY = 0;
        float panelH = screenH;

        // 半透明背景
        neon.drawRect(panelX, panelY, visibleW, panelH, 0, 0, new Color(0.1f, 0.1f, 0.15f, 0.85f * eased), true);
        // 左边缘分割线
        neon.drawRect(panelX, panelY, 2, panelH, 0, 0, new Color(0.4f, 0.6f, 1f, 0.7f * eased), true);

        // 面板内容（仅当展开足够时绘制文字，避免裁剪闪烁）
        if (eased < 0.3f) return;

        float textX = panelX + 12;
        float textY = screenH - 20;
        float lineH = 18f;

        // 标题
        titleFont.setColor(new Color(0.5f, 0.8f, 1f, eased));
        titleFont.draw(neon, "房间成员", textX, textY);
        textY -= lineH + 6;

        // 分割线
        neon.drawRect(textX, textY, DRAWER_WIDTH - 24, 1, 0, 0, new Color(0.3f, 0.5f, 0.8f, 0.6f * eased), true);
        textY -= 8;

        if (isServerRole) {
            // Server 端：展示 clientTanks 映射中的每个成员
            for (Map.Entry<Integer, TankBehaviour> entry : clientTanks.entrySet()) {
                int ownerId = entry.getKey();
                TankBehaviour tank = entry.getValue();
                textY = drawMemberRow(textX, textY, lineH, eased, ownerId, tank);
            }
        } else {
            // Client 端：展示 manager 中的所有 NetworkObject
            for (NetworkObject obj : manager.getAllNetworkObjects()) {
                if (obj.getBehaviours().isEmpty()) continue;
                TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);
                int ownerId = obj.getOwnerClientId();
                textY = drawMemberRow(textX, textY, lineH, eased, ownerId, tank);
            }
        }

        // 底部提示
        textY -= 10;
        font.setColor(new Color(0.5f, 0.5f, 0.5f, eased));
        font.draw(neon, "[I] 收起面板", textX, textY);
    }

    /** 绘制单个成员行，返回下一个 Y 坐标 */
    private float drawMemberRow(float textX, float textY, float lineH, float alpha, int ownerId, TankBehaviour tank) {
        String name = tank.playerName.getValue();
        if (name == null || name.isEmpty()) name = (ownerId == -1 ? "Host" : "Client#" + ownerId);

        // 颜色方块标识
        Color tankColor = tank.color.getValue();
        neon.drawRect(textX, textY - 10, 10, 10, 0, 0,
            new Color(tankColor.r, tankColor.g, tankColor.b, alpha), true);

        // 名称
        font.setColor(new Color(1f, 1f, 1f, alpha));
        font.draw(neon, name, textX + 16, textY);
        textY -= lineH;

        // HP
        int hp = tank.hp.getValue();
        boolean dead = tank.isDead.getValue();
        String hpText = dead ? "  已阵亡 (复活:" + String.format("%.1f", tank.respawnTimer.getValue()) + "s)"
                             : "  HP: " + hp + "/4";
        font.setColor(new Color(dead ? 1f : 0.5f, dead ? 0.4f : 1f, dead ? 0.4f : 0.5f, alpha));
        font.draw(neon, hpText, textX, textY);
        textY -= lineH;

        // 坐标
        font.setColor(new Color(0.6f, 0.6f, 0.6f, alpha));
        font.draw(neon, String.format("  Pos: (%.0f, %.0f)", tank.x.getValue(), tank.y.getValue()), textX, textY);
        textY -= lineH + 4;

        return textY;
    }
}
