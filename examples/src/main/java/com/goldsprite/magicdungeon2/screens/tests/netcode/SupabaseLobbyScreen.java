package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.netcode.supabase.PresenceLobbyManager;
import com.goldsprite.gdengine.netcode.supabase.PresenceRoomInfo;
import com.goldsprite.gdengine.netcode.supabase.PublicIPResolver;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * Supabase Realtime Presence 云大厅屏幕
 * <p>
 * 功能: 实时浏览房间列表、创建房间、加入房间。
 * <br>基于 WebSocket Presence 实现，无需轮询——房间变动毫秒级推送。
 * <br>遵循 GScreen 最佳实践: 继承 ExampleGScreen, 使用 render0() 而非 render(),
 * 所有 UI 使用 VisUI 控件, 通过 ScreenManager 管理导航。
 */
public class SupabaseLobbyScreen extends ExampleGScreen {

    // === UI 控件 ===
    private Stage uiStage;
    private VisTable roomListTable;
    private VisLabel statusLabel;
    private VisLabel connectionIndicator;
    private VisTextField playerNameField;
    private VisTextField hostIpField;
    private VisTextButton createRoomBtn;

    // === 业务层 (Presence 替代旧的 RoomManager) ===
    private PresenceLobbyManager lobbyManager;

    // === 状态 ===
    private PresenceRoomInfo myRoomInfo;
    /** 本机公网 IP（进入大厅后异步获取，加入房间时用于判断是否同网络） */
    private String myPublicIp = null;
    /** 本机局域网 IP */
    private String myLocalIp = null;
    private static final String TAG = "SupabaseLobby";
    private static final int DEFAULT_UDP_PORT = 20000;

    @Override
    public String getIntroduction() {
        return "Supabase Presence 云大厅: 实时房间列表 / 创建房间 / 加入房间";
    }

    // ==================== 生命周期 ====================

    @Override
    public void create() {
        setDrawScreenBack(false);

        uiStage = new Stage(getUIViewport());
        getImp().addProcessor(uiStage);

        buildUI();
        connectToLobby();

        // 提前缓存本机 IP，供加入房间时判断是否同网络
        myLocalIp = PublicIPResolver.getLocalIP();
        DLog.logT(TAG, "本机局域网 IP: " + myLocalIp);

        PublicIPResolver.resolvePublicIP(new PublicIPResolver.ResolveCallback() {
            @Override
            public void onSuccess(String ip) {
                myPublicIp = ip;
                DLog.logT(TAG, "本机公网 IP: " + ip);
            }
            @Override
            public void onError(Throwable t) {
                DLog.logErrT(TAG, "获取本机公网 IP 失败: " + t);
            }
        });
    }

    @Override
    public void render0(float delta) {
        // 不需要轮询定时器了！Presence 事件会自动推送更新。
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void dispose() {
        // 断开 Presence 连接 (房间状态会自动从列表消失)
        if (lobbyManager != null) {
            lobbyManager.disconnect();
            lobbyManager = null;
        }
        if (uiStage != null) {
            uiStage.dispose();
            uiStage = null;
        }
    }

    // ==================== 连接大厅 ====================

    /**
     * 建立 WebSocket 连接并加入 Presence 频道
     */
    private void connectToLobby() {
        setStatus("正在连接大厅...", Color.YELLOW);
        setConnectionIndicator("连接中", Color.YELLOW);

        lobbyManager = new PresenceLobbyManager();
        lobbyManager.connect(
            // 房间列表同步回调 (Presence 事件驱动，无需轮询)
            new PresenceLobbyManager.OnRoomsSyncListener() {
                @Override
                public void onSync(List<PresenceRoomInfo> allRooms) {
                    // 注意: 已在 LibGDX 主线程中回调 (postRunnable 已在底层处理)
                    rebuildRoomList(allRooms);
                    setStatus("在线 (" + allRooms.size() + " 个房间)", Color.GREEN);
                }
            },
            // 连接状态回调
            new PresenceLobbyManager.OnStatusListener() {
                @Override
                public void onConnected() {
                    setConnectionIndicator("已连接", Color.GREEN);
                }

                @Override
                public void onJoined() {
                    setStatus("已加入大厅频道", Color.GREEN);
                    if (createRoomBtn != null) createRoomBtn.setDisabled(false);
                }

                @Override
                public void onError(String message) {
                    setStatus("错误: " + message, Color.RED);
                    setConnectionIndicator("错误", Color.RED);
                }

                @Override
                public void onDisconnected(String reason) {
                    setStatus("连接断开，正在重连...", Color.ORANGE);
                    setConnectionIndicator("重连中", Color.ORANGE);
                    if (createRoomBtn != null) createRoomBtn.setDisabled(true);
                }
            }
        );
    }

    // ==================== UI 构建 ====================

    private void buildUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.pad(20);
        uiStage.addActor(root);

        // --- 标题栏 ---
        VisTable titleBar = new VisTable();
        VisLabel titleLabel = new VisLabel("Supabase Presence 云大厅");
        titleLabel.setColor(Color.CYAN);
        titleBar.add(titleLabel).expandX().left();

        // 连接状态指示器
        connectionIndicator = new VisLabel("[连接中]");
        connectionIndicator.setColor(Color.YELLOW);
        titleBar.add(connectionIndicator).padRight(8);

        statusLabel = new VisLabel("初始化...");
        statusLabel.setColor(Color.YELLOW);
        titleBar.add(statusLabel).right();

        root.add(titleBar).expandX().fillX().padBottom(16).row();

        // --- 主体: 左右分栏 ---
        VisTable body = new VisTable();

        // 左侧：房间列表
        body.add(buildLeftPanel()).expand().fill().padRight(16);

        // 右侧：操作面板
        body.add(buildRightPanel()).width(280).fillY();

        root.add(body).expand().fill();
    }

    /** 左侧面板: 房间列表 (无需手动刷新按钮——Presence 自动推送) */
    private VisTable buildLeftPanel() {
        VisTable panel = new VisTable();

        VisLabel listTitle = new VisLabel("房间列表 (实时更新)");
        listTitle.setColor(Color.WHITE);
        panel.add(listTitle).left().padBottom(8).row();

        roomListTable = new VisTable();
        roomListTable.top().left();

        VisScrollPane scrollPane = new VisScrollPane(roomListTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).expand().fill().row();

        // 保留一个手动刷新提示 (Presence 模式下一般不需要)
        VisLabel hintLabel = new VisLabel("房间变动自动推送，无需手动刷新");
        hintLabel.setColor(Color.DARK_GRAY);
        panel.add(hintLabel).left().padTop(4).row();

        return panel;
    }

    /** 右侧面板: 玩家昵称 / 建房 / 返回 */
    private VisTable buildRightPanel() {
        VisTable panel = new VisTable();
        panel.top();

        // 玩家昵称输入
        panel.add(new VisLabel("玩家昵称")).left().row();
        playerNameField = new VisTextField("Player_" + (int) (Math.random() * 1000));
        panel.add(playerNameField).expandX().fillX().padBottom(16).row();

        // 主机地址输入 (留空 = 自动检测公网IP，填入 = 使用自定义IP，如 Frp 代理)
        panel.add(new VisLabel("主机地址(可选)")).left().row();
        hostIpField = new VisTextField("");
        hostIpField.setMessageText("留空=自动检测, 或输入Frp地址");
        panel.add(hostIpField).expandX().fillX().padBottom(16).row();

        // 创建房间按钮
        createRoomBtn = new VisTextButton("创建房间");
        createRoomBtn.setDisabled(true); // 等待频道加入成功后启用
        createRoomBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!createRoomBtn.isDisabled()) {
                    createAndPublishRoom();
                }
            }
        });
        panel.add(createRoomBtn).expandX().fillX().padBottom(8).row();

        // 弹性间距
        panel.add().expandY().row();

        // 返回按钮 (通过 ScreenManager 回退)
        VisTextButton backBtn = new VisTextButton("返回");
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                getScreenManager().popLastScreen();
            }
        });
        panel.add(backBtn).expandX().fillX().padTop(16).row();

        return panel;
    }

    // ==================== 房间列表渲染 ====================

    /** 重建房间列表表格 (由 Presence sync 事件触发) */
    private void rebuildRoomList(List<PresenceRoomInfo> rooms) {
        roomListTable.clearChildren();

        if (rooms == null || rooms.isEmpty()) {
            VisLabel emptyLabel = new VisLabel("暂无房间，试试自己创建一个?");
            emptyLabel.setColor(Color.GRAY);
            roomListTable.add(emptyLabel).pad(20);
            return;
        }

        // 表头行
        VisTable header = new VisTable();
        addHeaderLabel(header, "房间名", 180);
        addHeaderLabel(header, "地址", 180);
        addHeaderLabel(header, "人数", 60);
        addHeaderLabel(header, "状态", 60);
        header.add().width(80); // 占位: 加入按钮列
        roomListTable.add(header).expandX().fillX().padBottom(4).row();

        // 数据行
        for (PresenceRoomInfo room : rooms) {
            roomListTable.add(buildRoomRow(room)).expandX().fillX().padBottom(4).row();
        }
    }

    private void addHeaderLabel(VisTable header, String text, float width) {
        VisLabel label = new VisLabel(text);
        label.setColor(Color.LIGHT_GRAY);
        header.add(label).width(width).left();
    }

    /** 构建单行房间条目 */
    private VisTable buildRoomRow(final PresenceRoomInfo room) {
        VisTable row = new VisTable();

        // 房间名
        row.add(new VisLabel(room.roomName)).width(180).left();

        // 地址 (IP:Port)
        String addr = (room.hostIp.length() > 15 ? room.hostIp.substring(0, 15) + ".." : room.hostIp)
                + ":" + room.hostPort;
        VisLabel addrLabel = new VisLabel(addr);
        addrLabel.setColor(Color.LIGHT_GRAY);
        row.add(addrLabel).width(180).left();

        // 人数
        row.add(new VisLabel(room.getPlayerCountDisplay())).width(60).center();

        // 状态
        VisLabel statusLbl = new VisLabel(getStatusText(room.status));
        statusLbl.setColor(getStatusColor(room.status));
        row.add(statusLbl).width(60).center();

        // 加入按钮
        VisTextButton joinBtn = new VisTextButton("加入");
        if (!room.isJoinable()) {
            joinBtn.setDisabled(true);
        }
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!joinBtn.isDisabled()) {
                    joinRoom(room);
                }
            }
        });
        row.add(joinBtn).width(80).right();

        return row;
    }

    // ==================== 建房 ====================

    /**
     * 创建房间: 获取公网 IP → 发布到 Presence → 跳转到游戏屏幕 (Server 模式)
     */
    private void createAndPublishRoom() {
        String name = playerNameField.getText().trim();
        if (name.isEmpty()) {
            setStatus("请输入玩家昵称", Color.RED);
            return;
        }

        if (!lobbyManager.isReady()) {
            setStatus("大厅未就绪，请稍候", Color.RED);
            return;
        }

        setStatus("获取公网 IP 中...", Color.YELLOW);
        createRoomBtn.setDisabled(true);

        final String roomName = name + " 的房间";
        final int udpPort = DEFAULT_UDP_PORT;

        // 检查是否有自定义 IP (如 Frp 代理地址)
        final String customIp = hostIpField != null ? hostIpField.getText().trim() : "";

        if (!customIp.isEmpty()) {
            // 解析自定义地址，支持 ip:port 格式（如 Frp 代理地址 119.84.246.217:59473）
            String frpIp = customIp;
            int frpPort = udpPort;
            if (customIp.contains(":")) {
                int colonIdx = customIp.lastIndexOf(':');
                String portStr = customIp.substring(colonIdx + 1);
                try {
                    frpPort = Integer.parseInt(portStr);
                    frpIp = customIp.substring(0, colonIdx);
                } catch (NumberFormatException ignored) {
                    // 端口格式错误，整个字符串作为 IP，使用默认端口
                }
            }
            DLog.logT(TAG, "使用自定义主机地址: " + frpIp + ":" + frpPort);
            String localIp = PublicIPResolver.getLocalIP();
            // 房间信息使用 Frp 公网端口，Server 本地仍监听 DEFAULT_UDP_PORT
            myRoomInfo = new PresenceRoomInfo(roomName, frpIp, localIp, frpPort, 1, 6);

            lobbyManager.publishRoom(myRoomInfo);
            setStatus("建房成功! 正在进入游戏...", Color.GREEN);

            NetcodeTankOnlineScreen.preConfigureAsHost(udpPort);
            NetcodeTankOnlineScreen.preConfigureRoomInfo(roomName);
            getScreenManager().goScreen(NetcodeTankOnlineScreen.class);
            return;
        }

        PublicIPResolver.resolvePublicIP(new PublicIPResolver.ResolveCallback() {
            @Override
            public void onSuccess(final String ip) {
                Gdx.app.postRunnable(() -> {
                    // 构造房间信息（同时携带公网 IP 和局域网 IP）
                    String localIp = PublicIPResolver.getLocalIP();
                    myRoomInfo = new PresenceRoomInfo(roomName, ip, localIp, udpPort, 1, 6);
                    DLog.logT(TAG, "建房: 公网=" + ip + ", 局域网=" + localIp);

                    // 发布到 Presence (其他所有客户端会实时收到)
                    lobbyManager.publishRoom(myRoomInfo);
                    setStatus("建房成功! 正在进入游戏...", Color.GREEN);

                    // 携带房主参数跳转到 NetcodeTankOnlineScreen
                    NetcodeTankOnlineScreen.preConfigureAsHost(udpPort);
                    NetcodeTankOnlineScreen.preConfigureRoomInfo(roomName);
                    getScreenManager().goScreen(NetcodeTankOnlineScreen.class);
                });
            }

            @Override
            public void onError(final Throwable t) {
                Gdx.app.postRunnable(() -> {
                    setStatus("获取公网 IP 失败: " + t.getMessage(), Color.RED);
                    createRoomBtn.setDisabled(false);
                });
            }
        });
    }

    // ==================== 加入房间 ====================

    /**
     * 加入房间: 携带房主 IP 和端口跳转到游戏屏幕 (Client 模式)
     */
    private void joinRoom(PresenceRoomInfo room) {
        if (room.hostIp == null || room.hostIp.isEmpty() || room.hostPort <= 0) {
            setStatus("房间信息不完整，无法加入", Color.RED);
            return;
        }

        // 三级 IP 智能选择：同机 → 同 LAN → 公网
        String connectIp = room.hostIp;
        if (myPublicIp != null && myPublicIp.equals(room.hostIp)) {
            if (myLocalIp != null && myLocalIp.equals(room.localIp)) {
                // 同一台机器：用回环地址
                connectIp = "127.0.0.1";
                DLog.logT(TAG, "检测到同机，使用回环地址: 127.0.0.1");
            } else if (room.localIp != null && !room.localIp.isEmpty()) {
                // 同局域网不同机器：用局域网 IP
                connectIp = room.localIp;
                DLog.logT(TAG, "检测到同 LAN，使用局域网 IP: " + connectIp);
            }
        }

        DLog.logT(TAG, "加入房间: " + room.roomName + " -> " + connectIp + ":" + room.hostPort);
        setStatus("正在连接 " + connectIp + ":" + room.hostPort + "...", Color.YELLOW);

        // 携带客户端参数跳转到 NetcodeTankOnlineScreen
        NetcodeTankOnlineScreen.preConfigureAsClient(connectIp, room.hostPort);
        NetcodeTankOnlineScreen.preConfigureRoomInfo(room.roomName);
        getScreenManager().goScreen(NetcodeTankOnlineScreen.class);
    }

    // ==================== 工具方法 ====================

    private void setStatus(String text, Color color) {
        if (statusLabel != null) {
            statusLabel.setText(text);
            statusLabel.setColor(color);
        }
    }

    private void setConnectionIndicator(String text, Color color) {
        if (connectionIndicator != null) {
            connectionIndicator.setText("[" + text + "]");
            connectionIndicator.setColor(color);
        }
    }

    private String getStatusText(String status) {
        if ("waiting".equals(status)) return "等待中";
        if ("playing".equals(status)) return "游戏中";
        if ("full".equals(status)) return "已满";
        return status;
    }

    private Color getStatusColor(String status) {
        if ("waiting".equals(status)) return Color.GREEN;
        if ("playing".equals(status)) return Color.ORANGE;
        if ("full".equals(status)) return Color.RED;
        return Color.WHITE;
    }
}
