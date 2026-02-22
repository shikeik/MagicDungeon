package com.goldsprite.magicdungeon2.input.virtual;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon2.assets.TextureManager;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.input.VirtualInputProvider;

/**
 * 虚拟控件覆盖层
 *
 * 管理虚拟摇杆和虚拟按钮的创建、布局和显隐。
 * 在 Android 平台默认显示，PC 平台默认隐藏。
 * 当检测到手柄/键盘输入时自动淡出，触摸输入时自动淡入。
 *
 * 使用方式:
 *   VirtualControlsOverlay overlay = new VirtualControlsOverlay(uiViewport);
 *   // 在 render 中:
 *   overlay.update(delta);
 *   overlay.render();
 *   // 在 resize 中:
 *   overlay.resize(width, height);
 */
public class VirtualControlsOverlay implements VirtualInputProvider, Disposable {
	private static final String TAG = "VirtualControls";

	private final Stage stage;
	private boolean visible;

	// 虚拟控件
	private VirtualJoystick moveStick;
	private VirtualButton attackBtn;
	private VirtualButton interactBtn;
	private VirtualButton skillBtn;
	private VirtualButton backBtn;

	// 布局参数（相对于视口尺寸的比例）
	private static final float JOYSTICK_SIZE_RATIO = 0.25f;   // 摇杆占视口短边的比例
	private static final float BUTTON_SIZE_RATIO = 0.10f;     // 按钮占视口短边的比例
	private static final float MARGIN_RATIO = 0.03f;          // 边距比例
	private static final float DEADZONE = 10f;                // 摇杆死区（像素）

	public VirtualControlsOverlay(Viewport viewport) {
		stage = new Stage(viewport);
		visible = PlatformImpl.isAndroidUser(); // Android 默认显示

		createControls();
		layoutControls();

		DLog.logT(TAG, "初始化完成, 默认%s", visible ? "显示" : "隐藏");
	}

	private void createControls() {
		// 尝试加载 UI 纹理（可能尚未生成，此时用 null 回退）
		TextureRegion baseTex = TextureManager.get("ui/joystick_base");
		TextureRegion knobTex = TextureManager.get("ui/joystick_knob");
		TextureRegion atkTex = TextureManager.get("ui/btn_attack");
		TextureRegion actTex = TextureManager.get("ui/btn_interact");
		TextureRegion sklTex = TextureManager.get("ui/btn_skill");
		TextureRegion bckTex = TextureManager.get("ui/btn_back");

		// baseTex/knobTex 可为 null，VirtualJoystick 内部有 fallback 圆形绘制
		if (baseTex == null || knobTex == null) {
			DLog.logT(TAG, "⚠ 摇杆纹理未找到，使用 fallback 圆形");
		}
		moveStick = new VirtualJoystick(baseTex, knobTex, DEADZONE, InputManager.AXIS_LEFT);

		// 按钮尺寸暂定为 64，layoutControls() 时会重新设置
		float defaultSize = 64f;
		attackBtn = new VirtualButton(atkTex, InputAction.ATTACK, defaultSize);
		interactBtn = new VirtualButton(actTex, InputAction.INTERACT, defaultSize);
		skillBtn = new VirtualButton(sklTex, InputAction.SKILL, defaultSize);
		backBtn = new VirtualButton(bckTex, InputAction.BACK, defaultSize);

		stage.addActor(moveStick);
		stage.addActor(attackBtn);
		stage.addActor(interactBtn);
		stage.addActor(skillBtn);
		stage.addActor(backBtn);
	}

	/**
	 * 根据视口尺寸计算控件大小和位置
	 *
	 * 布局:
	 *   左下角: 虚拟摇杆
	 *   右下角: 动作按钮 (菱形排列: 上=Skill, 右=Attack, 下=Interact, 左=Back)
	 */
	private void layoutControls() {
		float vw = stage.getViewport().getWorldWidth();
		float vh = stage.getViewport().getWorldHeight();
		float shortSide = Math.min(vw, vh);
		float margin = shortSide * MARGIN_RATIO;

		// 摇杆：可视圆 + 扩展矩形触摸区域
		float stickDiameter = shortSide * JOYSTICK_SIZE_RATIO;
		float stickRadius = stickDiameter / 2f;
		float touchPadding = stickRadius * 0.75f; // 圆外额外触摸边距
		float rectW = stickDiameter + touchPadding * 2;
		float rectH = stickDiameter + touchPadding * 2;
		moveStick.setJoystickRadius(stickRadius);
		moveStick.setSize(rectW, rectH);
		// 保持摇杆圆心位于 (margin + stickRadius, margin + stickRadius)
		moveStick.setPosition(
			Math.max(0, margin - touchPadding),
			Math.max(0, margin - touchPadding));

		// 按钮
		float btnSize = shortSide * BUTTON_SIZE_RATIO;
		float btnSpacing = btnSize * 0.3f; // 按钮间距

		// 右下角菱形布局的中心点
		float centerX = vw - margin - btnSize - btnSpacing - 70;
		float centerY = margin + btnSize + btnSpacing;

		// 右 = Attack (主攻击, 最常用)
		attackBtn.setSize(btnSize, btnSize);
		attackBtn.setPosition(centerX + btnSize/2 + btnSpacing, centerY - btnSize / 2);

		// 下 = Interact
		interactBtn.setSize(btnSize, btnSize);
		interactBtn.setPosition(centerX - btnSize / 2 + btnSize / 2, centerY - btnSize - btnSpacing);

		// 上 = Skill
		skillBtn.setSize(btnSize, btnSize);
		skillBtn.setPosition(centerX - btnSize / 2 + btnSize / 2, centerY + btnSpacing);

		// 左 = Back
		backBtn.setSize(btnSize, btnSize);
		backBtn.setPosition(centerX - btnSize/2- btnSpacing, centerY - btnSize / 2);
	}

	// === VirtualInputProvider 实现 ===

	@Override
	public void update(float delta) {
		if (!visible) return;

		// 自动显隐逻辑
		InputManager.InputType type = InputManager.getInstance().getInputType();
		if (type == InputManager.InputType.GAMEPAD || type == InputManager.InputType.KEYBOARD) {
			// 检测到非触控输入，淡出（但不完全隐藏，允许用户触摸恢复）
			stage.getRoot().getColor().a = Math.max(0.15f, stage.getRoot().getColor().a - delta * 2f);
		} else {
			// 触控模式，淡入
			stage.getRoot().getColor().a = Math.min(1f, stage.getRoot().getColor().a + delta * 3f);
		}

		stage.act(delta);
	}

	public void render() {
		if (!visible) return;
		stage.draw();
	}

	@Override
	public void setVisible(boolean visible) {
		this.visible = visible;
		if (visible) {
			stage.getRoot().getColor().a = 1f;
		}
	}

	@Override
	public boolean isVisible() {
		return visible;
	}

	/**
	 * 获取内部 Stage（用于添加到 InputMultiplexer）
	 */
	public Stage getStage() {
		return stage;
	}

	/**
	 * 设置摇杆方向扇区半角（度），同步菱形指示器与移动逻辑
	 */
	public void setStickHalfAngle(float degrees) {
		if (moveStick != null) moveStick.setStickHalfAngle(degrees);
	}

	/**
	 * 屏幕尺寸变化时重新布局
	 */
	public void resize(int width, int height) {
		stage.getViewport().update(width, height, true);
		layoutControls();
	}

	@Override
	public void dispose() {
		if (moveStick != null) moveStick.dispose();
		stage.dispose();
	}
}
