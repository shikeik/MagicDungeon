package com.goldsprite.magicdungeon.testing;

import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.gdengine.ui.widget.single.DialogUI;
import com.goldsprite.magicdungeon.screens.TestSelectionScreen;
import com.goldsprite.magicdungeon.testing.IGameAutoTest;

/**
 * 游戏特定的自动测试配置
 */
public class GameAutoTests implements IGameAutoTest {

	@Override
	public void run() {
		// 使用新的人类模拟测试流程
		//new HumanSimulatorTest().run();
	}
}
