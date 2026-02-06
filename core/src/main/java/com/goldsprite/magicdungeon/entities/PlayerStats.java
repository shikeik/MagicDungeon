package com.goldsprite.magicdungeon.entities;

public class PlayerStats {
	public int hp;
	public int maxHp;
	public int level;
	public int xp;
	public int xpToNextLevel;
	public int atk;
	public int def;
	public int mana;
	public int maxMana;

	public PlayerStats() {
		this.maxHp = 100;
		this.hp = 100;
		this.level = 1;
		this.xp = 0;
		this.xpToNextLevel = 100;
		this.atk = 5;
		this.def = 0;
		this.maxMana = 50;
		this.mana = 50;
	}

	public void addXp(int amount) {
		this.xp += amount;
		while (this.xp >= this.xpToNextLevel) {
			levelUp();
		}
	}

	private void levelUp() {
		this.level++;
		this.xp -= this.xpToNextLevel;
		this.xpToNextLevel = (int) (this.xpToNextLevel * 1.5f);

		// Stat increases
		this.maxHp += 20;
		this.hp = this.maxHp;
		this.maxMana += 10;
		this.mana = this.maxMana;
		this.atk += 2;
	}
}
