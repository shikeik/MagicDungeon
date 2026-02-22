package com.goldsprite.magicdungeon2.screens.tests;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.magicdungeon2.network.lan.LanMultiplayerService;
import com.goldsprite.magicdungeon2.network.lan.LanNetworkEvent;
import com.goldsprite.magicdungeon2.network.lan.LanRoomPlayer;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

public class LanPlaygroundScreen extends ExampleGScreen {
    private Stage uiStage;
    private ShapeRenderer shapeRenderer;

    private final LanMultiplayerService lanService = new LanMultiplayerService();

    private VisTextField nameInput;
    private VisTextField ipInput;
    private VisTextField portInput;
    private VisTextField chatInput;

    private VisLabel statusLabel;
    private VisLabel playersLabel;
    private VisLabel logsLabel;

    private float localX;
    private float localY;
    private static final float MOVE_SPEED = 220f;

    private float roomRefreshTimer = 0f;
    private final ArrayDeque<String> logs = new ArrayDeque<>();
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.CHINA);
    private final Map<Integer, RemoteInterpState> remoteInterpStates = new HashMap<>();
    private static final long INTERP_DELAY_MS = 80L;

    @Override
    public String getIntroduction() {
        return "局域网联机测试：支持开房/加入、玩家位置同步、房间成员同步、聊天广播";
    }

    @Override
    public void create() {
        setDrawScreenBack(false);
        uiStage = new Stage(getUIViewport());
        getImp().addProcessor(uiStage);

        shapeRenderer = new ShapeRenderer();
        localX = getViewCenter().x;
        localY = getViewCenter().y;

        buildUi();
        appendLog("已进入联机测试屏");
    }

    private void buildUi() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.top().left().pad(16);
        uiStage.addActor(root);

        nameInput = new VisTextField("PlayerA");
        ipInput = new VisTextField("127.0.0.1");
        portInput = new VisTextField("34001");
        chatInput = new VisTextField("");

        VisTextButton hostBtn = new VisTextButton("启动房主");
        VisTextButton joinBtn = new VisTextButton("加入房间");
        VisTextButton stopBtn = new VisTextButton("断开联机");
        VisTextButton refreshBtn = new VisTextButton("刷新成员");
        VisTextButton chatBtn = new VisTextButton("发送聊天");

        statusLabel = new VisLabel("状态: 未连接");
        playersLabel = new VisLabel("成员: 0");
        playersLabel.setAlignment(Align.left);
        logsLabel = new VisLabel("日志:\n");
        logsLabel.setAlignment(Align.topLeft);

        hostBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    lanService.startHost(nameInput.getText().trim(), parsePort());
                    updateStatusLabel();
                } catch (Exception e) {
                    appendLog("启动房主失败: " + e.getMessage());
                }
            }
        });

        joinBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                try {
                    lanService.join(nameInput.getText().trim(), ipInput.getText().trim(), parsePort());
                    updateStatusLabel();
                } catch (Exception e) {
                    appendLog("加入房间失败: " + e.getMessage());
                }
            }
        });

        stopBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lanService.stop();
                updateStatusLabel();
            }
        });

        refreshBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                lanService.requestRoomPlayers();
            }
        });

        chatBtn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String text = chatInput.getText();
                lanService.sendChat(text);
                if (text != null && !text.trim().isEmpty()) {
                    appendLog("我: " + text.trim());
                }
                chatInput.setText("");
            }
        });

        root.add(new VisLabel("昵称")).left();
        root.add(nameInput).width(160).padRight(8);
        root.add(new VisLabel("房主IP")).left();
        root.add(ipInput).width(160).padRight(8);
        root.add(new VisLabel("端口")).left();
        root.add(portInput).width(90).padRight(8);
        root.row().padTop(8);

        root.add(hostBtn).left().padRight(6);
        root.add(joinBtn).left().padRight(6);
        root.add(stopBtn).left().padRight(6);
        root.add(refreshBtn).left().padRight(6);
        root.add(statusLabel).left().colspan(2).expandX().fillX();
        root.row().padTop(8);

        root.add(chatInput).left().colspan(4).expandX().fillX().padRight(6);
        root.add(chatBtn).left().colspan(2);
        root.row().padTop(8);

        root.add(playersLabel).left().colspan(6).expandX().fillX();
        root.row().padTop(8);
        root.add(logsLabel).left().top().colspan(6).expand().fill();
    }

    @Override
    public void render0(float delta) {
        long nowMs = System.currentTimeMillis();
        drawWorldBackground();

        handleInput(delta);
        consumeEvents();

        if (lanService.isConnected()) {
            roomRefreshTimer += delta;
            if (roomRefreshTimer >= 1.0f) {
                roomRefreshTimer = 0f;
                lanService.requestRoomPlayers();
            }
            lanService.sendLocalState(localX, localY, velocityX(), velocityY(), isMoving() ? "move" : "idle");
        }

        updateRemoteInterpStates(lanService.getRemotePlayers());
        drawPlayers(nowMs);
        updatePlayersLabel();
        updateStatusLabel();

        uiStage.act(delta);
        uiStage.draw();
    }

    private void drawWorldBackground() {
        shapeRenderer.setProjectionMatrix(getUIViewport().getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0.07f, 0.09f, 0.12f, 1f));
        shapeRenderer.rect(0, 0, getViewSize().x, getViewSize().y);
        shapeRenderer.end();
    }

    private void drawPlayers(long nowMs) {
        long renderTime = nowMs - INTERP_DELAY_MS;
        shapeRenderer.setProjectionMatrix(getUIViewport().getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        shapeRenderer.setColor(Color.valueOf("2ECC71"));
        shapeRenderer.circle(localX, localY, 12f);

        for (RemoteInterpState state : remoteInterpStates.values()) {
            float[] pos = state.interpolate(renderTime);
            float x = pos[0];
            float y = pos[1];
            shapeRenderer.setColor(Color.valueOf("F39C12"));
            shapeRenderer.circle(x, y, 10f);
        }

        shapeRenderer.end();
    }

    private void handleInput(float delta) {
        float dx = velocityX();
        float dy = velocityY();
        if (dx != 0f || dy != 0f) {
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 0f) {
                dx /= len;
                dy /= len;
            }
            localX += dx * MOVE_SPEED * delta;
            localY += dy * MOVE_SPEED * delta;

            localX = clamp(localX, 20f, getViewSize().x - 20f);
            localY = clamp(localY, 20f, getViewSize().y - 20f);
        }
    }

    private float velocityX() {
        float v = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.A)) v -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) v += 1f;
        return v;
    }

    private float velocityY() {
        float v = 0f;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) v -= 1f;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) v += 1f;
        return v;
    }

    private boolean isMoving() {
        return velocityX() != 0f || velocityY() != 0f;
    }

    private void consumeEvents() {
        List<LanNetworkEvent> events = lanService.drainEvents();
        for (LanNetworkEvent event : events) {
            appendLog(event.getMessage());
        }
    }

    private void updateStatusLabel() {
        statusLabel.setText(
            "状态: " + lanService.getMode() +
                (lanService.isConnected() ? " | 已连接" : " | 未连接") +
                " | guid=" + lanService.getLocalGuid()
        );
    }

    private void updatePlayersLabel() {
        StringBuilder sb = new StringBuilder();
        sb.append("成员: ").append(lanService.getRemotePlayerCount()).append(" | 操作: WASD移动");
        for (LanRoomPlayer player : lanService.getRemotePlayers()) {
            sb.append("\n#").append(player.getGuid())
                .append(" ").append(player.getName())
                .append(" @(")
                .append((int) player.getX()).append(",")
                .append((int) player.getY()).append(") ")
                .append(player.getAction());
        }
        playersLabel.setText(sb.toString());
    }

    private void appendLog(String msg) {
        String time = timeFormat.format(new Date());
        logs.addLast("[" + time + "] " + msg);
        while (logs.size() > 10) {
            logs.removeFirst();
        }
        StringBuilder sb = new StringBuilder("日志:\n");
        for (String line : logs) {
            sb.append(line).append('\n');
        }
        logsLabel.setText(sb.toString());
    }

    private void updateRemoteInterpStates(List<LanRoomPlayer> latest) {
        for (LanRoomPlayer player : latest) {
            RemoteInterpState state = remoteInterpStates.get(player.getGuid());
            if (state == null) {
                remoteInterpStates.put(player.getGuid(), new RemoteInterpState(player));
                continue;
            }
            if (player.getTimestamp() > state.getCurrentTimestamp()) {
                state.push(player);
            }
        }
    }

    private static class RemoteInterpState {
        private LanRoomPlayer previous;
        private LanRoomPlayer current;

        RemoteInterpState(LanRoomPlayer first) {
            this.previous = first;
            this.current = first;
        }

        void push(LanRoomPlayer next) {
            this.previous = this.current;
            this.current = next;
        }

        long getCurrentTimestamp() {
            return current == null ? 0L : current.getTimestamp();
        }

        float[] interpolate(long renderTime) {
            if (previous == null || current == null) return new float[]{0f, 0f};
            long prevTime = previous.getTimestamp();
            long currTime = current.getTimestamp();
            if (currTime <= prevTime) {
                return new float[]{current.getX(), current.getY()};
            }
            float t = (renderTime - prevTime) / (float) (currTime - prevTime);
            if (t < 0f) t = 0f;
            if (t > 1f) t = 1f;
            float x = previous.getX() + (current.getX() - previous.getX()) * t;
            float y = previous.getY() + (current.getY() - previous.getY()) * t;
            return new float[]{x, y};
        }
    }

    private int parsePort() {
        try {
            return Integer.parseInt(portInput.getText().trim());
        } catch (Exception ignored) {
            return 34001;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    @Override
    public void dispose() {
        lanService.stop();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (uiStage != null) uiStage.dispose();
    }
}
