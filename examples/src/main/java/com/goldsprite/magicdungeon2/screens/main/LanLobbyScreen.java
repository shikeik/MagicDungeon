package com.goldsprite.magicdungeon2.screens.main;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.network.lan.LanMultiplayerService;
import com.goldsprite.magicdungeon2.network.lan.LanNetworkEvent;
import com.goldsprite.magicdungeon2.network.lan.LanRoomPlayer;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;

/**
 * 局域网联机大厅
 * <p>
 * 三阶段面板：
 * 1. 未连接 — 显示昵称/IP/端口 + 创建房间/加入房间
 * 2. 房主等待 — 显示成员列表 + 开始游戏/断开连接
 * 3. 客户端等待 — 显示成员列表 + 等待房主开始/断开连接
 */
public class LanLobbyScreen extends GScreen {

	private enum Phase { IDLE, HOST_WAITING, CLIENT_WAITING }

	private LanMultiplayerService lanService;
	private Phase phase = Phase.IDLE;
	private boolean startingGame = false; // 防止转场期间重复操作

	// 根容器
	private VisTable root;

	// --- 未连接面板 ---
	private VisTable idlePanel;
	private VisTextField nameInput;
	private VisTextField ipInput;
	private VisTextField portInput;

	// --- 房间面板（房主/客户端共用部分） ---
	private VisTable roomPanel;
	private VisLabel statusLabel;
	private VisLabel playersLabel;
	private VisTextButton startBtn;  // 仅房主可见
	private VisTextButton stopBtn;

	// --- 日志 ---
	private VisLabel logsLabel;
	private StringBuilder logBuffer = new StringBuilder();
	private static final int MAX_LOG_LINES = 20;

	// 房间刷新计时器
	private float refreshTimer = 0f;
	// 连接宽限期（秒）：切换到 HOST/CLIENT 阶段后，等待异步登录完成，期间不检测断线
	private float connectionGraceTimer = 0f;
	private static final float CONNECTION_GRACE_SECONDS = 5f;

	@Override
	public void create() {
		lanService = new LanMultiplayerService();
		buildUI();
		if (imp != null) imp.addProcessor(stage);
		showPhase(Phase.IDLE);
		appendLog("欢迎来到联机大厅！请创建或加入房间。");
	}

	private void buildUI() {
		root = new VisTable();
		root.setFillParent(true);
		root.pad(20);
		root.top();
		stage.addActor(root);

		// 标题
		VisLabel title = new VisLabel("局 域 网 联 机 大 厅");
		title.setColor(Color.CYAN);
		root.add(title).colspan(6).center().padBottom(16);
		root.row();

		// ============ 未连接面板 ============
		idlePanel = new VisTable();
		buildIdlePanel();

		// ============ 房间面板（连接后） ============
		roomPanel = new VisTable();
		buildRoomPanel();

		// ============ 日志区域（公共） ============
		VisLabel logTitle = new VisLabel("--- 日志 ---");
		logTitle.setColor(Color.GRAY);

		logsLabel = new VisLabel("");
		logsLabel.setAlignment(Align.topLeft);
		logsLabel.setWrap(true);
		VisScrollPane scrollPane = new VisScrollPane(logsLabel);
		scrollPane.setFlickScroll(false);
		scrollPane.setScrollingDisabled(true, false);

		// 动态面板占位（由 showPhase 切换）
		root.add(idlePanel).colspan(6).expandX().fillX().left();
		root.row().padTop(8);
		root.add(roomPanel).colspan(6).expandX().fillX().left();
		root.row().padTop(12);
		root.add(logTitle).colspan(6).left();
		root.row().padTop(4);
		root.add(scrollPane).colspan(6).expand().fill().left();
		root.row().padTop(12);

		// 返回按钮
		VisTextButton backBtn = new VisTextButton("返回");
		backBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (lanService != null) lanService.stop();
				getScreenManager().popLastScreen();
			}
		});
		root.add(backBtn).colspan(6).left();
	}

	private void buildIdlePanel() {
		// 昵称
		nameInput = new VisTextField("Player_" + (int)(Math.random() * 1000));
		idlePanel.add(new VisLabel("昵称:")).left().padRight(4);
		idlePanel.add(nameInput).width(140).padRight(12);

		// 房主IP
		ipInput = new VisTextField("127.0.0.1");
		idlePanel.add(new VisLabel("房主IP:")).left().padRight(4);
		idlePanel.add(ipInput).width(140).padRight(12);

		// 端口
		portInput = new VisTextField("25565");
		idlePanel.add(new VisLabel("端口:")).left().padRight(4);
		idlePanel.add(portInput).width(80);
		idlePanel.row().padTop(12);

		// 操作按钮
		VisTextButton hostBtn = new VisTextButton("创建房间");
		VisTextButton joinBtn = new VisTextButton("加入房间");

		hostBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					int port = Integer.parseInt(portInput.getText().trim());
					lanService.startHost(nameInput.getText().trim(), port);
					appendLog("正在创建房间...");
					showPhase(Phase.HOST_WAITING);
				} catch (Exception e) {
					appendLog("创建失败: " + e.getMessage());
				}
			}
		});

		joinBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				try {
					int port = Integer.parseInt(portInput.getText().trim());
					lanService.join(nameInput.getText().trim(), ipInput.getText().trim(), port);
					appendLog("正在加入房间...");
					showPhase(Phase.CLIENT_WAITING);
				} catch (Exception e) {
					appendLog("加入失败: " + e.getMessage());
				}
			}
		});

		idlePanel.add(hostBtn).left().padRight(8);
		idlePanel.add(joinBtn).left().colspan(5);
	}

	private void buildRoomPanel() {
		statusLabel = new VisLabel("状态: 连接中...");
		statusLabel.setColor(Color.GREEN);
		roomPanel.add(statusLabel).colspan(6).left();
		roomPanel.row().padTop(8);

		playersLabel = new VisLabel("房间成员:\n  等待中...");
		playersLabel.setAlignment(Align.topLeft);
		roomPanel.add(playersLabel).colspan(6).left().expandX().fillX();
		roomPanel.row().padTop(12);

		// 操作按钮行
		startBtn = new VisTextButton("开始游戏!");
		startBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (startingGame) return;
				if (lanService == null || !lanService.isConnected()) {
					appendLog("连接已断开，无法开始！");
					return;
				}
				startGame();
			}
		});

		stopBtn = new VisTextButton("断开连接");
		stopBtn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				if (lanService != null) lanService.stop();
				appendLog("已断开连接");
				showPhase(Phase.IDLE);
			}
		});

		roomPanel.add(startBtn).left().padRight(8);
		roomPanel.add(stopBtn).left().colspan(5);
	}

	/** 切换显示阶段 */
	private void showPhase(Phase newPhase) {
		this.phase = newPhase;
		switch (newPhase) {
			case IDLE:
				idlePanel.setVisible(true);
				roomPanel.setVisible(false);
				connectionGraceTimer = 0f;
				break;
			case HOST_WAITING:
				idlePanel.setVisible(false);
				roomPanel.setVisible(true);
				startBtn.setVisible(true);  // 房主可见
				connectionGraceTimer = CONNECTION_GRACE_SECONDS; // 给异步登录留宽限期
				break;
			case CLIENT_WAITING:
				idlePanel.setVisible(false);
				roomPanel.setVisible(true);
				startBtn.setVisible(false); // 客户端隐藏
				connectionGraceTimer = CONNECTION_GRACE_SECONDS; // 给异步登录留宽限期
				break;
		}
	}

	@Override
	public void render0(float delta) {
		ScreenUtils.clear(0.06f, 0.07f, 0.1f, 1);

		// 转场中不做网络操作
		if (!startingGame && lanService != null) {
			consumeEvents();

			// consumeEvents() 内部可能触发 startGame()，导致 lanService 被置 null，需要重新检查
			if (startingGame || lanService == null) {
				// 已进入转场流程，跳过后续网络操作
			} else {
				// 定期刷新房间成员
				if (lanService.isConnected()) {
					refreshTimer += delta;
					if (refreshTimer >= 1.5f) {
						refreshTimer = 0f;
						lanService.requestRoomPlayers();
					}
				}

				updateRoomUI();

				// 宽限期倒计时（等待异步登录回调完成）
				if (connectionGraceTimer > 0f) {
					connectionGraceTimer -= delta;
				}

				// 如果在房间阶段但连接断开了，且宽限期已过，自动回到未连接面板
				if (phase != Phase.IDLE && !lanService.isConnected() && connectionGraceTimer <= 0f) {
					appendLog("连接已断开");
					showPhase(Phase.IDLE);
				}
			}
		}

		stage.act(delta);
		stage.draw();
	}

	private void consumeEvents() {
		if (lanService == null) return;
		List<LanNetworkEvent> events = lanService.drainEvents();
		for (LanNetworkEvent e : events) {
			appendLog("[" + e.getType().name() + "] " + e.getMessage());

			// 客户端收到"开始游戏"广播，自动进入游戏
			if (e.getType() == LanNetworkEvent.Type.GAME_START
				&& phase == Phase.CLIENT_WAITING && !startingGame) {
				startGame();
				return; // 已进入转场，不再处理后续事件
			}
		}
	}

	private void updateRoomUI() {
		if (lanService == null || !lanService.isConnected()) return;

		String modeText = phase == Phase.HOST_WAITING ? "房主" : "成员";
		statusLabel.setText("身份: " + modeText
			+ " | guid=" + lanService.getLocalGuid()
			+ " | 玩家数: " + (lanService.getRemotePlayerCount() + 1));

		List<LanRoomPlayer> players = lanService.getRemotePlayers();
		StringBuilder sb = new StringBuilder("房间成员:\n");
		sb.append("  [我] ").append(lanService.getLocalName())
			.append(" (guid=").append(lanService.getLocalGuid()).append(")\n");
		for (LanRoomPlayer p : players) {
			sb.append("  ").append(p.getName())
				.append(" (guid=").append(p.getGuid())
				.append(")\n");
		}
		if (phase == Phase.CLIENT_WAITING) {
			sb.append("\n等待房主开始游戏...");
		}
		playersLabel.setText(sb.toString());
	}

	/** 开始游戏 — 资源已在进入大厅时预加载，此处仅做渐变转场 */
	private void startGame() {
		startingGame = true;
		appendLog("正在开始游戏...");

		// 房主需要先广播"开始游戏"信号给所有客户端
		if (phase == Phase.HOST_WAITING && lanService != null) {
			lanService.broadcastGameStart();
		}

		final LanMultiplayerService service = this.lanService;
		this.lanService = null; // 移交所有权

		// 用轻量渐变转场（资源已预加载，不需要再走 playLoadingTransition）
		getScreenManager().playOverlayFade(() -> {
			SimpleGameScreen gameScreen = new SimpleGameScreen(service);
			getScreenManager().goScreen(gameScreen);
		}, 0.5f);
	}

	private void appendLog(String msg) {
		logBuffer.append(msg).append("\n");
		String[] lines = logBuffer.toString().split("\n");
		if (lines.length > MAX_LOG_LINES) {
			logBuffer = new StringBuilder();
			for (int i = lines.length - MAX_LOG_LINES; i < lines.length; i++) {
				logBuffer.append(lines[i]).append("\n");
			}
		}
		if (logsLabel != null) {
			logsLabel.setText(logBuffer.toString());
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (lanService != null) {
			lanService.stop();
		}
	}
}
