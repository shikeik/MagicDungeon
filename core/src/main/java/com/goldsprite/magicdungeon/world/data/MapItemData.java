package com.goldsprite.magicdungeon.world.data;

/**
 * 地图物品数据
 */
public class MapItemData {
    public int x;
    public int y;
    public String itemId; // ItemData.name()
    
    public MapItemData() {}
    
    public MapItemData(int x, int y, String itemId) {
        this.x = x;
        this.y = y;
        this.itemId = itemId;
    }
}
