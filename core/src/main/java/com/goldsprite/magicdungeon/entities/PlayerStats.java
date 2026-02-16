package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.math.MathUtils;

public class PlayerStats {
	// Base Stats
	public int baseHp = 100;
	public int baseMana = 50;
	public int baseAtk = 5;
	public int baseDef = 0;
	public float baseSpeed = 1.0f;

	// Growth Rates (Per Level) - Can be upgraded
	public float hpGrowth = 20f;
	public float manaGrowth = 10f;
	public float atkGrowth = 2f;
	public float defGrowth = 0.5f;
	public float speedGrowth = 0.05f;

	// Caps (Hard Limits)
	public int hpCap = 9999;
	public int manaCap = 9999;
	public int atkCap = 999;
	public int defCap = 999;
	public float speedCap = 5.0f;

	// Current Runtime Stats
	public int hp;
	public int maxHp;
	public int mana;
	public int maxMana;
	public int atk;
	public int def;
	public float attackSpeed;

	public int level;
	public int xp; // Current level XP
	public int xpToNextLevel;
	
	// Regeneration (Amount per 5 seconds)
	public int hpRegen;
	public int manaRegen;

	public PlayerStats() {
		this.level = 1;
		this.xp = 0;
		this.hpRegen = 1;
		this.manaRegen = 1;
		
		recalculateStats();
		this.hp = this.maxHp;
		this.mana = this.maxMana;
		this.xpToNextLevel = calculateRequiredXp(level);
	}

	public void addXp(int amount) {
		this.xp += amount;
		checkLevelUp();
	}

	private void checkLevelUp() {
		while (this.xp >= this.xpToNextLevel) {
			this.xp -= this.xpToNextLevel;
			this.level++;
			
			// Recalculate requirements for next level
			this.xpToNextLevel = calculateRequiredXp(this.level);
			
			// Update stats
			recalculateStats();
			
			// Heal on level up?
			this.hp = this.maxHp;
			this.mana = this.maxMana;
		}
	}

	/**
	 * Formula: XP = Base * (Level ^ 1.5)
	 * Level 1->2: 100
	 * Level 2->3: ~280
	 */
	public int calculateRequiredXp(int lvl) {
		return (int) (100 * Math.pow(lvl, 1.5f));
	}

	/**
	 * Recalculate stats based on Level and Growth Rates.
	 * Stat = Base + (Level - 1) * Growth
	 */
	public void recalculateStats() {
		this.maxHp = (int) Math.min(hpCap, baseHp + (level - 1) * hpGrowth);
		this.maxMana = (int) Math.min(manaCap, baseMana + (level - 1) * manaGrowth);
		this.atk = (int) Math.min(atkCap, baseAtk + (level - 1) * atkGrowth);
		this.def = (int) Math.min(defCap, baseDef + (level - 1) * defGrowth);
		this.attackSpeed = Math.min(speedCap, baseSpeed + (level - 1) * speedGrowth);
		
		// Ensure current HP/Mana don't exceed new Max
		if (this.hp > this.maxHp) this.hp = this.maxHp;
		if (this.mana > this.maxMana) this.mana = this.maxMana;
	}
}
