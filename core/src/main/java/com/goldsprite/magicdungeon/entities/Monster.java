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

    public Monster(int x, int y, MonsterType type) {
        super(x, y, type.color);
        this.type = type;
        this.name = type.name;
        this.maxHp = type.maxHp;
        this.hp = type.maxHp;
        this.atk = type.atk;
        this.moveDelay = type.speed;
        this.moveTimer = 0;

        // Visual speed cap
        this.visualSpeed = Math.max(Constants.TILE_SIZE / this.moveDelay, 64);
    }

    public int update(float dt, Player player, Dungeon dungeon, List<Monster> otherMonsters) {
        if (this.hp <= 0) return 0;

        this.moveTimer -= dt;
        if (this.moveTimer > 0) return 0;

        // Simple chase logic
        int dx = player.x - this.x;
        int dy = player.y - this.y;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);

        if (dist < 10) { // Aggro range
            int nextX = this.x;
            int nextY = this.y;

            if (Math.abs(dx) > Math.abs(dy)) {
                nextX += dx > 0 ? 1 : -1;
            } else {
                nextY += dy > 0 ? 1 : -1;
            }

            // Check collision with map
            if (dungeon.isWalkable(nextX, nextY)) {
                // Check collision with player
                if (nextX == player.x && nextY == player.y) {
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
        return 0;
    }
}
