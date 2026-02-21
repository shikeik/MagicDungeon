package com.goldsprite.magicdungeon2.input.virtual;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Widget;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.magicdungeon2.input.InputManager;

/**
 * 自定义虚拟摇杆控件
 *
 * 功能:
 * <ul>
 *   <li>圆形摇杆 + 矩形扩展触摸区域</li>
 *   <li>4向菱形方向指示器（进入扇区时亮起）</li>
 *   <li>每帧自动注入轴值到 InputManager</li>
 *   <li>支持有纹理/无纹理两种模式（无纹理时使用程序生成的圆形）</li>
 * </ul>
 *
 * 控件的 Actor 尺寸 = 扩展矩形触摸区域。
 * 摇杆可视圆居中绘制，半径由 {@link #setJoystickRadius(float)} 设置。
 *
 * 触摸规则:
 * <ul>
 *   <li>方向: 触摸点 → 摇杆中心</li>
 *   <li>幅度: 距离 / 摇杆半径，超出圆外按 1.0</li>
 * </ul>
 */
public class VirtualJoystick extends Widget implements Disposable {

	// === 轴注入 ===
	private final int axisId;

	// === 视觉参数 ===
	/** 摇杆可视圆半径（像素），由外部 setJoystickRadius() 设置 */
	private float joystickRadius;
	/** 底盘纹理（可为 null） */
	private TextureRegion baseTex;
	/** 旋钮纹理（可为 null） */
	private TextureRegion knobTex;

	// === 内部纹理 ===
	/** 1×1 白色像素，用于绘制矩形和菱形 */
	private Texture whiteTex;
	private TextureRegion whiteRegion;
	/** 64×64 填充圆，无纹理时的 fallback 绘制 */
	private Texture circleTex;
	private TextureRegion circleRegion;

	// === 死区 ===
	/** 死区半径（像素） */
	private final float deadzonePixels;

	// === 4向指示器 ===
	/** 方向扇区半角（度） */
	private float stickHalfAngle = 22.5f;
	/** 方向激活最小幅度（归一化 0~1） */
	private static final float DIRECTION_THRESHOLD = 0.25f;
	/** 菱形大小 = 摇杆半径 × 此比例 */
	private static final float DIAMOND_SIZE_RATIO = 0.17f;
	/** 菱形距中心 = 摇杆半径 × 此比例 */
	private static final float DIAMOND_OFFSET_RATIO = 0.72f;
	/** 菱形空闲颜色 */
	private static final Color DIAMOND_IDLE = new Color(1f, 1f, 1f, 0.2f);
	/** 菱形激活颜色（亮黄色） */
	private static final Color DIAMOND_ACTIVE = new Color(1f, 1f, 0.2f, 0.9f);
	/** 矩形背景填充颜色 */
	private static final Color RECT_BG = new Color(0.3f, 0.3f, 0.3f, 0.12f);
	/** 矩形边框颜色 */
	private static final Color RECT_BORDER = new Color(0.6f, 0.6f, 0.6f, 0.18f);

	// === 触摸状态 ===
	private boolean isTouched;
	private int touchPointer = -1;
	/** 归一化旋钮偏移 [-1, 1] */
	private float knobPercX, knobPercY;
	/** 当前激活方向: 0=右 1=上 2=左 3=下, -1=无 */
	private int activeDir = -1;

	// ================================================================
	// 构造
	// ================================================================

	/**
	 * 构造虚拟摇杆
	 *
	 * @param baseTex  底盘纹理（可为 null，使用 fallback 圆形）
	 * @param knobTex  旋钮纹理（可为 null，使用 fallback 圆点）
	 * @param deadzone 死区半径（像素）
	 * @param axisId   InputManager.AXIS_LEFT 或 AXIS_RIGHT
	 */
	public VirtualJoystick(TextureRegion baseTex, TextureRegion knobTex,
						   float deadzone, int axisId) {
		this.axisId = axisId;
		this.baseTex = baseTex;
		this.knobTex = knobTex;
		this.deadzonePixels = deadzone;

		createInternalTextures();
		setupTouchListener();
		setTouchable(Touchable.enabled);
	}

	// ================================================================
	// 初始化
	// ================================================================

	/** 程序生成 1×1 白色像素 和 64×64 填充圆 */
	private void createInternalTextures() {
		// 1×1 白色像素
		Pixmap p1 = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p1.setColor(Color.WHITE);
		p1.fill();
		whiteTex = new Texture(p1);
		whiteRegion = new TextureRegion(whiteTex);
		p1.dispose();

		// 64×64 填充圆（fallback 底盘/旋钮用）
		int sz = 64;
		Pixmap p2 = new Pixmap(sz, sz, Pixmap.Format.RGBA8888);
		p2.setColor(0, 0, 0, 0);
		p2.fill();
		p2.setColor(Color.WHITE);
		p2.fillCircle(sz / 2, sz / 2, sz / 2 - 1);
		circleTex = new Texture(p2);
		circleRegion = new TextureRegion(circleTex);
		p2.dispose();
	}

	/** 注册触摸监听器 */
	private void setupTouchListener() {
		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y,
									 int pointer, int button) {
				if (isTouched) return false;
				isTouched = true;
				touchPointer = pointer;
				calcKnob(x, y);
				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y,
									 int pointer) {
				if (pointer != touchPointer) return;
				calcKnob(x, y);
			}

			@Override
			public void touchUp(InputEvent event, float x, float y,
								int pointer, int button) {
				if (pointer != touchPointer) return;
				isTouched = false;
				touchPointer = -1;
				knobPercX = 0;
				knobPercY = 0;
				activeDir = -1;
			}
		});
	}

	// ================================================================
	// 触摸 → 旋钮 计算
	// ================================================================

	/**
	 * 根据本地触摸坐标计算旋钮偏移及激活方向
	 *
	 * @param localX Actor 本地坐标 X
	 * @param localY Actor 本地坐标 Y
	 */
	private void calcKnob(float localX, float localY) {
		float cx = getWidth() / 2f;
		float cy = getHeight() / 2f;
		float dx = localX - cx;
		float dy = localY - cy;
		float dist = Vector2.len(dx, dy);
		float r = getEffectiveRadius();

		if (dist < 0.001f) {
			knobPercX = knobPercY = 0;
			activeDir = -1;
			return;
		}

		// 归一化幅度，超出摇杆半径部分截断为 1
		float norm = Math.min(dist / r, 1f);
		float dirX = dx / dist;
		float dirY = dy / dist;

		// 死区判定
		float dzNorm = deadzonePixels / r;
		if (norm <= dzNorm) {
			knobPercX = knobPercY = 0;
			activeDir = -1;
			return;
		}

		// 重映射: [deadzone, 1] → [0, 1] 使边界过渡平滑
		float mapped = (norm - dzNorm) / (1f - dzNorm);
		knobPercX = dirX * mapped;
		knobPercY = dirY * mapped;

		// 方向判定
		updateActiveDir(dx, dy, mapped);
	}

	/** 根据偏移方向和幅度更新 activeDir */
	private void updateActiveDir(float dx, float dy, float magnitude) {
		if (magnitude < DIRECTION_THRESHOLD) {
			activeDir = -1;
			return;
		}
		float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
		if (angle < 0) angle += 360f;

		float h = stickHalfAngle;
		if      (angle >= 90 - h  && angle < 90 + h)   activeDir = 1; // 上
		else if (angle >= 270 - h && angle < 270 + h)   activeDir = 3; // 下
		else if (angle >= 180 - h && angle < 180 + h)   activeDir = 2; // 左
		else if (angle < h || angle >= 360 - h)          activeDir = 0; // 右
		else                                             activeDir = -1; // 对角间隙
	}

	// ================================================================
	// 每帧更新（注入轴值）
	// ================================================================

	@Override
	public void act(float delta) {
		super.act(delta);
		InputManager.getInstance().injectAxis(axisId, knobPercX, knobPercY);
	}

	// ================================================================
	// 绘制
	// ================================================================

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// 合并自身 Alpha 和父级 Alpha（支持淡入淡出）
		float alpha = getColor().a * parentAlpha;

		float x = getX(), y = getY();
		float w = getWidth(), h = getHeight();
		float cx = x + w / 2f, cy = y + h / 2f;
		float r = getEffectiveRadius();

		// [1] 矩形触摸区域背景
		drawRectBackground(batch, x, y, w, h, alpha);

		// [2] 摇杆底盘
		drawBase(batch, cx, cy, r, alpha);

		// [3] 4向菱形指示器
		drawDiamonds(batch, cx, cy, r, alpha);

		// [4] 旋钮
		drawKnob(batch, cx, cy, r, alpha);
	}

	/** 绘制半透明矩形区域 + 细边框 */
	private void drawRectBackground(Batch batch, float x, float y,
									float w, float h, float alpha) {
		// 填充
		batch.setColor(RECT_BG.r, RECT_BG.g, RECT_BG.b, RECT_BG.a * alpha);
		batch.draw(whiteRegion, x, y, w, h);

		// 1px 边框
		float bw = 1f;
		batch.setColor(RECT_BORDER.r, RECT_BORDER.g, RECT_BORDER.b,
			RECT_BORDER.a * alpha);
		batch.draw(whiteRegion, x, y, w, bw);              // 下
		batch.draw(whiteRegion, x, y + h - bw, w, bw);     // 上
		batch.draw(whiteRegion, x, y, bw, h);               // 左
		batch.draw(whiteRegion, x + w - bw, y, bw, h);     // 右
	}

	/** 绘制底盘（有纹理用纹理，否则用半透明圆） */
	private void drawBase(Batch batch, float cx, float cy,
						  float r, float alpha) {
		float d = r * 2;
		if (baseTex != null) {
			batch.setColor(1, 1, 1, alpha);
			batch.draw(baseTex, cx - r, cy - r, d, d);
		} else {
			// fallback: 半透明圆
			batch.setColor(0.4f, 0.4f, 0.4f, 0.3f * alpha);
			batch.draw(circleRegion, cx - r, cy - r, d, d);
		}
	}

	/** 绘制 4 个菱形方向指示器 */
	private void drawDiamonds(Batch batch, float cx, float cy,
							  float r, float alpha) {
		float offset = r * DIAMOND_OFFSET_RATIO;
		float size = r * DIAMOND_SIZE_RATIO;
		float half = size / 2f;

		// 方向偏移: 右(0), 上(1), 左(2), 下(3)
		float[][] ofs = {
			{ offset, 0}, { 0, offset}, {-offset, 0}, { 0, -offset}
		};
		for (int i = 0; i < 4; i++) {
			Color c = (activeDir == i) ? DIAMOND_ACTIVE : DIAMOND_IDLE;
			batch.setColor(c.r, c.g, c.b, c.a * alpha);

			float dx = cx + ofs[i][0] - half;
			float dy = cy + ofs[i][1] - half;

			// 正方形旋转 45° = 菱形
			batch.draw(whiteRegion,
				dx, dy,         // 绘制位置
				half, half,     // 旋转原点（正方形中心）
				size, size,     // 宽高
				1f, 1f,         // 缩放
				45f);           // 旋转角度
		}
	}

	/** 绘制旋钮 */
	private void drawKnob(Batch batch, float cx, float cy,
						  float r, float alpha) {
		float kx = cx + knobPercX * r;
		float ky = cy + knobPercY * r;
		float knobR = r * 0.22f; // 旋钮半径 = 摇杆半径的 22%
		float knobD = knobR * 2;

		if (knobTex != null) {
			batch.setColor(1, 1, 1, alpha);
			batch.draw(knobTex, kx - knobR, ky - knobR, knobD, knobD);
		} else {
			// fallback: 白色圆点
			batch.setColor(0.9f, 0.9f, 0.9f, 0.8f * alpha);
			batch.draw(circleRegion, kx - knobR, ky - knobR, knobD, knobD);
		}
	}

	// ================================================================
	// 公共 API
	// ================================================================

	/** 设置摇杆可视半径（像素） */
	public void setJoystickRadius(float radius) {
		this.joystickRadius = radius;
	}

	/** 设置方向扇区半角（度），默认 22.5 */
	public void setStickHalfAngle(float degrees) {
		this.stickHalfAngle = degrees;
	}

	/** 获取旋钮 X 归一化值 [-1, 1] */
	public float getKnobPercentX() { return knobPercX; }

	/** 获取旋钮 Y 归一化值 [-1, 1] */
	public float getKnobPercentY() { return knobPercY; }

	/** 获取当前激活方向 (0=右 1=上 2=左 3=下, -1=无) */
	public int getActiveDirection() { return activeDir; }

	/** 运行时替换底盘纹理 */
	public void setBaseTexture(TextureRegion tex) { this.baseTex = tex; }

	/** 运行时替换旋钮纹理 */
	public void setKnobTexture(TextureRegion tex) { this.knobTex = tex; }

	// ================================================================
	// 工具
	// ================================================================

	/** 获取有效摇杆半径（若未设置则取控件短边一半） */
	private float getEffectiveRadius() {
		return joystickRadius > 0 ? joystickRadius
			: Math.min(getWidth(), getHeight()) / 2f;
	}

	@Override
	public void dispose() {
		if (whiteTex != null) { whiteTex.dispose(); whiteTex = null; }
		if (circleTex != null) { circleTex.dispose(); circleTex = null; }
	}
}
