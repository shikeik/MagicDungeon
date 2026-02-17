package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.graphics.Color;

/**
 * 统一主题配色配置
 * 用于 SpriteGenerator 和 UI 渲染
 */
public class ThemeConfig {
    // --- Environment ---
    public static final Color FLOOR_BASE = Color.valueOf("#3E3E3E");
    public static final Color FLOOR_DARK = Color.valueOf("#2E2E2E");
    public static final Color FLOOR_HIGHLIGHT = Color.valueOf("#4E4E4E");
    
    public static final Color WALL_TOP = Color.valueOf("#666666"); // approximate from logic
    public static final Color WALL_FACE = Color.valueOf("#555555");
    
    public static final Color GRASS_BG = Color.valueOf("#4caf50");
    public static final Color SAND_BG = Color.valueOf("#fff59d");

    // --- Character ---
    public static final Color SKIN_DEFAULT = Color.valueOf("#ffccaa");
    public static final Color PANTS_BROWN = Color.valueOf("#8d6e63");
    public static final Color HAIR_BROWN = Color.valueOf("#5d4037");
    
    // --- Monsters ---
    public static final Color SLIME_BODY = Color.valueOf("#44aaff");
    public static final Color SLIME_HIGHLIGHT = Color.valueOf("#88ccff");
    
    // --- Items ---
    public static final Color IRON_GRAY = Color.GRAY;
    public static final Color WOOD_BROWN = Color.valueOf("#5d4037");
    public static final Color LEATHER_BROWN = Color.valueOf("#3e2723");
    public static final Color POTION_RED = Color.valueOf("#e53935");
    public static final Color POTION_BLUE = Color.valueOf("#1e88e5");
}
