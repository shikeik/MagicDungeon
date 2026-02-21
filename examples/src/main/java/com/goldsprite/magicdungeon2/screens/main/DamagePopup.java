package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.graphics.Color;

/**
 * 伤害飘字数据类
 * 用于在世界坐标上显示伤害数值、状态提示等文字动画
 */
public class DamagePopup {
	public float x, y, timer;
	public String text;
	public Color color;

	public DamagePopup(float x, float y, String text, Color color) {
		this.x = x;
		this.y = y;
		this.text = text;
		this.color = color;
		this.timer = 1.0f;
	}
}
