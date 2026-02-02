package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import java.util.List;

public class GameState {
    public int dungeonLevel;
    public PlayerStats playerStats;
    public List<ItemData> inventory;

    public GameState() {
        // No-arg constructor for Json
    }
}
