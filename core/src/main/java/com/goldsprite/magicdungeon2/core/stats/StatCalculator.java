package com.goldsprite.magicdungeon2.core.stats;

/**
 * 属性计算器 — 纯静态工具类。
 * <p>
 * 提供等级状态快照计算、冷却时间计算等功能。
 */
public final class StatCalculator {

    private StatCalculator() {}

    /**
     * 计算 L 级角色某项属性的固定点数（每项）。
     * = level + 1（含0级基础的1点）
     */
    public static int fixedPointsPerStat(int level) {
        return level + 1;
    }

    /**
     * 计算 L 级角色的总固定属性点（四项之和）。
     * = (level + 1) × 4
     */
    public static int totalFixedPoints(int level) {
        return fixedPointsPerStat(level) * 4;
    }

    /**
     * 计算 L 级角色的总自由属性点。
     * = level × 4
     */
    public static int totalFreePoints(int level) {
        return level * 4;
    }

    /**
     * 计算 L 级角色的总属性点（固定 + 自由）。
     * = 8L + 4
     */
    public static int totalPoints(int level) {
        return totalFixedPoints(level) + totalFreePoints(level);
    }

    /**
     * 计算攻击冷却时间。
     *
     * @param baseCd 基础冷却时间（秒）
     * @param aspMultiplier ASP 最终倍率（如 1.0 = 100%）
     * @return 实际冷却时间
     */
    public static float attackCooldown(float baseCd, float aspMultiplier) {
        if (aspMultiplier <= 0) return baseCd;
        return baseCd / aspMultiplier;
    }

    /**
     * 计算移动冷却时间。
     *
     * @param baseCd 基础冷却时间（秒）
     * @param movMultiplier MOV 最终倍率（如 1.5 = 150%）
     * @return 实际冷却时间
     */
    public static float moveCooldown(float baseCd, float movMultiplier) {
        if (movMultiplier <= 0) return baseCd;
        return baseCd / movMultiplier;
    }
}
