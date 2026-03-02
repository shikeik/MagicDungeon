package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.ReliableUdpTransport;

/**
 * 坦克联机对战的渲染器 —— 负责所有阶段（CONFIG/WAITING/PLAYING）的绘制。
 * <p>
 * 拥有渲染资源（NeonBatch、字体），由 Screen 在 create() 时创建、dispose() 时销毁。
 * 各渲染方法接收当前帧的数据快照，不持有游戏状态的引用。
 */
public class TankGameRenderer {

    // ── 渲染资源 ──
    private NeonBatch neon;
    private BitmapFont font;
    private BitmapFont titleFont;

    // ── 地图渲染常量 ──
    private static final Color WALL_COLOR = new Color(0.4f, 0.6f, 1f, 0.7f);
    private static final Color WALL_FILL_COLOR = new Color(0.2f, 0.3f, 0.5f, 0.3f);
    private static final Color BOUNDARY_COLOR = new Color(0.3f, 0.8f, 0.3f, 0.6f);
    private static final float WALL_LINE_WIDTH = 2f;
    private static final float BOUNDARY_LINE_WIDTH = 3f;

    // ── 抽屉面板常量 ──
    public static final float DRAWER_WIDTH = 220f;

    // ── 断线覆盖层常量 ──
    public static final float MAX_RECONNECT_WAIT_SEC = 15f;

    // ══════════════ 构造 / 销毁 ══════════════

    public TankGameRenderer() {
        neon = new NeonBatch();
        font = FontUtils.generate(14, 2);
        titleFont = FontUtils.generate(18, 2);
    }

    /** 获取 NeonBatch（供需要在外部 begin/end 之间绘制的场景使用） */
    public NeonBatch getNeon() { return neon; }
    /** 获取正文字体 */
    public BitmapFont getFont() { return font; }

    public void dispose() {
        if (neon != null) { neon.dispose(); neon = null; }
        if (font != null) { font.dispose(); font = null; }
        if (titleFont != null) { titleFont.dispose(); titleFont = null; }
    }

    // ══════════════ CONFIG 阶段渲染 ══════════════

    /**
     * 渲染配置界面（角色选择 + IP/端口输入）。
     */
    public void renderConfig(Viewport uiViewport, boolean isServerRole,
                             StringBuilder ipInput, StringBuilder portInput,
                             int editFocus, String configError) {
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

    // ══════════════ WAITING 阶段渲染 ══════════════

    /**
     * 渲染等待连接界面。
     */
    public void renderWaiting(Viewport uiViewport, boolean isServerRole,
                              String configIp, int configPort,
                              int tankCount, int entityCount) {
        neon.setProjectionMatrix(uiViewport.getCamera().combined);
        neon.begin();
        font.setColor(Color.YELLOW);
        float cx = uiViewport.getWorldWidth() / 2f - 120;
        float cy = uiViewport.getWorldHeight() / 2f;

        if (isServerRole) {
            font.draw(neon, "Server 等待客户端连接...", cx, cy + 20);
            font.draw(neon, "监听端口: " + configPort, cx, cy - 10);
            font.draw(neon, "已连接坦克: " + tankCount, cx, cy - 30);
        } else {
            font.draw(neon, "连接中: " + configIp + ":" + configPort, cx, cy + 20);
            font.draw(neon, "等待 Spawn 数据...", cx, cy - 10);
            font.draw(neon, "已收到实体: " + entityCount, cx, cy - 30);
        }
        neon.end();
    }

    // ══════════════ PLAYING 阶段渲染 ══════════════

    /**
     * 渲染 PLAYING 阶段的完整画面：游戏世界 + UI HUD + 抽屉面板 + 断线覆盖层。
     *
     * @param worldCamera      游戏世界相机
     * @param uiViewport       UI 视口
     * @param isServerRole     是否为 Server 端
     * @param configIp         连接 IP
     * @param configPort       端口
     * @param roomName         房间名称
     * @param clientTanks      Server 端坦克映射
     * @param manager          网络管理器
     * @param transport        可靠传输层
     * @param bulletSystem     子弹系统
     * @param gameMap          地图
     * @param drawerAnimProgress 抽屉动画进度 (0~1)
     * @param connectionLost   是否已断线
     * @param connectionLostTimer 断线计时器
     */
    public void renderPlaying(
        Camera worldCamera, Viewport uiViewport,
        boolean isServerRole, String configIp, int configPort, String roomName,
        Map<Integer, TankBehaviour> clientTanks, NetworkManager manager,
        ReliableUdpTransport transport, BulletSystem bulletSystem, TankGameMap gameMap,
        float drawerAnimProgress, boolean connectionLost, float connectionLostTimer) {

        // ── Pass 1: 游戏世界（跟随相机）──
        neon.setProjectionMatrix(worldCamera.combined);
        neon.begin();

        renderMapElements(gameMap);

        if (isServerRole) {
            renderServerEntities(clientTanks.values(), bulletSystem.getServerBulletsMutable());
        } else {
            renderClientEntities(manager.getAllNetworkObjects(), bulletSystem.getClientBullets());
        }

        neon.end();

        // ── Pass 2: UI 固定层（HUD + 抽屉面板 + 断线覆盖层）──
        neon.setProjectionMatrix(uiViewport.getCamera().combined);
        neon.begin();

        if (isServerRole) {
            renderServerHud(uiViewport, configPort, clientTanks.size(),
                transport != null ? transport.getActiveClientCount() : 0);
        } else {
            renderClientHud(uiViewport, configIp, configPort, manager, transport);
        }

        renderDrawerPanel(uiViewport, drawerAnimProgress, isServerRole, configIp, configPort,
            transport, roomName, clientTanks, manager);

        if (connectionLost) {
            renderDisconnectOverlay(uiViewport, connectionLostTimer);
        }

        neon.end();
    }

    // ── 游戏世界子方法 ──

    /** 渲染地图边界和墙体 */
    private void renderMapElements(TankGameMap gameMap) {
        neon.drawRect(0, 0, gameMap.getMapWidth(), gameMap.getMapHeight(),
            0, BOUNDARY_LINE_WIDTH, BOUNDARY_COLOR, false);

        for (float[] w : gameMap.getWalls()) {
            neon.drawRect(w[0], w[1], w[2], w[3], 0, 0, WALL_FILL_COLOR, true);
            neon.drawRect(w[0], w[1], w[2], w[3], 0, WALL_LINE_WIDTH, WALL_COLOR, false);
        }
    }

    /** 渲染 Server 端的坦克和子弹 */
    private void renderServerEntities(Collection<TankBehaviour> tanks, List<Bullet> bullets) {
        for (TankBehaviour tank : tanks) {
            TankSandboxUtils.drawTank(neon, font, tank, 0);
        }
        TankSandboxUtils.drawBullets(neon, bullets, 0);
    }

    /** 渲染 Client 端的坦克和子弹 */
    private void renderClientEntities(Collection<NetworkObject> networkObjects, List<Bullet> bullets) {
        for (NetworkObject obj : networkObjects) {
            if (!obj.getBehaviours().isEmpty()) {
                TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);
                TankSandboxUtils.drawTank(neon, font, tank, 0);
            }
        }
        TankSandboxUtils.drawBullets(neon, bullets, 0);
    }

    // ── HUD 子方法 ──

    private void renderServerHud(Viewport uiViewport, int port, int tankCount, int activeClientCount) {
        font.setColor(Color.YELLOW);
        font.draw(neon, "Server (Port:" + port + ")  坦克数:" + tankCount
            + "  远程客户端:" + activeClientCount,
            10, uiViewport.getWorldHeight() - 10);
        font.draw(neon, "Host: WASD + J  |  远程客户端通过 ServerRpc 操控",
            10, uiViewport.getWorldHeight() - 30);
    }

    private void renderClientHud(Viewport uiViewport, String ip, int port,
                                 NetworkManager manager, ReliableUdpTransport transport) {
        TankBehaviour myTank = findLocalPlayerTank(manager);
        font.setColor(Color.YELLOW);
        long pingMs = transport != null ? transport.getPingMs() : -1;
        font.draw(neon, "Client -> " + ip + ":" + port
            + "  Ping:" + (pingMs >= 0 ? pingMs + "ms" : "-")
            + "  实体:" + manager.getNetworkObjectCount()
            + "  myId:" + manager.getLocalClientId(),
            10, uiViewport.getWorldHeight() - 10);
        font.draw(neon, myTank != null ? "WASD/方向键 移动 + J/Enter 开火" : "等待分配坦克...",
            10, uiViewport.getWorldHeight() - 30);
    }

    // ══════════════ 抽屉面板 ══════════════

    private void renderDrawerPanel(Viewport uiViewport, float drawerAnimProgress,
                                   boolean isServerRole, String configIp, int configPort,
                                   ReliableUdpTransport transport, String roomName,
                                   Map<Integer, TankBehaviour> clientTanks,
                                   NetworkManager manager) {
        if (drawerAnimProgress <= 0.001f) {
            font.setColor(Color.GRAY);
            font.draw(neon, "[I] 信息面板",
                uiViewport.getWorldWidth() - 105, uiViewport.getWorldHeight() / 2f + 8);
            return;
        }

        float screenW = uiViewport.getWorldWidth();
        float screenH = uiViewport.getWorldHeight();

        float eased = 1f - (1f - drawerAnimProgress) * (1f - drawerAnimProgress);
        float visibleW = DRAWER_WIDTH * eased;

        float panelX = screenW - visibleW;
        float panelH = screenH;

        // 半透明背景 + 分割线
        neon.drawRect(panelX, 0, visibleW, panelH, 0, 0,
            new Color(0.1f, 0.1f, 0.15f, 0.85f * eased), true);
        neon.drawRect(panelX, 0, 2, panelH, 0, 0,
            new Color(0.4f, 0.6f, 1f, 0.7f * eased), true);

        if (eased < 0.3f) return;

        float textX = panelX + 12;
        float textY = screenH - 20;
        float lineH = 18f;

        // ── 房间信息区 ──
        titleFont.setColor(new Color(0.5f, 0.8f, 1f, eased));
        titleFont.draw(neon, "房间信息", textX, textY);
        textY -= lineH + 6;

        neon.drawRect(textX, textY, DRAWER_WIDTH - 24, 1, 0, 0,
            new Color(0.3f, 0.5f, 0.8f, 0.6f * eased), true);
        textY -= 8;

        font.setColor(new Color(0.8f, 0.8f, 0.8f, eased));
        font.draw(neon, "房间: " + (roomName != null ? roomName : "(手动配置)"), textX, textY);
        textY -= lineH;
        font.draw(neon, "角色: " + (isServerRole ? "Server (房主)" : "Client (玩家)"), textX, textY);
        textY -= lineH;
        if (isServerRole) {
            font.draw(neon, "监听: 0.0.0.0:" + configPort, textX, textY);
        } else {
            font.draw(neon, "连接: " + configIp + ":" + configPort, textX, textY);
        }
        textY -= lineH;
        font.draw(neon, "模式: " + getConnectionMode(isServerRole, configIp), textX, textY);
        textY -= lineH;
        long drawerPingMs = transport != null ? transport.getPingMs() : -1;
        String pingText = drawerPingMs >= 0 ? drawerPingMs + " ms"
            : (isServerRole ? "N/A(Host)" : "测量中...");
        font.draw(neon, "Ping: " + pingText, textX, textY);
        textY -= lineH;
        int tankCount = isServerRole ? clientTanks.size() : manager.getNetworkObjectCount();
        font.draw(neon, "坦克数: " + tankCount, textX, textY);
        textY -= lineH + 10;

        // ── 房间成员区 ──
        titleFont.setColor(new Color(0.5f, 0.8f, 1f, eased));
        titleFont.draw(neon, "房间成员", textX, textY);
        textY -= lineH + 6;

        neon.drawRect(textX, textY, DRAWER_WIDTH - 24, 1, 0, 0,
            new Color(0.3f, 0.5f, 0.8f, 0.6f * eased), true);
        textY -= 8;

        if (isServerRole) {
            for (Map.Entry<Integer, TankBehaviour> entry : clientTanks.entrySet()) {
                textY = drawMemberRow(textX, textY, lineH, eased, entry.getKey(), entry.getValue());
            }
        } else {
            for (NetworkObject obj : manager.getAllNetworkObjects()) {
                if (obj.getBehaviours().isEmpty()) continue;
                TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);
                textY = drawMemberRow(textX, textY, lineH, eased, obj.getOwnerClientId(), tank);
            }
        }

        textY -= 10;
        font.setColor(new Color(0.5f, 0.5f, 0.5f, eased));
        font.draw(neon, "[I] 收起面板", textX, textY);
    }

    // ══════════════ 断线覆盖层 ══════════════

    private void renderDisconnectOverlay(Viewport uiViewport, float connectionLostTimer) {
        float screenW = uiViewport.getWorldWidth();
        float screenH = uiViewport.getWorldHeight();

        neon.drawRect(0, 0, screenW, screenH, 0, 0, new Color(0, 0, 0, 0.75f), true);

        float cx = screenW / 2f;
        float cy = screenH / 2f;

        titleFont.setColor(Color.RED);
        titleFont.draw(neon, "⚠ 连接已断开", cx - 70, cy + 55);

        font.setColor(Color.YELLOW);
        font.draw(neon, String.format("已断开 %.1f 秒", connectionLostTimer), cx - 50, cy + 18);

        float remaining = MAX_RECONNECT_WAIT_SEC - connectionLostTimer;
        font.setColor(Color.WHITE);
        font.draw(neon, String.format("%.0f 秒后自动返回大厅...", Math.max(0, remaining)), cx - 70, cy - 12);

        float barW = 200f, barH = 8f;
        float barX = cx - barW / 2f;
        float barY = cy - 50;
        neon.drawRect(barX, barY, barW, barH, 0, 0, new Color(0.3f, 0.3f, 0.3f, 0.8f), true);

        float progress = Math.max(0, 1f - connectionLostTimer / MAX_RECONNECT_WAIT_SEC);
        neon.drawRect(barX, barY, barW * progress, barH, 0, 0, new Color(1f, 0.4f, 0.2f, 0.9f), true);

        font.setColor(Color.GRAY);
        font.draw(neon, "按 ESC 立即返回", cx - 50, barY - 25);
    }

    // ══════════════ 辅助方法 ══════════════

    /** 绘制单个成员行，返回下一个 Y 坐标 */
    private float drawMemberRow(float textX, float textY, float lineH, float alpha,
                                int ownerId, TankBehaviour tank) {
        String name = tank.playerName.getValue();
        if (name == null || name.isEmpty()) name = (ownerId == -1 ? "Host" : "Client#" + ownerId);

        Color tankColor = tank.color.getValue();
        neon.drawRect(textX, textY - 10, 10, 10, 0, 0,
            new Color(tankColor.r, tankColor.g, tankColor.b, alpha), true);

        font.setColor(new Color(1f, 1f, 1f, alpha));
        font.draw(neon, name, textX + 16, textY);
        textY -= lineH;

        int hp = tank.hp.getValue();
        boolean dead = tank.isDead.getValue();
        String hpText = dead
            ? "  已阵亡 (复活:" + String.format("%.1f", tank.respawnTimer.getValue()) + "s)"
            : "  HP: " + hp + "/4";
        font.setColor(new Color(dead ? 1f : 0.5f, dead ? 0.4f : 1f, dead ? 0.4f : 0.5f, alpha));
        font.draw(neon, hpText, textX, textY);
        textY -= lineH;

        font.setColor(new Color(0.6f, 0.6f, 0.6f, alpha));
        font.draw(neon, String.format("  Pos: (%.0f, %.0f)", tank.x.getValue(), tank.y.getValue()), textX, textY);
        textY -= lineH + 4;

        return textY;
    }

    /** 从 manager 中找到本地玩家坦克 */
    private TankBehaviour findLocalPlayerTank(NetworkManager manager) {
        for (NetworkObject obj : manager.getAllNetworkObjects()) {
            if (obj.isLocalPlayer && !obj.getBehaviours().isEmpty()) {
                return (TankBehaviour) obj.getBehaviours().get(0);
            }
        }
        return null;
    }

    /** 获取连接模式描述 */
    private static String getConnectionMode(boolean isServerRole, String configIp) {
        if (isServerRole) return "本机服务端";
        if ("127.0.0.1".equals(configIp) || "localhost".equals(configIp)) return "回环 (同机)";
        if (configIp.startsWith("192.168.") || configIp.startsWith("10.") || configIp.startsWith("172."))
            return "LAN (局域网)";
        return "公网/Frp";
    }
}
