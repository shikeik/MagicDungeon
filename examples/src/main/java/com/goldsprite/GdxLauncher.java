package com.goldsprite;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.audio.SynthAudio;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.gdengine.log.DebugConsole;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.ui.widget.single.ToastUI;
import com.goldsprite.gdengine.ui.widget.single.DialogUI;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.gdengine.web.DocServer;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;
import com.goldsprite.magicdungeon.screens.ExampleSelectScreen;
import com.goldsprite.magicdungeon.testing.GameAutoTests;
import com.kotcrab.vis.ui.VisUI;

public class GdxLauncher extends Game {int k4;
	private Stage toastStage;
	public Debug debug;
	private Application.ApplicationType userType;

	// [新增] 标记是否已初始化完成
	private boolean isInitialized = false;

	boolean enableAutoTests = false; // 是否开启全局自动测试流程

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
		SynthAudio.init();
		userType = Gdx.app.getType();
		debug = Debug.getInstance();
		debug.initUI();
		toastStage = new Stage(new ScreenViewport());
		toastStage.addActor(ToastUI.inst());
		toastStage.addActor(DialogUI.getInstance());

		ScreenManager sm = new ScreenManager()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);

		// [修复] 全局配置输入更新钩子，确保 InputManager 在启动时立即生效
		// 解决初次进入演示屏手柄无法操作及鼠标锁定后无法解锁的问题
		ScreenManager.inputUpdater = () -> InputManager.getInstance().update();
		ScreenManager.backKeyTrigger = () -> InputManager.getInstance().isJustPressed(InputAction.BACK);
        
        // Catch Android Back Key
        Gdx.input.setCatchKey(Input.Keys.BACK, true);

		// 确保 toastStage 能接收输入 (插入到最前面)
		sm.getImp().addProcessor(toastStage);

		isInitialized = true;

		// [Global AutoTest]
		if(enableAutoTests) GameAutoTests.setup();
	}

	@Override
	public void render() {
		// [核心] 如果还没初始化，只清屏，不跑逻辑
		if (!isInitialized) {
			ScreenUtils.clear(0, 0, 0, 1);
			return;
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
//			Debug.showDebugUI = !Debug.showDebugUI;
			if(Debug.shortcuts) DebugConsole.autoSwitchState();
		}

		ScreenManager.getInstance().render();
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
			if(SynthAudio.isInitialized()) SynthAudio.dispose();
			DocServer.stopServer(); // [Fix] 停止文档服务器线程，防止 JVM 挂起
			if(VisUI.isLoaded()) VisUI.dispose();
		}catch (Throwable ignored){} finally {
			System.exit(0); // 莫名奇妙的visUI.dispose bug导致运行程序不结束所以这里手动强制停止
		}
	}
}
