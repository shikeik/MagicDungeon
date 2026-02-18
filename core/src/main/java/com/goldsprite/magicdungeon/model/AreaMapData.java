package com.goldsprite.magicdungeon.model;

import java.util.HashMap;
import java.util.Map;

public class AreaMapData {
    public String areaId;
    // Map<FloorNumber, LayerData>
    public Map<Integer, LayerData> floors = new HashMap<>();
    
    public AreaMapData() {}
    public AreaMapData(String areaId) {
        this.areaId = areaId;
    }
}
