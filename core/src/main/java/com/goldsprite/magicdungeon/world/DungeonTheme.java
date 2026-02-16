package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.graphics.Color;

public enum DungeonTheme {
    DEFAULT("默认地牢", Color.GRAY, Color.DARK_GRAY),
    FOREST("迷雾森林", Color.FOREST, Color.OLIVE),
    DESERT("灼热沙漠", Color.GOLD, Color.ORANGE),
    CASTLE("深渊城堡", Color.PURPLE, Color.MAROON);

    public final String name;
    public final Color primaryColor;
    public final Color secondaryColor;

    DungeonTheme(String name, Color primary, Color secondary) {
        this.name = name;
        this.primaryColor = primary;
        this.secondaryColor = secondary;
    }
}
