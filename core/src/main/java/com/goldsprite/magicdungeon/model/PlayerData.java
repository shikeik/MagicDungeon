package com.goldsprite.magicdungeon.model;

import com.badlogic.gdx.math.Vector2;
import com.goldsprite.magicdungeon.core.EquipmentState;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import java.util.List;

/**
 * 玩家数据 (player.json)
 */
public class PlayerData {
    public String name;
    public PlayerStats stats;
    public List<InventoryItem> inventory;
    public EquipmentState equipment;
    public float x, y; // Position

    public PlayerData() {
    }

    public PlayerData(String name, PlayerStats stats, List<InventoryItem> inventory, EquipmentState equipment, float x, float y) {
        this.name = name;
        this.stats = stats;
        this.inventory = inventory;
        this.equipment = equipment;
        this.x = x;
        this.y = y;
    }
}
