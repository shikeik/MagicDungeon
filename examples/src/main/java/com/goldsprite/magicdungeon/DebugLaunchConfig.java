package com.goldsprite.magicdungeon;

import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.screens.tests.neonskel.NeonSkelEditorScreen;

/**
 * 调试启动配置
 * 用于开发阶段快速进入特定场景并执行自动测试
 * 注意：提交代码时请确保 ENABLE_DIRECT_LAUNCH 为 false
 */
public class DebugLaunchConfig {
    
    /** 是否启用直接启动 (跳过选单) */
    public static boolean ENABLE_DIRECT_LAUNCH = false;
    
    /** 目标启动场景 */
    public static Class<? extends GScreen> TARGET_SCREEN = NeonSkelEditorScreen.class;
    
    /** 是否在启动后立即运行自动测试 */
    public static boolean ENABLE_AUTO_TEST = false;

}
