package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum MonsterType {
    SLIME("Slime", 20, 5, Color.BLUE, 1.0f),
    SKELETON("Skeleton", 40, 10, Color.LIGHT_GRAY, 0.8f),
    ORC("Orc", 60, 15, Color.OLIVE, 0.6f),
    BAT("Bat", 10, 3, Color.DARK_GRAY, 0.4f),
    BOSS("Dragon", 200, 30, Color.RED, 0.5f);

    public final String name;
    public final int maxHp;
    public final int atk;
    public final Color color;
    public final float speed;

    MonsterType(String name, int maxHp, int atk, Color color, float speed) {
        this.name = name;
        this.maxHp = maxHp;
        this.atk = atk;
        this.color = color;
        this.speed = speed;
    }
}
