package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.assets.TextureManager;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty;
import com.goldsprite.magicdungeon2.core.growth.GrowthCalculator;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.input.virtual.VirtualControlsOverlay;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_H;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_W;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MP_REGEN_RATE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.POPUP_RISE_SPEED;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.STICK_DEADZONE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.TILE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_FLOOR;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_STAIRS;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_WALL;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.WORLD_VIEW_SIZE;

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
public class SimpleGameScreen extends GScreen {
	// 摇杆四向判定半角（度），默认45°，即每个方向占 90° 扇形（全覆盖）
	private static float stickHalfAngle = 45;

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

	// ============ 公共访问方法（供自动测试读取状态） ============

	/** 获取玩家实体 */
	public GameEntity getPlayer() { return player; }

	/** 获取敌人列表 */
	public Array<GameEntity> getEnemies() { return enemies; }

	/** 获取地图数据 */
	public int[][] getMap() { return map; }

	/** 获取游戏时间（秒） */
	public float getGameTime() { return gameTime; }

	/** 获取击杀数 */
	public int getKillCount() { return killCount; }

	/** 获取日志文本 */
	public String getLogText() { return logText; }

	// ============ 向后兼容别名（过渡期后移除） ============
	/** @deprecated 使用 {@link GameEntity} 替代 */
	@Deprecated
	public static class Entity extends GameEntity {
		public Entity(int x, int y, String texName, float hp, float atk, float def,
					   float moveDelay, int xpReward, com.goldsprite.magicdungeon2.core.combat.WeaponRange weaponRange) {
			super(x, y, texName, hp, atk, def, moveDelay, xpReward, weaponRange);
		}
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

		// 创建虚拟触控覆盖层（Android 默认显示）
		virtualControls = new VirtualControlsOverlay(new ExtendViewport(
			getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight()));
		virtualControls.setStickHalfAngle(stickHalfAngle); // 同步角度判定
		if (imp != null) imp.addProcessor(virtualControls.getStage());
	}

	@Override
	protected void resizeWorldCamera(boolean centerCamera) {
		if (worldCamera == null) return;
		// 保持与旧版 ExtendViewport(400,400) 相同的世界视野
		float aspect = (float) Gdx.graphics.getWidth() / Math.max(Gdx.graphics.getHeight(), 1);
		if (aspect >= 1) {
			worldCamera.viewportHeight = WORLD_VIEW_SIZE;
			worldCamera.viewportWidth = WORLD_VIEW_SIZE * aspect;
		} else {
			worldCamera.viewportWidth = WORLD_VIEW_SIZE;
			worldCamera.viewportHeight = WORLD_VIEW_SIZE / aspect;
		}
		worldCamera.update();
	}

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
		updatePopups(delta);

		// 世界相机跟随玩家（使用视觉坐标更平滑）
		OrthographicCamera worldCam = getWorldCamera();
		worldCam.position.set(
			player.visualX + TILE * 0.5f,
			player.visualY + TILE * 0.5f, 0);
		worldCam.update();

		// 世界渲染（地图+实体+飘字）
		batch.setProjectionMatrix(worldCam.combined);
		batch.begin();
		drawMap();
		drawEntities();
		drawPopups();
		batch.end();

		// HUD 渲染（使用 UI 视口）
		drawHUD();

		// 死亡覆盖层
		if (!player.alive && deathResult != null) {
			drawDeathOverlay();
		}

		// 渲染虚拟触控控件（在 HUD 之上）
		if (virtualControls != null) virtualControls.render();
	}

	// ============ 半即时制核心逻辑 ============

	/** 更新玩家（冷却驱动） */
	private void updatePlayer(float delta) {
		player.moveTimer -= delta;
		if (player.moveTimer > 0) return; // 冷却中，忽略输入

		InputManager input = InputManager.getInstance();

		// 魔法攻击检测 (J键 / ATTACK)
		if (input.isJustPressed(InputAction.ATTACK)) {
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

	// ============ 碰撞与查询 ============

	private void updatePopups(float delta) {
		for (int i = popups.size - 1; i >= 0; i--) {
			DamagePopup p = popups.get(i);
			p.timer -= delta;
			p.y += POPUP_RISE_SPEED * delta; // 向上飘
			if (p.timer <= 0) popups.removeIndex(i);
		}
	}

	// ============ 渲染（使用视觉坐标） ============

	private void drawMap() {
		for (int y = 0; y < MAP_H; y++) {
			for (int x = 0; x < MAP_W; x++) {
				String texName;
				switch (map[y][x]) {
					case T_WALL: texName = "wall"; break;
					case T_STAIRS: texName = "stairs_down"; break;
					default: texName = "floor"; break;
				}
				TextureRegion tex = TextureManager.get(texName);
				if (tex != null) {
					batch.draw(tex, x * TILE, y * TILE, TILE, TILE);
				} else {
					Color c = map[y][x] == T_WALL ? Color.GRAY : Color.DARK_GRAY;
					batch.drawRect(x * TILE, y * TILE, TILE, TILE, 0, 0, c, true);
				}
			}
		}
	}

	private void drawEntities() {
		// 绘制敌人
		for (int i = 0; i < enemies.size; i++) {
			GameEntity e = enemies.get(i);
			if (!e.alive) continue;
			drawEntity(e);
		}
		// 绘制玩家
		if (player.alive) drawEntity(player);
	}

	private void drawEntity(GameEntity e) {
		float drawX = e.visualX + e.bumpX;
		float drawY = e.visualY + e.bumpY;

		TextureRegion tex = TextureManager.get(e.texName);
		if (tex != null) {
			batch.draw(tex, drawX, drawY, TILE, TILE);
		} else {
			batch.drawRect(drawX + 2, drawY + 2, TILE - 4, TILE - 4, 0, 0, Color.CYAN, true);
		}

		// 血条
		if (e.hp < e.getMaxHp()) {
			float barW = TILE - 4;
			float barH = 3;
			float barX = drawX + 2;
			float barY = drawY + TILE + 1;
			batch.drawRect(barX, barY, barW, barH, 0, 0, Color.DARK_GRAY, true);
			batch.drawRect(barX, barY, barW * (e.hp / e.getMaxHp()), barH, 0, 0,
				e == player ? Color.GREEN : Color.RED, true);
		}

		// 冷却条（显示在实体下方）
		if (e.moveTimer > 0) {
			float cdRatio = e.moveTimer / e.getMoveCooldown();
			float barW = TILE - 8;
			float barX = drawX + 4;
			float barY = drawY - 3;
			batch.drawRect(barX, barY, barW, 2, 0, 0, Color.DARK_GRAY, true);
			batch.drawRect(barX, barY, barW * (1 - cdRatio), 2, 0, 0, Color.SKY, true);
		}
	}

	private void drawPopups() {
		for (int i = 0; i < popups.size; i++) {
			DamagePopup p = popups.get(i);
			float alpha = Math.min(p.timer * 2, 1f);
			Color c = new Color(p.color.r, p.color.g, p.color.b, alpha);
			font.setColor(c);
			font.draw(batch, p.text, p.x - 10, p.y);
		}
		font.setColor(Color.WHITE);
	}

	private void drawHUD() {
		batch.setProjectionMatrix(getUICamera().combined);
		batch.begin();

		float vh = getUIViewport().getWorldHeight();

		// 第1行: 等级 + HP + MP + XP
		float[] xpProg = GrowthCalculator.xpProgress(player.totalXp);
		hudFont.setColor(Color.WHITE);
		hudFont.draw(batch, String.format(
			"Lv.%d | HP:%.0f/%.0f | MP:%.0f/%.0f | XP:%.0f/%.0f",
			player.stats.getLevel(), player.hp, player.getMaxHp(),
			player.mp, player.getMaxMp(),
			xpProg[0], xpProg[1]), 10, vh - 10);

		// 第2行: ATK DEF 金币 击杀
		hudFont.setColor(Color.LIGHT_GRAY);
		hudFont.draw(batch, String.format(
			"ATK:%.0f | DEF:%.0f | Gold:%d | 击杀:%d | %.0fs",
			player.stats.getATK(), player.stats.getDEF(),
			player.gold, killCount, gameTime), 10, vh - 30);

		// 第3行: 日志
		hudFont.setColor(Color.YELLOW);
		hudFont.draw(batch, logText, 10, vh - 50);

		// 第4行: 提示
		hudFont.setColor(Color.GRAY);
		hudFont.draw(batch, String.format("敌人:%d | ESC返回", enemies.size), 10, vh - 70);
		hudFont.setColor(Color.WHITE);

		batch.end();
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

	/** 绘制死亡覆盖层 */
	private void drawDeathOverlay() {
		batch.setProjectionMatrix(getUICamera().combined);
		batch.begin();

		float vw = getUIViewport().getWorldWidth();
		float vh = getUIViewport().getWorldHeight();

		// 半透明黑色背景
		batch.drawRect(0, 0, vw, vh, 0, 0, new Color(0, 0, 0, 0.7f), true);

		float cx = vw / 2;
		float cy = vh / 2;
		float lineH = 28;

		hudFont.setColor(Color.RED);
		hudFont.draw(batch, "你被击败了！",
			cx - 200, cy + lineH * 3, 400, Align.center, false);

		hudFont.setColor(Color.WHITE);
		hudFont.draw(batch, String.format("经验损失: -%d XP", deathResult.xpLost),
			cx - 200, cy + lineH, 400, Align.center, false);

		if (deathResult.levelBefore != deathResult.levelAfter) {
			hudFont.setColor(Color.ORANGE);
			hudFont.draw(batch, String.format("等级变化: Lv.%d → Lv.%d",
				deathResult.levelBefore, deathResult.levelAfter),
				cx - 200, cy, 400, Align.center, false);
		}

		hudFont.setColor(Color.GOLD);
		hudFont.draw(batch, String.format("金币掉落: -%dG (可拾回:%dG)",
			deathResult.goldDropped, deathResult.goldRecoverable),
			cx - 200, cy - lineH, 400, Align.center, false);

		hudFont.setColor(Color.GRAY);
		hudFont.draw(batch, "按 R 键重生",
			cx - 200, cy - lineH * 3, 400, Align.center, false);

		hudFont.setColor(Color.WHITE);
		batch.end();
	}
}
