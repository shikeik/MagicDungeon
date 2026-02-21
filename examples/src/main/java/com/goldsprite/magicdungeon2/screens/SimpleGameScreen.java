package com.goldsprite.magicdungeon2.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
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

/**
 * 简易地牢游戏场景
 * 小型网格地图 + 玩家/怪物 + 回合制移动战斗
 */
public class SimpleGameScreen extends GScreen {
	// 地图尺寸
	private static final int MAP_W = 9, MAP_H = 9;
	private static final int TILE = 32;

	// 图块类型
	private static final int T_FLOOR = 0, T_WALL = 1, T_STAIRS = 2;

	private NeonBatch batch;
	private OrthographicCamera camera;
	private Viewport viewport;
	private BitmapFont font, hudFont;

	private int[][] map;
	private Entity player;
	private Array<Entity> enemies = new Array<>();
	private Array<DamagePopup> popups = new Array<>();
	private String logText = "移动: WASD/方向键 | 走向敌人即攻击";
	private int turnCount = 0;

	// ============ 公共访问方法（供自动测试读取状态） ============

	/** 获取玩家实体 */
	public Entity getPlayer() { return player; }

	/** 获取敌人列表 */
	public Array<Entity> getEnemies() { return enemies; }

	/** 获取地图数据 */
	public int[][] getMap() { return map; }

	/** 获取回合数 */
	public int getTurnCount() { return turnCount; }

	/** 获取日志文本 */
	public String getLogText() { return logText; }

	/** 简易实体 */
	public static class Entity {
		public int x, y;
		public String texName;
		public StatData stats;
		public float hp, maxHp;
		public boolean alive = true;

		public Entity(int x, int y, String texName, float hp, float atk, float def) {
			this.x = x;
			this.y = y;
			this.texName = texName;
			this.hp = hp;
			this.maxHp = hp;
			stats = new StatData();
			stats.setLevel(1);
			stats.setEquipFixed(StatType.HP, hp - StatType.HP.valuePerPoint);
			stats.setEquipFixed(StatType.ATK, atk - StatType.ATK.valuePerPoint);
			stats.setEquipFixed(StatType.DEF, def - StatType.DEF.valuePerPoint);
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
		player = new Entity(4, 4, "player", 100, 12, 5);
		enemies.add(new Entity(2, 2, "slime", 20, 4, 1));
		enemies.add(new Entity(6, 6, "skeleton", 35, 8, 3));
		enemies.add(new Entity(2, 6, "bat", 15, 6, 1));
		enemies.add(new Entity(6, 2, "wolf", 30, 10, 2));
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0.05f, 0.05f, 0.08f, 1);
		handleInput();
		updatePopups(delta);

		// 摄像机跟随玩家
		camera.position.set(
			(player.x + 0.5f) * TILE,
			(player.y + 0.5f) * TILE, 0);
		viewport.apply();
		camera.update();

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		drawMap();
		drawEntities();
		drawPopups();
		batch.end();

		drawHUD();
	}

	private void handleInput() {
		InputManager input = InputManager.getInstance();
		int dx = 0, dy = 0;

		if (input.isJustPressed(InputAction.MOVE_UP)) dy = 1;
		else if (input.isJustPressed(InputAction.MOVE_DOWN)) dy = -1;
		else if (input.isJustPressed(InputAction.MOVE_LEFT)) dx = -1;
		else if (input.isJustPressed(InputAction.MOVE_RIGHT)) dx = 1;

		if (dx == 0 && dy == 0) return;

		int nx = player.x + dx, ny = player.y + dy;
		if (nx < 0 || ny < 0 || nx >= MAP_W || ny >= MAP_H) return;
		if (map[ny][nx] == T_WALL) return;

		// 检查目标格子是否有敌人
		Entity target = findEnemy(nx, ny);
		if (target != null) {
			// 战斗
			float dmg = CombatEngine.calcDamage(player.stats.getATK(), target.stats.getDEF());
			dmg = Math.max(dmg, 1);
			target.hp -= dmg;
			popups.add(new DamagePopup(
				(target.x + 0.5f) * TILE,
				(target.y + 1.0f) * TILE,
				String.format("-%.0f", dmg), Color.YELLOW));

			if (target.hp <= 0) {
				target.alive = false;
				enemies.removeValue(target, true);
				logText = String.format("击败了 %s！", target.texName);
			} else {
				logText = String.format("攻击 %s: %.0f伤害 (HP:%.0f/%.0f)",
					target.texName, dmg, target.hp, target.maxHp);
			}
		} else {
			// 移动
			player.x = nx;
			player.y = ny;

			// 踩到楼梯
			if (map[ny][nx] == T_STAIRS) {
				logText = "踏上楼梯... 重置地图！";
				buildMap();
				spawnEntities();
				return;
			}
		}

		turnCount++;
		enemyTurn();
	}

	private void enemyTurn() {
		for (int i = 0; i < enemies.size; i++) {
			Entity e = enemies.get(i);
			if (!e.alive) continue;

			// 简单AI：向玩家靠近
			int dx = Integer.signum(player.x - e.x);
			int dy = Integer.signum(player.y - e.y);

			// 随机选择水平或垂直方向移动
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
				float dmg = CombatEngine.calcDamage(e.stats.getATK(), player.stats.getDEF());
				dmg = Math.max(dmg, 1);
				player.hp -= dmg;
				popups.add(new DamagePopup(
					(player.x + 0.5f) * TILE,
					(player.y + 1.2f) * TILE,
					String.format("-%.0f", dmg), Color.RED));

				if (player.hp <= 0) {
					logText = "你被击败了... 按R重置";
					player.alive = false;
				}
			} else if (canMove(e, mx, my)) {
				e.x = nx;
				e.y = ny;
			}
		}
	}

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

		// 重置逻辑
		if (!player.alive) {
			InputManager input = InputManager.getInstance();
			if (input.isJustPressed(InputAction.RESET_MAP)) {
				buildMap();
				spawnEntities();
				logText = "地牢重置！继续冒险...";
			}
		}
	}

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
		TextureRegion tex = TextureManager.get(e.texName);
		if (tex != null) {
			batch.draw(tex, e.x * TILE, e.y * TILE, TILE, TILE);
		} else {
			batch.drawRect(e.x * TILE + 2, e.y * TILE + 2, TILE - 4, TILE - 4, 0, 0, Color.CYAN, true);
		}

		// 血条
		if (e.hp < e.maxHp) {
			float barW = TILE - 4;
			float barH = 3;
			float barX = e.x * TILE + 2;
			float barY = (e.y + 1) * TILE + 1;
			batch.drawRect(barX, barY, barW, barH, 0, 0, Color.DARK_GRAY, true);
			batch.drawRect(barX, barY, barW * (e.hp / e.maxHp), barH, 0, 0,
				e == player ? Color.GREEN : Color.RED, true);
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
			"HP: %.0f/%.0f | ATK: %.0f | DEF: %.0f | 回合: %d",
			player.hp, player.maxHp,
			player.stats.getATK(), player.stats.getDEF(),
			turnCount), 10, top - 10);

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
	}

	@Override
	public void dispose() {
		super.dispose();
		if (batch != null) batch.dispose();
	}
}
