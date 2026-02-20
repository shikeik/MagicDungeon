package com.goldsprite.magicdungeon2.android;

import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.goldsprite.GdxLauncher;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.screens.ScreenManager;

import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.gdengine.log.DLog;

public class AndroidGdxLauncher extends AndroidApplication {
	private static AndroidGdxLauncher ctx;

	private FrameLayout rootFrame;
	private RelativeLayout overlayLayer;
	private View gameView;
	private int screenWidth, screenHeight;

	private VirtualKeyboard virtualKeyboard;

	public static AndroidGdxLauncher getCtx() { return ctx; }

	// [新增] 持有 Launcher 引用以便后期注入
	private GdxLauncher gdxLauncher;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//android 初始配置
		ctx = this;
		ScreenUtils.hideBlackBar(this);
		UncaughtExceptionActivity.setUncaughtExceptionHandler(this, AndroidGdxLauncher.class);

		// 平台实现 初始配置
		boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
		PlatformImpl.AndroidExternalStoragePath = Environment.getExternalStorageDirectory().getAbsolutePath();
		PlatformImpl.defaultOrientation = isPortrait ? ScreenManager.Orientation.Portrait : ScreenManager.Orientation.Landscape;

		// 实现虚拟键盘回调支持
		setupViewportListener();

		// GLog注册logcat输出端
		DLog.registerLogOutput((level, tag, msg) -> {
			switch (level) {
				case ERROR -> Log.e(tag, msg);
				case WARN -> Log.w(tag, msg);
				case INFO -> Log.i(tag, msg);
				case DEBUG -> Log.d(tag, msg);
			}
		});


	 	// gdx启动配置
		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useImmersiveMode = true;
		cfg.numSamples = 2;

		// 初始化gdx
		gdxLauncher = new GdxLauncher();

		gameView = initializeForView(gdxLauncher, cfg);

		// 2. 设置布局
		rootFrame = new FrameLayout(this);
		rootFrame.addView(gameView, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		setContentView(rootFrame);

		// 3. UI 初始化
		new Handler(getMainLooper()).post(this::initOverlayUI);

		// 4. 设置监听器 (这些可以现在就设，虽然游戏还没逻辑)
		setupGdxListeners();

	}

	private void setupGdxListeners() {
		ScreenManager.exitGame.add(() -> moveTaskToBack(true));
		PlatformImpl.showSoftInputKeyBoard = (show) -> runOnUiThread(() -> {
			if (virtualKeyboard != null) {
				virtualKeyboard.setKeyboardVisibility(show);
			}
		});

		// 注意：orientationChanger 可能会在 GdxLauncher 初始化前被调用吗？
		// 应该不会，因为 ScreenManager 还没初始化。但为了安全，保持原样即可。
		ScreenManager.orientationChanger = (orientation) -> runOnUiThread(() -> {
			setRequestedOrientation(orientation == ScreenManager.Orientation.Landscape ?
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		});

		gameView.setFocusable(true);
		gameView.setFocusableInTouchMode(true);
		gameView.requestFocus();
	}

	private void initOverlayUI() {
		// ... (保持原样)
		overlayLayer = new RelativeLayout(this);
		overlayLayer.setBackgroundColor(Color.TRANSPARENT);
		// 让它可以点击穿透（点击空白处传给游戏），但子 View (按钮) 依然可以点击
		// RelativeLayout 默认就是不拦截空白点击的，只要不设 ClickListener

		rootFrame.addView(overlayLayer, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		virtualKeyboard = new VirtualKeyboard(this, overlayLayer, gameView, (mode) -> {
			InputManager.getInstance().setVirtualGamepad(mode == VirtualKeyboard.InputMode.GAMEPAD);
		});
	}

	private void setupViewportListener() {
		getWindow().getDecorView().getViewTreeObserver().addOnGlobalLayoutListener(() -> {
			Rect r = new Rect();
			getWindow().getDecorView().getWindowVisibleDisplayFrame(r);
			int w = r.width();
			int h = r.height();
			if (w != screenWidth || h != screenHeight) {
				screenWidth = w;
				screenHeight = h;
				if (virtualKeyboard != null) {
					virtualKeyboard.onScreenResize(screenWidth, screenHeight);
				}
			}
		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			// 返回 true 拦截事件，阻止系统默认的 finish() 行为
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}
