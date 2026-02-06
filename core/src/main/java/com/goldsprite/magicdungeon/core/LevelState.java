package com.goldsprite.magicdungeon.core;

import java.util.List;

public class LevelState {
	public List<MonsterState> monsters;
	public List<ItemState> items;
	
	public LevelState() {}
	
	public LevelState(List<MonsterState> monsters, List<ItemState> items) {
		this.monsters = monsters;
		this.items = items;
	}
}
