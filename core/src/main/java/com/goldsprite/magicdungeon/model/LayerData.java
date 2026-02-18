package com.goldsprite.magicdungeon.model;

import com.goldsprite.magicdungeon.core.ItemState;
import com.goldsprite.magicdungeon.core.MonsterState;
import java.util.ArrayList;
import java.util.List;

/**
 * 单层地图数据 (floor_x.json)
 */
public class LayerData {
    public int width;
    public int height;
    
    // 地皮层 (Ground Layer) - 存储 TileType.name()
    public String[] floorIds;
    
    // 方块层 (Block Layer) - 存储 TileType.name() (Wall, Decor, Interactive)
    public String[] blockIds;
    
    // 实体列表
    public List<MonsterState> monsters = new ArrayList<>();
    
    // 掉落物列表
    public List<ItemState> items = new ArrayList<>();

    public LayerData() {
    }

    public LayerData(int width, int height) {
        this.width = width;
        this.height = height;
        this.floorIds = new String[width * height];
        this.blockIds = new String[width * height];
    }
}
