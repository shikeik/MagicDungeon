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
    
    // 压缩后的地图数据 (Space separated IDs)
    // ID 对应 TileType.ordinal()
    public String compressedFloors;
    public String compressedBlocks;
    
    // 实体列表
    public List<MonsterState> monsters = new ArrayList<>();
    
    // 掉落物列表
    public List<ItemState> items = new ArrayList<>();

    public LayerData() {
    }

    public LayerData(int width, int height) {
        this.width = width;
        this.height = height;
        // Data initialized empty
    }
}
