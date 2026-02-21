package com.goldsprite.magicdungeon2.screens.basics;

import java.util.Map;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon2.screens.main.SimpleGameScreen;

/**
 * MagicDungeon2 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心场景 ---
		map.put("开始游戏", SimpleGameScreen.class);

		map.put("", null); // 分隔线

		// --- 功能测试 ---
		map.put("测试", TestSelectionScreen.class);
	}

	@Override
	protected void onScreenSelected(Class<? extends GScreen> screenClass) {
		if (screenClass == SimpleGameScreen.class) {
			// 进入游戏场景使用加载转场（小人动画），加载完所有资源后再完成转场
			getScreenManager().playLoadingTransition((finishCallback) -> {
				getScreenManager().replaceScreen(screenClass);
				finishCallback.run();
			}, "正在加载地牢资源...", 1.5f);
		} else {
			super.onScreenSelected(screenClass);
		}
	}
}
