package com.goldsprite.magicdungeon.testing;

import com.badlogic.gdx.Gdx;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon.core.screens.GameScreen;
import com.goldsprite.magicdungeon.core.screens.MainMenuScreen;
import com.goldsprite.magicdungeon.input.InputAction;
import com.goldsprite.magicdungeon.input.InputManager;

/**
 * 模拟人类可视化测试流程
 * 模拟用户从主菜单进入游戏，移动，操作背包等
 */
public class HumanSimulatorTest {

    public static void setup() {
        AutoTestManager.ENABLED = true;
        AutoTestManager atm = AutoTestManager.getInstance();
        atm.log("启动模拟人类可视化测试流程...");

        // 1. 进入主菜单 (重置状态)
        atm.addAction("Go to MainMenu", () -> {
            ScreenManager.getInstance().setCurScreen(MainMenuScreen.class, true);
        });
        atm.addWait(1.0f);

        // 2. 模拟点击 "开始游戏"
        atm.addAction("Press UI_CONFIRM (Start Game)", () -> {
            InputManager.getInstance().simulatePress(InputAction.UI_CONFIRM);
        });
        atm.addAction("Release UI_CONFIRM", () -> {
            InputManager.getInstance().simulateRelease(InputAction.UI_CONFIRM);
        });

        atm.addWait(2.0f); // 等待游戏加载

        // 3. 验证进入了 GameScreen
        atm.add(new AutoTestManager.AssertTask("Check in GameScreen", () -> {
            return ScreenManager.getInstance().getCurScreen() instanceof GameScreen;
        }));

        // 4. 模拟移动玩家 (Move Right)
        atm.addAction("Move Right Start", () -> InputManager.getInstance().simulatePress(InputAction.MOVE_RIGHT));
        atm.addWait(1.0f);
        atm.addAction("Move Right Stop", () -> InputManager.getInstance().simulateRelease(InputAction.MOVE_RIGHT));
        atm.addWait(0.5f);

        // Move Down
        atm.addAction("Move Down Start", () -> InputManager.getInstance().simulatePress(InputAction.MOVE_DOWN));
        atm.addWait(1.0f);
        atm.addAction("Move Down Stop", () -> InputManager.getInstance().simulateRelease(InputAction.MOVE_DOWN));
        atm.addWait(0.5f);

        // 5. 打开背包 (BAG)
        atm.addAction("Open Inventory", () -> {
            InputManager.getInstance().simulatePress(InputAction.BAG);
        });
        atm.addAction("Release BAG", () -> InputManager.getInstance().simulateRelease(InputAction.BAG));
        atm.addWait(1.0f);

        // 6. 模拟拖拽装备
        // 获取屏幕中心
        final float w = Gdx.graphics.getWidth();
        final float h = Gdx.graphics.getHeight();
        final float cx = w / 2f;
        final float cy = h / 2f;

        // 模拟从中心偏左拖动到中心偏右
        atm.add(new AutoTestManager.DragTask(cx - 100, cy, cx + 100, cy, 0.5f));
        atm.addWait(1.0f);

        // 7. 关闭背包
        atm.addAction("Close Inventory", () -> {
            InputManager.getInstance().simulatePress(InputAction.BAG);
        });
        atm.addAction("Release BAG", () -> InputManager.getInstance().simulateRelease(InputAction.BAG));
        atm.addWait(0.5f);

        // 8. 结束测试
        atm.addAction("Test Complete", () -> {
            atm.logPass("模拟人类测试流程执行完毕。");
            AutoTestManager.ENABLED = false;
        });
    }
}
