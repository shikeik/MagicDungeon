package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {
	public float moveTimer;
	public float moveDelay;

	// Stats
	public PlayerStats stats;
	public List<InventoryItem> inventory;
	public Equipment equipment;

	// Equipment class for storing equipped items
	public static class Equipment {
		public InventoryItem weapon;
		public InventoryItem armor;
	}
	public Player(int x, int y) {
		super(x, y, Color.GREEN);

		this.moveTimer = 0;
		this.moveDelay = 0.12f;
		this.visualSpeed = Constants.TILE_SIZE / this.moveDelay;

		this.stats = new PlayerStats();
		this.inventory = new ArrayList<>();
		this.equipment = new Equipment();
	}

	public void update(float dt, Dungeon dungeon, int dx, int dy, List<Monster> monsters, AudioSystem audio) {
		this.moveTimer -= dt;
		if (this.moveTimer > 0) return;

		if (dx != 0 || dy != 0) {
			// Prevent diagonal movement
			if (dx != 0 && dy != 0) {
				dy = 0;
			}

			int nextX = this.x + dx;
			int nextY = this.y + dy;

			// Check for monsters
			Monster targetMonster = null;
			for (Monster m : monsters) {
				if (m.hp > 0 && m.x == nextX && m.y == nextY) {
					targetMonster = m;
					break;
				}
			}

			if (targetMonster != null) {
				// Attack Monster
				triggerBump(dx, dy);
				audio.playHit(); // Play hit sound
				targetMonster.hp -= this.stats.atk;
				targetMonster.triggerHitFlash(0.2f); // Trigger hit flash
				// Simple knockback/hit effect could be added here
				if (targetMonster.hp <= 0) {
					// Monster died - Gain XP
					this.stats.addXp(10 + targetMonster.maxHp / 2);
				}
				this.moveTimer = this.moveDelay;
				return;
			}

			if (dungeon.isWalkable(nextX, nextY)) {
				this.x = nextX;
				this.y = nextY;
				this.moveTimer = this.moveDelay;
				audio.playMove();
			} else {
				// Wall bump - Cancelled as per request
				// triggerBump(dx, dy);
				this.moveTimer = this.moveDelay;
			}
		}
	}

	public void useSkill() {
		if (this.stats.mana >= 10) {
			this.stats.mana -= 10;
			this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + 20);
		}
	}

	public void equip(InventoryItem item) {
		if (item.data.type == ItemType.WEAPON) {
			this.equipment.weapon = item;
			updateStats();
		} else if (item.data.type == ItemType.ARMOR) {
			this.equipment.armor = item;
			updateStats();
		} else if (item.data.type == ItemType.POTION) {
			usePotion(item);
		}
	}

	private void usePotion(InventoryItem item) {
		if (item.data.heal > 0) {
			this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + item.data.heal);
			// Remove potion from inventory
			this.inventory.remove(item);
		}
	}

	private void updateStats() {
		int baseAtk = 5; // Base attack
		int baseDef = 0; // Base defense

		if (equipment.weapon != null) {
			baseAtk += equipment.weapon.data.atk;
		}
		if (equipment.armor != null) {
			baseDef += equipment.armor.data.def;
		}

		this.stats.atk = baseAtk;
		this.stats.def = baseDef;
	}
}
