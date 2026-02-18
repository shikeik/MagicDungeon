package com.goldsprite.magicdungeon.screens.tests.dualgrid;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GridData {
    private final List<int[][]> layers = new ArrayList<>();

    public GridData() {
        for (int i = 0; i < DualGridConfig.DEFAULT_LAYERS; i++) {
            addLayer();
        }
    }

    public void addLayer() {
        int[][] layer = new int[DualGridConfig.GRID_W][DualGridConfig.GRID_H];
        for (int x = 0; x < DualGridConfig.GRID_W; x++) {
            Arrays.fill(layer[x], -1);
        }
        layers.add(layer);
    }

    public boolean removeLayer(int index) {
        if (index >= 0 && index < layers.size()) {
            layers.remove(index);
            return true;
        }
        return false;
    }

    public int getLayerCount() {
        return layers.size();
    }

    public void clearAll() {
        for (int[][] layer : layers) {
            for (int x = 0; x < DualGridConfig.GRID_W; x++) {
                Arrays.fill(layer[x], -1);
            }
        }
    }

    public void setTile(int layer, int x, int y, TerrainType type) {
        if (layer < 0 || layer >= layers.size()) return;
        if (x < 0 || x >= DualGridConfig.GRID_W || y < 0 || y >= DualGridConfig.GRID_H) return;
        layers.get(layer)[x][y] = type.id; // 同层 ID 互斥，直接覆盖
    }

    public int getTileId(int layer, int x, int y) {
        if (layer < 0 || layer >= layers.size() || x < 0 || x >= DualGridConfig.GRID_W || y < 0 || y >= DualGridConfig.GRID_H) return -1;
        return layers.get(layer)[x][y];
    }
}
