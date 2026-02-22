package com.goldsprite.magicdungeon2.screens.basics;

import java.util.Map;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon2.screens.main.LanLobbyScreen;
import com.goldsprite.magicdungeon2.screens.main.SimpleGameScreen;

/**
 * MagicDungeon2 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心场景 ---
		map.put("单人游戏", SimpleGameScreen.class);
		map.put("联机游玩", LanLobbyScreen.class);

		map.put("", null); // 分隔线

		// --- 功能测试 ---
		map.put("测试", TestSelectionScreen.class);
	}

	@Override
	protected void onScreenSelected(Class<? extends GScreen> screenClass) {
		if (screenClass == SimpleGameScreen.class) {
			// 单人模式：加载转场后进入游戏（lanService 为 null）
			getScreenManager().playLoadingTransition((finishCallback) -> {
				SimpleGameScreen gameScreen = new SimpleGameScreen(null);
				getScreenManager().goScreen(gameScreen);
				finishCallback.run();
			}, "正在加载地牢资源...", 1.5f);
		} else if (screenClass == LanLobbyScreen.class) {
			// 联机大厅：直接进入（不需要加载资源）
			getScreenManager().goScreen(screenClass);
		} else {
			super.onScreenSelected(screenClass);
		}
	}
}
