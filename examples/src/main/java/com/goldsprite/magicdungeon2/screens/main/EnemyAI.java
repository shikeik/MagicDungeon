package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.IDLE_CD_FACTOR;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_H;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.MAP_W;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.T_WALL;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.WANDER_CHANCE;

/**
 * 敌人AI系统
 * 管理所有敌人的冷却驱动更新：仇恨追踪、随机游荡、自动攻击
 */
public class EnemyAI {

	private final int[][] map;
	private final Array<GameEntity> enemies;
	private final GameEntity player;
	private final CombatHelper combatHelper;

	public EnemyAI(int[][] map, Array<GameEntity> enemies, GameEntity player, CombatHelper combatHelper) {
		this.map = map;
		this.enemies = enemies;
		this.player = player;
		this.combatHelper = combatHelper;
	}

	/**
	 * 更新所有敌人（各自独立冷却）
	 * @param delta 帧间隔（秒）
	 */
	public void update(float delta) {
		for (int i = enemies.size - 1; i >= 0; i--) {
			GameEntity e = enemies.get(i);
			if (!e.alive) continue;

			e.moveTimer -= delta;
			if (e.moveTimer > 0) continue; // 冷却中，跳过

			// 先检查是否可以在当前位置远程攻击玩家
			if (combatHelper.tryAttackPlayer(e)) {
				e.moveTimer = e.getAttackCooldown();
				continue;
			}

			// AI 决策：检测玩家距离（曼哈顿距离）
			float dist = Math.abs(player.x - e.x) + Math.abs(player.y - e.y);

			int dx = 0, dy = 0;
			if (dist <= e.aggroRange) {
				// 追踪玩家
				dx = Integer.signum(player.x - e.x);
				dy = Integer.signum(player.y - e.y);
			} else {
				// 随机游荡
				if (MathUtils.randomBoolean(WANDER_CHANCE)) {
					switch (MathUtils.random(3)) {
						case 0: dx = 1; break;
						case 1: dx = -1; break;
						case 2: dy = 1; break;
						case 3: dy = -1; break;
					}
				}
			}

			if (dx == 0 && dy == 0) {
				e.moveTimer = e.getMoveCooldown() * IDLE_CD_FACTOR;
				continue;
			}

			// 随机选择水平或垂直方向（避免对角线移动）
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

			// 走到玩家格子 = 近战攻击
			if (nx == player.x && ny == player.y) {
				combatHelper.meleeAttackPlayer(e, mx, my);
			} else if (canMove(e, mx, my)) {
				e.x = nx;
				e.y = ny;
			}

			e.moveTimer = e.getMoveCooldown();
		}
	}

	// ============ 移动碰撞检测 ============

	/**
	 * 检查敌人是否可以在指定方向移动
	 * 会检查边界、墙壁和其他敌人的占位
	 */
	private boolean canMove(GameEntity e, int dx, int dy) {
		int nx = e.x + dx, ny = e.y + dy;
		if (nx < 0 || ny < 0 || nx >= MAP_W || ny >= MAP_H) return false;
		if (map[ny][nx] == T_WALL) return false;
		// 不能走到其他敌人身上
		for (int i = 0; i < enemies.size; i++) {
			GameEntity other = enemies.get(i);
			if (other != e && other.alive && other.x == nx && other.y == ny) return false;
		}
		return true;
	}
}
