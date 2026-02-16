package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState {
	public int dungeonLevel;
	public String theme;
	public int maxDepth = 1; // New: Max Depth reached
	public long seed;
	public PlayerStats playerStats;
	public List<InventoryItem> inventory;
	public EquipmentState equipment; // New: Equipment
	
	// World State Snapshot (Current Level)
	public int playerX;
	public int playerY;
	public List<MonsterState> monsters;
	public List<ItemState> items;
	
	// History of visited levels
	// Key: Level Index, Value: Level State
	public Map<Integer, LevelState> visitedLevels = new HashMap<>();

	public GameState() {
		// No-arg constructor for Json
	}
}
