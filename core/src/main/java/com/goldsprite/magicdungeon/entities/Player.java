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
		public InventoryItem mainHand;
		public InventoryItem offHand;
		public InventoryItem helmet;
		public InventoryItem armor;
		public InventoryItem boots;
		public InventoryItem[] accessories = new InventoryItem[3];
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
				audio.playAttack(); // Play attack sound
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

	public boolean useSkill(AudioSystem audio) {
		if (this.stats.mana >= 10) {
			this.stats.mana -= 10;
			this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + 20);
			audio.playSkill();
			return true;
		}
		return false;
	}

	public void equip(InventoryItem item) {
		switch (item.data.type) {
			case MAIN_HAND:
				this.equipment.mainHand = item;
				break;
			case OFF_HAND:
				this.equipment.offHand = item;
				break;
			case HELMET:
				this.equipment.helmet = item;
				break;
			case ARMOR:
				this.equipment.armor = item;
				break;
			case BOOTS:
				this.equipment.boots = item;
				break;
			case ACCESSORY:
				equipAccessory(item);
				break;
			case POTION:
				usePotion(item);
				return; // Don't update stats for potion use
			default:
				// Do nothing for unknown types
				return;
		}
		updateStats();
		updateVisuals();
	}

	private void updateVisuals() {
		// Update player texture in TextureManager or notify systems
		// Since TextureManager is global, we can regenerate the "PLAYER" texture
		// But "PLAYER" texture is shared. If we have multiple players or want dynamic updates,
		// we should probably store the texture in the Player entity or update the shared one.
		// For single player game, updating the global texture is fine.
		
		String mainHand = equipment.mainHand != null ? equipment.mainHand.data.name() : null;
		String offHand = equipment.offHand != null ? equipment.offHand.data.name() : null;
		String helmet = equipment.helmet != null ? equipment.helmet.data.name() : null;
		String armor = equipment.armor != null ? equipment.armor.data.name() : null;
		String boots = equipment.boots != null ? equipment.boots.data.name() : null;
		
		com.badlogic.gdx.graphics.Texture newTex = com.goldsprite.magicdungeon.utils.SpriteGenerator.generateCharacterTexture(mainHand, offHand, helmet, armor, boots);
		com.goldsprite.magicdungeon.assets.TextureManager.getInstance().updateTexture("PLAYER", newTex);
	}

	private void equipAccessory(InventoryItem item) {
		// 1. Try to fill empty slot
		for (int i = 0; i < equipment.accessories.length; i++) {
			if (equipment.accessories[i] == null) {
				equipment.accessories[i] = item;
				return;
			}
		}
		// 2. If full, replace the first one (or maybe implement a way to choose?)
		// For now, just replace slot 0
		equipment.accessories[0] = item;
	}

	public boolean addItem(InventoryItem newItem) {
		// 1. Try to stack if it's a potion
		if (newItem.data.type == ItemType.POTION) {
			// Iterate backwards to find stackable item
			for (int i = inventory.size() - 1; i >= 0; i--) {
				InventoryItem existing = inventory.get(i);
				if (existing.data == newItem.data && existing.quality == newItem.quality) {
					existing.count += newItem.count;
					return true;
				}
			}
		}
		
		// 2. Add to new slot if space available
		if (inventory.size() < 30) {
			inventory.add(newItem);
			return true;
		}
		
		// 3. Inventory full
		return false;
	}

	public void usePotion(InventoryItem item) {
		if (item.data == ItemData.Mana_Potion) {
			this.stats.mana = Math.min(this.stats.maxMana, this.stats.mana + item.heal);
		} else if (item.heal > 0) {
			this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + item.heal);
		}
		
		item.count--;
		if (item.count <= 0) {
			this.inventory.remove(item);
		}
	}

	private void updateStats() {
		int baseAtk = 5; // Base attack
		int baseDef = 0; // Base defense

		if (equipment.mainHand != null) baseAtk += equipment.mainHand.atk;
		if (equipment.offHand != null) baseDef += equipment.offHand.def; // Shields give def, maybe atk?
		if (equipment.helmet != null) baseDef += equipment.helmet.def;
		if (equipment.armor != null) baseDef += equipment.armor.def;
		if (equipment.boots != null) baseDef += equipment.boots.def;
		
		for (InventoryItem acc : equipment.accessories) {
			if (acc != null) {
				baseAtk += acc.atk;
				baseDef += acc.def;
			}
		}

		this.stats.atk = baseAtk;
		this.stats.def = baseDef;
	}
}
