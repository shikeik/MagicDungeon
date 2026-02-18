package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.world.Dungeon;
import java.util.List;

public class Monster extends Entity {
	public String name;
	public MonsterType type;
	public int maxHp;
	public int hp;
	public int atk;
	public float moveDelay;
	public float moveTimer;

	public enum State { IDLE, WANDER, CHASE }
	public State state = State.IDLE;
	public float stateTimer;
	public int targetX, targetY;

	// Runtime visual state (not serialized)
	public transient Object visualState;

	public Monster(int x, int y, MonsterType type) {
		super(x, y, type.color);
		this.type = type;
		this.name = type.name;
		this.maxHp = type.maxHp;
		this.hp = type.maxHp;
		this.atk = type.atk;
		this.moveDelay = type.speed;
		this.moveTimer = 0;
		this.stateTimer = MathUtils.random(1f, 3f); // Initial delay

		// Visual speed cap
		this.visualSpeed = Math.max(Constants.TILE_SIZE / this.moveDelay, 64);
	}

	public int update(float dt, Player player, Dungeon dungeon, List<Monster> otherMonsters) {
		if (this.hp <= 0) return 0;

		this.moveTimer -= dt;
		this.stateTimer -= dt;

		// 1. Determine State
		float distToPlayer = Float.MAX_VALUE;
		if (player != null) {
			float dx = player.x - this.x;
			float dy = player.y - this.y;
			distToPlayer = (float)Math.sqrt(dx*dx + dy*dy);
		}

		if (distToPlayer < 10) { // Aggro range
			state = State.CHASE;
		} else if (state == State.CHASE) {
			state = State.IDLE; // Lost target
			stateTimer = MathUtils.random(1f, 3f);
		}

		// 2. State Logic
		if (state == State.IDLE) {
			if (stateTimer <= 0) {
				state = State.WANDER;
				// Pick random target
				pickRandomTarget(dungeon);
			}
		} else if (state == State.WANDER) {
			if (this.x == targetX && this.y == targetY) {
				state = State.IDLE;
				stateTimer = MathUtils.random(2f, 5f); // Rest for a while
			} else if (stateTimer <= -5f) { // Timeout (stuck?)
				state = State.IDLE;
				stateTimer = MathUtils.random(1f, 2f);
			}
		}

		// 3. Movement Logic
		if (this.moveTimer <= 0) {
			int nextX = this.x;
			int nextY = this.y;

			if (state == State.CHASE && player != null) {
				int dx = player.x - this.x;
				int dy = player.y - this.y;
				if (Math.abs(dx) > Math.abs(dy)) {
					nextX += dx > 0 ? 1 : -1;
				} else {
					nextY += dy > 0 ? 1 : -1;
				}
			} else if (state == State.WANDER) {
				int dx = targetX - this.x;
				int dy = targetY - this.y;
				if (dx != 0) nextX += dx > 0 ? 1 : -1;
				else if (dy != 0) nextY += dy > 0 ? 1 : -1;
			}

			// Perform Move if changed
			if (nextX != this.x || nextY != this.y) {
				// Check collision with map
				if (dungeon.isWalkable(nextX, nextY)) {
					// Check collision with player
					if (player != null && nextX == player.x && nextY == player.y) {
						// Attack Player!
						triggerBump(nextX - this.x, nextY - this.y);
						this.moveTimer = this.moveDelay;
						return this.atk; // Return damage
					}

					// Check collision with other monsters
					boolean blocked = false;
					for (Monster m : otherMonsters) {
						if (m != this && m.hp > 0 && m.x == nextX && m.y == nextY) {
							blocked = true;
							break;
						}
					}

					if (!blocked) {
						this.x = nextX;
						this.y = nextY;
					}
				}
				this.moveTimer = this.moveDelay;
			}
		}
		return 0;
	}

	private void pickRandomTarget(Dungeon dungeon) {
		int radius = 5;
		int attempts = 0;
		while (attempts < 5) {
			int tx = this.x + MathUtils.random(-radius, radius);
			int ty = this.y + MathUtils.random(-radius, radius);
			
			if (tx >= 0 && tx < dungeon.width && ty >= 0 && ty < dungeon.height && dungeon.isWalkable(tx, ty)) {
				this.targetX = tx;
				this.targetY = ty;
				return;
			}
			attempts++;
		}
		// Failed to find target, stay idle
		state = State.IDLE;
		stateTimer = MathUtils.random(1f, 2f);
	}
}
