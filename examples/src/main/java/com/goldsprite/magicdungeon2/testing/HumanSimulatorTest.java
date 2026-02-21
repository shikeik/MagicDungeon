package com.goldsprite.magicdungeon2.testing;

import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.screens.SimpleGameScreen;
import com.goldsprite.magicdungeon2.screens.SimpleGameScreen.Entity;

/**
 * 人类模拟可视化测试（半即时制版本）
 * 模拟玩家进入简易地牢，按住方向键移动并击杀怪物
 * <p>
 * 半即时制特性：
 * - 使用 simulatePress 按住方向键（持续移动，受冷却限制）
 * - 使用 simulateRelease 松开（停止移动）
 * - 敌人独立冷却，不等玩家行动
 * - 等待时间 = 移动冷却 × 需要的步数
 * <p>
 * 测试流程：
 * 1. 进入 SimpleGameScreen
 * 2. 验证初始状态
 * 3. 按住方向键移向 slime 并击杀
 * 4. 转向击杀 wolf
 * 5. 验证战斗状态
 * 6. 追杀剩余敌人
 * 7. 最终验证
 */
public class HumanSimulatorTest implements IGameAutoTest {

	// 玩家冷却约 0.2秒/步，留余量用 0.3秒/步估算
	private static final float STEP_TIME = 0.3f;

	@Override
	public void run() {
		AutoTestManager.ENABLED = true;
		AutoTestManager atm = AutoTestManager.getInstance();
		atm.log("=== 启动人类模拟测试：半即时制地牢战斗 ===");

		// ========== 第一阶段：等待加载转场完成并验证进入游戏场景 ==========
		// GdxLauncher 已通过 playLoadingTransition 进入 SimpleGameScreen
		// 这里等待转场动画结束 + 屏幕就绪后再开始测试操作

		atm.addWaitCondition("等待转场完成并进入SimpleGameScreen", () -> {
			return !ScreenManager.getInstance().isTransitioning()
				&& ScreenManager.getInstance().getCurScreen() instanceof SimpleGameScreen;
		}, 15f); // 15秒超时（含加载转场时间）

		atm.addWait(0.5f); // 额外等待半秒确保画面稳定

		// ========== 第二阶段：验证初始状态 ==========

		atm.add(new AutoTestManager.AssertTask("验证玩家存活", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().alive;
		}));

		atm.add(new AutoTestManager.AssertTask("验证玩家在(4,4)", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().x == 4 && gs.getPlayer().y == 4;
		}));

		atm.add(new AutoTestManager.AssertTask("验证4只敌人", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getEnemies().size == 4;
		}));

		atm.add(new AutoTestManager.AssertTask("验证玩家HP=100", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().hp == 100f;
		}));

		atm.addWait(0.5f);

		// ========== 第三阶段：按住左键移向 slime(2,2) 并击杀 ==========
		// 玩家在(4,4)，slime在(2,2)
		// 路径：按住左 2步 → (2,4)，按住下 2步+2步攻击 → 击杀slime

		atm.log("--- 目标：击杀 slime(2,2) ---");

		// 按住左移 2步 (4→3→2)，约 0.6秒
		holdDirection(atm, InputAction.MOVE_LEFT, STEP_TIME * 2.5f, "按住左移到x=2");

		// 按住下移 2步到(2,2)附近 + 攻击 slime
		// slime hp=20, player atk=12, def=1 → dmg=11 → 需要2击
		// 下移2步到(2,2)时会撞到slime触发攻击，再继续按住攻击第2次
		holdDirection(atm, InputAction.MOVE_DOWN, STEP_TIME * 5, "按住下移并攻击slime");

		atm.add(new AutoTestManager.AssertTask("slime已被击杀", () -> {
			SimpleGameScreen gs = getGameScreen();
			if (gs == null) return false;
			Entity slime = findEnemyByName(gs, "slime");
			return slime == null;
		}));

		atm.add(new AutoTestManager.AssertTask("敌人剩余≤3只", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getEnemies().size <= 3;
		}));

		atm.addWait(0.3f);

		// ========== 第四阶段：移向 wolf(6,2) 并击杀 ==========
		// 当前大约在(2,2-3)附近，wolf在(6,2)
		// wolf: hp=30, def=2 → dmg=10 → 需要3击

		atm.log("--- 目标：击杀 wolf(6,2) ---");

		// 按住右移接近wolf (约4步)
		holdDirection(atm, InputAction.MOVE_RIGHT, STEP_TIME * 5, "按住右移接近wolf");

		// 按住下移调整Y + 攻击wolf (3击)
		holdDirection(atm, InputAction.MOVE_DOWN, STEP_TIME * 4, "下移+攻击wolf");

		// 继续右移补刀
		holdDirection(atm, InputAction.MOVE_RIGHT, STEP_TIME * 4, "右移补刀wolf");

		atm.addWait(0.3f);

		// ========== 第五阶段：验证战斗状态 ==========

		atm.add(new AutoTestManager.AssertTask("玩家仍然存活", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().alive && gs.getPlayer().hp > 0;
		}));

		atm.add(new AutoTestManager.AssertTask("玩家受到过伤害(HP<100)", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().hp < 100f;
		}));

		atm.add(new AutoTestManager.AssertTask("游戏时间 > 0", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getGameTime() > 0;
		}));

		// ========== 第六阶段：追杀剩余敌人 ==========
		// skeleton(6,6) 和 bat(2,6) 在上方

		atm.log("--- 清扫剩余敌人 ---");

		// 大幅上移追向上方敌人
		holdDirection(atm, InputAction.MOVE_UP, STEP_TIME * 6, "上移追敌");
		holdDirection(atm, InputAction.MOVE_LEFT, STEP_TIME * 5, "左移追bat");
		holdDirection(atm, InputAction.MOVE_UP, STEP_TIME * 4, "上移补刀bat");
		holdDirection(atm, InputAction.MOVE_RIGHT, STEP_TIME * 6, "右移追skeleton");
		holdDirection(atm, InputAction.MOVE_UP, STEP_TIME * 4, "上移补刀skeleton");
		holdDirection(atm, InputAction.MOVE_DOWN, STEP_TIME * 3, "下移扫荡");
		holdDirection(atm, InputAction.MOVE_LEFT, STEP_TIME * 4, "左移扫荡");
		holdDirection(atm, InputAction.MOVE_UP, STEP_TIME * 3, "上移扫荡");
		holdDirection(atm, InputAction.MOVE_RIGHT, STEP_TIME * 5, "右移扫荡");
		holdDirection(atm, InputAction.MOVE_DOWN, STEP_TIME * 4, "下移扫荡");

		// ========== 第七阶段：最终验证 ==========

		atm.addWait(0.5f);

		atm.add(new AutoTestManager.AssertTask("最终验证：玩家存活", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().alive;
		}));

		atm.add(new AutoTestManager.AssertTask("最终验证：有击杀记录", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getKillCount() > 0;
		}));

		// 记录最终状态
		atm.addAction("输出最终状态", () -> {
			SimpleGameScreen gs = getGameScreen();
			if (gs == null) {
				atm.logFail("无法获取游戏屏幕");
				return;
			}
			Entity p = gs.getPlayer();
			atm.log("=== 战斗结束 ===");
			atm.log(String.format("玩家 HP: %.0f/%.0f", p.hp, p.getMaxHp()));
			atm.log(String.format("存活: %s", p.alive ? "是" : "否"));
			atm.log(String.format("击杀数: %d", gs.getKillCount()));
			atm.log(String.format("剩余敌人: %d", gs.getEnemies().size));
			atm.log(String.format("游戏时间: %.1f秒", gs.getGameTime()));
			atm.log(String.format("日志: %s", gs.getLogText()));
		});

		// ========== 结束 ==========

		atm.addAction("测试完成", () -> {
			atm.logPass("=== 半即时制人类模拟测试流程执行完毕 ===");
			AutoTestManager.ENABLED = false;
		});
	}

	// ============ 辅助方法 ============

	/**
	 * 模拟按住方向键一段时间（半即时制核心操作）
	 * 按住期间玩家会按冷却节奏自动重复移动/攻击
	 */
	private void holdDirection(AutoTestManager atm, InputAction action, float duration, String desc) {
		atm.addAction("按住:" + desc, () -> {
			InputManager.getInstance().simulatePress(action);
		});
		atm.addWait(duration);
		atm.addAction("松开:" + desc, () -> {
			InputManager.getInstance().simulateRelease(action);
		});
		atm.addWait(0.1f); // 短暂间隔防止连续操作粘连
	}

	/** 安全获取当前 SimpleGameScreen 实例 */
	private SimpleGameScreen getGameScreen() {
		var screen = ScreenManager.getInstance().getCurScreen();
		if (screen instanceof SimpleGameScreen) {
			return (SimpleGameScreen) screen;
		}
		return null;
	}

	/** 在敌人列表中按名字查找 */
	private Entity findEnemyByName(SimpleGameScreen gs, String name) {
		var enemies = gs.getEnemies();
		for (int i = 0; i < enemies.size; i++) {
			Entity e = enemies.get(i);
			if (e.alive && e.texName.equals(name)) return e;
		}
		return null;
	}
}
