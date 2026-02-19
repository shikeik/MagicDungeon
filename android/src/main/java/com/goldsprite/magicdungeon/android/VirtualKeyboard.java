package com.goldsprite.magicdungeon.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.graphics.Rect;

import com.goldsprite.gdengine.log.DLog; // 使用项目 Log

import java.util.HashMap;
import java.util.Map;
import android.view.HapticFeedbackConstants;

/**
 * 虚拟键盘管理类
 * 负责管理安卓端的虚拟键盘 UI、交互逻辑以及按键事件分发
 */
public class VirtualKeyboard {

	private final Activity activity;
	private final RelativeLayout parentView;
	private final View gameView; // 游戏视图，用于键盘隐藏时获取焦点

	// 全键盘高度: 横/竖屏
	private final float HEIGHT_RATIO_LANDSCAPE = 0.30f;
	private final float HEIGHT_RATIO_PORTRAIT = 0.25f;

	// 模拟手柄: 基础比例 0.15，但会根据屏幕宽度自动调整以容纳按键
	//private float GAMEPAD_PANEL_RATIO = 0.12f;
	private float GAMEPAD_PANEL_RATIO = 0f;
	//private float GAMEPAD_USED_SPACE = GAMEPAD_PANEL_RATIO*1.12f;
	private float GAMEPAD_USED_SPACE = 0.16f;
	private final int MIN_PANEL_WIDTH_DP = 0; // 面板最小宽度 (ABXY=120dp + margin)

	private final int padding = -16;

	// Xbox Controller Key Codes (Standard Android Mapping)
	// A=KEYCODE_BUTTON_A (96), B=KEYCODE_BUTTON_B (97), X=KEYCODE_BUTTON_X (99), Y=KEYCODE_BUTTON_Y (100)
	// LB=KEYCODE_BUTTON_L1 (102), RB=KEYCODE_BUTTON_R1 (103)
	// LT=KEYCODE_BUTTON_L2 (104), RT=KEYCODE_BUTTON_R2 (105)
	// Select/Back=KEYCODE_BUTTON_SELECT (109), Start=KEYCODE_BUTTON_START (108)
	// L3=KEYCODE_BUTTON_THUMBL (106), R3=KEYCODE_BUTTON_THUMBR (107)

	private final String[][] terminalLayout = {
		{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Del"},
		{"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
		{"Esc", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"},
		{"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "↑", "/"},
		{"Ctrl", "Alt", "Sym", "Space", "←", "↓", "→"}
	};

	private final Map<String, Integer> keyMap = new HashMap<>();

	private LinearLayout keyboardContainer;
	private Button floatingToggleBtn;

	private boolean isKeyboardVisible = false;
	private int screenWidth, screenHeight;

	// 输入模式枚举
	public enum InputMode {
		FULL_KEYBOARD,
		GAMEPAD // 预留
	}

	private InputMode currentMode = InputMode.GAMEPAD;

	private GestureDetector gestureDetector; // 添加手势检测器

	// 长按选择模式相关变量
	private LinearLayout modeSelectionView;
	private boolean isLongPressMode = false;
	private int selectedModeIndex = -1;
	private Runnable[] modeActions;
	private View[] modeItemViews;
	private Rect[] modeItemRects;

	public interface OnInputModeChangeListener {
		void onModeChanged(InputMode mode);
	}
	private OnInputModeChangeListener modeChangeListener;

	public VirtualKeyboard(Activity activity, RelativeLayout parentView, View gameView, OnInputModeChangeListener listener) {
		this.activity = activity;
		this.parentView = parentView;
		this.gameView = gameView;
		this.modeChangeListener = listener;

		// 尝试获取初始屏幕尺寸
		DisplayMetrics dm = activity.getResources().getDisplayMetrics();
		this.screenWidth = dm.widthPixels;
		this.screenHeight = dm.heightPixels;

		// 计算合适的比例
		calculateGamepadRatio();

		initKeyMap();
		initGestureDetector(); // 初始化手势检测

		// 初始化悬浮按钮 (只创建一次)
		initFloatingButton();

		// UI 初始化
		initUI();
	}

	private void calculateGamepadRatio() {
		if (screenWidth == 0) return;

		int minPx = dpToPx(MIN_PANEL_WIDTH_DP);
		float minRatio = (float) minPx / screenWidth;

		// 取 0.15 和 最小需求比例 中的较大值，确保按键不溢出
		this.GAMEPAD_PANEL_RATIO = Math.max(0.15f, minRatio);

		DLog.logT("VirtualKeyboard", "Screen: %d, MinPx: %d, Ratio: %.2f", screenWidth, minPx, GAMEPAD_PANEL_RATIO);
	}

	private void initModeSelectionView() {
		modeSelectionView = new LinearLayout(activity);
		modeSelectionView.setOrientation(LinearLayout.VERTICAL);
		// 背景: 半透明黑色圆角
		GradientDrawable bg = new GradientDrawable();
		bg.setColor(0xEE222222);
		bg.setCornerRadius(dpToPx(8));
		bg.setStroke(dpToPx(1), 0xFF00EAFF);
		modeSelectionView.setBackground(bg);

		modeSelectionView.setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4));

		modeActions = new Runnable[2];
		modeItemViews = new View[2];

		// 选项 1: 全键盘
		TextView item1 = createModeItem("⌨ 全键盘", InputMode.FULL_KEYBOARD);
		modeItemViews[0] = item1;
		modeActions[0] = () -> {
			if (currentMode != InputMode.FULL_KEYBOARD) {
				toggleInputMode(InputMode.FULL_KEYBOARD);
				setKeyboardVisibility(true);
				dockFloatingButton(floatingToggleBtn);
			}
		};
		modeSelectionView.addView(item1);

		// 分割线
		View divider = new View(activity);
		divider.setBackgroundColor(0x55AAAAAA);
		LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(1));
		divParams.topMargin = dpToPx(2);
		divParams.bottomMargin = dpToPx(2);
		modeSelectionView.addView(divider, divParams);

		// 选项 2: 手柄
		TextView item2 = createModeItem("🎮 手柄", InputMode.GAMEPAD);
		modeItemViews[1] = item2;
		modeActions[1] = () -> {
			if (currentMode != InputMode.GAMEPAD) {
				toggleInputMode(InputMode.GAMEPAD);
				setKeyboardVisibility(true);
				dockFloatingButton(floatingToggleBtn);
			}
		};
		modeSelectionView.addView(item2);

		modeSelectionView.setVisibility(View.GONE);
		parentView.addView(modeSelectionView);
	}

	private TextView createModeItem(String text, InputMode mode) {
		TextView tv = new TextView(activity);
		tv.setText(text);
		tv.setTextColor(Color.LTGRAY);
		tv.setTextSize(16);
		tv.setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8));
		tv.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
		tv.setBackgroundColor(Color.TRANSPARENT);
		tv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return tv;
	}

	private void showModeSelectionView() {
		if (modeSelectionView == null) {
			initModeSelectionView();
		}

		// 强制测量以获取尺寸
		modeSelectionView.measure(
			// View.MeasureSpec.makeMeasureSpec(screenWidth / 2, View.MeasureSpec.AT_MOST),
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), 
			View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		int menuW = modeSelectionView.getMeasuredWidth()/3;
		int menuH = modeSelectionView.getMeasuredHeight();

		int btnW = floatingToggleBtn.getWidth();
		int btnH = floatingToggleBtn.getHeight();

		float btnX = floatingToggleBtn.getX();
		float btnY = floatingToggleBtn.getY();

		float x = btnX + (btnW - menuW) / 2f;
		float y = btnY - menuH - dpToPx(10); // 上方

		if (y < 0) {
			y = btnY + btnH + dpToPx(10);
		}

		x = Math.max(dpToPx(10), Math.min(screenWidth - menuW - dpToPx(10), x));

		modeSelectionView.setX(x);
		modeSelectionView.setY(y);

		modeSelectionView.setVisibility(View.VISIBLE);
		modeSelectionView.setAlpha(0f);
		modeSelectionView.animate().alpha(1f).setDuration(150).start();

		// 强制 Layout
		modeSelectionView.layout((int)x, (int)y, (int)x + menuW, (int)y + menuH);
	}

	private void hideModeSelectionView() {
		if (modeSelectionView != null) {
			modeSelectionView.animate().alpha(0f).setDuration(150).withEndAction(() -> {
				modeSelectionView.setVisibility(View.GONE);
			}).start();
		}
		selectedModeIndex = -1;
		updateItemStyles();
	}

	private void updateItemStyles() {
		if (modeItemViews == null) return;
		for (int i = 0; i < modeItemViews.length; i++) {
			if (i == selectedModeIndex) {
				modeItemViews[i].setBackgroundColor(0xFF00EAFF);
				((TextView)modeItemViews[i]).setTextColor(Color.BLACK);
			} else {
				modeItemViews[i].setBackgroundColor(Color.TRANSPARENT);
				((TextView)modeItemViews[i]).setTextColor(Color.LTGRAY);
			}
		}
	}

	private void handleLongPressSelection(MotionEvent event) {
		int action = event.getAction();

		if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_DOWN) {
			float rawX = event.getRawX();
			float rawY = event.getRawY();

			int[] parentLoc = new int[2];
			parentView.getLocationOnScreen(parentLoc);
			float relX = rawX - parentLoc[0];
			float relY = rawY - parentLoc[1];

			float menuX = modeSelectionView.getX();
			float menuY = modeSelectionView.getY();

			int newSelection = -1;

			if (relX >= menuX - dpToPx(20) && relX <= menuX + modeSelectionView.getWidth() + dpToPx(20) &&
				relY >= menuY - dpToPx(20) && relY <= menuY + modeSelectionView.getHeight() + dpToPx(20)) {

				float localY = relY - menuY;

				for (int i = 0; i < modeItemViews.length; i++) {
					 View item = modeItemViews[i];
					 if (localY >= item.getTop() && localY <= item.getBottom()) {
						 newSelection = i;
						 break;
					 }
				}
			}

			if (newSelection != selectedModeIndex) {
				selectedModeIndex = newSelection;
				updateItemStyles();
			}

		} else if (action == MotionEvent.ACTION_UP) {
			if (selectedModeIndex != -1) {
				if (modeActions[selectedModeIndex] != null) {
					modeActions[selectedModeIndex].run();
				}
			}
			hideModeSelectionView();
			isLongPressMode = false;
		} else if (action == MotionEvent.ACTION_CANCEL) {
			hideModeSelectionView();
			isLongPressMode = false;
		}
	}

	private void initGestureDetector() {
		gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
			@Override
			public void onLongPress(MotionEvent e) {
				// 长按触发模式选择 (新逻辑: 不弹 Dialog，而是进入长按选择模式)
				isLongPressMode = true;
				showModeSelectionView();
				// 震动反馈
				floatingToggleBtn.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			}

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				// 点击触发显示/隐藏 (逻辑移到这里更准确)
				setKeyboardVisibility(!isKeyboardVisible);
				dockFloatingButton(floatingToggleBtn);
				return true;
			}
		});
	}

	private int dpToPx(float dp) {
		return (int) (dp * activity.getResources().getDisplayMetrics().density + 0.5f);
	}

	private void initFloatingButton() {
		// --- 悬浮开关按钮 (显示/隐藏) ---
		floatingToggleBtn = new Button(activity);
		floatingToggleBtn.setText("⌨");
		floatingToggleBtn.setTextColor(Color.CYAN);
		floatingToggleBtn.setTextSize(20);
		int p = dpToPx(8);
		floatingToggleBtn.setPadding(p, p, p, p); // 给点内边距

		GradientDrawable shape = new GradientDrawable();
		shape.setCornerRadius(100);
		shape.setColor(0x44000000);
		shape.setStroke(dpToPx(1), 0xFF00EAFF);
		floatingToggleBtn.setBackground(shape);

		// [v19.5 修复] 使用 WRAP_CONTENT，这样文字长了会自动变大，不会被切
		int btnSize = dpToPx(50);
		FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(btnSize, btnSize);

		// 初始位置不要用 Margin，直接设为 0，靠 setX/Y 移动
		btnParams.topMargin = 0;
		btnParams.leftMargin = 0;

		floatingToggleBtn.setLayoutParams(btnParams);

		// 拖拽逻辑
		floatingToggleBtn.setOnTouchListener(new View.OnTouchListener() {
			float dX, dY;
			float downRawX, downRawY;
			boolean isDrag = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// 优先交给手势检测器处理 (点击、长按)
				boolean gestureHandled = gestureDetector.onTouchEvent(event);

				// 如果处于长按选择模式，拦截所有事件
				if (isLongPressMode) {
					handleLongPressSelection(event);
					return true;
				}

				if (gestureHandled) {
					return true;
				}

				switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						dX = v.getX() - event.getRawX();
						dY = v.getY() - event.getRawY();
						downRawX = event.getRawX();
						downRawY = event.getRawY();
						isDrag = false;
						v.animate().alpha(1.0f).scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
						return true;

					case MotionEvent.ACTION_MOVE:
						if (Math.abs(event.getRawX() - downRawX) > 10 || Math.abs(event.getRawY() - downRawY) > 10) {
							isDrag = true;
						}

						if (isDrag) {
							// 这里的 setX/Y 是相对于 parentView 的
							float newX = event.getRawX() + dX;
							float newY = event.getRawY() + dY;

							// 边界限制
							newX = Math.max(0, Math.min(screenWidth - v.getWidth(), newX));
							newY = Math.max(0, Math.min(screenHeight - v.getHeight(), newY));

							v.setX(newX);
							v.setY(newY);
						}
						return true;

					case MotionEvent.ACTION_UP:
						v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();

						// 无论是否拖拽，抬起时都进行吸附
						// 注意：如果不是拖拽，onSingleTapUp 已经触发了点击逻辑
						dockFloatingButton(v);
						return true;
				}
				return false;
			}
		});

		floatingToggleBtn.setAlpha(0.3f);
		parentView.addView(floatingToggleBtn);

		// 初始位置设置 (延时一帧确保宽高已计算)
		floatingToggleBtn.post(() -> {
			// 再次确保尺寸正确（有时候 onCreate 时获取的可能不准）
			if (screenWidth == 0 || screenHeight == 0) {
				DisplayMetrics dm = activity.getResources().getDisplayMetrics();
				screenWidth = dm.widthPixels;
				screenHeight = dm.heightPixels;
			}

			if (screenHeight > 0) {
				// 修改 Y 轴位置为上方 1/4
				floatingToggleBtn.setY(screenHeight / 4f - floatingToggleBtn.getHeight() / 2f);
				dockFloatingButton(floatingToggleBtn);
			}
		});
	}

	private Button createHideButton() {
		Button hideBtn = createRoundButton("Hide", 10, -1); // -1 is dummy code
		// Override touch listener for hide logic
		hideBtn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_UP) {
				setKeyboardVisibility(false);
			}
			return true;
		});
		return hideBtn;
	}

	private void initUI() {
		if (currentMode == InputMode.FULL_KEYBOARD) {
			initFullKeyboardUI();
		} else if (currentMode == InputMode.GAMEPAD) {
			initGamepadUI();
		}

		// 刷新布局
		refreshKeyboardLayout();
	}

	private void initFullKeyboardUI() {
		keyboardContainer = new LinearLayout(activity);
		keyboardContainer.setOrientation(LinearLayout.VERTICAL);
		keyboardContainer.setVisibility(View.GONE);

		RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0);
		kbParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		parentView.addView(keyboardContainer, kbParams);
	}

	private void initGamepadUI() {
		// Create left and right panels
		keyboardContainer = new LinearLayout(activity);
		keyboardContainer.setOrientation(LinearLayout.HORIZONTAL);
		keyboardContainer.setVisibility(View.GONE);
		keyboardContainer.setWeightSum(1.0f);

		// Allow touch-through for the container itself
		keyboardContainer.setClickable(false);
		keyboardContainer.setFocusable(false);
		// keyboardContainer.setTouchscreenBlocksFocus(false); // Min SDK check might fail

		float ratio = GAMEPAD_PANEL_RATIO, gdxRatio = 1 - ratio*2;
		// Left Panel (Compressed Width: 25%)
		FrameLayout leftPanel = new FrameLayout(activity);
		LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
			0, ViewGroup.LayoutParams.MATCH_PARENT, ratio);
		leftPanel.setLayoutParams(leftParams);
		// Ensure panel itself doesn't block touches unless a child handles it
		leftPanel.setClickable(false);
		leftPanel.setFocusable(false);
		createLeftJoyCon(leftPanel);

		// Middle Space (Expanded Width: 50%) - Transparent and click-through
		View middleSpace = new View(activity);
		middleSpace.setClickable(false);
		middleSpace.setFocusable(false);
		LinearLayout.LayoutParams midParams = new LinearLayout.LayoutParams(
			0, ViewGroup.LayoutParams.MATCH_PARENT, gdxRatio);
		middleSpace.setLayoutParams(midParams);

		// Right Panel (Compressed Width: 25%)
		FrameLayout rightPanel = new FrameLayout(activity);
		LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
			0, ViewGroup.LayoutParams.MATCH_PARENT, ratio);
		rightPanel.setLayoutParams(rightParams);
		// Ensure panel itself doesn't block touches unless a child handles it
		rightPanel.setClickable(false);
		rightPanel.setFocusable(false);
		createRightJoyCon(rightPanel);

		keyboardContainer.addView(leftPanel);
		keyboardContainer.addView(middleSpace);
		keyboardContainer.addView(rightPanel);

		// Fill Parent
		RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		parentView.addView(keyboardContainer, kbParams);
	}

	private void createLeftJoyCon(FrameLayout panel) {
		// Layout: Top=Stick, Bottom=D-Pad

		// LB/LT Buttons
		Button lt = createRoundButton("LT", 12, KeyEvent.KEYCODE_BUTTON_L2);
		FrameLayout.LayoutParams ltParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(30));
		ltParams.gravity = Gravity.TOP | Gravity.LEFT;
		ltParams.topMargin = dpToPx(5);
		ltParams.leftMargin = dpToPx(5);
		panel.addView(lt, ltParams);

		Button lb = createRoundButton("LB", 12, KeyEvent.KEYCODE_BUTTON_L1);
		FrameLayout.LayoutParams lbParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(30));
		lbParams.gravity = Gravity.TOP | Gravity.RIGHT;
		lbParams.topMargin = dpToPx(20);
		lbParams.rightMargin = dpToPx(10);
		panel.addView(lb, lbParams);

		// Stick
		View stick = createJoystick(true); // Left stick
		FrameLayout.LayoutParams stickParams = new FrameLayout.LayoutParams(dpToPx(70), dpToPx(70));
		stickParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		stickParams.topMargin = dpToPx(60);
		panel.addView(stick, stickParams);

		// D-Pad (Simulated by 4 buttons)
		View dpad = createDPad();
		FrameLayout.LayoutParams dpadParams = new FrameLayout.LayoutParams(dpToPx(100), dpToPx(100));
		dpadParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		dpadParams.bottomMargin = dpToPx(30);
		panel.addView(dpad, dpadParams);

		// Select / Back
		Button selectBtn = createRoundButton("Back", 10, KeyEvent.KEYCODE_BUTTON_SELECT);
		FrameLayout.LayoutParams selectParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(24));
		selectParams.gravity = Gravity.CENTER_VERTICAL | Gravity.RIGHT;
		selectParams.rightMargin = dpToPx(5);
		panel.addView(selectBtn, selectParams);
	}

	private void createRightJoyCon(FrameLayout panel) {
		// RB/RT Buttons
		Button rt = createRoundButton("RT", 12, KeyEvent.KEYCODE_BUTTON_R2);
		FrameLayout.LayoutParams rtParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(30));
		rtParams.gravity = Gravity.TOP | Gravity.RIGHT;
		rtParams.topMargin = dpToPx(5);
		rtParams.rightMargin = dpToPx(5);
		panel.addView(rt, rtParams);

		Button rb = createRoundButton("RB", 12, KeyEvent.KEYCODE_BUTTON_R1);
		FrameLayout.LayoutParams rbParams = new FrameLayout.LayoutParams(dpToPx(50), dpToPx(30));
		rbParams.gravity = Gravity.TOP | Gravity.LEFT;
		rbParams.topMargin = dpToPx(20);
		rbParams.leftMargin = dpToPx(5);
		panel.addView(rb, rbParams);

		// ABXY Buttons (Xbox Layout)
		View abxy = createABXY();
		FrameLayout.LayoutParams abxyParams = new FrameLayout.LayoutParams(dpToPx(120), dpToPx(120));
		abxyParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
		abxyParams.topMargin = dpToPx(60);
		panel.addView(abxy, abxyParams);

		// Right Stick
		View stick = createJoystick(false); // Right stick
		FrameLayout.LayoutParams stickParams = new FrameLayout.LayoutParams(dpToPx(60), dpToPx(60));
		stickParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
		stickParams.bottomMargin = dpToPx(40);
		panel.addView(stick, stickParams);

		// Start
		Button startBtn = createRoundButton("Start", 10, KeyEvent.KEYCODE_BUTTON_START);
		FrameLayout.LayoutParams startParams = new FrameLayout.LayoutParams(dpToPx(40), dpToPx(24));
		startParams.gravity = Gravity.CENTER_VERTICAL | Gravity.LEFT;
		startParams.leftMargin = dpToPx(5);
		panel.addView(startBtn, startParams);

		// Home Button
		Button homeBtn = createRoundButton("⌂", 14, KeyEvent.KEYCODE_HOME);
		FrameLayout.LayoutParams homeParams = new FrameLayout.LayoutParams(dpToPx(28), dpToPx(28));
		homeParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
		homeParams.bottomMargin = dpToPx(8);
		homeParams.rightMargin = dpToPx(16);
		panel.addView(homeBtn, homeParams);

		// Hide Button removed
	}

	private View createJoystick(boolean isLeft) {
		// 使用 FrameLayout 作为容器
		FrameLayout stickContainer = new FrameLayout(activity);

		// 底座
		View stickBase = new View(activity);
		GradientDrawable baseShape = new GradientDrawable();
		baseShape.setShape(GradientDrawable.OVAL);
		baseShape.setColor(0x88333333);
		baseShape.setStroke(dpToPx(2), 0xFF666666);
		stickBase.setBackground(baseShape);

		FrameLayout.LayoutParams baseParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		stickContainer.addView(stickBase, baseParams);

		// 摇杆头 (Knob)
		View stickKnob = new View(activity);
		GradientDrawable knobShape = new GradientDrawable();
		knobShape.setShape(GradientDrawable.OVAL);
		knobShape.setColor(0xAA888888); // 亮一点的灰色
		knobShape.setStroke(dpToPx(1), 0xFFAAAAAA);
		stickKnob.setBackground(knobShape);

		int knobSize = isLeft ? dpToPx(30) : dpToPx(25); // 左摇杆稍大
		FrameLayout.LayoutParams knobParams = new FrameLayout.LayoutParams(knobSize, knobSize);
		knobParams.gravity = Gravity.CENTER;
		stickContainer.addView(stickKnob, knobParams);

		// Attach touch listener for movement (For both sticks now)
		stickContainer.setOnTouchListener(new View.OnTouchListener() {
			private float centerX, centerY;
			private float maxRadius;

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					centerX = v.getWidth() / 2f;
					centerY = v.getHeight() / 2f;
					maxRadius = v.getWidth() / 3f; // 限制移动范围
					stickKnob.setAlpha(0.7f);
				}

				if (event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_DOWN) {
					float dx = event.getX() - centerX;
					float dy = event.getY() - centerY;

					// 限制摇杆头移动范围
					float distance = (float) Math.sqrt(dx * dx + dy * dy);
					if (distance > maxRadius) {
						float ratio = maxRadius / distance;
						dx *= ratio;
						dy *= ratio;
					}

					stickKnob.setTranslationX(dx);
					stickKnob.setTranslationY(dy);

					// 逻辑触发
					int threshold = dpToPx(8);
					float rawDx = event.getX() - centerX;
					float rawDy = event.getY() - centerY;

					if (isLeft) {
						// Left Stick: WASD
						if (Math.abs(rawDx) > Math.abs(rawDy)) {
							if (rawDx > threshold) sendKeyOnce(KeyEvent.KEYCODE_D);
							else if (rawDx < -threshold) sendKeyOnce(KeyEvent.KEYCODE_A);
						} else {
							if (rawDy > threshold) sendKeyOnce(KeyEvent.KEYCODE_S);
							else if (rawDy < -threshold) sendKeyOnce(KeyEvent.KEYCODE_W);
						}
					} else {
						// Right Stick: Arrow Keys (Camera/Aim)
						if (Math.abs(rawDx) > Math.abs(rawDy)) {
							if (rawDx > threshold) sendKeyOnce(KeyEvent.KEYCODE_DPAD_RIGHT);
							else if (rawDx < -threshold) sendKeyOnce(KeyEvent.KEYCODE_DPAD_LEFT);
						} else {
							if (rawDy > threshold) sendKeyOnce(KeyEvent.KEYCODE_DPAD_DOWN);
							else if (rawDy < -threshold) sendKeyOnce(KeyEvent.KEYCODE_DPAD_UP);
						}
					}
				} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					stickKnob.animate().translationX(0).translationY(0).setDuration(100).start();
					stickKnob.setAlpha(1.0f);
				}
				return true;
			}
		});

		return stickContainer;
	}

	private View createDPad() {
		// Cross layout container
		RelativeLayout dpad = new RelativeLayout(activity);

		int btnSize = dpToPx(30);

		Button up = createArrowButton("▲", KeyEvent.KEYCODE_W); // Up
		Button down = createArrowButton("▼", KeyEvent.KEYCODE_S); // Down
		Button left = createArrowButton("◀", KeyEvent.KEYCODE_A); // Left
		Button right = createArrowButton("▶", KeyEvent.KEYCODE_D); // Right

		// Positioning
		RelativeLayout.LayoutParams upP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		upP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		upP.addRule(RelativeLayout.CENTER_HORIZONTAL);
		dpad.addView(up, upP);

		RelativeLayout.LayoutParams downP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		downP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		downP.addRule(RelativeLayout.CENTER_HORIZONTAL);
		dpad.addView(down, downP);

		RelativeLayout.LayoutParams leftP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		leftP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		leftP.addRule(RelativeLayout.CENTER_VERTICAL);
		dpad.addView(left, leftP);

		RelativeLayout.LayoutParams rightP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		rightP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		rightP.addRule(RelativeLayout.CENTER_VERTICAL);
		dpad.addView(right, rightP);

		return dpad;
	}

	private View createABXY() {
		RelativeLayout abxy = new RelativeLayout(activity);
		int btnSize = dpToPx(35);

		// Xbox Layout:
		// Top: Y
		// Right: B
		// Bottom: A
		// Left: X

		// Y (Top)
		Button btnY = createRoundButton("Y", 14, KeyEvent.KEYCODE_BUTTON_Y);
		// B (Right)
		Button btnB = createRoundButton("B", 14, KeyEvent.KEYCODE_BUTTON_B);
		// A (Bottom)
		Button btnA = createRoundButton("A", 14, KeyEvent.KEYCODE_BUTTON_A);
		// X (Left)
		Button btnX = createRoundButton("X", 14, KeyEvent.KEYCODE_BUTTON_X);

		// Layout Y (Top)
		RelativeLayout.LayoutParams yP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		yP.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		yP.addRule(RelativeLayout.CENTER_HORIZONTAL);
		abxy.addView(btnY, yP);

		// Layout B (Right)
		RelativeLayout.LayoutParams bP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		bP.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		bP.addRule(RelativeLayout.CENTER_VERTICAL);
		abxy.addView(btnB, bP);

		// Layout X (Left)
		RelativeLayout.LayoutParams xP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		xP.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		xP.addRule(RelativeLayout.CENTER_VERTICAL);
		abxy.addView(btnX, xP);

		// Layout A (Bottom)
		RelativeLayout.LayoutParams aP = new RelativeLayout.LayoutParams(btnSize, btnSize);
		aP.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		aP.addRule(RelativeLayout.CENTER_HORIZONTAL);
		abxy.addView(btnA, aP);

		return abxy;
	}

	private Button createRoundButton(String text, int textSize, int keyCode) {
		Button btn = new Button(activity);
		btn.setText(text);
		btn.setTextSize(textSize);
		btn.setTextColor(Color.WHITE);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(0, 0, 0, 0); // 移除内边距，确保文字居中

		// Use resource for style
		btn.setBackgroundResource(R.drawable.gamepad_button_selector);

		btn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true);
				sendKeyEvent(keyCode, true);
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				v.setPressed(false);
				sendKeyEvent(keyCode, false);
			}
			return true;
		});

		return btn;
	}

	private Button createArrowButton(String text, int keyCode) {
		Button btn = new Button(activity);
		btn.setText(text);
		btn.setTextColor(Color.WHITE);
		btn.setGravity(Gravity.CENTER);
		btn.setPadding(0, 0, 0, 0);
		btn.setBackgroundColor(0xAA333333); // Simple square/rect
		btn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true);
				v.setAlpha(0.6f);
				sendKeyEvent(keyCode, true);
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				v.setPressed(false);
				v.setAlpha(1.0f);
				sendKeyEvent(keyCode, false);
			}
			return true;
		});
		return btn;
	}

	private void sendKeyEvent(int keyCode, boolean isDown) {
		String actionName = isDown ? "DOWN" : "UP";
		String keyName = KeyEvent.keyCodeToString(keyCode);
		DLog.logT("VirtualKeyboard", "Key: %s(%d) | Action: %s", keyName, keyCode, actionName);

		long time = System.currentTimeMillis();
		int action = isDown ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
		activity.dispatchKeyEvent(new KeyEvent(time, time, action, keyCode, 0));
	}

	private void sendKeyOnce(int keyCode) {
		// For joystick continuous hold, we might need state management
		// This is a simplified version
		String keyName = KeyEvent.keyCodeToString(keyCode);
		DLog.logT("VirtualKeyboard", "Joystick Key: %s(%d)", keyName, keyCode);

		activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode));
		activity.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
	}

	private void toggleInputMode(InputMode newMode) {
		currentMode = newMode;
		if (modeChangeListener != null) {
			modeChangeListener.onModeChanged(newMode);
		}

		// Remove old UI
		if (keyboardContainer != null) {
			parentView.removeView(keyboardContainer);
			keyboardContainer = null;
		}

		// Re-init UI
		initUI();

		// Refresh visibility state
		if (isKeyboardVisible) {
			setKeyboardVisibility(true);
		}
	}

	private void toggleInputMode() {
		toggleInputMode(currentMode == InputMode.FULL_KEYBOARD ? InputMode.GAMEPAD : InputMode.FULL_KEYBOARD);
	}

	private void dockFloatingButton(View v) {
		if (screenWidth == 0) return;

		float centerX = v.getX() + v.getWidth() / 2f;
		float screenMid = screenWidth / 2f;
		float targetX;

		// 隐藏 50%
		float hideRatio = 0.5f;
		float hiddenOffset = v.getWidth() * hideRatio;

		if (centerX < screenMid) {
			targetX = -hiddenOffset;
		} else {
			targetX = screenWidth - v.getWidth() + hiddenOffset;
		}

		v.animate().x(targetX).alpha(0.3f).setDuration(300).start();
	}

	public void setKeyboardVisibility(boolean visible) {
		isKeyboardVisible = visible;

		// 如果要显示键盘，检查一下是否需要重新布局（防止尺寸未初始化）
		if (visible) {
			 if (keyboardContainer != null && keyboardContainer.getChildCount() == 0) {
				 if (screenWidth == 0 || screenHeight == 0) {
					DisplayMetrics dm = activity.getResources().getDisplayMetrics();
					screenWidth = dm.widthPixels;
					screenHeight = dm.heightPixels;
				 }
				 refreshKeyboardLayout();
			 }
		}

		updateGameViewLayout(visible);

		if (keyboardContainer != null) {
			keyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
		}

		// 始终保持悬浮按钮显示
		if (floatingToggleBtn != null) {
			floatingToggleBtn.setVisibility(View.VISIBLE);
		}

		if (!visible && gameView != null) {
			gameView.requestFocus();
		}
	}

	/**
	 * 根据当前模式和键盘可见性动态调整 gameView 的 LayoutParams
	 * 从而实现挤占视图的效果
	 */
	private void updateGameViewLayout(boolean visible) {
		if (gameView == null || screenWidth == 0 || screenHeight == 0) return;

		// 假设 gameView 的父容器是 FrameLayout，这也是 AndroidGdxLauncher 中的情况
		if (!(gameView.getLayoutParams() instanceof FrameLayout.LayoutParams)) {
			return;
		}

		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) gameView.getLayoutParams();

		// 重置所有边距
		params.bottomMargin = 0;
		params.leftMargin = 0;
		params.rightMargin = 0;
		params.topMargin = 0;
		// 确保 Gravity 是顶部对齐，这样底部留出的空间才是给键盘的
		params.gravity = Gravity.TOP | Gravity.LEFT;

		if (visible) {
			if (currentMode == InputMode.FULL_KEYBOARD) {
				// 全键盘模式：底部挤占
				boolean isLandscape = screenWidth > screenHeight;
				float ratio = isLandscape ? HEIGHT_RATIO_LANDSCAPE : HEIGHT_RATIO_PORTRAIT;
				int keyboardHeight = (int) (screenHeight * ratio);
				params.bottomMargin = keyboardHeight;
			} else if (currentMode == InputMode.GAMEPAD) {
				// 手柄模式：两侧挤占
				int panelWidth = (int) (screenWidth * GAMEPAD_USED_SPACE);
				params.leftMargin = panelWidth;
				params.rightMargin = panelWidth;
				// 为了让画面居中，我们需要确保 FrameLayout 的 Gravity 是 Center
				// 或者通过 margin 挤压。由于是 FrameLayout，设置左右 margin 后，如果 width 是 MATCH_PARENT，
				// 剩余空间就是中间区域。
				// 注意：如果 gameView 也是 MATCH_PARENT，它会被压缩到中间。
			}
		}

		gameView.setLayoutParams(params);
		gameView.requestLayout(); // 强制请求重新布局，确保触发 surfaceChanged
	}

	public boolean isVisible() {
		return isKeyboardVisible;
	}

	/**
	 * 当屏幕尺寸变化时调用
	 */
	public void onScreenResize(int width, int height) {
		this.screenWidth = width;
		this.screenHeight = height;

		calculateGamepadRatio(); // 屏幕旋转或尺寸变化时重新计算比例

		refreshKeyboardLayout();

		// 屏幕旋转后，键盘高度可能变化，需要更新 gameView 的挤压布局
		if (isKeyboardVisible) {
			updateGameViewLayout(true);
		}
	}

	private void refreshKeyboardLayout() {
		if (keyboardContainer == null || screenWidth == 0 || screenHeight == 0) return;

		if (currentMode == InputMode.GAMEPAD) {
			// 手柄模式下确保占满全屏，且不重置子View
			ViewGroup.LayoutParams params = keyboardContainer.getLayoutParams();
			if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
				params.height = ViewGroup.LayoutParams.MATCH_PARENT;
				keyboardContainer.setLayoutParams(params);
			}
			return;
		}

		keyboardContainer.removeAllViews();

		boolean isLandscape = screenWidth > screenHeight;
		float ratio = isLandscape ? HEIGHT_RATIO_LANDSCAPE : HEIGHT_RATIO_PORTRAIT;
		int totalHeight = (int) (screenHeight * ratio);

		ViewGroup.LayoutParams params = keyboardContainer.getLayoutParams();
		params.height = totalHeight;
		keyboardContainer.setLayoutParams(params);

		for (String[] rowKeys : terminalLayout) {
			LinearLayout row = new LinearLayout(activity);
			row.setOrientation(LinearLayout.HORIZONTAL);
			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
			row.setLayoutParams(rowParams);

			for (String key : rowKeys) {
				Button keyBtn = createKeyButton(key);
				float weight = key.equals("Space") ? 6.0f : 1.0f;

				LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
					0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
				btnParams.setMargins(0, 0, 0, 0);
				keyBtn.setLayoutParams(btnParams);

				row.addView(keyBtn);
			}
			keyboardContainer.addView(row);
		}
	}

	private Button createKeyButton(String text) {
		Button btn = new Button(activity);
		btn.setText(text);
		btn.setTextColor(Color.WHITE);
		btn.setTextSize(13);
		btn.setGravity(Gravity.CENTER);
		int p = (int) (padding * activity.getResources().getDisplayMetrics().density);
		btn.setPadding(p, p, p, p);
		btn.setBackgroundResource(R.drawable.virtual_key_background);

		btn.setOnTouchListener((v, event) -> {
			if (event.getAction() == MotionEvent.ACTION_DOWN) {
				v.setPressed(true);
				handleKeyPress(text, true);
			} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
				v.setPressed(false);
				handleKeyPress(text, false);
			}
			return true;
		});
		return btn;
	}

	private void handleKeyPress(String key, boolean down) {
		if (down && key.equals("Hide")) {
			setKeyboardVisibility(false);
			return;
		}

		Integer code = keyMap.get(key);
		if (code != null) {
			sendKeyEvent(code, down);
		}
	}

	private void initKeyMap() {
		for (char c = 'A'; c <= 'Z'; c++) keyMap.put(String.valueOf(c), KeyEvent.keyCodeFromString(String.valueOf(c)));
		for (char c = '0'; c <= '9'; c++) keyMap.put(String.valueOf(c), KeyEvent.KEYCODE_0 + (c - '0'));

		keyMap.put("-", KeyEvent.KEYCODE_MINUS);
		keyMap.put("=", KeyEvent.KEYCODE_EQUALS);
		keyMap.put("[", KeyEvent.KEYCODE_LEFT_BRACKET);
		keyMap.put("]", KeyEvent.KEYCODE_RIGHT_BRACKET);
		keyMap.put(";", KeyEvent.KEYCODE_SEMICOLON);
		keyMap.put("'", KeyEvent.KEYCODE_APOSTROPHE);
		keyMap.put(",", KeyEvent.KEYCODE_COMMA);
		keyMap.put(".", KeyEvent.KEYCODE_PERIOD);
		keyMap.put("/", KeyEvent.KEYCODE_SLASH);

		keyMap.put("Del", KeyEvent.KEYCODE_DEL);
		keyMap.put("Tab", KeyEvent.KEYCODE_TAB);
		keyMap.put("Esc", KeyEvent.KEYCODE_ESCAPE);
		keyMap.put("Enter", KeyEvent.KEYCODE_ENTER);
		keyMap.put("Space", KeyEvent.KEYCODE_SPACE);
		keyMap.put("Shift", KeyEvent.KEYCODE_SHIFT_LEFT);
		keyMap.put("Ctrl", KeyEvent.KEYCODE_CTRL_LEFT);
		keyMap.put("Alt", KeyEvent.KEYCODE_ALT_LEFT);
		keyMap.put("Sym", KeyEvent.KEYCODE_SYM);

		keyMap.put("↑", KeyEvent.KEYCODE_DPAD_UP);
		keyMap.put("↓", KeyEvent.KEYCODE_DPAD_DOWN);
		keyMap.put("←", KeyEvent.KEYCODE_DPAD_LEFT);
		keyMap.put("→", KeyEvent.KEYCODE_DPAD_RIGHT);
	}
}
