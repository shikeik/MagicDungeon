package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.DebugLaunchConfig;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.log.DebugConsole;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.gdengine.ui.widget.single.ToastUI;
import com.goldsprite.magicdungeon.config.LaunchMode;
import com.goldsprite.magicdungeon.screens.ExampleSelectScreen;
import com.goldsprite.magicdungeon.testing.IGameAutoTest;
import com.kotcrab.vis.ui.VisUI;
import com.goldsprite.magicdungeon.ui.MagicDungeonLoadingRenderer;
import com.goldsprite.magicdungeon.input.InputManager;
import com.goldsprite.magicdungeon.input.InputAction;

public class GdxLauncher extends Game {int k11;
	private Stage toastStage;
	public DLog debug;
	private Application.ApplicationType userType;

	// [新增] 标记是否已初始化完成
	private boolean isInitialized = false;

	public GdxLauncher() {
	}

	@Override
	public void create() {
		initGameContent();
	}

	// 真正的初始化逻辑提取出来
	public void initGameContent() {
		if (isInitialized) return;

		VisUIHelper.loadWithChineseFont();
		userType = Gdx.app.getType();
		debug = DLog.getInstance();
		debug.initUI();
		toastStage = new Stage(new ScreenViewport());
		toastStage.addActor(ToastUI.inst());

		ScreenManager sm = new ScreenManager()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);
		sm.setLoadingRenderer(new MagicDungeonLoadingRenderer());

		// [修复] 全局配置输入更新钩子，确保 InputManager 在启动时立即生效
		// 解决初次进入演示屏手柄无法操作及鼠标锁定后无法解锁的问题
		ScreenManager.inputUpdater = () -> InputManager.getInstance().update();
		ScreenManager.backKeyTrigger = () -> InputManager.getInstance().isJustPressed(InputAction.BACK);

		// Catch Android Back Key
//		Gdx.input.setCatchKey(Input.Keys.BACK, true);

		// 确保 toastStage 能接收输入 (插入到最前面)
		sm.getImp().addProcessor(0, toastStage);

		isInitialized = true;

		// [Launch Mode Dispatch]
		switch (DebugLaunchConfig.currentMode) {
			case NORMAL:
				// 正常流程，ScreenManager 已配置默认启动屏
				break;
			case DIRECT_SCENE:
			case AUTO_TEST:
				if (DebugLaunchConfig.targetScreen != null) {
					DLog.log("启动模式: 自动测试 -> 场景: " + DebugLaunchConfig.targetScreen.getSimpleName());
					sm.goScreen(DebugLaunchConfig.targetScreen);
				}
				if(DebugLaunchConfig.currentMode == LaunchMode.DIRECT_SCENE) break;

				// 自动测试模式继续执行测试用例
				if (DebugLaunchConfig.autoTestClass != null) {
					DLog.log("启动模式: 自动测试 -> 用例: " + DebugLaunchConfig.autoTestClass.getSimpleName());
					try {
						// 使用反射实例化测试类
						// 注意：测试类必须有无参构造函数
						IGameAutoTest test = DebugLaunchConfig.autoTestClass.getDeclaredConstructor().newInstance();
						test.run();
					} catch (Exception e) {
						DLog.logErr("无法实例化测试用例: " + e.getMessage());
						e.printStackTrace();
					}
				}
				break;
		}
	}

	@Override
	public void render() {
		// [核心] 如果还没初始化，只清屏，不跑逻辑
		if (!isInitialized) {
			ScreenUtils.clear(0, 0, 0, 1);
			return;
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
//			DLog.showDebugUI = !DLog.showDebugUI;
			if(DLog.shortcuts) DebugConsole.autoSwitchState();
		}

		ScreenManager.getInstance().render();

		DLog.infoT("ScreenStack", ScreenManager.getInstance().getScreenStackInfo());

		if (debug != null) debug.render();

		// 自动测试驱动
		AutoTestManager.getInstance().update(Gdx.graphics.getDeltaTime());

		// toast
		toastStage.act();
		toastStage.draw();
	}

	@Override
	public void resize(int width, int height) {
		ScreenManager.getInstance().resize(width, height);
		if (debug != null) debug.resize(width, height);
	}

	@Override
	public void dispose() {
		try{
			ScreenManager.getInstance().dispose();
			if (debug != null) debug.dispose();
			if(VisUI.isLoaded()) VisUI.dispose();
		}catch (Throwable ignored){} finally {
			System.exit(0); // 莫名奇妙的visUI.dispose bug导致运行程序不结束所以这里手动强制停止
		}
	}
}
