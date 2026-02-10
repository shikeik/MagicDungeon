package com.goldsprite.magicdungeon.android;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.HashMap;
import java.util.Map;

/**
 * 虚拟键盘管理类
 * 负责管理安卓端的虚拟键盘 UI、交互逻辑以及按键事件分发
 */
public class VirtualKeyboard {

    private final Activity activity;
    private final RelativeLayout parentView;
    private final View gameView; // 游戏视图，用于键盘隐藏时获取焦点

    private final float HEIGHT_RATIO_LANDSCAPE = 0.45f;
    private final float HEIGHT_RATIO_PORTRAIT = 0.35f;
    private final int padding = -16;
    
    private final String[][] terminalLayout = {
        {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "-", "=", "Del"},
        {"Tab", "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "[", "]"},
        {"Esc", "A", "S", "D", "F", "G", "H", "J", "K", "L", ";", "'", "Enter"},
        {"Shift", "Z", "X", "C", "V", "B", "N", "M", ",", ".", "/", "↑"},
        {"Ctrl", "Alt", "Sym", "Space", "←", "↓", "→", "Hide"}
    };
    
    private final Map<String, Integer> keyMap = new HashMap<>();
    
    private LinearLayout keyboardContainer;
    private Button floatingToggleBtn;
    
    private boolean isKeyboardVisible = false;
    private int screenWidth, screenHeight;

    public VirtualKeyboard(Activity activity, RelativeLayout parentView, View gameView) {
        this.activity = activity;
        this.parentView = parentView;
        this.gameView = gameView;
        
        initKeyMap();
        // UI 初始化需要在主线程进行，但通常构造函数也是在主线程调用的
        // 为保险起见，如果不在主线程，可以 post 出去，但这里假设调用者会处理或就在主线程
        initUI();
    }

    private void initUI() {
        // --- A. 键盘容器 ---
        keyboardContainer = new LinearLayout(activity);
        keyboardContainer.setOrientation(LinearLayout.VERTICAL);
        // [回归] 不设背景色或透明
        // keyboardContainer.setBackgroundColor(0x88000000);
        keyboardContainer.setVisibility(View.GONE);

        RelativeLayout.LayoutParams kbParams = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0);
        kbParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        parentView.addView(keyboardContainer, kbParams);

        // --- B. 悬浮开关按钮 ---
        floatingToggleBtn = new Button(activity);
        floatingToggleBtn.setText("⌨");
        floatingToggleBtn.setTextColor(Color.CYAN);
        floatingToggleBtn.setTextSize(20);
        floatingToggleBtn.setPadding(20, 20, 20, 20); // 给点内边距

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(100);
        shape.setColor(0x44000000);
        shape.setStroke(2, 0xFF00EAFF);
        floatingToggleBtn.setBackground(shape);

        // [v19.5 修复] 使用 WRAP_CONTENT，这样文字长了会自动变大，不会被切
        int btnSize = 120;
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

                        if (!isDrag) {
                            setKeyboardVisibility(true);
                            dockFloatingButton(v);
                        } else {
                            dockFloatingButton(v);
                        }
                        return true;
                }
                return false;
            }
        });

        floatingToggleBtn.setAlpha(0.3f);
        parentView.addView(floatingToggleBtn);

        // 初始位置设置 (延时一帧确保宽高已计算)
        floatingToggleBtn.post(() -> {
            if (screenHeight > 0) {
                floatingToggleBtn.setY(screenHeight / 2f - floatingToggleBtn.getHeight() / 2f);
                dockFloatingButton(floatingToggleBtn);
            }
        });
        
        // 初始布局刷新
        refreshKeyboardLayout();
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
        if (keyboardContainer != null) {
            keyboardContainer.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
        if (floatingToggleBtn != null) {
            floatingToggleBtn.setVisibility(visible ? View.GONE : View.VISIBLE);
        }

        if (!visible && gameView != null) {
            gameView.requestFocus();
        }
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
        refreshKeyboardLayout();
    }

    private void refreshKeyboardLayout() {
        if (keyboardContainer == null || screenWidth == 0 || screenHeight == 0) return;

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
                float weight = key.equals("Space") ? 3.0f : 1.0f;

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
            long time = System.currentTimeMillis();
            int action = down ? KeyEvent.ACTION_DOWN : KeyEvent.ACTION_UP;
            activity.dispatchKeyEvent(new KeyEvent(time, time, action, code, 0));
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
