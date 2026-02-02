package com.goldsprite.screens;

import com.goldsprite.magicdungeon.screens.GScreen;
import com.goldsprite.magicdungeon.screens.basics.BaseSelectionScreen;
import com.goldsprite.magicdungeon.screens.ecs.hub.MagicDungeonHubScreen;
import com.goldsprite.magicdungeon.screens.ecs.editor.EditorGameScreen;

import java.util.Map;

/**
 * MagicDungeon 开发主入口
 * 策略：只展示核心工具，旧有的业务逻辑归档至二级菜单
 */
public class ExampleSelectScreen extends BaseSelectionScreen {

	@Override
	protected void initScreenMapping(Map<String, Class<? extends GScreen>> map) {
		// --- 核心开发工具 ---
		map.put(">>> MagicDungeon Hub (项目管理) <<<", MagicDungeonHubScreen.class);

		map.put("", null); // 分隔线

		// --- 引擎功能测试 ---
		map.put("测试", MagicDungeonSelectionScreen.class);
	}
}
