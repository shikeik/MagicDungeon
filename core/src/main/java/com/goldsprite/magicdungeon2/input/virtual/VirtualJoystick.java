package com.goldsprite.magicdungeon2.input.virtual;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.magicdungeon2.input.InputManager;

/**
 * 基于 libGDX Touchpad 的虚拟摇杆
 *
 * 继承 Touchpad，每帧将旋钮偏移量通过 InputManager.injectAxis() 注入。
 * Touchpad 已内置死区、旋钮拖拽限制、多指隔离等功能，
 * 本类只需关注信号注入和纹理设置。
 *
 * 使用方式:
 *   VirtualJoystick stick = new VirtualJoystick(baseTex, knobTex, 10f, InputManager.AXIS_LEFT);
 *   stage.addActor(stick);
 */
public class VirtualJoystick extends Touchpad {

	// 注入的轴向 ID
	private final int axisId;

	/**
	 * 使用纹理构造
	 * @param baseTex  底盘纹理
	 * @param knobTex  旋钮纹理
	 * @param deadzone 死区半径（像素）
	 * @param axisId   InputManager.AXIS_LEFT 或 AXIS_RIGHT
	 */
	public VirtualJoystick(TextureRegion baseTex, TextureRegion knobTex, float deadzone, int axisId) {
		super(deadzone, buildStyle(baseTex, knobTex));
		this.axisId = axisId;
	}

	/**
	 * 使用 Skin 构造
	 * @param deadzone 死区半径
	 * @param skin     包含 "touchpadBackground" 和 "touchpadKnob" 的 Skin
	 * @param axisId   InputManager.AXIS_LEFT 或 AXIS_RIGHT
	 */
	public VirtualJoystick(float deadzone, Skin skin, int axisId) {
		super(deadzone, skin);
		this.axisId = axisId;
	}

	/**
	 * 使用已有 Style 构造
	 */
	public VirtualJoystick(float deadzone, TouchpadStyle style, int axisId) {
		super(deadzone, style);
		this.axisId = axisId;
	}

	/**
	 * 每帧由 Stage 自动调用，将摇杆偏移注入到 InputManager
	 */
	@Override
	public void act(float delta) {
		super.act(delta);
		// getKnobPercentX/Y 返回 [-1, 1] 归一化值（已考虑死区）
		float px = getKnobPercentX();
		float py = getKnobPercentY();
		InputManager.getInstance().injectAxis(axisId, px, py);
	}

	// ---- 工具方法 ----

	private static TouchpadStyle buildStyle(TextureRegion baseTex, TextureRegion knobTex) {
		TouchpadStyle style = new TouchpadStyle();
		if (baseTex != null) {
			style.background = new TextureRegionDrawable(baseTex);
		}
		if (knobTex != null) {
			style.knob = new TextureRegionDrawable(knobTex);
		}
		return style;
	}

	/** 运行时替换底盘纹理 */
	public void setBaseTexture(TextureRegion tex) {
		TouchpadStyle s = getStyle();
		s.background = tex != null ? new TextureRegionDrawable(tex) : null;
		setStyle(s);
	}

	/** 运行时替换旋钮纹理 */
	public void setKnobTexture(TextureRegion tex) {
		TouchpadStyle s = getStyle();
		s.knob = tex != null ? new TextureRegionDrawable(tex) : null;
		setStyle(s);
	}
}
