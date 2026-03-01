package com.goldsprite.magicdungeon2.screens.tests.netcode;

import com.badlogic.gdx.graphics.Color;

/**
 * 子弹数据结构，Server 和 Client 共用。
 */
public class Bullet {
    public float x, y, vx, vy;
    public int ownerId;
    public Color color;
}
