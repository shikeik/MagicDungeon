package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.magicdungeon2.core.combat.CombatEngine;
import com.goldsprite.magicdungeon2.core.combat.WeaponRange;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAGIC_MP_COST;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_H;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_W;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.TILE;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_WALL;

/**
 * 战斗辅助工具
 * 统一管理方向扫描、范围攻击、魔法攻击和敌人远程攻击的逻辑
 * 通过 {@link CombatListener} 回调通知上层游戏状态变更
 */
public class CombatHelper {

	/** 战斗事件回调接口 */
	public interface CombatListener {
		/** 敌人被击杀 */
		void onEnemyKilled(GameEntity enemy);
		/** 玩家死亡 */
		void onPlayerDeath();
		/** 日志文本更新 */
		void onLogUpdate(String text);
	}

	private final int[][] map;
	private final Array<GameEntity> enemies;
	private final Array<DamagePopup> popups;
	private final GameEntity player;
	private CombatListener listener;

	public CombatHelper(int[][] map, Array<GameEntity> enemies, Array<DamagePopup> popups,
						GameEntity player, CombatListener listener) {
		this.map = map;
		this.enemies = enemies;
		this.popups = popups;
		this.player = player;
		this.listener = listener;
	}

	public void setListener(CombatListener listener) {
		this.listener = listener;
	}

	// ============ 公共战斗方法 ============

	/**
	 * 执行方向范围攻击（支持射程和穿透），返回是否命中
	 * 用于玩家物理攻击（Bump 式）
	 */
	public boolean performRangedAttack(GameEntity attacker, int dx, int dy) {
		WeaponRange wr = attacker.weaponRange;
		Array<GameEntity> hitTargets = scanEnemiesInDirection(attacker.x, attacker.y, dx, dy, wr.range, wr.piercing);

		if (hitTargets.size == 0) return false;

		attacker.triggerBump(dx, dy);
		GameEntity first = hitTargets.get(0);
		float baseDmg = Math.max(1, CombatEngine.calcPhysicalDamage(
			attacker.stats.getATK(), first.stats.getDEF()));

		for (int i = 0; i < hitTargets.size; i++) {
			GameEntity target = hitTargets.get(i);
			float dmg = wr.piercing ? CombatEngine.calcPierceDamage(baseDmg, i) : baseDmg;
			if (dmg < CombatEngine.MIN_DAMAGE_THRESHOLD) continue;

			target.hp -= dmg;
			Color popColor = (wr.piercing && i > 0) ? Color.ORANGE : Color.YELLOW;
			popups.add(new DamagePopup(
				target.visualX + TILE * 0.5f,
				target.visualY + TILE,
				String.format("-%.0f", dmg), popColor));

			if (target.hp <= 0) {
				killEnemy(target);
			} else {
				notifyLog(String.format("攻击 %s: %.0f伤害 (HP:%.0f/%.0f)",
					target.texName, dmg, target.hp, target.getMaxHp()));
			}
		}
		return true;
	}

	/**
	 * 魔法攻击：消耗MP，沿面朝方向发射魔法弹
	 * 返回是否成功释放（MP不足返回false）
	 */
	public boolean performMagicAttack() {
		if (player.mp < MAGIC_MP_COST) {
			popups.add(new DamagePopup(
				player.visualX + TILE * 0.5f,
				player.visualY + TILE * 1.2f,
				"MP不足", Color.BLUE));
			return false;
		}

		int dx = player.faceDx, dy = player.faceDy;
		if (dx == 0 && dy == 0) dy = 1; // 默认朝上

		player.mp -= MAGIC_MP_COST;
		boolean hit = false;

		// 魔法攻击使用 ENERGY 范围（5格，无穿透），伤害类型为魔法
		for (int r = 1; r <= WeaponRange.ENERGY.range; r++) {
			int cx = player.x + dx * r;
			int cy = player.y + dy * r;
			if (!isInBounds(cx, cy) || map[cy][cx] == T_WALL) break;
			GameEntity target = findEnemy(cx, cy);
			if (target != null) {
				float dmg = Math.max(1, CombatEngine.calcMagicDamage(
					player.stats.getATK(), target.stats.getMDEF()));
				target.hp -= dmg;
				popups.add(new DamagePopup(
					target.visualX + TILE * 0.5f,
					target.visualY + TILE,
					String.format("-%.0f", dmg), Color.PURPLE));

				if (target.hp <= 0) {
					killEnemy(target);
				} else {
					notifyLog(String.format("魔法攻击 %s: %.0f伤害 (HP:%.0f/%.0f)",
						target.texName, dmg, target.hp, target.getMaxHp()));
				}
				hit = true;
				break; // ENERGY 无穿透
			}
		}

		if (!hit) notifyLog("魔法射向虚空...");
		return true;
	}

	/**
	 * 敌人尝试远程攻击玩家（检查四方向射程内是否有玩家）
	 * 返回是否成功发动攻击
	 */
	public boolean tryAttackPlayer(GameEntity e) {
		if (e.weaponRange.range <= 1) return false; // MELEE 不需远程检查

		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		for (int[] d : dirs) {
			for (int r = 1; r <= e.weaponRange.range; r++) {
				int cx = e.x + d[0] * r, cy = e.y + d[1] * r;
				if (!isInBounds(cx, cy) || map[cy][cx] == T_WALL) break;
				if (cx == player.x && cy == player.y) {
					e.triggerBump(d[0], d[1]);
					applyDamageToPlayer(e);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 敌人近战攻击玩家（走到玩家格子时触发）
	 */
	public void meleeAttackPlayer(GameEntity e, int mx, int my) {
		e.triggerBump(mx, my);
		applyDamageToPlayer(e);
	}

	// ============ 内部辅助方法 ============

	/** 在指定方向扫描敌人目标 */
	private Array<GameEntity> scanEnemiesInDirection(int ox, int oy, int dx, int dy,
													  int range, boolean piercing) {
		Array<GameEntity> targets = new Array<>();
		for (int r = 1; r <= range; r++) {
			int cx = ox + dx * r;
			int cy = oy + dy * r;
			if (!isInBounds(cx, cy) || map[cy][cx] == T_WALL) break;
			GameEntity target = findEnemy(cx, cy);
			if (target != null) {
				targets.add(target);
				if (!piercing) break;
			}
		}
		return targets;
	}

	/** 对玩家施加物理伤害（含飘字和死亡检测） */
	private void applyDamageToPlayer(GameEntity attacker) {
		float dmg = Math.max(1, CombatEngine.calcPhysicalDamage(
			attacker.stats.getATK(), player.stats.getDEF()));
		player.hp -= dmg;
		popups.add(new DamagePopup(
			player.visualX + TILE * 0.5f,
			player.visualY + TILE * 1.2f,
			String.format("-%.0f", dmg), Color.RED));
		if (player.hp <= 0 && listener != null) {
			listener.onPlayerDeath();
		}
	}

	/** 处理敌人死亡（从列表移除 + 回调） */
	private void killEnemy(GameEntity target) {
		target.alive = false;
		enemies.removeValue(target, true);
		if (listener != null) listener.onEnemyKilled(target);
	}

	/** 在敌人列表中按坐标查找存活敌人 */
	public GameEntity findEnemy(int x, int y) {
		for (int i = 0; i < enemies.size; i++) {
			GameEntity e = enemies.get(i);
			if (e.alive && e.x == x && e.y == y) return e;
		}
		return null;
	}

	/** 坐标边界检查 */
	private boolean isInBounds(int x, int y) {
		return x >= 0 && y >= 0 && x < MAP_W && y < MAP_H;
	}

	/** 通知日志更新 */
	private void notifyLog(String text) {
		if (listener != null) listener.onLogUpdate(text);
	}
}
