package com.goldsprite.magicdungeon.model;

import java.util.HashMap;
import java.util.Map;

/**
 * 区域数据 (运行时/编辑器使用)
 * 包含该区域的所有层级数据
 */
public class AreaData {
    public String areaId;
    
    // 层数据映射 (层号 -> 数据)
    public Map<Integer, LayerData> layers = new HashMap<>();

    public AreaData() {
    }

    public AreaData(String areaId) {
        this.areaId = areaId;
    }
    
    public void setLayer(int level, LayerData data) {
        layers.put(level, data);
    }
    
    public LayerData getLayer(int level) {
        return layers.get(level);
    }
}
