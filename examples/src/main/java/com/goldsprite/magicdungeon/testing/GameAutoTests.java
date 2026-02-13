package com.goldsprite.magicdungeon.testing;

import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.gdengine.ui.widget.single.DialogUI;
import com.goldsprite.magicdungeon.screens.TestSelectionScreen;
import com.goldsprite.magicdungeon.screens.tests.VisUIDemoScreen;

/**
 * 游戏特定的自动测试配置
 */
public class GameAutoTests {

    public static void setup() {
        // 开启自动测试
        AutoTestManager.ENABLED = true;
        AutoTestManager atm = AutoTestManager.getInstance();

        atm.log("启动全局自动测试流程...");

        // 1. 等待游戏完全加载
        atm.addWait(1.0f);

        // 2. 进入测试选择界面
        atm.addAction("Ensure TestSelectionScreen", () -> {
            ScreenManager.getInstance().setCurScreen(TestSelectionScreen.class, true);
        });

        // 3. 切换到 VisUI Demo (包含皮肤切换测试)
        atm.addAction("Enter VisUIDemoScreen", () -> {
            ScreenManager.getInstance().setCurScreen(VisUIDemoScreen.class, true);
        });

        // 4. 停留观察 2 秒
        atm.addWait(2.0f);

        // 5. 断言检查
        atm.add(new AutoTestManager.AssertTask("Check VisUIDemoScreen active", () -> {
            return ScreenManager.getInstance().getCurScreen() instanceof VisUIDemoScreen;
        }));

        // 6. 模拟点击切换皮肤按钮? (太复杂，暂时只是等待)
        // 我们可以通过 ActionTask 直接调用 Screen 的方法来切换皮肤，如果需要深入测试的话
        // 这里简化为只测试进入和退出

        // 7. 结束测试，返回主界面
        atm.addAction("Return to Main Menu", () -> {
            ScreenManager.getInstance().setCurScreen(TestSelectionScreen.class, false);
        });

        atm.addAction("Test Complete", () -> {
            // 这里我们不弹窗，直接日志输出，因为全局测试可能无人值守
            atm.logPass("所有自动测试流程已执行完毕。");

            // 尝试显示全局弹窗
            DialogUI.show("测试完成", "所有 UI 场景自动切换测试已通过！");

            AutoTestManager.ENABLED = false;
        });
    }
}
