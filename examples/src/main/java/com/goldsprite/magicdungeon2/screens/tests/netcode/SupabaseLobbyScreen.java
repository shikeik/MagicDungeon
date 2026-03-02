package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.GdxLauncher;
import com.goldsprite.gdengine.netcode.supabase.RoomManager;
import com.goldsprite.gdengine.netcode.supabase.RoomModel;

public class SupabaseLobbyScreen extends ScreenAdapter {

    private final GdxLauncher game;
    private Stage stage;
    private Skin skin;
    private Table roomsTable;
    private RoomManager roomManager;
    private Label statusLabel;
    private TextField playerNameField;

    // TODO: 替换为您自建的 Supabase 网址和密钥
    // 必须在部署之前更换
    private static final String SUPABASE_URL = "https://ijiuncznaasfbamjuinm.supabase.co";
    private static final String SUPABASE_KEY = "sb_publishable_pEqL-0pZ4m6Q8zU5vUtTJg_bO2g2oSE";

    public SupabaseLobbyScreen(GdxLauncher game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // 为了快速测试，我们使用 Libgdx 自带或你项目默认的皮肤
        // 如果这里获取不到 default skin，请自行修改为加载你项目的正确 Skin
        skin = new Skin(Gdx.files.internal("ui_skins/ui_skin.json"));
        if(skin == null) {
            System.err.println("未能找到皮肤, 请确保有合适的 UI 皮肤文件");
            return;
        }

        roomManager = new RoomManager(SUPABASE_URL, SUPABASE_KEY);

        setupUI();
        refreshRoomList();
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // --- 左侧：房间列表 ---
        Table leftPanel = new Table();
        leftPanel.add(new Label("Server List / 大厅", skin)).padBottom(10).row();
        
        roomsTable = new Table();
        ScrollPane scrollPane = new ScrollPane(roomsTable, skin);
        leftPanel.add(scrollPane).width(400).height(400).row();
        
        TextButton refreshBtn = new TextButton("Refresh / 刷新", skin);
        refreshBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                refreshRoomList();
            }
        });
        leftPanel.add(refreshBtn).padTop(10).fillX();

        // --- 右侧：控制面板 ---
        Table rightPanel = new Table();
        
        rightPanel.add(new Label("Player Name:", skin)).left().row();
        playerNameField = new TextField("Player_" + (int)(Math.random()*1000), skin);
        rightPanel.add(playerNameField).fillX().padBottom(20).row();
        
        TextButton createRoomBtn = new TextButton("Create Room / 建房", skin);
        createRoomBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                createAndJoinRoom();
            }
        });
        rightPanel.add(createRoomBtn).fillX().padBottom(10).row();
        
        statusLabel = new Label("Status: 待命", skin);
        statusLabel.setColor(Color.YELLOW);
        rightPanel.add(statusLabel).fillX().padTop(20).row();
        
        TextButton backBtn = new TextButton("Back / 返回", skin);
        backBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 路由回主菜单或前一个屏幕 
                // game.setScreen(new MainMenuScreen(game));
            }
        });
        rightPanel.add(backBtn).fillX().padTop(50).row();

        // 组装左右面板
        root.add(leftPanel).padRight(20);
        root.add(rightPanel).top();
    }

    private void refreshRoomList() {
        statusLabel.setText("Status: 拉取房间列表中...");
        roomManager.fetchRooms(new RoomManager.FetchRoomsCallback() {
            @Override
            public void onSuccess(List<RoomModel> rooms) {
                Gdx.app.postRunnable(() -> {
                    roomsTable.clearChildren();
                    if (rooms == null || rooms.isEmpty()) {
                        roomsTable.add(new Label("No rooms found.", skin)).row();
                    } else {
                        for (RoomModel room : rooms) {
                            addRoomRow(room);
                        }
                    }
                    statusLabel.setText("Status: 列表已更新");
                });
            }

            @Override
            public void onError(Throwable t) {
                Gdx.app.postRunnable(() -> statusLabel.setText("Status: 拉取失败 " + t.getMessage()));
            }
        });
    }

    private void addRoomRow(final RoomModel room) {
        Table row = new Table();
        row.add(new Label(room.getRoom_name(), skin)).width(150);
        row.add(new Label(room.getPlayer_count() + "/" + room.getMax_players(), skin)).width(50).padLeft(10);
        
        TextButton joinBtn = new TextButton("Join", skin);
        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinRoom(room);
            }
        });
        row.add(joinBtn).padLeft(10);
        
        roomsTable.add(row).padBottom(5).row();
    }

    private void createAndJoinRoom() {
        statusLabel.setText("Status: 获取外网IP并创建房间...");
        String roomName = playerNameField.getText() + "'s Room";
        int myPort = 20000; // 你预设的 UDP 监听端口
        
        roomManager.createRoom(roomName, 6, myPort, new RoomManager.CreateRoomCallback() {
            @Override
            public void onSuccess(String roomId) {
                Gdx.app.postRunnable(() -> {
                    statusLabel.setText("Status: 建房成功, ID:" + roomId);
                    // 房间注册成功后，带着作为房主的标识跳转到联机对战屏幕中
                    // 注意在此要将你的联机游戏场景进行对应修改，能传入"是否Host"及"RoomManager"的引用以便退出时销毁房间
                    // game.setScreen(new NetcodeTankOnlineScreen(game, true, myPort, roomManager));
                });
            }

            @Override
            public void onError(Throwable t) {
                Gdx.app.postRunnable(() -> statusLabel.setText("Status: 建房失败 " + t.getMessage()));
            }
        });
    }

    private void joinRoom(RoomModel room) {
        statusLabel.setText("Status: 连接中 " + room.getHost_ip() + ":" + room.getHost_port());
        // 跳转到游戏屏幕，作为客户端开始连接指定的 IP 和端口
        // game.setScreen(new NetcodeTankOnlineScreen(game, false, room.getHost_ip(), room.getHost_port()));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (roomManager != null) roomManager.destroyRoom(); // 页面强退清理
    }
}
