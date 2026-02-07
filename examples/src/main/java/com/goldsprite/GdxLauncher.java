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
import com.goldsprite.gdengine.ui.widget.ToastUI;
import com.goldsprite.magicdungeon.screens.ExampleSelectScreen;
import com.kotcrab.vis.ui.VisUI;

public class GdxLauncher extends Game {int k6;
	private Stage toastStage;
	public Debug debug;
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
		SynthAudio.init();
		userType = Gdx.app.getType();
		debug = Debug.getInstance();
		debug.initUI();
		toastStage = new Stage(new ScreenViewport());
		toastStage.addActor(ToastUI.inst());

		new ScreenManager()
			.addScreen(new ExampleSelectScreen())
			.setLaunchScreen(ExampleSelectScreen.class);

		isInitialized = true;
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
		ScreenManager.getInstance().dispose();
		if (debug != null) debug.dispose();
		VisUI.dispose();
		SynthAudio.dispose();
	}
}
