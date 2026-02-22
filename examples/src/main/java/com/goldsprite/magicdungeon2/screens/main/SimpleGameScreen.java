package com.goldsprite.magicdungeon2.screens.main;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.assets.TextureManager;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.input.virtual.VirtualControlsOverlay;
import com.goldsprite.magicdungeon2.network.lan.LanMultiplayerService;
import com.goldsprite.magicdungeon2.network.lan.LanNetworkEvent;
import com.goldsprite.magicdungeon2.network.lan.LanRoomPlayer;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_H;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_W;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MP_REGEN_RATE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.STICK_DEADZONE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.TILE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_FLOOR;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_STAIRS;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_WALL;

/**
 * 简易地牢游戏场景
 * 小型网格地图 + 玩家/怪物 + 半即时制（冷却驱动）
 * <p>
 * 核心机制：
 * - 按住方向键持续移动，受 moveTimer 冷却限制
 * - 走向敌人格子自动攻击（Bump Attack）
 * - 敌人各自独立冷却，不等玩家行动
 * - 逻辑坐标(x,y)立即跳变，视觉坐标(visualX,visualY)平滑插值
 */
public class SimpleGameScreen extends GScreen implements GameRenderer.GameState {
	// 摇杆四向判定半角（度），默认45°，即每个方向占 90° 扇形（全覆盖）
	private static float stickHalfAngle = 45;

	/**
	 * 构造方法
	 * @param lanService 联机服务（可为 null 表示单人模式）
	 */
	public SimpleGameScreen(LanMultiplayerService lanService) {
		this.lanService = lanService;
	}

	private BitmapFont font, hudFont;

	private int[][] map;
	private GameEntity player;
	private Array<GameEntity> enemies = new Array<>();
	private Array<DamagePopup> popups = new Array<>();
	private String logText = "WASD移动 | 撞击攻击 | J魔法 | R重置";
	private float gameTime = 0;
	private int killCount = 0;

	// 死亡惩罚结果（非null时显示死亡覆盖层）
	private DeathPenalty.DeathResult deathResult;

	// 虚拟触控控件
	private VirtualControlsOverlay virtualControls;

	// 逻辑子系统
	private CombatHelper combatHelper;
	private GrowthHelper growthHelper;
	private EnemyAI enemyAI;

	// 渲染子系统
	private GameRenderer renderer;

	// 联机子系统（可为 null 表示单人模式）
	private LanMultiplayerService lanService;
	private Array<GameEntity> remotePlayers = new Array<>();
	private ConcurrentHashMap<Integer, GameEntity> remotePlayerMap = new ConcurrentHashMap<>();

	// ============ 公共访问方法（供自动测试读取状态） ============

	/** 获取玩家实体 */
	public GameEntity getPlayer() { return player; }

	/** 获取敌人列表 */
	public Array<GameEntity> getEnemies() { return enemies; }

	/** 获取远程玩家列表 */
	@Override public Array<GameEntity> getRemotePlayers() { return remotePlayers; }

	/** 获取地图数据 */
	public int[][] getMap() { return map; }

	/** 获取游戏时间（秒） */
	public float getGameTime() { return gameTime; }

	/** 获取击杀数 */
	public int getKillCount() { return killCount; }

	/** 获取日志文本 */
	public String getLogText() { return logText; }

	/** 获取飘字列表 */
	@Override public Array<DamagePopup> getPopups() { return popups; }

	@Override public boolean isShowLanMenu() { return false; }
	@Override public boolean isLanConnected() { return lanService != null && lanService.isConnected(); }
	@Override public String getLanStatus() { return lanService != null ? lanService.getMode().toString() : "单人"; }
	@Override public int getLanPlayerCount() { return lanService != null ? lanService.getRemotePlayerCount() + 1 : 1; }

	/** 获取死亡惩罚结果 */
	@Override public DeathPenalty.DeathResult getDeathResult() { return deathResult; }

	// ============ 向后兼容别名（过渡期后移除） ============
	/** @deprecated 使用 {@link GameEntity} 替代 */
	@Deprecated
	public static class Entity extends GameEntity {
		public Entity(int x, int y, String texName, float hp, float atk, float def,
					   float moveDelay, int xpReward, com.goldsprite.magicdungeon2.core.combat.WeaponRange weaponRange) {
			super(x, y, texName, hp, atk, def, moveDelay, xpReward, weaponRange);
		}
	}

	private float uiScl = 0.65f, worldScl = 0.6f;
	@Override
	protected void initViewport() {
		uiViewportScale = uiScl * (PlatformImpl.isDesktopUser()? 1: 0.9f);
		worldScale = worldScl * (PlatformImpl.isDesktopUser()? 1: 0.9f);
		super.initViewport();
	}

	@Override
	public void create() {
		// 使用 GScreen 提供的 batch、worldCamera、uiViewport，不再自建
		autoCenterWorldCamera = false; // 我们手动控制相机跟随玩家

		font = FontUtils.generate(10, 2);
		hudFont = FontUtils.generate(14, 3);

		TextureManager.init();
		buildMap();
		spawnEntities();
		initHelpers();

		// 初始化渲染器（在 font/hudFont/helpers 准备好之后）
		renderer = new GameRenderer(batch, font, hudFont,
			getUICamera(), getUIViewport(), this);

		// 创建虚拟触控覆盖层（Android 默认显示）
		virtualControls = new VirtualControlsOverlay(new ExtendViewport(
			getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight()));
		virtualControls.setStickHalfAngle(stickHalfAngle); // 同步角度判定
		if (imp != null) imp.addProcessor(virtualControls.getStage());
	}

//	@Override
//	protected void resizeWorldCamera(boolean centerCamera) {
//		if (worldCamera == null) return;
//		// 保持与旧版 ExtendViewport(400,400) 相同的世界视野
//		float aspect = (float) Gdx.graphics.getWidth() / Math.max(Gdx.graphics.getHeight(), 1);
//		if (aspect >= 1) {
//			worldCamera.viewportHeight = WORLD_VIEW_SIZE;
//			worldCamera.viewportWidth = WORLD_VIEW_SIZE * aspect;
//		} else {
//			worldCamera.viewportWidth = WORLD_VIEW_SIZE;
//			worldCamera.viewportHeight = WORLD_VIEW_SIZE / aspect;
//		}
//		worldCamera.update();
//	}

	private void buildMap() {
		map = new int[MAP_H][MAP_W];
		// 四周墙壁
		for (int y = 0; y < MAP_H; y++)
			for (int x = 0; x < MAP_W; x++)
				map[y][x] = (x == 0 || y == 0 || x == MAP_W - 1 || y == MAP_H - 1) ? T_WALL : T_FLOOR;

		// 中心附近放几堵墙增加趣味
		map[3][3] = T_WALL;
		map[5][5] = T_WALL;
		map[3][5] = T_WALL;
		map[5][3] = T_WALL;

		// 楼梯（右下角）
		map[1][MAP_W - 2] = T_STAIRS;
	}

	private void spawnEntities() {
		enemies.clear();
		killCount = 0;
		player = EnemyDefs.createPlayer();
		spawnEnemies();
	}

	/** 生成敌人（不重建玩家，保留进度） */
	private void spawnEnemies() {
		enemies.addAll(EnemyDefs.createDefaultEnemies());
	}

	/** 初始化逻辑子系统（在 player/map/enemies 准备好之后调用） */
	private void initHelpers() {
		// 成长系统
		growthHelper = new GrowthHelper(player, popups, text -> logText = text);
		// 战斗系统
		combatHelper = new CombatHelper(map, enemies, popups, player, new CombatHelper.CombatListener() {
			@Override
			public void onEnemyKilled(GameEntity enemy) {
				killCount++;
				growthHelper.onEnemyKilled(enemy, killCount);
			}
			@Override
			public void onPlayerDeath() {
				deathResult = growthHelper.handlePlayerDeath();
			}
			@Override
			public void onLogUpdate(String text) {
				logText = text;
			}
		});
		// 敌人AI
		enemyAI = new EnemyAI(map, enemies, player, combatHelper);
	}

	@Override
	public void render0(float delta) {
		ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1);

		// 更新虚拟触控控件
		if (virtualControls != null) virtualControls.update(delta);

		if (player.alive) {
			gameTime += delta;
			updatePlayer(delta);
			enemyAI.update(delta);
			// MP 自然回复
			player.mp = Math.min(player.getMaxMp(),
				player.mp + player.getMaxMp() * MP_REGEN_RATE * delta);
		} else {
			// 死亡后检测重生
			InputManager input = InputManager.getInstance();
			if (input.isJustPressed(InputAction.RESET_MAP)) {
				respawnAfterDeath();
			}
		}

		// 更新所有实体的视觉插值
		player.updateVisuals(delta);
		for (int i = 0; i < enemies.size; i++) {
			enemies.get(i).updateVisuals(delta);
		}
		updateLan(delta);
		renderer.updatePopups(delta);

		// 世界相机跟随玩家（使用视觉坐标更平滑）
		OrthographicCamera worldCam = getWorldCamera();
		worldCam.position.set(
			player.visualX + TILE * 0.5f,
			player.visualY + TILE * 0.5f, 0);
		worldCam.update();

		// 世界渲染（地图+实体+飘字）
		batch.setProjectionMatrix(worldCam.combined);
		batch.begin();
		renderer.drawWorld();
		batch.end();

		// HUD 渲染（使用 UI 视口）
		renderer.drawHUD();

		// 死亡覆盖层
		if (!player.alive && deathResult != null) {
			renderer.drawDeathOverlay();
		}

		// 渲染虚拟触控控件（在 HUD 之上）
		if (virtualControls != null) virtualControls.render();

		// HUD 上显示联机状态提示
		if (lanService != null && lanService.isConnected()) {
			batch.setProjectionMatrix(getUICamera().combined);
			batch.begin();
			hudFont.setColor(com.badlogic.gdx.graphics.Color.CYAN);
			hudFont.draw(batch, "联机中: " + lanService.getMode() + " | 玩家: " + (lanService.getRemotePlayerCount() + 1),
				10, getUIViewport().getWorldHeight() - 90);
			hudFont.setColor(com.badlogic.gdx.graphics.Color.WHITE);
			batch.end();
		}
	}

	// ============ 联机逻辑 ============
	private void updateLan(float delta) {
		if (lanService == null || !lanService.isConnected()) return;

		// 处理网络事件
		List<LanNetworkEvent> events = lanService.drainEvents();
		for (LanNetworkEvent e : events) {
			if (e.getType() == LanNetworkEvent.Type.CHAT) {
				logText = "[联机] " + e.getMessage();
			} else if (e.getType() == LanNetworkEvent.Type.INFO) {
				logText = "[系统] " + e.getMessage();
			}
		}

		// 发送本地状态
		if (player.alive) {
			lanService.sendLocalState(player.x, player.y, player.visualX, player.visualY, "idle");
		}

		// 更新远程玩家
		List<LanRoomPlayer> lanPlayers = lanService.getRemotePlayers();
		remotePlayers.clear();
		for (LanRoomPlayer lp : lanPlayers) {
			GameEntity re = remotePlayerMap.get(lp.getGuid());
			if (re == null) {
				re = new GameEntity((int)lp.getX(), (int)lp.getY(), "player", 100, 10, 5, 0.2f, 0, com.goldsprite.magicdungeon2.core.combat.WeaponRange.MELEE);
				remotePlayerMap.put(lp.getGuid(), re);
			}
			// 更新逻辑坐标和视觉坐标
			re.x = (int)lp.getX();
			re.y = (int)lp.getY();
			// 简单插值
			re.visualX += (lp.getVx() - re.visualX) * 10f * delta;
			re.visualY += (lp.getVy() - re.visualY) * 10f * delta;
			remotePlayers.add(re);
		}
	}

	// ============ 半即时制核心逻辑 ============

	/** 更新玩家（冷却驱动） */
	private void updatePlayer(float delta) {
		player.moveTimer -= delta;
		if (player.moveTimer > 0) return; // 冷却中，忽略输入

		InputManager input = InputManager.getInstance();

		// 魔法攻击检测 (J键 / ATTACK)
		if (input.isJustPressed(InputAction.ATTACK)) {int k;
			if (combatHelper.performMagicAttack()) {
				player.moveTimer = player.getAttackCooldown();
			} else {
				player.moveTimer = player.getAttackCooldown() * GameConfig.MP_FAIL_CD_FACTOR;
			}
			return;
		}

		// 统一通过 getAxis 读取所有输入源（键盘WASD + 手柄 + 虚拟摇杆）
		// 不使用 isPressed(MOVE_*) 因为其内部 isAxisMappedAction 是简单阈值判定,
		// 会绕过角度扇区检测导致表现与数据不一致
		int dx = 0, dy = 0;

		Vector2 axis = input.getAxis(InputManager.AXIS_LEFT);
		if (axis.len() >= STICK_DEADZONE) {
			// 角度判定：0°=右, 90°=上, 180°=左, 270°=下
			float angle = (float) Math.toDegrees(Math.atan2(axis.y, axis.x));
			if (angle < 0) angle += 360; // 归一化到0~360
			if (angle >= 90 - stickHalfAngle && angle < 90 + stickHalfAngle) dy = 1;       // 上
			else if (angle >= 270 - stickHalfAngle && angle < 270 + stickHalfAngle) dy = -1; // 下
			else if (angle >= 180 - stickHalfAngle && angle < 180 + stickHalfAngle) dx = -1; // 左
			else if (angle < stickHalfAngle || angle >= 360 - stickHalfAngle) dx = 1;       // 右
		}

		if (dx == 0 && dy == 0) return;

		// 更新面朝方向
		player.faceDx = dx;
		player.faceDy = dy;

		// 尝试武器范围攻击（Bump式物理攻击）
		if (combatHelper.performRangedAttack(player, dx, dy)) {
			player.moveTimer = player.getAttackCooldown();
			return;
		}

		// 无目标，尝试移动
		int nx = player.x + dx, ny = player.y + dy;
		if (nx < 0 || ny < 0 || nx >= MAP_W || ny >= MAP_H || map[ny][nx] == T_WALL) {
			player.moveTimer = player.getMoveCooldown(); // 撞墙/边界也消耗冷却
			return;
		}

		// 移动
		player.x = nx;
		player.y = ny;
		player.moveTimer = player.getMoveCooldown();

		// 踩到楼梯 — 使用无等待渐变转场（保留玩家进度）
		if (map[ny][nx] == T_STAIRS) {
			logText = "踏上楼梯... 前往下一层！";
			getScreenManager().playOverlayFade(() -> {
				buildMap();
				player.x = 4; player.y = 4;
				player.visualX = player.x * TILE;
				player.visualY = player.y * TILE;
				enemies.clear();
				spawnEnemies();
				killCount = 0;
			}, 0.6f);
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (virtualControls != null) virtualControls.resize(width, height);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (virtualControls != null) virtualControls.dispose();
		if (lanService != null) {
			lanService.stop();
			lanService = null;
		}
	}

	/** 死亡后重生 */
	private void respawnAfterDeath() {
		buildMap();
		growthHelper.resetPlayerForRespawn();
		enemies.clear();
		spawnEnemies();
		killCount = 0;
		deathResult = null;
		gameTime = 0;
	}

}
