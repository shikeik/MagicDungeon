package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.netcode.supabase.RoomManager;
import com.goldsprite.gdengine.netcode.supabase.RoomModel;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * Supabase 云大厅屏幕
 * <p>
 * 功能: 浏览/刷新房间列表、创建房间、加入房间。
 * <br>遵循 GScreen 最佳实践: 继承 ExampleGScreen, 使用 render0() 而非 render(),
 * 所有 UI 使用 VisUI 控件, 通过 ScreenManager 管理导航。
 */
public class SupabaseLobbyScreen extends ExampleGScreen {

    // === UI 控件 ===
    private Stage uiStage;
    private VisTable roomListTable;
    private VisLabel statusLabel;
    private VisTextField playerNameField;

    // === 业务层 ===
    private RoomManager roomManager;

    // === 自动刷新 ===
    private float autoRefreshTimer = 0f;
    private static final float AUTO_REFRESH_INTERVAL = 10f;

    @Override
    public String getIntroduction() {
        return "Supabase 云大厅: 房间列表浏览 / 创建房间 / 加入房间";
    }

    // ==================== 生命周期 ====================

    @Override
    public void create() {
        setDrawScreenBack(false);
        roomManager = new RoomManager();

        uiStage = new Stage(getUIViewport());
        getImp().addProcessor(uiStage);

        buildUI();
        refreshRoomList();
    }

    @Override
    public void render0(float delta) {
        // 定时自动刷新房间列表
        autoRefreshTimer += delta;
        if (autoRefreshTimer >= AUTO_REFRESH_INTERVAL) {
            autoRefreshTimer = 0f;
            refreshRoomList();
        }

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void dispose() {
        if (roomManager != null) roomManager.destroyRoom();
        if (uiStage != null) uiStage.dispose();
    }

    // ==================== UI 构建 ====================

    private void buildUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.pad(20);
        uiStage.addActor(root);

        // --- 标题栏 ---
        VisTable titleBar = new VisTable();
        VisLabel titleLabel = new VisLabel("Supabase 云大厅");
        titleLabel.setColor(Color.CYAN);
        titleBar.add(titleLabel).expandX().left();

        statusLabel = new VisLabel("待命");
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

    /** 左侧面板: 房间列表 + 刷新按钮 */
    private VisTable buildLeftPanel() {
        VisTable panel = new VisTable();

        panel.add(new VisLabel("房间列表")).left().padBottom(8).row();

        roomListTable = new VisTable();
        roomListTable.top().left();

        VisScrollPane scrollPane = new VisScrollPane(roomListTable);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        panel.add(scrollPane).expand().fill().row();

        VisTextButton refreshBtn = new VisTextButton("刷新列表");
        refreshBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                refreshRoomList();
            }
        });
        panel.add(refreshBtn).expandX().fillX().padTop(8).row();

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

        // 创建房间按钮
        VisTextButton createRoomBtn = new VisTextButton("创建房间");
        createRoomBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createAndJoinRoom();
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

    // ==================== 房间列表操作 ====================

    private void refreshRoomList() {
        setStatus("拉取房间列表中...", Color.YELLOW);
        roomManager.fetchRooms(new RoomManager.FetchRoomsCallback() {
            @Override
            public void onSuccess(List<RoomModel> rooms) {
                Gdx.app.postRunnable(() -> {
                    rebuildRoomList(rooms);
                    int count = rooms != null ? rooms.size() : 0;
                    setStatus("列表已更新 (" + count + " 个房间)", Color.GREEN);
                });
            }

            @Override
            public void onError(Throwable t) {
                Gdx.app.postRunnable(() -> setStatus("拉取失败: " + t.getMessage(), Color.RED));
            }
        });
    }

    /** 重建房间列表表格 (含表头) */
    private void rebuildRoomList(List<RoomModel> rooms) {
        roomListTable.clearChildren();

        if (rooms == null || rooms.isEmpty()) {
            VisLabel emptyLabel = new VisLabel("暂无房间, 试试自己创建一个?");
            emptyLabel.setColor(Color.GRAY);
            roomListTable.add(emptyLabel).pad(20);
            return;
        }

        // 表头行
        VisTable header = new VisTable();
        VisLabel nameHeader = new VisLabel("房间名");
        nameHeader.setColor(Color.LIGHT_GRAY);
        header.add(nameHeader).width(200).left();
        VisLabel countHeader = new VisLabel("人数");
        countHeader.setColor(Color.LIGHT_GRAY);
        header.add(countHeader).width(80).center();
        header.add().width(100); // 占位: 加入按钮列
        roomListTable.add(header).expandX().fillX().padBottom(4).row();

        // 数据行
        for (RoomModel room : rooms) {
            roomListTable.add(buildRoomRow(room)).expandX().fillX().padBottom(4).row();
        }
    }

    /** 构建单行房间条目 */
    private VisTable buildRoomRow(final RoomModel room) {
        VisTable row = new VisTable();
        row.add(new VisLabel(room.getRoom_name())).width(200).left();
        row.add(new VisLabel(room.getPlayer_count() + "/" + room.getMax_players())).width(80).center();

        VisTextButton joinBtn = new VisTextButton("加入");
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinRoom(room);
            }
        });
        row.add(joinBtn).width(100).right();

        return row;
    }

    // ==================== 建房 / 加入 ====================

    private void createAndJoinRoom() {
        String name = playerNameField.getText().trim();
        if (name.isEmpty()) {
            setStatus("请输入玩家昵称", Color.RED);
            return;
        }

        setStatus("获取外网IP并创建房间...", Color.YELLOW);
        String roomName = name + " 的房间";
        int myPort = 20000; // 预设 UDP 监听端口

        roomManager.createRoom(roomName, 6, myPort, new RoomManager.CreateRoomCallback() {
            @Override
            public void onSuccess(String roomId) {
                Gdx.app.postRunnable(() -> {
                    setStatus("建房成功 ID:" + roomId, Color.GREEN);
                    // TODO: 携带房主标识跳转到联机对战屏幕
                    // getScreenManager().goScreen(NetcodeTankOnlineScreen.class);
                });
            }

            @Override
            public void onError(Throwable t) {
                Gdx.app.postRunnable(() -> setStatus("建房失败: " + t.getMessage(), Color.RED));
            }
        });
    }

    private void joinRoom(RoomModel room) {
        setStatus("连接中 " + room.getHost_ip() + ":" + room.getHost_port(), Color.YELLOW);
        // TODO: 作为客户端跳转到联机对战屏幕
        // getScreenManager().goScreen(NetcodeTankOnlineScreen.class);
    }

    // ==================== 工具方法 ====================

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setColor(color);
    }
}
