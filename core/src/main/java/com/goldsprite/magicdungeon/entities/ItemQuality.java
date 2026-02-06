package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum ItemQuality {
	POOR("劣质", Color.GRAY, 0.8f),
	COMMON("普通", Color.WHITE, 1.0f),
	RARE("精致", Color.CYAN, 1.2f),
	EPIC("完美", Color.PURPLE, 1.5f);

	public final String name;
	public final Color color;
	public final float multiplier;

	ItemQuality(String name, Color color, float multiplier) {
		this.name = name;
		this.color = color;
		this.multiplier = multiplier;
	}
}
