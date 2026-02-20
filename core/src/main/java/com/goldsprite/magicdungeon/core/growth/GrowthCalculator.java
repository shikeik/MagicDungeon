package com.goldsprite.magicdungeon.core.growth;

/**
 * 经验与等级计算器。
 * <p>
 * 采用累积制经验：仅记录总经验值，等级由总经验实时推导。
 * <p>
 * 升级所需经验公式：{@code XP(L) = floor(100 × 1.2^(L-1))}
 * <ul>
 *   <li>1→2级：100 XP</li>
 *   <li>10→11级：约 516 XP</li>
 *   <li>20→21级：约 3200 XP</li>
 * </ul>
 */
public final class GrowthCalculator {

    /** 经验公式基数 */
    public static final int XP_BASE = 100;
    /** 经验公式递增倍率 */
    public static final double XP_GROWTH_RATE = 1.2;

    private GrowthCalculator() {}

    /**
     * 计算从 level 升到 level+1 所需的经验值。
     * <p>
     * 公式：{@code floor(100 × 1.2^(level-1))}
     * <p>
     * level=0 时表示从0级升到1级，需要 {@code floor(100 × 1.2^(-1)) = 83}。
     *
     * @param level 当前等级（>=0）
     * @return 升到下一级所需经验
     */
    public static int xpForNextLevel(int level) {
        return (int) Math.floor(XP_BASE * Math.pow(XP_GROWTH_RATE, level - 1));
    }

    /**
     * 计算从0级升到目标等级所需的累计总经验值。
     *
     * @param targetLevel 目标等级（>=0）
     * @return 达到该等级所需的最低总经验
     */
    public static long totalXpForLevel(int targetLevel) {
        long total = 0;
        for (int i = 0; i < targetLevel; i++) {
            total += xpForNextLevel(i);
        }
        return total;
    }

    /**
     * 根据当前总经验值推导等级。
     *
     * @param totalXp 当前总经验值
     * @return 当前等级（>=0）
     */
    public static int levelFromXp(long totalXp) {
        int level = 0;
        long accumulated = 0;
        while (true) {
            int needed = xpForNextLevel(level);
            if (accumulated + needed > totalXp) break;
            accumulated += needed;
            level++;
        }
        return level;
    }

    /**
     * 计算当前等级内的经验进度（用於显示经验条）。
     *
     * @param totalXp 当前总经验值
     * @return float[2]: [0]=当前级已有经验, [1]=当前级升级需要总经验
     */
    public static float[] xpProgress(long totalXp) {
        int level = 0;
        long accumulated = 0;
        while (true) {
            int needed = xpForNextLevel(level);
            if (accumulated + needed > totalXp) {
                return new float[]{totalXp - accumulated, needed};
            }
            accumulated += needed;
            level++;
        }
    }
}
