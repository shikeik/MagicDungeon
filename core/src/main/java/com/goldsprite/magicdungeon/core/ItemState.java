package com.goldsprite.magicdungeon.core;

public class ItemState {
	public int x;
	public int y;
	public String itemName;
	
	// Dynamic Stats
	public String quality;
	public int atk;
	public int def;
	public int heal;
	public int manaRegen;
	public int count = 1;

	public ItemState() {}

	public ItemState(int x, int y, String itemName, String quality, int atk, int def, int heal, int manaRegen, int count) {
		this.x = x;
		this.y = y;
		this.itemName = itemName;
		this.quality = quality;
		this.atk = atk;
		this.def = def;
		this.heal = heal;
		this.manaRegen = manaRegen;
		this.count = count;
	}
}
