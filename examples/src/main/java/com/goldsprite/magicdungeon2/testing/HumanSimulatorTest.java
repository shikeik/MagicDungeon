package com.goldsprite.magicdungeon2.testing;

import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.testing.AutoTestManager;
import com.goldsprite.magicdungeon2.input.InputAction;
import com.goldsprite.magicdungeon2.input.InputManager;
import com.goldsprite.magicdungeon2.screens.SimpleGameScreen;
import com.goldsprite.magicdungeon2.screens.SimpleGameScreen.Entity;

/**
 * 人类模拟可视化测试
 * 模拟玩家进入简易地牢，移动并击杀所有怪物
 * <p>
 * 测试流程：
 * 1. 进入 SimpleGameScreen
 * 2. 验证初始状态（玩家在4,4 / 4只怪物）
 * 3. 向左下移动接近 slime(2,2)
 * 4. 攻击并击杀 slime
 * 5. 移动去击杀 wolf(6,2)
 * 6. 验证玩家存活 + 敌人数量减少
 * 7. 清扫剩余敌人
 * 8. 验证全部击杀
 */
public class HumanSimulatorTest implements IGameAutoTest {

	@Override
	public void run() {
		AutoTestManager.ENABLED = true;
		AutoTestManager atm = AutoTestManager.getInstance();
		atm.log("=== 启动人类模拟测试：简易地牢战斗 ===");

		// ========== 第一阶段：进入游戏场景 ==========

		atm.addAction("进入SimpleGameScreen", () -> {
			ScreenManager.getInstance().goScreen(SimpleGameScreen.class);
		});
		atm.addWait(1.0f);

		// 验证已进入 SimpleGameScreen
		atm.add(new AutoTestManager.AssertTask("验证进入SimpleGameScreen", () -> {
			return ScreenManager.getInstance().getCurScreen() instanceof SimpleGameScreen;
		}));

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

		// ========== 第三阶段：移动到 slime(2,2) 并击杀 ==========
		// 玩家在(4,4)，slime在(2,2)
		// 路径：左→左→下→下 到达(2,2)附近并攻击

		atm.log("--- 目标：击杀 slime(2,2) ---");

		// 向左移动 (4,4) → (3,4)
		addMove(atm, InputAction.MOVE_LEFT, "左移到(3,4)");
		atm.addWait(0.3f);

		// 向下移动 (3,4) → (3,3) 是墙！改为 (3,4) → (2,4)
		// 注意(3,3)是墙壁，所以需要先下再左
		// 实际路线：(4,4) → (3,4) → (2,4) → (2,3) → 攻击(2,2)

		addMove(atm, InputAction.MOVE_LEFT, "左移到(2,4)");
		atm.addWait(0.3f);

		addMove(atm, InputAction.MOVE_DOWN, "下移到(2,3)");
		atm.addWait(0.3f);

		// 现在玩家在(2,3)，slime在(2,2)，向下攻击
		addMove(atm, InputAction.MOVE_DOWN, "攻击slime(2,2)");
		atm.addWait(0.3f);

		// slime: hp=20, def=1; player: atk=12 => dmg = max(12-1, 1) = 11
		// 第一击: 20-11=9
		atm.add(new AutoTestManager.AssertTask("slime受伤(HP<20)", () -> {
			SimpleGameScreen gs = getGameScreen();
			if (gs == null) return false;
			Entity slime = findEnemyByName(gs, "slime");
			return slime != null && slime.hp < 20f;
		}));

		// 继续攻击 slime (第二击: 9-11 <= 0，击杀)
		addMove(atm, InputAction.MOVE_DOWN, "再次攻击slime");
		atm.addWait(0.3f);

		atm.add(new AutoTestManager.AssertTask("slime已被击杀", () -> {
			SimpleGameScreen gs = getGameScreen();
			if (gs == null) return false;
			Entity slime = findEnemyByName(gs, "slime");
			return slime == null; // 被移除了
		}));

		atm.add(new AutoTestManager.AssertTask("敌人剩余3只", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getEnemies().size == 3;
		}));

		atm.addWait(0.5f);

		// ========== 第四阶段：移动去击杀 wolf(6,2) ==========
		// slime被击杀后玩家可能还在(2,2)或(2,3)附近
		// wolf 在 (6,2)，需要向右移动

		atm.log("--- 目标：击杀 wolf(6,2) ---");

		// 向右连续移动靠近 wolf
		addMove(atm, InputAction.MOVE_RIGHT, "右移1");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "右移2");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "右移3");
		atm.addWait(0.3f);

		// 可能需要下移调整Y坐标到wolf所在行
		addMove(atm, InputAction.MOVE_DOWN, "下移调整");
		atm.addWait(0.3f);

		// 继续右移接近 wolf
		addMove(atm, InputAction.MOVE_RIGHT, "右移4");
		atm.addWait(0.3f);

		// wolf: hp=30, def=2; player: atk=12 => dmg = max(12-2, 1) = 10
		// 需要3击: 30→20→10→0

		// 如果还没到wolf的位置，继续移动
		addMove(atm, InputAction.MOVE_RIGHT, "尝试攻击wolf/继续右移");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "尝试攻击wolf");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_DOWN, "下移寻找wolf");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "攻击wolf");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "攻击wolf(2)");
		atm.addWait(0.3f);
		addMove(atm, InputAction.MOVE_RIGHT, "攻击wolf(3)");
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

		atm.add(new AutoTestManager.AssertTask("回合数 > 0", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getTurnCount() > 0;
		}));

		atm.addWait(0.5f);

		// ========== 第六阶段：追杀剩余敌人 ==========
		// 用暴力搜索方式：反复向各方向移动，直到所有敌人被击杀

		atm.log("--- 清扫剩余敌人 ---");

		// skeleton(6,6) 和 bat(2,6) 在上方
		// 先向上移动去打它们
		for (int round = 0; round < 6; round++) {
			addMove(atm, InputAction.MOVE_UP, "上移追敌(" + round + ")");
			atm.addWait(0.2f);
		}

		// 左移寻找 bat
		for (int round = 0; round < 5; round++) {
			addMove(atm, InputAction.MOVE_LEFT, "左移追敌(" + round + ")");
			atm.addWait(0.2f);
		}

		// 上下来回清扫
		for (int round = 0; round < 4; round++) {
			addMove(atm, InputAction.MOVE_UP, "上移清扫(" + round + ")");
			atm.addWait(0.2f);
		}

		// 右移寻找 skeleton
		for (int round = 0; round < 6; round++) {
			addMove(atm, InputAction.MOVE_RIGHT, "右移追敌(" + round + ")");
			atm.addWait(0.2f);
		}

		// 上移补刀
		for (int round = 0; round < 4; round++) {
			addMove(atm, InputAction.MOVE_UP, "上移补刀(" + round + ")");
			atm.addWait(0.2f);
		}

		// 再做一轮大范围扫荡
		for (int round = 0; round < 3; round++) {
			addMove(atm, InputAction.MOVE_LEFT, "扫荡左(" + round + ")");
			atm.addWait(0.15f);
			addMove(atm, InputAction.MOVE_UP, "扫荡上(" + round + ")");
			atm.addWait(0.15f);
			addMove(atm, InputAction.MOVE_RIGHT, "扫荡右(" + round + ")");
			atm.addWait(0.15f);
			addMove(atm, InputAction.MOVE_DOWN, "扫荡下(" + round + ")");
			atm.addWait(0.15f);
		}

		// ========== 第七阶段：最终验证 ==========

		atm.addWait(0.5f);

		atm.add(new AutoTestManager.AssertTask("最终验证：玩家存活", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getPlayer().alive;
		}));

		atm.add(new AutoTestManager.AssertTask("最终验证：进行了多个回合", () -> {
			SimpleGameScreen gs = getGameScreen();
			return gs != null && gs.getTurnCount() >= 5;
		}));

		// 记录最终状态
		atm.addAction("输出最终状态", () -> {
			SimpleGameScreen gs = getGameScreen();
			if (gs == null) {
				atm.logFail("无法获取游戏屏幕");
				return;
			}
			Entity p = gs.getPlayer();
			atm.log(String.format("=== 战斗结束 ==="));
			atm.log(String.format("玩家 HP: %.0f/%.0f", p.hp, p.maxHp));
			atm.log(String.format("存活: %s", p.alive ? "是" : "否"));
			atm.log(String.format("回合数: %d", gs.getTurnCount()));
			atm.log(String.format("剩余敌人: %d", gs.getEnemies().size));
			atm.log(String.format("日志: %s", gs.getLogText()));
		});

		// ========== 结束 ==========

		atm.addAction("测试完成", () -> {
			atm.logPass("=== 人类模拟测试流程执行完毕 ===");
			AutoTestManager.ENABLED = false;
		});
	}

	// ============ 辅助方法 ============

	/** 模拟一次方向输入（按下+释放） */
	private void addMove(AutoTestManager atm, InputAction action, String desc) {
		atm.addAction(desc, () -> {
			InputManager.getInstance().simulatePress(action);
		});
		atm.addAction("释放" + desc, () -> {
			InputManager.getInstance().simulateRelease(action);
		});
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
