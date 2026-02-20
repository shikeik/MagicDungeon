package com.goldsprite.magicdungeon2.screens;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.basics.BaseSelectionScreen;
import java.util.Map;

/**
 * MagicDungeon2 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心场景 ---
		// [修改] 开始游戏指向预加载屏幕，以实现统一资源加载
		map.put("开始游戏", null);

		map.put("", null); // 分隔线

		// --- 功能测试 ---
		map.put("测试", TestSelectionScreen.class);
	}
}
