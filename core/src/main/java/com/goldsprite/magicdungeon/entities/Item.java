package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public class Item {
    public int x;
    public int y;
    public ItemData data;

    // Visual Position (Pixels) - Static for items usually, but kept for consistency if we animate drop
    public float visualX;
    public float visualY;

    public Item(int x, int y, ItemData data) {
        this.x = x;
        this.y = y;
        this.data = data;

        this.visualX = x * 32; // Hardcoded TILE_SIZE for now to avoid circular dependency or import issues if Constants not available
        this.visualY = y * 32;
    }
}
