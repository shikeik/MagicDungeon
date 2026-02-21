package com.goldsprite.magicdungeon2.input.virtual;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;

/**
 * Scene2D 虚拟按钮控件
 *
 * 按下时调用 InputManager.injectAction(action, true)，
 * 松开时调用 InputManager.injectAction(action, false)。
 * 仅作为"信号发生器"，业务逻辑层无需知道信号来源。
 *
 * 使用方式:
 *   VirtualButton btn = new VirtualButton(attackTex, InputAction.ATTACK, 64);
 *   stage.addActor(btn);
 */
public class VirtualButton extends Widget {

	private TextureRegion texture;
	private final InputAction action;
	private final float btnSize;

	private boolean pressed = false;
	private int touchPointer = -1;

	// 按下时的颜色变暗系数
	private static final float PRESS_TINT = 0.7f;

	/**
	 * @param texture 按钮纹理 (可为 null，则不绘制)
	 * @param action  绑定的 InputAction
	 * @param size    按钮尺寸（宽高相同，世界坐标）
	 */
	public VirtualButton(TextureRegion texture, InputAction action, float size) {
		this.texture = texture;
		this.action = action;
		this.btnSize = size;
		setSize(size, size);

		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (pressed) return false;
				pressed = true;
				touchPointer = pointer;
				InputManager.getInstance().injectAction(VirtualButton.this.action, true);
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				if (pointer != touchPointer) return;
				pressed = false;
				touchPointer = -1;
				InputManager.getInstance().injectAction(VirtualButton.this.action, false);
			}
		});
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		if (texture == null) return;

		Color old = batch.getColor().cpy();
		if (pressed) {
			batch.setColor(PRESS_TINT, PRESS_TINT, PRESS_TINT, parentAlpha * 0.9f);
		} else {
			batch.setColor(1f, 1f, 1f, parentAlpha * 0.7f);
		}
		batch.draw(texture, getX(), getY(), getWidth(), getHeight());
		batch.setColor(old);
	}

	@Override
	public float getPrefWidth() {
		return btnSize;
	}

	@Override
	public float getPrefHeight() {
		return btnSize;
	}

	public boolean isPressed() {
		return pressed;
	}

	public InputAction getAction() {
		return action;
	}

	/** 运行时替换纹理 */
	public void setTexture(TextureRegion tex) {
		this.texture = tex;
	}
}
