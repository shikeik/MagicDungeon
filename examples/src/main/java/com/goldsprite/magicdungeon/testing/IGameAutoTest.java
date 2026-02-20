package com.goldsprite.magicdungeon2.testing;

/**
 * 统一的自动测试接口
 */
public interface IGameAutoTest {
	/**
	 * 执行测试逻辑
	 * 通常在这里调用 AutoTestManager.getInstance() 添加任务
	 */
	void run();
}
