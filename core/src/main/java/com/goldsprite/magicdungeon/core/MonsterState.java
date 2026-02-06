package com.goldsprite.magicdungeon.core;

public class MonsterState {
	public int x;
	public int y;
	public String typeName;
	public int hp;
	public int maxHp;

	public MonsterState() {}

	public MonsterState(int x, int y, String typeName, int hp, int maxHp) {
		this.x = x;
		this.y = y;
		this.typeName = typeName;
		this.hp = hp;
		this.maxHp = maxHp;
	}
}
