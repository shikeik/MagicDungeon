package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import java.util.List;

public class GameState {
    public int dungeonLevel;
    public PlayerStats playerStats;
    public List<InventoryItem> inventory;

    public GameState() {
        // No-arg constructor for Json
    }
}
