package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty;
import com.goldsprite.magicdungeon2.core.growth.GrowthCalculator;
import com.goldsprite.magicdungeon2.core.stats.StatType;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.GOLD_XP_RATIO;
import static com.goldsprite.magicdungeon2.screens.main.GameConfig.TILE;

/**
 * 成长系统辅助工具
 * 处理击杀奖励（经验/金币/升级）、死亡惩罚、属性自动分配等逻辑
 */
public class GrowthHelper {

	/** 成长事件回调接口 */
	public interface GrowthListener {
		/** 日志文本更新 */
		void onLogUpdate(String text);
	}

	private final GameEntity player;
	private final Array<DamagePopup> popups;
	private GrowthListener listener;

	public GrowthHelper(GameEntity player, Array<DamagePopup> popups, GrowthListener listener) {
		this.player = player;
		this.popups = popups;
		this.listener = listener;
	}

	public void setListener(GrowthListener listener) {
		this.listener = listener;
	}

	// ============ 击杀奖励 ============

	/**
	 * 击杀敌人时的奖励处理（经验、金币、升级）
	 * @param enemy  被击杀的敌人
	 * @param killCount  当前总击杀数（用于日志显示）
	 */
	public void onEnemyKilled(GameEntity enemy, int killCount) {
		player.totalXp += enemy.xpReward;
		player.gold += (int) (enemy.xpReward * GOLD_XP_RATIO);

		int oldLevel = player.stats.getLevel();
		int newLevel = GrowthCalculator.levelFromXp(player.totalXp);

		if (newLevel > oldLevel) {
			player.stats.setLevel(newLevel);
			autoAllocateFreePoints(player);
			player.hp = player.getMaxHp(); // 升级回满
			player.mp = player.getMaxMp();

			popups.add(new DamagePopup(
				player.visualX + TILE * 0.5f,
				player.visualY + TILE * 1.5f,
				"升级! Lv." + newLevel, Color.GOLD));
			notifyLog(String.format("升级至 Lv.%d！属性全面提升！", newLevel));
		} else {
			notifyLog(String.format("击败 %s！(+%dXP, 击杀:%d)",
				enemy.texName, enemy.xpReward, killCount));
		}
	}

	// ============ 死亡惩罚 ============

	/**
	 * 处理玩家死亡（计算惩罚并应用）
	 * @return 死亡惩罚结果，用于界面显示
	 */
	public DeathPenalty.DeathResult handlePlayerDeath() {
		player.alive = false;
		DeathPenalty.DeathResult result = DeathPenalty.calcPenalty(player.totalXp, player.gold);
		DeathPenalty.applyLevelLoss(player.stats, result.levelBefore, result.levelAfter);
		player.totalXp = result.xpAfter;
		player.gold = Math.max(0, player.gold - result.goldDropped);
		notifyLog("你被击败了...按R重生");
		return result;
	}

	// ============ 属性分配 ============

	/** 自动均匀分配自由属性点到 HP/ATK/DEF */
	public static void autoAllocateFreePoints(GameEntity e) {
		StatType[] targets = {StatType.HP, StatType.ATK, StatType.DEF};
		while (e.stats.getRemainingFreePoints() > 0) {
			boolean allocated = false;
			for (StatType type : targets) {
				if (e.stats.getRemainingFreePoints() <= 0) break;
				if (e.stats.addFreePoints(type, 1)) allocated = true;
			}
			if (!allocated) break; // 安全退出
		}
	}

	// ============ 重生 ============

	/**
	 * 重置玩家状态用于重生
	 * 注意：地图重建和敌人重生由上层 Screen 负责
	 */
	public void resetPlayerForRespawn() {
		player.x = 4;
		player.y = 4;
		player.visualX = player.x * TILE;
		player.visualY = player.y * TILE;
		player.hp = player.getMaxHp();
		player.mp = player.getMaxMp();
		player.alive = true;
		player.moveTimer = 0;
		notifyLog("重生！继续冒险...");
	}

	/** 通知日志更新 */
	private void notifyLog(String text) {
		if (listener != null) listener.onLogUpdate(text);
	}
}
