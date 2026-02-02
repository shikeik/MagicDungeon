package com.goldsprite.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;

import java.util.Map;

/**
 * MagicDungeon 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心开发工具 ---

		map.put("", null); // 分隔线

		// --- 引擎功能测试 ---
		map.put("测试", MagicDungeonSelectionScreen.class);
	}
}
