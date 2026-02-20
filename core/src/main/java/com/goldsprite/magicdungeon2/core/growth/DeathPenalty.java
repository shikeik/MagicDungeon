package com.goldsprite.magicdungeon2.core.growth;

import com.goldsprite.magicdungeon2.core.stats.StatData;
import com.goldsprite.magicdungeon2.core.stats.StatType;

/**
 * 死亡惩罚处理器。
 * <p>
 * 死亡惩罚规则：
 * <ul>
 *   <li>位置回溯：回到营地（由上层调用方处理）</li>
 *   <li>经验掉落：扣除总经验的 20%，等级可能回落</li>
 *   <li>固定点：因降级失去的固定点全部扣除（自动计算）</li>
 *   <li>自由点：因降级失去的自由点平均扣除（各属性均匀扣减）</li>
 *   <li>金币掉落：掉落 50%，其中 60% 永久损失，仅可拾回 40%</li>
 *   <li>装备掉落：随机 2~5 件背包装备直接消失（由上层调用方处理）</li>
 * </ul>
 */
public final class DeathPenalty {

    /** 经验掉落比例 */
    public static final float XP_LOSS_RATE = 0.20f;
    /** 金币掉落比例 */
    public static final float GOLD_DROP_RATE = 0.50f;
    /** 掉落金币的永久损失比例 */
    public static final float GOLD_PERMANENT_LOSS = 0.60f;
    /** 装备掉落最小数量 */
    public static final int EQUIP_DROP_MIN = 2;
    /** 装备掉落最大数量 */
    public static final int EQUIP_DROP_MAX = 5;

    private DeathPenalty() {}

    /**
     * 死亡时的经验惩罚结果。
     */
    public static class DeathResult {
        /** 惩罚前总经验 */
        public final long xpBefore;
        /** 惩罚后总经验 */
        public final long xpAfter;
        /** 惩罚前等级 */
        public final int levelBefore;
        /** 惩罚后等级 */
        public final int levelAfter;
        /** 损失的经验值 */
        public final long xpLost;
        /** 掉落的金币总量（含永久损失部分） */
        public final int goldDropped;
        /** 可拾回的金币 */
        public final int goldRecoverable;
        /** 永久损失的金币 */
        public final int goldPermanentLost;

        public DeathResult(long xpBefore, long xpAfter, int levelBefore, int levelAfter,
                           long xpLost, int goldDropped, int goldRecoverable, int goldPermanentLost) {
            this.xpBefore = xpBefore;
            this.xpAfter = xpAfter;
            this.levelBefore = levelBefore;
            this.levelAfter = levelAfter;
            this.xpLost = xpLost;
            this.goldDropped = goldDropped;
            this.goldRecoverable = goldRecoverable;
            this.goldPermanentLost = goldPermanentLost;
        }
    }

    /**
     * 计算死亡惩罚结果（不修改任何数据，纯计算）。
     *
     * @param totalXp 当前总经验
     * @param currentGold 当前金币
     * @return 死亡结果
     */
    public static DeathResult calcPenalty(long totalXp, int currentGold) {
        // 经验惩罚
        long xpLost = (long) (totalXp * XP_LOSS_RATE);
        long xpAfter = totalXp - xpLost;
        int levelBefore = GrowthCalculator.levelFromXp(totalXp);
        int levelAfter = GrowthCalculator.levelFromXp(xpAfter);

        // 金币惩罚
        int goldDropped = (int) (currentGold * GOLD_DROP_RATE);
        int goldPermanentLost = (int) (goldDropped * GOLD_PERMANENT_LOSS);
        int goldRecoverable = goldDropped - goldPermanentLost;

        return new DeathResult(
            totalXp, xpAfter, levelBefore, levelAfter,
            xpLost, goldDropped, goldRecoverable, goldPermanentLost
        );
    }

    /**
     * 将死亡惩罚应用到 StatData 上（处理降级后的自由点平均扣除）。
     *
     * @param statData 角色属性数据
     * @param levelBefore 惩罚前等级
     * @param levelAfter 惩罚后等级
     */
    public static void applyLevelLoss(StatData statData, int levelBefore, int levelAfter) {
        if (levelAfter >= levelBefore) return;

        // 计算降级导致的自由点损失
        int freePointsBefore = levelBefore * 4;
        int freePointsAfter = levelAfter * 4;
        int lostFreePoints = freePointsBefore - freePointsAfter;

        if (lostFreePoints <= 0) return;

        // 平均扣除：从已分配的自由点中均匀扣减
        StatType[] allocatable = StatType.ALLOCATABLE;
        int perStat = lostFreePoints / allocatable.length;
        int remainder = lostFreePoints % allocatable.length;

        for (int i = 0; i < allocatable.length; i++) {
            StatType type = allocatable[i];
            int toRemove = perStat + (i < remainder ? 1 : 0);
            int current = statData.getFreePoints(type);
            statData.setFreePoints(type, Math.max(0, current - toRemove));
        }

        // 更新等级
        statData.setLevel(levelAfter);
    }
}
