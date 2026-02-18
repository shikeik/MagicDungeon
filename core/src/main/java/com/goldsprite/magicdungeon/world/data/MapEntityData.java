package com.goldsprite.magicdungeon.world.data;

/**
 * 地图实体数据
 */
public class MapEntityData {
    public int x;
    public int y;
    public String type; // MonsterType.name()
    
    public MapEntityData() {}
    
    public MapEntityData(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}
