package com.goldsprite.magicdungeon2.core.growth;

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
 *
 * @see com.goldsprite.magicdungeon2.core.stats.StatCalculator
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
     * <p>
     * 结果超过 {@code Long.MAX_VALUE} 时返回 {@code Long.MAX_VALUE}（溢出保护）。
     *
     * @param level 当前等级（>=0）
     * @return 升到下一级所需经验
     */
    public static long xpForNextLevel(int level) {
        double raw = Math.floor(XP_BASE * Math.pow(XP_GROWTH_RATE, level - 1));
        if (raw >= (double) Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) raw;
    }

    /**
     * 计算从0级升到目标等级所需的累计总经验值。
     * <p>
     * 使用闭合公式避免 O(L) 循环：
     * <pre>
     * S = Σ floor(100 × 1.2^(i-1)),  i=0..L-1
     * </pre>
     * 由于每项取了 floor，闭合几何级数仅作近似，需逐项求和以保持精度。
     * 实际采用缓存友好的累加实现，但保留接口语义不变。
     * <p>
     * 注意：因为 {@code xpForNextLevel} 内含 {@code floor}，纯几何级数
     * {@code 100×(1.2^L - 1)/0.2} 会产生舍入偏差，所以仍保留循环，
     * 但改为 O(1) 查表优化预留接口。
     *
     * @param targetLevel 目标等级（>=0）
     * @return 达到该等级所需的最低总经验
     */
    public static long totalXpForLevel(int targetLevel) {
        // 因 floor 截断会导致几何级数闭合公式有累积误差，
        // 保留循环累加以确保精确性，但使用局部变量优化性能
        if (targetLevel <= 0) return 0;
        long total = 0;
        double base = XP_BASE;
        double rate = XP_GROWTH_RATE;
        double power = Math.pow(rate, -1); // 初始指数 level=0 → 1.2^(-1)
        for (int i = 0; i < targetLevel; i++) {
            double raw = Math.floor(base * power);
            if (raw >= (double) Long.MAX_VALUE || total > Long.MAX_VALUE - (long) raw) {
                return Long.MAX_VALUE; // 溢出保护
            }
            total += (long) raw;
            power *= rate;
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
            long needed = xpForNextLevel(level);
            if (needed == Long.MAX_VALUE) break; // 溢出保护，不再升级
            if (needed > totalXp - accumulated) break; // 经验不足以升级
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
            long needed = xpForNextLevel(level);
            if (needed == Long.MAX_VALUE || accumulated > totalXp - needed) {
                return new float[]{totalXp - accumulated, needed};
            }
            accumulated += needed;
            level++;
        }
    }
}
