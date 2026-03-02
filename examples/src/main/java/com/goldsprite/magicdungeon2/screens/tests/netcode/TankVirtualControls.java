package com.goldsprite.magicdungeon2.screens.tests.netcode;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.input.virtual.VirtualButton;
import com.goldsprite.magicdungeon2.input.virtual.VirtualJoystick;

/**
 * 坦克屏专用虚拟触控覆盖层（精简版）
 * <p>
 * 左半区: 四向摇杆（注入 AXIS_LEFT，驱动移动）
 * 右半区: 单个攻击按钮（注入 InputAction.ATTACK）
 * <p>
 * 复用引擎的 {@link VirtualJoystick} 和 {@link VirtualButton}，
 * 布局比魔法地牢屏更简单（不需要交互/技能/返回按钮）。
 * Android 默认显示，PC 默认隐藏。
 */
public class TankVirtualControls implements Disposable {
    private static final String TAG = "TankVirtualCtrl";

    private final Stage stage;
    private boolean visible;

    // 虚拟控件
    private VirtualJoystick moveStick;
    private VirtualButton attackBtn;

    // 布局参数
    private static final float JOYSTICK_SIZE_RATIO = 0.30f;   // 摇杆占视口短边比例（坦克屏稍大一些）
    private static final float BUTTON_SIZE_RATIO   = 0.14f;   // 攻击按钮占视口短边比例
    private static final float MARGIN_RATIO        = 0.04f;   // 边距比例
    private static final float DEADZONE            = 10f;     // 摇杆死区（像素）

    public TankVirtualControls(Viewport viewport) {
        stage = new Stage(viewport);
        visible = PlatformImpl.isAndroidUser(); // Android 默认显示

        createControls();
        layoutControls();

        DLog.logT(TAG, "初始化完成, 默认%s", visible ? "显示" : "隐藏");
    }

    private void createControls() {
        // 无纹理模式 — VirtualJoystick/VirtualButton 内部有 fallback 绘制
        moveStick = new VirtualJoystick(null, null, DEADZONE, InputManager.AXIS_LEFT);
        attackBtn = new VirtualButton(null, InputAction.ATTACK, 64f);

        stage.addActor(moveStick);
        stage.addActor(attackBtn);
    }

    /**
     * 根据视口尺寸自动布局
     * <p>
     * 左下角: 摇杆
     * 右下角: 攻击按钮（居中偏下）
     */
    private void layoutControls() {
        float vw = stage.getViewport().getWorldWidth();
        float vh = stage.getViewport().getWorldHeight();
        float shortSide = Math.min(vw, vh);
        float margin = shortSide * MARGIN_RATIO;

        // ── 摇杆 ──
        float stickDiameter = shortSide * JOYSTICK_SIZE_RATIO;
        float stickRadius = stickDiameter / 2f;
        float touchPadding = stickRadius * 0.75f;
        float rectW = stickDiameter + touchPadding * 2;
        float rectH = stickDiameter + touchPadding * 2;
        moveStick.setJoystickRadius(stickRadius);
        moveStick.setSize(rectW, rectH);
        moveStick.setPosition(
            Math.max(0, margin - touchPadding),
            Math.max(0, margin - touchPadding));

        // ── 攻击按钮（右下角） ──
        float btnSize = shortSide * BUTTON_SIZE_RATIO;
        attackBtn.setSize(btnSize, btnSize);
        attackBtn.setPosition(vw - margin - btnSize, margin);
    }

    // ═══════════ 公共 API ═══════════

    public void update(float delta) {
        if (!visible) return;

        // 自动淡入淡出：非触控输入时半透明，触控时全透明
        InputManager.InputType type = InputManager.getInstance().getInputType();
        if (type == InputManager.InputType.GAMEPAD || type == InputManager.InputType.KEYBOARD) {
            stage.getRoot().getColor().a = Math.max(0.15f, stage.getRoot().getColor().a - delta * 2f);
        } else {
            stage.getRoot().getColor().a = Math.min(1f, stage.getRoot().getColor().a + delta * 3f);
        }

        stage.act(delta);
    }

    public void render() {
        if (!visible) return;
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        layoutControls();
    }

    public Stage getStage() {
        return stage;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        if (visible) stage.getRoot().getColor().a = 1f;
    }

    public boolean isVisible() {
        return visible;
    }

    @Override
    public void dispose() {
        if (moveStick != null) moveStick.dispose();
        stage.dispose();
    }
}
