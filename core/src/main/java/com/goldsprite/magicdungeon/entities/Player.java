package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import com.badlogic.gdx.graphics.Texture;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;

public class Player extends Entity {
	public float moveTimer;
	public float moveDelay;
	
	private float regenTimer = 0;
	
	public long coins = 0;
	
	public Set<String> discoveredItems = new HashSet<>();

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
		// Regeneration Logic (Every 5 seconds)
		regenTimer += dt;
		if (regenTimer >= 5.0f) {
			regenTimer = 0;
			if (this.stats.hp < this.stats.maxHp) {
				this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + this.stats.hpRegen);
			}
			if (this.stats.mana < this.stats.maxMana) {
				this.stats.mana = Math.min(this.stats.maxMana, this.stats.mana + this.stats.manaRegen);
			}
		}

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
		equip(item, -1);
	}

	public void equip(InventoryItem item, int slotIndex) {
		switch (item.data.type) {
			case MAIN_HAND:
				if (this.equipment.mainHand == item) {
					this.equipment.mainHand = null;
				} else {
					this.equipment.mainHand = item;
				}
				break;
			case OFF_HAND:
				if (this.equipment.offHand == item) {
					this.equipment.offHand = null;
				} else {
					this.equipment.offHand = item;
				}
				break;
			case HELMET:
				if (this.equipment.helmet == item) {
					this.equipment.helmet = null;
				} else {
					this.equipment.helmet = item;
				}
				break;
			case ARMOR:
				if (this.equipment.armor == item) {
					this.equipment.armor = null;
				} else {
					this.equipment.armor = item;
				}
				break;
			case BOOTS:
				if (this.equipment.boots == item) {
					this.equipment.boots = null;
				} else {
					this.equipment.boots = item;
				}
				break;
			case ACCESSORY:
				equipAccessory(item, slotIndex);
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

	public void updateVisuals() {
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
		
		Texture newTex = SpriteGenerator.generateCharacterTexture(mainHand, offHand, helmet, armor, boots);
		TextureManager.getInstance().updateTexture("PLAYER", newTex);
	}

	private void equipAccessory(InventoryItem item, int slotIndex) {
		// 0. If slotIndex is valid, use that specific slot
		if (slotIndex >= 0 && slotIndex < equipment.accessories.length) {
			// If clicking the same item in the same slot -> Unequip
			if (equipment.accessories[slotIndex] == item) {
				equipment.accessories[slotIndex] = null;
			} else {
				equipment.accessories[slotIndex] = item;
			}
			return;
		}

		// 1. Check if already equipped (Unequip)
		for (int i = 0; i < equipment.accessories.length; i++) {
			if (equipment.accessories[i] == item) {
				equipment.accessories[i] = null;
				return;
			}
		}

		// 2. Try to fill empty slot
		for (int i = 0; i < equipment.accessories.length; i++) {
			if (equipment.accessories[i] == null) {
				equipment.accessories[i] = item;
				return;
			}
		}
		// 3. If full, replace the first one
		equipment.accessories[0] = item;
	}

	public boolean addItem(InventoryItem newItem) {
		if (newItem != null && newItem.data != null) {
			discoveredItems.add(newItem.data.name());
		}

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
		if (inventory.size() < Constants.MAX_INVENTORY_SLOTS) {
			inventory.add(newItem);
			return true;
		}
		
		// 3. Inventory full
		return false;
	}

	public void sellItem(InventoryItem item) {
		// Unequip first if equipped
		if (equipment.mainHand == item) equipment.mainHand = null;
		else if (equipment.offHand == item) equipment.offHand = null;
		else if (equipment.helmet == item) equipment.helmet = null;
		else if (equipment.armor == item) equipment.armor = null;
		else if (equipment.boots == item) equipment.boots = null;
		else {
			for(int i=0; i<equipment.accessories.length; i++) {
				if (equipment.accessories[i] == item) {
					equipment.accessories[i] = null;
					break;
				}
			}
		}
		
		// Calculate Price
		// Base value * quality
		int price = item.getValue();
		this.coins += price * item.count;
		
		this.inventory.remove(item);
		updateStats();
		updateVisuals();
	}

	public void usePotion(InventoryItem item) {
		if (item.data == ItemData.Mana_Potion) {
			this.stats.mana = Math.min(this.stats.maxMana, this.stats.mana + item.manaRegen);
		} else if (item.data == ItemData.Elixir) {
			this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + item.heal);
			this.stats.mana = Math.min(this.stats.maxMana, this.stats.mana + item.manaRegen);
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
		int baseHpRegen = 1; // Base HP Regen
		int baseManaRegen = 1; // Base Mana Regen

		if (equipment.mainHand != null) {
			baseAtk += equipment.mainHand.atk;
			baseHpRegen += equipment.mainHand.heal;
			baseManaRegen += equipment.mainHand.manaRegen;
		}
		if (equipment.offHand != null) {
			baseDef += equipment.offHand.def; // Shields give def, maybe atk?
			baseHpRegen += equipment.offHand.heal;
			baseManaRegen += equipment.offHand.manaRegen;
		}
		if (equipment.helmet != null) {
			baseDef += equipment.helmet.def;
			baseHpRegen += equipment.helmet.heal;
			baseManaRegen += equipment.helmet.manaRegen;
		}
		if (equipment.armor != null) {
			baseDef += equipment.armor.def;
			baseHpRegen += equipment.armor.heal;
			baseManaRegen += equipment.armor.manaRegen;
		}
		if (equipment.boots != null) {
			baseDef += equipment.boots.def;
			baseHpRegen += equipment.boots.heal;
			baseManaRegen += equipment.boots.manaRegen;
		}
		
		for (InventoryItem acc : equipment.accessories) {
			if (acc != null) {
				baseAtk += acc.atk;
				baseDef += acc.def;
				baseHpRegen += acc.heal;
				baseManaRegen += acc.manaRegen;
			}
		}

		this.stats.atk = baseAtk;
		this.stats.def = baseDef;
		this.stats.hpRegen = baseHpRegen;
		this.stats.manaRegen = baseManaRegen;
	}
}
