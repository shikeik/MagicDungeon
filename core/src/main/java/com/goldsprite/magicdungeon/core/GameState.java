package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import java.util.List;

public class GameState {
	public int dungeonLevel;
	public long seed;
	public PlayerStats playerStats;
	public List<InventoryItem> inventory;
	
	// World State Snapshot
	public int playerX;
	public int playerY;
	public List<MonsterState> monsters;
	public List<ItemState> items;

	public GameState() {
		// No-arg constructor for Json
	}
}
