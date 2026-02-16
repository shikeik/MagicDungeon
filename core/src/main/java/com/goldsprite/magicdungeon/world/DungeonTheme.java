package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.magicdungeon.entities.MonsterType;

public enum DungeonTheme {
    DEFAULT("默认地牢", Color.GRAY, Color.DARK_GRAY, 
            Color.valueOf("#3E3E3E"), Color.valueOf("#2E2E2E"), Color.valueOf("#4E4E4E"),
            new MonsterType[]{MonsterType.Slime, MonsterType.Bat}),
    FOREST("迷雾森林", Color.FOREST, Color.OLIVE,
            Color.valueOf("#4E5E3E"), Color.valueOf("#3E4E2E"), Color.valueOf("#5E6E4E"),
            new MonsterType[]{MonsterType.Wolf, MonsterType.Slime, MonsterType.Bat}),
    DESERT("灼热沙漠", Color.GOLD, Color.ORANGE,
            Color.valueOf("#8D6E63"), Color.valueOf("#6D4C41"), Color.valueOf("#A1887F"),
            new MonsterType[]{MonsterType.Skeleton, MonsterType.Orc}),
    CASTLE("深渊城堡", Color.PURPLE, Color.MAROON,
            Color.valueOf("#3E3E4E"), Color.valueOf("#2E2E3E"), Color.valueOf("#4E4E5E"),
            new MonsterType[]{MonsterType.Skeleton, MonsterType.Bat, MonsterType.Orc, MonsterType.Boss});

    public final String name;
    public final Color primaryColor;
    public final Color secondaryColor;
    public final Color floorBase;
    public final Color floorDark;
    public final Color floorHighlight;
    public final MonsterType[] allowedMonsters;

    DungeonTheme(String name, Color primary, Color secondary, Color fBase, Color fDark, Color fHigh, MonsterType[] monsters) {
        this.name = name;
        this.primaryColor = primary;
        this.secondaryColor = secondary;
        this.floorBase = fBase;
        this.floorDark = fDark;
        this.floorHighlight = fHigh;
        this.allowedMonsters = monsters;
    }
}
