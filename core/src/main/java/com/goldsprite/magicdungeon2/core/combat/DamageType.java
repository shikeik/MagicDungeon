package com.goldsprite.magicdungeon2.core.combat;

/**
 * 伤害类型枚举。
 */
public enum DamageType {
    /** 物理伤害 — 减去目标 DEF */
    PHYSICAL,
    /** 魔法伤害 — 减去目标 MDEF (= DEF/2) */
    MAGIC
}
