package com.goldsprite;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon2.config.LaunchMode;
import com.goldsprite.magicdungeon2.testing.IGameAutoTest;
import com.goldsprite.magicdungeon2.testing.GameAutoTests;
import com.goldsprite.magicdungeon2.screens.ExampleSelectScreen;

/**
 * 调试启动配置
 * 用于开发阶段快速进入特定场景并执行自动测试
 * 注意：提交代码时请确保 LaunchMode 为 NORMAL
 */
public class DebugLaunchConfig {
	
	/** 当前启动模式 */
	public static LaunchMode currentMode = LaunchMode.NORMAL;
	
	/** 目标启动场景 (仅在 DIRECT_SCENE 或 AUTO_TEST 模式下有效) */
	public static Class<? extends GScreen> targetScreen = ExampleSelectScreen.class;
	
	/** 自动测试类 (仅在 AUTO_TEST 模式下有效) */
	public static Class<? extends IGameAutoTest> autoTestClass = GameAutoTests.class;

}
