package com.goldsprite.magicdungeon2.screens.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import com.goldsprite.gdengine.log.DLog;

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
import com.goldsprite.magicdungeon2.network.lan.packet.EnemyStateSnapshot;
import com.goldsprite.magicdungeon2.network.lan.packet.LanAttackRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanDamageResultBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerHurtBroadcastPacket;
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
	 * @param mapSeed 地图种子（联机时由房主生成，所有端用同一种子确保地图一致）
	 */
	public SimpleGameScreen(LanMultiplayerService lanService, long mapSeed) {
		this.lanService = lanService;
		this.mapSeed = mapSeed;
	}

	/** 向后兼容的单参数构造（单人模式使用随机种子） */
	public SimpleGameScreen(LanMultiplayerService lanService) {
		this(lanService, ThreadLocalRandom.current().nextLong());
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

	// Phase 1: 共享地图种子
	private long mapSeed;
	private int currentFloor = 1;

	// Phase 2: 敌人ID分配（房主递增分配）
	private int nextEnemyId = 1;
	// Phase 2: 客户端用 — 网络敌人缓存（从房主广播接收）
	private ConcurrentHashMap<Integer, GameEntity> networkEnemyMap = new ConcurrentHashMap<>();
	private Array<GameEntity> networkEnemies = new Array<>(); // 供渲染器读取

	// ============ 公共访问方法（供自动测试读取状态） ============

	/** 获取玩家实体 */
	public GameEntity getPlayer() { return player; }

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
		buildMap(mapSeed);
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

	/**
	 * 种子驱动的地图生成
	 * 所有使用相同种子的端会生成完全一致的地图布局
	 */
	private void buildMap(long seed) {
		Random rng = new Random(seed);
		map = new int[MAP_H][MAP_W];

		// 四周墙壁
		for (int y = 0; y < MAP_H; y++)
			for (int x = 0; x < MAP_W; x++)
				map[y][x] = (x == 0 || y == 0 || x == MAP_W - 1 || y == MAP_H - 1) ? T_WALL : T_FLOOR;

		// 随机内部墙壁（4~8块）
		int wallCount = 4 + rng.nextInt(5);
		for (int i = 0; i < wallCount; i++) {
			int wx = 2 + rng.nextInt(MAP_W - 4);
			int wy = 2 + rng.nextInt(MAP_H - 4);
			// 不阻塞玩家出生点(4,4)
			if (wx == 4 && wy == 4) continue;
			map[wy][wx] = T_WALL;
		}

		// 随机楼梯位置（不在墙壁、不在出生点）
		int sx, sy;
		do {
			sx = 1 + rng.nextInt(MAP_W - 2);
			sy = 1 + rng.nextInt(MAP_H - 2);
		} while (map[sy][sx] != T_FLOOR || (sx == 4 && sy == 4));
		map[sy][sx] = T_STAIRS;
	}

	private void spawnEntities() {
		enemies.clear();
		killCount = 0;
		player = EnemyDefs.createPlayer();
		// 联机客户端不本地生成敌人（由房主广播）
		if (!isClient()) {
			spawnEnemies();
		}
	}

	/** 生成敌人（不重建玩家，保留进度） */
	private void spawnEnemies() {
		Array<GameEntity> newEnemies = EnemyDefs.createDefaultEnemies();
		// 为每个敌人分配唯一ID
		for (int i = 0; i < newEnemies.size; i++) {
			newEnemies.get(i).enemyId = nextEnemyId++;
		}
		enemies.addAll(newEnemies);
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
			// 只有房主（或单人模式）运行敌人AI
			if (!isClient()) {
				enemyAI.update(delta);
			}
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

	/** 是否为联机客户端（非房主） */
	private boolean isClient() {
		return lanService != null && lanService.getMode() == LanMultiplayerService.Mode.CLIENT;
	}

	/** 是否为联机房主 */
	private boolean isHost() {
		return lanService != null && lanService.getMode() == LanMultiplayerService.Mode.HOST;
	}

	/** 是否处于联机模式 */
	private boolean isMultiplayer() {
		return lanService != null && lanService.isConnected();
	}

	/** 获取渲染器应使用的敌人列表（客户端用网络敌人，房主/单人用本地敌人） */
	@Override
	public Array<GameEntity> getEnemies() {
		if (isClient()) return networkEnemies;
		return enemies;
	}

	private static final String LAN_TAG = "LAN";

	private void updateLan(float delta) {
		if (lanService == null || !lanService.isConnected()) {
			DLog.infoT(LAN_TAG, "updateLan跳过: service=%s connected=%s",
				lanService == null ? "null" : "ok",
				lanService != null ? lanService.isConnected() : "N/A");
			return;
		}

		// DLog: 每帧显示联机状态概要
		DLog.infoT(LAN_TAG, "模式=%s guid=%d 远程玩家=%d 本地pos=(%d,%d) vis=(%.0f,%.0f) HP=%.0f/%.0f Lv%d",
			lanService.getMode(), lanService.getLocalGuid(),
			lanService.getRemotePlayerCount(),
			player.x, player.y, player.visualX, player.visualY,
			player.hp, player.getMaxHp(), player.stats.getLevel());

		// 1. 处理网络事件（换层、消息等）
		List<LanNetworkEvent> events = lanService.drainEvents();
		for (LanNetworkEvent e : events) {
			if (e.getType() == LanNetworkEvent.Type.CHAT) {
				logText = "[联机] " + e.getMessage();
			} else if (e.getType() == LanNetworkEvent.Type.INFO) {
				logText = "[系统] " + e.getMessage();
			} else if (e.getType() == LanNetworkEvent.Type.FLOOR_CHANGE) {
				// 客户端收到换层信号
				handleFloorChange(e.getMapSeed(), e.getFloor());
			}
		}

		// 2. 发送本地状态（含属性信息 — Phase 3/4）
		if (player.alive) {
			// Phase 3: 动作状态可见（代替硬编码的 "idle"）
			String action = player.moveTimer > 0 ? "walk" : "idle";
			lanService.sendLocalState(
				player.x, player.y, player.visualX, player.visualY, action,
				player.hp, player.getMaxHp(), player.stats.getLevel(),
				player.stats.getATK(), player.stats.getDEF());
		}

		// 3. 房主专属：广播敌人状态 + 处理客户端攻击请求
		if (isHost()) {
			updateLanHost(delta);
		}

		// 4. 客户端专属：接收敌人状态
		if (isClient()) {
			updateLanClientEnemies(delta);
		}

		// 5. 处理伤害结果广播（房主和客户端都需要显示飘字）
		List<LanDamageResultBroadcastPacket> damageResults = lanService.drainDamageResults();
		for (LanDamageResultBroadcastPacket dr : damageResults) {
			// 客户端：从网络敌人中查找位置用于飘字
			GameEntity targetEnemy = findEnemyById(dr.getEnemyId());
			if (targetEnemy != null) {
				com.badlogic.gdx.graphics.Color popColor = dr.isKilled() ? com.badlogic.gdx.graphics.Color.GOLD : com.badlogic.gdx.graphics.Color.YELLOW;
				popups.add(new DamagePopup(
					targetEnemy.visualX + TILE * 0.5f,
					targetEnemy.visualY + TILE,
					String.format("-%.0f", dr.getDamage()), popColor));
			}
			// 客户端：如果自己是攻击者且敌人被杀，获得经验
			if (dr.isKilled() && dr.getAttackerGuid() == lanService.getLocalGuid()) {
				killCount++;
				// 模拟击杀奖励（用 xpReward）
				player.totalXp += dr.getXpReward();
				player.gold += (int)(dr.getXpReward() * GameConfig.GOLD_XP_RATIO);
				growthHelper.onEnemyKilledByNetwork(dr.getXpReward(), killCount);
			}
		}

		// 6. 处理玩家受伤广播
		List<LanPlayerHurtBroadcastPacket> playerHurts = lanService.drainPlayerHurts();
		for (LanPlayerHurtBroadcastPacket ph : playerHurts) {
			if (ph.getTargetGuid() == lanService.getLocalGuid()) {
				// 自己被攻击 — 应用伤害
				player.hp = ph.getRemainHp();
				popups.add(new DamagePopup(
					player.visualX + TILE * 0.5f,
					player.visualY + TILE * 1.2f,
					String.format("-%.0f", ph.getDamage()), com.badlogic.gdx.graphics.Color.RED));
				if (player.hp <= 0) {
					deathResult = growthHelper.handlePlayerDeath();
				}
			}
		}

		// 7. 更新远程玩家位置（所有端）
		updateRemotePlayers(delta);
	}

	/** 房主：广播敌人状态 + 处理远程玩家攻击请求 */
	private void updateLanHost(float delta) {
		// 广播敌人状态给客户端
		ArrayList<EnemyStateSnapshot> snapshots = new ArrayList<>();
		for (int i = 0; i < enemies.size; i++) {
			GameEntity e = enemies.get(i);
			snapshots.add(new EnemyStateSnapshot(
				e.enemyId, e.texName, e.x, e.y, e.visualX, e.visualY,
				e.hp, e.getMaxHp(), e.alive, "idle", System.currentTimeMillis()));
		}
		lanService.broadcastEnemyStates(snapshots);

		// 处理远程客户端的攻击请求
		List<LanAttackRequestPacket> attacks = lanService.drainAttackRequests();
		for (LanAttackRequestPacket req : attacks) {
			processRemoteAttack(req);
		}

		// 检查敌人是否攻击了远程玩家（简化版：检查敌人和远程玩家相邻）
		checkEnemyAttackRemotePlayers();
	}

	/** 房主处理远程客户端的攻击请求 */
	private void processRemoteAttack(LanAttackRequestPacket req) {
		int ax = (int)req.getX(), ay = (int)req.getY();
		int dx = req.getDx(), dy = req.getDy();
		float atk = req.getAtk();

		if ("magic".equals(req.getAttackType())) {
			// 魔法攻击：沿方向扫描
			for (int r = 1; r <= com.goldsprite.magicdungeon2.core.combat.WeaponRange.ENERGY.range; r++) {
				int cx = ax + dx * r, cy = ay + dy * r;
				if (cx < 0 || cy < 0 || cx >= MAP_W || cy >= MAP_H || map[cy][cx] == T_WALL) break;
				GameEntity target = combatHelper.findEnemy(cx, cy);
				if (target != null) {
					float dmg = Math.max(1, com.goldsprite.magicdungeon2.core.combat.CombatEngine.calcMagicDamage(
						atk, target.stats.getMDEF()));
					target.hp -= dmg;
					boolean killed = target.hp <= 0;
					if (killed) {
						target.alive = false;
						enemies.removeValue(target, true);
					}
					lanService.broadcastDamageResult(target.enemyId, dmg, target.hp,
						killed, req.getOwnerGuid(), target.xpReward);
					break;
				}
			}
		} else {
			// 物理攻击：沿方向检查（默认 MELEE 射程1）
			int cx = ax + dx, cy = ay + dy;
			if (cx >= 0 && cy >= 0 && cx < MAP_W && cy < MAP_H) {
				GameEntity target = combatHelper.findEnemy(cx, cy);
				if (target != null) {
					float dmg = Math.max(1, com.goldsprite.magicdungeon2.core.combat.CombatEngine.calcPhysicalDamage(
						atk, target.stats.getDEF()));
					target.hp -= dmg;
					boolean killed = target.hp <= 0;
					if (killed) {
						target.alive = false;
						enemies.removeValue(target, true);
					}
					lanService.broadcastDamageResult(target.enemyId, dmg, target.hp,
						killed, req.getOwnerGuid(), target.xpReward);
				}
			}
		}
	}

	/** 房主检查敌人是否攻击了远程玩家（简化：仅检查相邻） */
	private void checkEnemyAttackRemotePlayers() {
		List<LanRoomPlayer> lanPlayers = lanService.getRemotePlayers();
		for (int i = enemies.size - 1; i >= 0; i--) {
			GameEntity e = enemies.get(i);
			if (!e.alive || e.moveTimer > 0) continue;
			for (LanRoomPlayer rp : lanPlayers) {
				int rpx = (int) rp.getX(), rpy = (int) rp.getY();
				float dist = Math.abs(rpx - e.x) + Math.abs(rpy - e.y);
				if (dist <= 1 && dist > 0) {
					// 敌人相邻远程玩家 — 计算伤害
					float dmg = Math.max(1, com.goldsprite.magicdungeon2.core.combat.CombatEngine.calcPhysicalDamage(
						e.stats.getATK(), rp.getDef()));
					float remainHp = rp.getHp() - dmg;
					lanService.broadcastPlayerHurt(rp.getGuid(), dmg, remainHp, e.enemyId);
					e.moveTimer = e.getAttackCooldown(); // 敌人进入攻击冷却
					break;
				}
			}
		}
	}

	/** 客户端：从房主广播更新网络敌人列表 */
	private void updateLanClientEnemies(float delta) {
		List<EnemyStateSnapshot> states = lanService.getLatestEnemyStates();
		networkEnemies.clear();
		for (EnemyStateSnapshot es : states) {
			if (!es.isAlive()) continue;
			GameEntity cached = networkEnemyMap.get(es.getEnemyId());
			if (cached == null) {
				cached = new GameEntity(es.getX(), es.getY(), es.getEnemyType(),
					es.getMaxHp(), 0, 0, 0.5f, 0, com.goldsprite.magicdungeon2.core.combat.WeaponRange.MELEE);
				cached.enemyId = es.getEnemyId();
				networkEnemyMap.put(es.getEnemyId(), cached);
			}
			// 更新状态
			cached.x = es.getX();
			cached.y = es.getY();
			cached.hp = es.getHp();
			cached.alive = es.isAlive();
			// 平滑插值视觉坐标
			cached.visualX += (es.getVisualX() - cached.visualX) * 10f * delta;
			cached.visualY += (es.getVisualY() - cached.visualY) * 10f * delta;
			networkEnemies.add(cached);
		}
		// 清理已死亡的缓存
		networkEnemyMap.entrySet().removeIf(entry -> !entry.getValue().alive);
	}

	/** 更新远程玩家位置（房主和客户端通用） */
	private void updateRemotePlayers(float delta) {
		List<LanRoomPlayer> lanPlayers = lanService.getRemotePlayers();
		remotePlayers.clear();

		DLog.infoT(LAN_TAG, "远程玩家数据: lanPlayers.size=%d remotePlayerMap.size=%d",
			lanPlayers.size(), remotePlayerMap.size());

		for (LanRoomPlayer lp : lanPlayers) {
			// DLog: 显示每个远程玩家的原始数据
			DLog.infoT(LAN_TAG, "  远程[guid=%d] raw: x=%.1f y=%.1f vx=%.1f vy=%.1f hp=%.0f lv=%d",
				lp.getGuid(), lp.getX(), lp.getY(), lp.getVx(), lp.getVy(), lp.getHp(), lp.getLevel());

			if (lp.getX() == 0 && lp.getY() == 0 && lp.getVx() == 0 && lp.getVy() == 0) {
				DLog.infoT(LAN_TAG, "  远程[guid=%d] 跳过: 全零位置", lp.getGuid());
				continue;
			}

			GameEntity re = remotePlayerMap.get(lp.getGuid());
			if (re == null) {
				int spawnX = Math.max(1, (int)lp.getX());
				int spawnY = Math.max(1, (int)lp.getY());
				re = new GameEntity(spawnX, spawnY, "player", 100, 10, 5, 0.2f, 0,
					com.goldsprite.magicdungeon2.core.combat.WeaponRange.MELEE);
				re.visualX = lp.getVx();
				re.visualY = lp.getVy();
				remotePlayerMap.put(lp.getGuid(), re);
				DLog.logT(LAN_TAG, "新建远程玩家实体: guid=%d pos=(%d,%d)", lp.getGuid(), spawnX, spawnY);
			}
			re.x = (int)lp.getX();
			re.y = (int)lp.getY();
			re.visualX += (lp.getVx() - re.visualX) * 10f * delta;
			re.visualY += (lp.getVy() - re.visualY) * 10f * delta;
			// Phase 4: 同步血量和等级
			re.hp = lp.getHp();
			if (lp.getMaxHp() > 0) {
				re.stats.setLevel(lp.getLevel());
			}
			remotePlayers.add(re);
		}

		DLog.infoT(LAN_TAG, "最终远程玩家渲染数: %d", remotePlayers.size);
	}

	/** 通过 enemyId 查找敌人（优先本地，回退到网络缓存） */
	private GameEntity findEnemyById(int enemyId) {
		for (int i = 0; i < enemies.size; i++) {
			if (enemies.get(i).enemyId == enemyId) return enemies.get(i);
		}
		return networkEnemyMap.get(enemyId);
	}

	/** 在网络敌人列表中查找指定网格位置的敌人（客户端物理攻击用） */
	private GameEntity findNetworkEnemyAt(int x, int y) {
		for (GameEntity e : networkEnemies) {
			if (e.alive && e.x == x && e.y == y) return e;
		}
		return null;
	}

	/** 客户端收到换层信号时的处理 */
	private void handleFloorChange(long newSeed, int floor) {
		this.mapSeed = newSeed;
		this.currentFloor = floor;
		getScreenManager().playOverlayFade(() -> {
			buildMap(newSeed);
			player.x = 4; player.y = 4;
			player.visualX = player.x * TILE;
			player.visualY = player.y * TILE;
			// 客户端清空网络敌人缓存（等待房主广播新敌人）
			networkEnemyMap.clear();
			networkEnemies.clear();
			killCount = 0;
			logText = "进入第" + floor + "层！";
		}, 0.6f);
	}

	// ============ 半即时制核心逻辑 ============

	/** 更新玩家（冷却驱动） */
	private void updatePlayer(float delta) {
		player.moveTimer -= delta;
		if (player.moveTimer > 0) return; // 冷却中，忽略输入

		InputManager input = InputManager.getInstance();

		// 魔法攻击检测 (J键 / ATTACK)
		if (input.isJustPressed(InputAction.ATTACK)) {int k;
			if (isClient()) {
				// 客户端：发送攻击请求给房主
				int dx2 = player.faceDx, dy2 = player.faceDy;
				if (dx2 == 0 && dy2 == 0) dy2 = 1;
				lanService.sendAttackRequest("magic", player.x, player.y,
					dx2, dy2, player.stats.getATK());
				player.moveTimer = player.getAttackCooldown();
			} else if (combatHelper.performMagicAttack()) {
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
		if (isClient()) {
			// 客户端：检查网络敌人是否在攻击方向上
			GameEntity target = findNetworkEnemyAt(player.x + dx, player.y + dy);
			if (target != null) {
				lanService.sendAttackRequest("physical", player.x, player.y,
					dx, dy, player.stats.getATK());
				player.triggerBump(dx, dy);
				player.moveTimer = player.getAttackCooldown();
				return;
			}
		} else if (combatHelper.performRangedAttack(player, dx, dy)) {
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

		// 踩到楼梯 — Phase 5: 联机时只有房主能触发换层
		if (map[ny][nx] == T_STAIRS) {
			if (isClient()) {
				logText = "等待房主触发换层...";
				return;
			}
			currentFloor++;
			long newSeed = ThreadLocalRandom.current().nextLong();
			logText = "踏上楼梯... 前往第" + currentFloor + "层！";
			// 联机模式：广播换层信号给所有客户端
			if (isMultiplayer()) {
				lanService.broadcastFloorChange(newSeed, currentFloor);
			}
			mapSeed = newSeed;
			getScreenManager().playOverlayFade(() -> {
				buildMap(newSeed);
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
		long newSeed = ThreadLocalRandom.current().nextLong();
		mapSeed = newSeed;
		buildMap(newSeed);
		growthHelper.resetPlayerForRespawn();
		enemies.clear();
		spawnEnemies();
		killCount = 0;
		deathResult = null;
		gameTime = 0;
	}

}
