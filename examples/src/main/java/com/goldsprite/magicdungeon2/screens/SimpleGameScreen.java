package com.goldsprite.magicdungeon2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.assets.TextureManager;
import com.goldsprite.magicdungeon2.core.combat.CombatEngine;
import com.goldsprite.magicdungeon2.core.stats.StatData;
import com.goldsprite.magicdungeon2.core.stats.StatType;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.input.virtual.VirtualControlsOverlay;

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
	// 地图尺寸
	private static final int MAP_W = 9, MAP_H = 9;
	private static final int TILE = 32;

	// 图块类型
	private static final int T_FLOOR = 0, T_WALL = 1, T_STAIRS = 2;

	// 视觉插值速度（像素/秒）
	private static final float VISUAL_SPEED = 256f;
	// Bump 攻击动画衰减系数
	private static final float BUMP_DECAY = 10f;

	private NeonBatch batch;
	private OrthographicCamera camera;
	private Viewport viewport;
	private BitmapFont font, hudFont;

	private int[][] map;
	private Entity player;
	private Array<Entity> enemies = new Array<>();
	private Array<DamagePopup> popups = new Array<>();
	private String logText = "移动: 按住WASD/方向键/摇杆 | 走向敌人即攻击";
	private float gameTime = 0; // 游戏运行时间（秒）
	private int killCount = 0;  // 击杀数

	// 虚拟触控控件
	private VirtualControlsOverlay virtualControls;

	// ============ 公共访问方法（供自动测试读取状态） ============

	/** 获取玩家实体 */
	public Entity getPlayer() { return player; }

	/** 获取敌人列表 */
	public Array<Entity> getEnemies() { return enemies; }

	/** 获取地图数据 */
	public int[][] getMap() { return map; }

	/** 获取游戏时间（秒） */
	public float getGameTime() { return gameTime; }

	/** 获取击杀数 */
	public int getKillCount() { return killCount; }

	/** 获取日志文本 */
	public String getLogText() { return logText; }

	/**
	 * 简易实体（半即时制）
	 * 每个实体维护独立的移动冷却计时器和视觉插值坐标
	 */
	public static class Entity {
		// --- 逻辑状态 ---
		public int x, y;            // 网格坐标（立即跳变）
		public String texName;
		public StatData stats;
		public float hp, maxHp;
		public boolean alive = true;

		// --- 冷却系统 ---
		public float moveTimer = 0;  // 当前冷却剩余时间（秒）
		public float moveDelay;      // 基础冷却间隔（秒）

		// --- 视觉插值 ---
		public float visualX, visualY;   // 渲染像素坐标（平滑追赶逻辑坐标）
		public float bumpX, bumpY;       // Bump 攻击偏移（衰减动画）

		// --- 敌人AI ---
		public float aggroRange = 6f;    // 仇恨范围（格子距离）

		public Entity(int x, int y, String texName, float hp, float atk, float def, float moveDelay) {
			this.x = x;
			this.y = y;
			this.texName = texName;
			this.hp = hp;
			this.maxHp = hp;
			this.moveDelay = moveDelay;
			this.visualX = x * TILE;
			this.visualY = y * TILE;
			stats = new StatData();
			stats.setLevel(1);
			stats.setEquipFixed(StatType.HP, hp - StatType.HP.valuePerPoint);
			stats.setEquipFixed(StatType.ATK, atk - StatType.ATK.valuePerPoint);
			stats.setEquipFixed(StatType.DEF, def - StatType.DEF.valuePerPoint);
		}

		/** 获取实际冷却时间（受 ASP 属性加速） */
		public float getEffectiveCooldown() {
			float asp = stats.getASP(); // 默认 1.0，越高越快
			return moveDelay / Math.max(asp, 0.1f);
		}

		/** 更新视觉坐标（平滑追赶逻辑坐标） */
		public void updateVisuals(float dt) {
			float targetX = x * TILE;
			float targetY = y * TILE;

			// 线性插值到目标位置
			float distX = targetX - visualX;
			float distY = targetY - visualY;
			float move = VISUAL_SPEED * dt;

			if (Math.abs(distX) <= move) visualX = targetX;
			else visualX += Math.signum(distX) * move;

			if (Math.abs(distY) <= move) visualY = targetY;
			else visualY += Math.signum(distY) * move;

			// Bump 动画衰减
			bumpX += (0 - bumpX) * BUMP_DECAY * dt;
			bumpY += (0 - bumpY) * BUMP_DECAY * dt;
		}

		/** 触发 Bump 攻击动画（向目标方向弹一下） */
		public void triggerBump(int dx, int dy) {
			bumpX = dx * TILE * 0.3f;
			bumpY = dy * TILE * 0.3f;
		}
	}

	/** 伤害飘字 */
	public static class DamagePopup {
		float x, y, timer;
		String text;
		Color color;

		DamagePopup(float x, float y, String text, Color color) {
			this.x = x;
			this.y = y;
			this.text = text;
			this.color = color;
			this.timer = 1.0f;
		}
	}

	@Override
	public void create() {
		batch = new NeonBatch();
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(400, 400, camera);
		viewport.apply(true);

		font = FontUtils.generate(10, 2);
		hudFont = FontUtils.generate(14, 3);

		TextureManager.init();
		buildMap();
		spawnEntities();

		// 创建虚拟触控覆盖层（Android 默认显示）
		virtualControls = new VirtualControlsOverlay(new ExtendViewport(400, 400));
		if (imp != null) imp.addProcessor(virtualControls.getStage());
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
		// 玩家：HP=100, ATK=12, DEF=5, 冷却0.2秒
		player = new Entity(4, 4, "player", 100, 12, 5, 0.2f);
		// 敌人：各有不同的移动冷却（慢怪1秒/步，快怪0.4秒/步）
		enemies.add(new Entity(2, 2, "slime", 20, 4, 1, 1.0f));     // 最慢
		enemies.add(new Entity(6, 6, "skeleton", 35, 8, 3, 0.8f));
		enemies.add(new Entity(2, 6, "bat", 15, 6, 1, 0.4f));       // 最快
		enemies.add(new Entity(6, 2, "wolf", 30, 10, 2, 0.6f));
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1);

		// 更新虚拟触控控件
		if (virtualControls != null) virtualControls.update(delta);

		if (player.alive) {
			gameTime += delta;
			updatePlayer(delta);
			updateEnemies(delta);
		} else {
			// 死亡后检测重置
			InputManager input = InputManager.getInstance();
			if (input.isJustPressed(InputAction.RESET_MAP)) {
				buildMap();
				spawnEntities();
				gameTime = 0;
				logText = "地牢重置！继续冒险...";
			}
		}

		// 更新所有实体的视觉插值
		player.updateVisuals(delta);
		for (int i = 0; i < enemies.size; i++) {
			enemies.get(i).updateVisuals(delta);
		}
		updatePopups(delta);

		// 摄像机跟随玩家（使用视觉坐标更平滑）
		camera.position.set(
			player.visualX + TILE * 0.5f,
			player.visualY + TILE * 0.5f, 0);
		viewport.apply();
		camera.update();

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		drawMap();
		drawEntities();
		drawPopups();
		batch.end();

		drawHUD();

		// 渲染虚拟触控控件（在 HUD 之上）
		if (virtualControls != null) virtualControls.render();
	}

	// ============ 半即时制核心逻辑 ============

	/** 更新玩家（冷却驱动） */
	private void updatePlayer(float delta) {
		player.moveTimer -= delta;
		if (player.moveTimer > 0) return; // 冷却中，忽略输入

		// 持续按键检测（按住方向键/摇杆自动重复移动）
		InputManager input = InputManager.getInstance();
		int dx = 0, dy = 0;

		if (input.isPressed(InputAction.MOVE_UP)) dy = 1;
		else if (input.isPressed(InputAction.MOVE_DOWN)) dy = -1;
		else if (input.isPressed(InputAction.MOVE_LEFT)) dx = -1;
		else if (input.isPressed(InputAction.MOVE_RIGHT)) dx = 1;

		// 若键盘无输入，检查摇杆轴（虚拟摇杆或手柄）
		if (dx == 0 && dy == 0) {
			Vector2 axis = input.getAxis(InputManager.AXIS_LEFT);
			float threshold = 0.3f;
			if (Math.abs(axis.x) > Math.abs(axis.y)) {
				if (axis.x > threshold) dx = 1;
				else if (axis.x < -threshold) dx = -1;
			} else {
				if (axis.y > threshold) dy = 1;
				else if (axis.y < -threshold) dy = -1;
			}
		}

		if (dx == 0 && dy == 0) return;

		int nx = player.x + dx, ny = player.y + dy;
		if (nx < 0 || ny < 0 || nx >= MAP_W || ny >= MAP_H) {
			player.moveTimer = player.getEffectiveCooldown(); // 撞边界也消耗冷却
			return;
		}
		if (map[ny][nx] == T_WALL) {
			player.moveTimer = player.getEffectiveCooldown(); // 撞墙也消耗冷却
			return;
		}

		// 检查目标格子是否有敌人
		Entity target = findEnemy(nx, ny);
		if (target != null) {
			// Bump 攻击
			player.triggerBump(dx, dy);
			float dmg = CombatEngine.calcDamage(player.stats.getATK(), target.stats.getDEF());
			dmg = Math.max(dmg, 1);
			target.hp -= dmg;
			popups.add(new DamagePopup(
				target.visualX + TILE * 0.5f,
				target.visualY + TILE,
				String.format("-%.0f", dmg), Color.YELLOW));

			if (target.hp <= 0) {
				target.alive = false;
				enemies.removeValue(target, true);
				killCount++;
				logText = String.format("击败了 %s！(击杀:%d)", target.texName, killCount);
			} else {
				logText = String.format("攻击 %s: %.0f伤害 (HP:%.0f/%.0f)",
					target.texName, dmg, target.hp, target.maxHp);
			}
			player.moveTimer = player.getEffectiveCooldown();
		} else {
			// 移动
			player.x = nx;
			player.y = ny;
			player.moveTimer = player.getEffectiveCooldown();

			// 踩到楼梯 — 使用无等待渐变转场
			if (map[ny][nx] == T_STAIRS) {
				logText = "踏上楼梯... 前往下一层！";
				getScreenManager().playOverlayFade(() -> {
					buildMap();
					spawnEntities();
				}, 0.6f);
			}
		}
	}

	/** 更新所有敌人（各自独立冷却） */
	private void updateEnemies(float delta) {
		for (int i = enemies.size - 1; i >= 0; i--) {
			Entity e = enemies.get(i);
			if (!e.alive) continue;

			e.moveTimer -= delta;
			if (e.moveTimer > 0) continue; // 冷却中，跳过

			// AI决策：检测玩家距离
			float dist = Math.abs(player.x - e.x) + Math.abs(player.y - e.y); // 曼哈顿距离

			int dx = 0, dy = 0;
			if (dist <= e.aggroRange) {
				// 追踪玩家
				dx = Integer.signum(player.x - e.x);
				dy = Integer.signum(player.y - e.y);
			} else {
				// 随机游荡
				if (MathUtils.randomBoolean(0.3f)) { // 30%概率移动
					switch (MathUtils.random(3)) {
						case 0: dx = 1; break;
						case 1: dx = -1; break;
						case 2: dy = 1; break;
						case 3: dy = -1; break;
					}
				}
			}

			if (dx == 0 && dy == 0) {
				e.moveTimer = e.getEffectiveCooldown() * 0.5f; // 空闲时缩短等待
				continue;
			}

			// 随机选择水平或垂直方向
			boolean horizontal = MathUtils.randomBoolean();
			int mx, my;
			if (horizontal) {
				mx = dx; my = 0;
				if (mx == 0 || !canMove(e, mx, 0)) { mx = 0; my = dy; }
			} else {
				mx = 0; my = dy;
				if (my == 0 || !canMove(e, 0, my)) { mx = dx; my = 0; }
			}

			int nx = e.x + mx, ny = e.y + my;

			// 走到玩家格子 = 攻击
			if (nx == player.x && ny == player.y) {
				e.triggerBump(mx, my);
				float dmg = CombatEngine.calcDamage(e.stats.getATK(), player.stats.getDEF());
				dmg = Math.max(dmg, 1);
				player.hp -= dmg;
				popups.add(new DamagePopup(
					player.visualX + TILE * 0.5f,
					player.visualY + TILE * 1.2f,
					String.format("-%.0f", dmg), Color.RED));

				if (player.hp <= 0) {
					logText = "你被击败了... 按R重置";
					player.alive = false;
				}
			} else if (canMove(e, mx, my)) {
				e.x = nx;
				e.y = ny;
			}

			e.moveTimer = e.getEffectiveCooldown();
		}
	}

	// ============ 碰撞与查询 ============

	private boolean canMove(Entity e, int dx, int dy) {
		int nx = e.x + dx, ny = e.y + dy;
		if (nx < 0 || ny < 0 || nx >= MAP_W || ny >= MAP_H) return false;
		if (map[ny][nx] == T_WALL) return false;
		// 不能走到其他敌人身上
		for (int i = 0; i < enemies.size; i++) {
			Entity other = enemies.get(i);
			if (other != e && other.alive && other.x == nx && other.y == ny) return false;
		}
		return true;
	}

	private Entity findEnemy(int x, int y) {
		for (int i = 0; i < enemies.size; i++) {
			Entity e = enemies.get(i);
			if (e.alive && e.x == x && e.y == y) return e;
		}
		return null;
	}

	private void updatePopups(float delta) {
		for (int i = popups.size - 1; i >= 0; i--) {
			DamagePopup p = popups.get(i);
			p.timer -= delta;
			p.y += 30 * delta; // 向上飘
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
			Entity e = enemies.get(i);
			if (!e.alive) continue;
			drawEntity(e);
		}
		// 绘制玩家
		if (player.alive) drawEntity(player);
	}

	private void drawEntity(Entity e) {
		float drawX = e.visualX + e.bumpX;
		float drawY = e.visualY + e.bumpY;

		TextureRegion tex = TextureManager.get(e.texName);
		if (tex != null) {
			batch.draw(tex, drawX, drawY, TILE, TILE);
		} else {
			batch.drawRect(drawX + 2, drawY + 2, TILE - 4, TILE - 4, 0, 0, Color.CYAN, true);
		}

		// 血条
		if (e.hp < e.maxHp) {
			float barW = TILE - 4;
			float barH = 3;
			float barX = drawX + 2;
			float barY = drawY + TILE + 1;
			batch.drawRect(barX, barY, barW, barH, 0, 0, Color.DARK_GRAY, true);
			batch.drawRect(barX, barY, barW * (e.hp / e.maxHp), barH, 0, 0,
				e == player ? Color.GREEN : Color.RED, true);
		}

		// 冷却条（显示在实体下方）
		if (e.moveTimer > 0) {
			float cdRatio = e.moveTimer / e.getEffectiveCooldown();
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
		batch.setProjectionMatrix(
			batch.getProjectionMatrix().setToOrtho2D(0, 0,
				Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		batch.begin();

		float top = Gdx.graphics.getHeight();
		hudFont.setColor(Color.WHITE);
		hudFont.draw(batch, String.format(
			"HP: %.0f/%.0f | ATK: %.0f | DEF: %.0f | 击杀: %d | %.0fs",
			player.hp, player.maxHp,
			player.stats.getATK(), player.stats.getDEF(),
			killCount, gameTime), 10, top - 10);

		hudFont.setColor(Color.LIGHT_GRAY);
		hudFont.draw(batch, logText, 10, top - 30);

		// 敌人数量
		hudFont.setColor(Color.YELLOW);
		hudFont.draw(batch, String.format("敌人: %d | ESC返回", enemies.size), 10, top - 50);
		hudFont.setColor(Color.WHITE);

		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		if (virtualControls != null) virtualControls.resize(width, height);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (batch != null) batch.dispose();
		if (virtualControls != null) virtualControls.dispose();
	}
}
