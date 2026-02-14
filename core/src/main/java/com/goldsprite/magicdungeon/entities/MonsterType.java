package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum MonsterType {
	Slime("史莱姆", 20, 5, Color.BLUE, 1.0f),
	Wolf("恶狼", 30, 8, Color.GRAY, 0.6f),
	Skeleton("骷髅", 40, 10, Color.LIGHT_GRAY, 0.8f),
	Orc("兽人", 60, 15, Color.OLIVE, 0.6f),
	Bat("蝙蝠", 10, 3, Color.DARK_GRAY, 0.4f),
	Boss("魔龙", 200, 30, Color.RED, 0.5f);

	public final String name;
	public final int maxHp;
	public final int atk;
	public final Color color;
	public final float speed;

	MonsterType(String name, int maxHp, int atk, Color color, float speed) {
		this.name = name;
		this.maxHp = maxHp;
		this.atk = atk;
		this.color = color;
		this.speed = speed;
	}
}
