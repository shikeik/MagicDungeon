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
import com.goldsprite.magicdungeon.vfx.VFXManager;

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

		this.stats = new PlayerStats();
		
		this.moveTimer = 0;
		// Base move delay, will be modified by attackSpeed
		this.moveDelay = 0.25f; 
		this.visualSpeed = Constants.TILE_SIZE / 0.12f; // Keep visual speed fast

		this.inventory = new ArrayList<>();
		this.equipment = new Equipment();
	}

	public void update(float dt, Dungeon dungeon, int dx, int dy, List<Monster> monsters, AudioSystem audio, VFXManager vfx) {
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

				// VFX
				if (vfx != null) {
					vfx.spawnExplosion(targetMonster.visualX + 16, targetMonster.visualY + 16, Color.WHITE, 5);
					vfx.spawnFloatingText(targetMonster.visualX + 16, targetMonster.visualY + 32, "" + this.stats.atk, Color.WHITE);
				}

				// Simple knockback/hit effect could be added here
				if (targetMonster.hp <= 0) {
                    int oldLevel = this.stats.level;
					// Monster died - Gain XP
					this.stats.addXp(10 + targetMonster.maxHp / 2);
                    
                    if (this.stats.level > oldLevel) {
                        audio.playLevelUp();
                        if (vfx != null) {
                            vfx.spawnExplosion(this.visualX + 16, this.visualY + 16, Color.GOLD, 30);
                            vfx.spawnFloatingText(this.visualX + 16, this.visualY + 48, "LEVEL UP!", Color.GOLD);
                        }
                    }
				}
				
				// Calculate delay based on attack speed
				// Base 0.25s / speed
				this.moveTimer = this.moveDelay / this.stats.attackSpeed;
				return;
			}

			if (dungeon.isWalkable(nextX, nextY)) {
				this.x = nextX;
				this.y = nextY;
				// Movement also uses attack speed or separate move speed?
				// Usually move speed. For now share.
				this.moveTimer = this.moveDelay / this.stats.attackSpeed;
				audio.playMove();
			} else {
				// Wall bump - Cancelled as per request
				// triggerBump(dx, dy);
				this.moveTimer = this.moveDelay / this.stats.attackSpeed;
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
	
	private void equipAccessory(InventoryItem item, int slotIndex) {
		if (slotIndex >= 0 && slotIndex < 3) {
			if (this.equipment.accessories[slotIndex] == item) {
				this.equipment.accessories[slotIndex] = null;
			} else {
				this.equipment.accessories[slotIndex] = item;
			}
			return;
		}
		
		// Auto-equip to first empty or swap?
		for(int i=0; i<3; i++) {
			if (this.equipment.accessories[i] == null) {
				this.equipment.accessories[i] = item;
				return;
			}
		}
		// Swap first
		this.equipment.accessories[0] = item;
	}

	public void updateVisuals() {
		// Update player texture in TextureManager or notify systems
		// Since TextureManager is global, we can regenerate the "PLAYER" texture
		// But "PLAYER" texture is shared. If we have multiple players or want dynamic updates,
		// we should probably store the texture in the Player entity or update the shared one.
	}

	public boolean addItem(InventoryItem newItem) {
		// 1. Check for stackable items
		if (newItem.data.stackable) {
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

	public void applyDeathPenalty() {
		// 1. Level Penalty: Lose 3 levels (min level 1)
		int targetLevel = Math.max(1, stats.level - 3);
		
		// Reset stats
		stats.level = targetLevel;
		stats.xp = 0;
		stats.xpToNextLevel = stats.calculateRequiredXp(targetLevel);
		
		// Recalculate stats for new level
		stats.recalculateStats();
		stats.hp = stats.maxHp;
		stats.mana = stats.maxMana;

		// 2. Item Penalty: Lose 2-5 random items
		int dropCount = com.badlogic.gdx.math.MathUtils.random(2, 5);
		int dropped = 0;
		for (int i = 0; i < dropCount; i++) {
			if (inventory.isEmpty()) break;
			int idx = com.badlogic.gdx.math.MathUtils.random(inventory.size() - 1);
			inventory.remove(idx);
			dropped++;
		}
		System.out.println("Death Penalty: Level -> " + targetLevel + ", Dropped " + dropped + " items.");
		
		updateStats();
	}

	private void updateStats() {
		// Reset to base stats (level based)
		this.stats.recalculateStats();

		int baseHpRegen = 1; 
		int baseManaRegen = 1; 

		if (equipment.mainHand != null) {
			this.stats.atk += equipment.mainHand.atk;
			baseHpRegen += equipment.mainHand.heal;
			baseManaRegen += equipment.mainHand.manaRegen;
		}
		if (equipment.offHand != null) {
			this.stats.def += equipment.offHand.def; 
			baseHpRegen += equipment.offHand.heal;
			baseManaRegen += equipment.offHand.manaRegen;
		}
		if (equipment.helmet != null) {
			this.stats.def += equipment.helmet.def;
			baseHpRegen += equipment.helmet.heal;
			baseManaRegen += equipment.helmet.manaRegen;
		}
		if (equipment.armor != null) {
			this.stats.def += equipment.armor.def;
			baseHpRegen += equipment.armor.heal;
			baseManaRegen += equipment.armor.manaRegen;
		}
		if (equipment.boots != null) {
			this.stats.def += equipment.boots.def;
			baseHpRegen += equipment.boots.heal;
			baseManaRegen += equipment.boots.manaRegen;
		}
		
		for (InventoryItem acc : equipment.accessories) {
			if (acc != null) {
				this.stats.atk += acc.atk;
				this.stats.def += acc.def;
				baseHpRegen += acc.heal;
				baseManaRegen += acc.manaRegen;
			}
		}

        // Apply Caps
        this.stats.atk = Math.min(this.stats.atkCap, this.stats.atk);
        this.stats.def = Math.min(this.stats.defCap, this.stats.def);

		this.stats.hpRegen = baseHpRegen;
		this.stats.manaRegen = baseManaRegen;
	}
}
