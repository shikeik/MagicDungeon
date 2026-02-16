package com.goldsprite.magicdungeon.testing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
        
        // 5. 输入作弊码 "cheat666" (获取所有物品)
        atm.log("输入作弊码 cheat666...");
        addKeyStroke(atm, Input.Keys.C);
        addKeyStroke(atm, Input.Keys.H);
        addKeyStroke(atm, Input.Keys.E);
        addKeyStroke(atm, Input.Keys.A);
        addKeyStroke(atm, Input.Keys.T);
        addKeyStroke(atm, Input.Keys.NUMPAD_6);
        addKeyStroke(atm, Input.Keys.NUMPAD_6);
        addKeyStroke(atm, Input.Keys.NUMPAD_6);
        
        atm.addWait(1.0f);

        // 6. 打开背包 (BAG)
        atm.addAction("Open Inventory", () -> {
            InputManager.getInstance().simulatePress(InputAction.BAG);
        });
        atm.addAction("Release BAG", () -> InputManager.getInstance().simulateRelease(InputAction.BAG));
        atm.addWait(1.0f);
        
        // 7. 验证背包有物品
        atm.add(new AutoTestManager.AssertTask("Check Inventory Not Empty", () -> {
            if (ScreenManager.getInstance().getCurScreen() instanceof GameScreen) {
                GameScreen gs = (GameScreen) ScreenManager.getInstance().getCurScreen();
                return gs.getHud().getInventoryUIItemCount() > 0;
            }
            return false;
        }));

        // 8. 穿戴第一个物品 (Click Item 0)
        atm.addAction("Equip Item 0", () -> {
             if (ScreenManager.getInstance().getCurScreen() instanceof GameScreen) {
                GameScreen gs = (GameScreen) ScreenManager.getInstance().getCurScreen();
                gs.getHud().simulateInventoryItemClick(0);
            }
        });
        atm.addWait(1.0f);

        // 9. 模拟拖拽装备 (Drag Item 1 -> Item 2)
        atm.addAction("Drag Item 1 -> Item 2", () -> {
             if (ScreenManager.getInstance().getCurScreen() instanceof GameScreen) {
                GameScreen gs = (GameScreen) ScreenManager.getInstance().getCurScreen();
                // Convert Stage coordinates (Y-up) to Screen coordinates (Y-down) for DragTask
                com.badlogic.gdx.math.Vector2 p1 = gs.getHud().getInventorySlotCenter(1);
                com.badlogic.gdx.math.Vector2 p2 = gs.getHud().getInventorySlotCenter(2);
                
                if (p1 != null && p2 != null) {
                    float h = Gdx.graphics.getHeight();
                    AutoTestManager.getInstance().add(new AutoTestManager.DragTask(
                        p1.x, h - p1.y, 
                        p2.x, h - p2.y, 
                        1.0f
                    ));
                }
            }
        });
        atm.addWait(1.5f); // Wait for drag to finish

        // 10. 关闭背包
        atm.addAction("Close Inventory", () -> {
            InputManager.getInstance().simulatePress(InputAction.BAG);
        });
        atm.addAction("Release BAG", () -> InputManager.getInstance().simulateRelease(InputAction.BAG));
        atm.addWait(0.5f);

        // 11. 结束测试
        atm.addAction("Test Complete", () -> {
            atm.logPass("模拟人类测试流程执行完毕。");
            AutoTestManager.ENABLED = false;
        });
    }
    
    private static void addKeyStroke(AutoTestManager atm, int keycode) {
        atm.addAction("Press Key " + Input.Keys.toString(keycode), () -> {
            InputManager.getInstance().simulateKeyPress(keycode);
        });
        atm.addAction("Release Key " + Input.Keys.toString(keycode), () -> {
            InputManager.getInstance().simulateKeyRelease(keycode);
        });
        atm.addWait(0.1f);
    }
}
