package com.goldsprite.magicdungeon2.core.stats;

import java.util.EnumMap;

/**
 * 角色属性数据容器。
 * <p>
 * 负责管理六大属性的各层加成，按流水线计算最终值：
 * <pre>
 * 最终值 = (基础值 + 固定加点 + 自由加点 + 装备固定加成) × (1 + 百分比增益总和)
 * </pre>
 * ASP/MOV 的最终值受 300% 上限约束。
 */
public class StatData {

    /** ASP/MOV 常规上限倍率（初始值的 300%） */
    public static final float SPEED_CAP = 3.0f;

    /** 自由加点分配的属性点数（每项） */
    private final EnumMap<StatType, Integer> freePoints = new EnumMap<>(StatType.class);

    /** 装备提供的固定加成值 */
    private final EnumMap<StatType, Float> equipFixed = new EnumMap<>(StatType.class);

    /** 百分比增益总和（0.1 表示 +10%） */
    private final EnumMap<StatType, Float> percentBonus = new EnumMap<>(StatType.class);

    /** 特殊装备突破上限的额外加成（仅 ASP/MOV 使用） */
    private final EnumMap<StatType, Float> uncappedBonus = new EnumMap<>(StatType.class);

    /** 角色等级 */
    private int level = 0;

    public StatData() {
        // 初始化所有属性的默认值
        for (StatType type : StatType.values()) {
            freePoints.put(type, 0);
            equipFixed.put(type, 0f);
            percentBonus.put(type, 0f);
            uncappedBonus.put(type, 0f);
        }
    }

    // ============ 等级 ============

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(0, level);
    }

    // ============ 固定点数（自动计算） ============

    /**
     * 获取某属性的总固定属性点数。
     * 委托给 {@link StatCalculator#fixedPointsPerStat(int)}。
     */
    public int getFixedPoints(StatType type) {
        if (!type.isAllocatable()) return 0;
        return StatCalculator.fixedPointsPerStat(level);
    }

    /**
     * 获取全部固定属性点总计（四项之和）。
     * 委托给 {@link StatCalculator#totalFixedPoints(int)}。
     */
    public int getTotalFixedPoints() {
        return StatCalculator.totalFixedPoints(level);
    }

    // ============ 自由加点 ============

    /**
     * 获取某属性已分配的自由属性点。
     */
    public int getFreePoints(StatType type) {
        return freePoints.getOrDefault(type, 0);
    }

    /**
     * 设置某属性的自由属性点。
     */
    public void setFreePoints(StatType type, int points) {
        if (!type.isAllocatable()) return;
        freePoints.put(type, Math.max(0, points));
    }

    /**
     * 向某属性增加指定数量的自由属性点。
     */
    public void addFreePoints(StatType type, int points) {
        if (!type.isAllocatable()) return;
        freePoints.put(type, Math.max(0, getFreePoints(type) + points));
    }

    /**
     * 已使用的自由属性点总数。
     */
    public int getUsedFreePoints() {
        int sum = 0;
        for (StatType type : StatType.ALLOCATABLE) {
            sum += freePoints.getOrDefault(type, 0);
        }
        return sum;
    }

    /**
     * 可用的自由属性点总额。
     * 委托给 {@link StatCalculator#totalFreePoints(int)}。
     */
    public int getMaxFreePoints() {
        return StatCalculator.totalFreePoints(level);
    }

    /**
     * 剩余未分配的自由属性点。
     */
    public int getRemainingFreePoints() {
        return getMaxFreePoints() - getUsedFreePoints();
    }

    // ============ 装备固定加成 ============

    public float getEquipFixed(StatType type) {
        return equipFixed.getOrDefault(type, 0f);
    }

    public void setEquipFixed(StatType type, float value) {
        equipFixed.put(type, value);
    }

    // ============ 百分比增益 ============

    public float getPercentBonus(StatType type) {
        return percentBonus.getOrDefault(type, 0f);
    }

    public void setPercentBonus(StatType type, float value) {
        percentBonus.put(type, value);
    }

    // ============ 突破上限加成（仅 ASP/MOV） ============

    public float getUncappedBonus(StatType type) {
        return uncappedBonus.getOrDefault(type, 0f);
    }

    public void setUncappedBonus(StatType type, float value) {
        uncappedBonus.put(type, value);
    }

    // ============ 最终值计算 ============

    /**
     * 计算某属性的最终值（完整流水线）。
     * <p>
     * 对于 HP/MP/ATK/DEF:
     * <pre>
     * 最终值 = (固定点 × 单点值 + 自由点 × 单点值 + 装备固定) × (1 + 百分比)
     * </pre>
     * 对于 ASP/MOV（统一乘法管线）:
     * <pre>
     * 加成后 = min((基础 + 装备固定) × (1 + 百分比), SPEED_CAP) + 突破加成
     * </pre>
     */
    public float getFinalValue(StatType type) {
        if (type.isAllocatable()) {
            return calcAllocatableFinal(type);
        } else {
            return calcSpeedFinal(type);
        }
    }

    private float calcAllocatableFinal(StatType type) {
        int fixedPts = getFixedPoints(type);
        int freePts = getFreePoints(type);
        float baseValue = (fixedPts + freePts) * type.valuePerPoint;
        float equip = getEquipFixed(type);
        float pct = getPercentBonus(type);
        return (baseValue + equip) * (1f + pct);
    }

    private float calcSpeedFinal(StatType type) {
        float base = 1.0f; // 100% 基础
        float equip = getEquipFixed(type);
        float pct = getPercentBonus(type);
        // 统一使用乘法管线：(base + equip) × (1 + pct)，再裁剪到上限
        float capped = Math.min((base + equip) * (1f + pct), SPEED_CAP);
        float uncapped = getUncappedBonus(type);
        return capped + uncapped;
    }

    // ============ 便捷方法 ============

    /** 获取最终 HP */
    public float getHP() { return getFinalValue(StatType.HP); }
    /** 获取最终 MP */
    public float getMP() { return getFinalValue(StatType.MP); }
    /** 获取最终 ATK */
    public float getATK() { return getFinalValue(StatType.ATK); }
    /** 获取最终 DEF */
    public float getDEF() { return getFinalValue(StatType.DEF); }
    /** 魔法防御 = DEF / 2 */
    public float getMDEF() { return getDEF() / 2f; }
    /** 获取最终 ASP 倍率 */
    public float getASP() { return getFinalValue(StatType.ASP); }
    /** 获取最终 MOV 倍率 */
    public float getMOV() { return getFinalValue(StatType.MOV); }

    /** 获取总属性点数。委托给 {@link StatCalculator#totalPoints(int)}。 */
    public int getTotalPoints() {
        return StatCalculator.totalPoints(level);
    }

    @Override
    public String toString() {
        return String.format(
            "Lv.%d | HP:%.0f MP:%.0f ATK:%.0f DEF:%.0f MDEF:%.1f | ASP:%.0f%% MOV:%.0f%% | 自由点:%d/%d",
            level, getHP(), getMP(), getATK(), getDEF(), getMDEF(),
            getASP() * 100, getMOV() * 100,
            getRemainingFreePoints(), getMaxFreePoints()
        );
    }
}
