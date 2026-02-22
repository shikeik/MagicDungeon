package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon2.assets.TextureManager;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty;
import com.goldsprite.magicdungeon2.core.growth.GrowthCalculator;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_H;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_W;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.POPUP_RISE_SPEED;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.TILE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_STAIRS;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_WALL;

/**
 * 游戏渲染器 — 负责世界/HUD/死亡覆盖层的绘制
 * <p>
 * 通过 {@link GameState} 接口读取游戏状态，与逻辑完全解耦。
 * 所有绘制方法均为无副作用的纯渲染操作（updatePopups 除外，负责飘字动画）。
 */
public class GameRenderer {

	/** 渲染所需的游戏状态（只读接口） */
	public interface GameState {
		int[][] getMap();
		GameEntity getPlayer();
		Array<GameEntity> getEnemies();
		Array<GameEntity> getRemotePlayers();
		DeathPenalty.DeathResult getDeathResult();
		float getGameTime();
		int getKillCount();
		String getLogText();
		Array<DamagePopup> getPopups();
		boolean isShowLanMenu();
		boolean isLanConnected();
		String getLanStatus();
		int getLanPlayerCount();
	}

	private final NeonBatch batch;
	private final BitmapFont font;
	private final BitmapFont hudFont;
	private final OrthographicCamera uiCamera;
	private final Viewport uiViewport;
	private final GameState state;

	public GameRenderer(NeonBatch batch, BitmapFont font, BitmapFont hudFont,
						OrthographicCamera uiCamera, Viewport uiViewport,
						GameState state) {
		this.batch = batch;
		this.font = font;
		this.hudFont = hudFont;
		this.uiCamera = uiCamera;
		this.uiViewport = uiViewport;
		this.state = state;
	}

	// ============ 世界渲染 ============

	/** 绘制世界（地图+实体+飘字），需在 batch.begin/end 之间调用 */
	public void drawWorld() {
		drawMap();
		drawEntities();
		drawPopups();
	}

	/** 绘制地图瓦片 */
	private void drawMap() {
		int[][] map = state.getMap();
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

	/** 绘制所有实体（敌人+玩家+远程玩家） */
	private void drawEntities() {
		Array<GameEntity> enemies = state.getEnemies();
		GameEntity player = state.getPlayer();
		Array<GameEntity> remotePlayers = state.getRemotePlayers();

		for (int i = 0; i < enemies.size; i++) {
			GameEntity e = enemies.get(i);
			if (!e.alive) continue;
			drawEntity(e, player, false);
		}

		if (remotePlayers != null) {
			for (int i = 0; i < remotePlayers.size; i++) {
				GameEntity e = remotePlayers.get(i);
				if (!e.alive) continue;
				drawEntity(e, player, true);
			}
		}

		if (player.alive) drawEntity(player, player, false);
	}

	/** 绘制单个实体（贴图+血条+冷却条） */
	private void drawEntity(GameEntity e, GameEntity player, boolean isRemotePlayer) {
		float drawX = e.visualX + e.bumpX;
		float drawY = e.visualY + e.bumpY;

		TextureRegion tex = TextureManager.get(e.texName);
		if (tex != null) {
			batch.draw(tex, drawX, drawY, TILE, TILE);
		} else {
			Color c = isRemotePlayer ? Color.ORANGE : Color.CYAN;
			batch.drawRect(drawX + 2, drawY + 2, TILE - 4, TILE - 4, 0, 0, c, true);
		}

		// 血条
		if (e.hp < e.getMaxHp()) {
			float barW = TILE - 4;
			float barH = 3;
			float barX = drawX + 2;
			float barY = drawY + TILE + 1;
			batch.drawRect(barX, barY, barW, barH, 0, 0, Color.DARK_GRAY, true);
			batch.drawRect(barX, barY, barW * (e.hp / e.getMaxHp()), barH, 0, 0,
				e == player ? Color.GREEN : (isRemotePlayer ? Color.YELLOW : Color.RED), true);
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

	/** 绘制伤害飘字 */
	private void drawPopups() {
		Array<DamagePopup> popups = state.getPopups();
		for (int i = 0; i < popups.size; i++) {
			DamagePopup p = popups.get(i);
			float alpha = Math.min(p.timer * 2, 1f);
			Color c = new Color(p.color.r, p.color.g, p.color.b, alpha);
			font.setColor(c);
			font.draw(batch, p.text, p.x - 10, p.y);
		}
		font.setColor(Color.WHITE);
	}

	// ============ HUD 渲染 ============

	/** 绘制 HUD（等级/血量/经验/日志等），自行管理 batch begin/end */
	public void drawHUD() {
		GameEntity player = state.getPlayer();
		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();

		float vh = uiViewport.getWorldHeight();

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
			player.gold, state.getKillCount(), state.getGameTime()), 10, vh - 30);

		// 第3行: 日志
		hudFont.setColor(Color.YELLOW);
		hudFont.draw(batch, state.getLogText(), 10, vh - 50);

		// 第4行: 提示
		hudFont.setColor(Color.GRAY);
		hudFont.draw(batch, String.format("敌人:%d | ESC返回",
			state.getEnemies().size), 10, vh - 70);
		hudFont.setColor(Color.WHITE);

		batch.end();
	}

	// ============ 死亡覆盖层 ============

	/** 绘制死亡覆盖层（经验损失/等级变化/金币掉落），自行管理 batch begin/end */
	public void drawDeathOverlay() {
		DeathPenalty.DeathResult deathResult = state.getDeathResult();
		if (deathResult == null) return;

		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();

		float vw = uiViewport.getWorldWidth();
		float vh = uiViewport.getWorldHeight();

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

	// ============ 联机覆盖层 ============

	/** 绘制联机菜单覆盖层 */
	public void drawLanOverlay() {
		if (!state.isShowLanMenu()) return;

		batch.setProjectionMatrix(uiCamera.combined);
		batch.begin();

		float vw = uiViewport.getWorldWidth();
		float vh = uiViewport.getWorldHeight();

		// 半透明黑色背景
		batch.drawRect(0, 0, vw, vh, 0, 0, new Color(0, 0, 0, 0.8f), true);

		float cx = vw / 2;
		float cy = vh / 2;
		float lineH = 28;

		font.setColor(Color.CYAN);
		font.draw(batch, "联 机 菜 单", cx - 200, cy + lineH * 4, 400, Align.center, false);

		hudFont.setColor(Color.WHITE);
		hudFont.draw(batch, "状态: " + (state.isLanConnected() ? state.getLanStatus() : "未连接"),
			cx - 200, cy + lineH * 2, 400, Align.center, false);
		hudFont.draw(batch, "玩家数: " + state.getLanPlayerCount(),
			cx - 200, cy + lineH, 400, Align.center, false);

		hudFont.setColor(Color.YELLOW);
		if (!state.isLanConnected()) {
			hudFont.draw(batch, "按 [H] 创建房间", cx - 200, cy - lineH, 400, Align.center, false);
			hudFont.draw(batch, "按 [C] 加入房间(本地)", cx - 200, cy - lineH * 2, 400, Align.center, false);
		} else {
			hudFont.draw(batch, "按 [X] 断开连接", cx - 200, cy - lineH, 400, Align.center, false);
		}

		hudFont.setColor(Color.GRAY);
		hudFont.draw(batch, "按 [TAB] 关闭菜单", cx - 200, cy - lineH * 4, 400, Align.center, false);

		hudFont.setColor(Color.WHITE);
		batch.end();
	}

	// ============ 飘字动画更新 ============

	/** 更新飘字动画（上飘+淡出），应在逻辑帧调用 */
	public void updatePopups(float delta) {
		Array<DamagePopup> popups = state.getPopups();
		for (int i = popups.size - 1; i >= 0; i--) {
			DamagePopup p = popups.get(i);
			p.timer -= delta;
			p.y += POPUP_RISE_SPEED * delta; // 向上飘
			if (p.timer <= 0) popups.removeIndex(i);
		}
	}
}
