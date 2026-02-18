package com.goldsprite.magicdungeon.world.data;

import java.util.List;

/**
 * 游戏地图数据结构 (JSON 序列化用)
 * 用于编辑器导出和游戏加载
 */
public class GameMapData {
    public int width;
    public int height;
    public String themeName;
    
    // 地图网格数据 (扁平化后的单层)
    // 存储 TileType.name()
    public String[][] grid; 
    
    // 实体列表
    public List<MapEntityData> entities;
    
    // 物品列表
    public List<MapItemData> items;
    
    public GameMapData() {}
}
