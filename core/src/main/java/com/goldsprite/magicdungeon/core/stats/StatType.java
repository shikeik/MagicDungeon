package com.goldsprite.magicdungeon2.core.stats;

/**
 * 六大核心属性类型枚举。
 * <p>
 * HP/MP/ATK/DEF 可通过升级固定加点和自由加点提升；
 * ASP/MOV 仅可通过装备/道具提升，上限 300%。
 */
public enum StatType {
    /** 生命值，1属性点 = 20 */
    HP(20f),
    /** 魔法值，1属性点 = 10 */
    MP(10f),
    /** 攻击力，1属性点 = 1 */
    ATK(1f),
    /** 防御力，1属性点 = 1 */
    DEF(1f),
    /** 攻速（百分比），不可加点 */
    ASP(0f),
    /** 移速（百分比），不可加点 */
    MOV(0f);

    /** 每1属性点对应的数值增量 */
    public final float valuePerPoint;

    StatType(float valuePerPoint) {
        this.valuePerPoint = valuePerPoint;
    }

    /** 是否可通过加点提升（仅 HP/MP/ATK/DEF） */
    public boolean isAllocatable() {
        return this == HP || this == MP || this == ATK || this == DEF;
    }

    /** 可加点的四项基础属性 */
    public static final StatType[] ALLOCATABLE = {HP, MP, ATK, DEF};
}
