package com.goldsprite.magicdungeon.testing;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon.ai.AIDrawExecutor;

/**
 * 自动化测试：从 assets/ai_draw_cmds/ 读取 JSON 绘制指令并生成纹理，断言结果非空
 */
public class AIDrawAutoTest implements IGameAutoTest {

    @Override
    public void run() {
        AutoTestManager.ENABLED = true;
        AutoTestManager atm = AutoTestManager.getInstance();

        final boolean[] success = new boolean[1];

        atm.addAction("AIDraw: load plan and generate", () -> {
            try {
                FileHandle fh = Gdx.files.internal("ai_draw_cmds/auto_test_plan.json");
                if (!fh.exists()) {
                    System.err.println("AIDrawAutoTest: plan file not found: " + fh.path());
                    success[0] = false;
                    return;
                }
                String json = fh.readString();
                TextureRegion reg = AIDrawExecutor.generateFromJson(json);
                success[0] = (reg != null && reg.getTexture() != null);
            } catch (Throwable t) {
                success[0] = false;
                t.printStackTrace();
            }
        });

        atm.add(new AutoTestManager.AssertTask("AIDraw generation returned texture", () -> success[0]));
    }
}
