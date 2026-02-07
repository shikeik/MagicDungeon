package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import java.util.UUID;

public class InventoryItem {
	public String id;
	public ItemData data;
	
	// Dynamic Stats
	public ItemQuality quality;
	public int atk;
	public int def;
	public int heal;
	
	public int count = 1;

	public InventoryItem() {
	}

	public InventoryItem(ItemData data) {
		this(data, new RandomXS128()); // Default random if not provided
	}
	
	public InventoryItem(ItemData data, RandomXS128 rng) {
		this.id = UUID.randomUUID().toString();
		this.data = data;
		
		// Determine Quality
		float r = rng.nextFloat();
		if (r < 0.05f) this.quality = ItemQuality.EPIC;      // 5%
		else if (r < 0.20f) this.quality = ItemQuality.RARE; // 15%
		else if (r < 0.70f) this.quality = ItemQuality.COMMON; // 50%
		else this.quality = ItemQuality.POOR;                // 30%
		
		// Calculate Stats
		// Formula: Base * QualityMultiplier * Fluctuation(0.9 ~ 1.1)
		float fluctuation = 0.9f + rng.nextFloat() * 0.2f;
		float multiplier = this.quality.multiplier * fluctuation;
		
		this.atk = Math.max(0, Math.round(data.atk * multiplier));
		this.def = Math.max(0, Math.round(data.def * multiplier));
		this.heal = Math.max(0, Math.round(data.heal * multiplier));
		
		// Ensure non-zero base stats don't become zero unless intended
		if (data.atk > 0 && this.atk == 0) this.atk = 1;
		if (data.def > 0 && this.def == 0) this.def = 1;
		if (data.heal > 0 && this.heal == 0) this.heal = 1;
	}
	
	// Constructor for restoring from save
	public InventoryItem(ItemData data, ItemQuality quality, int atk, int def, int heal) {
		this.id = UUID.randomUUID().toString();
		this.data = data;
		this.quality = quality;
		this.atk = atk;
		this.def = def;
		this.heal = heal;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InventoryItem that = (InventoryItem) o;
		return id != null && id.equals(that.id);
	}

	@Override
	public int hashCode() {
		return id != null ? id.hashCode() : 0;
	}
}
