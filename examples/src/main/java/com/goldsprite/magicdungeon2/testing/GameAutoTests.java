package com.goldsprite.magicdungeon2.testing;

/**
 * 游戏特定的自动测试配置
 */
public class GameAutoTests implements IGameAutoTest {

	@Override
	public void run() {
		// 使用新的人类模拟测试流程
		new HumanSimulatorTest().run();
	}
}
