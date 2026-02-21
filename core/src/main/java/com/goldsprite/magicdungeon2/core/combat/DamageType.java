package com.goldsprite.magicdungeon2.core.combat;

/**
 * 伤害类型枚举。
 *
 * @deprecated 尚未接入游戏循环。当前仅有物理bump攻击，无魔法伤害系统。待实装后移除此注解。
 */
@Deprecated
public enum DamageType {
    /** 物理伤害 — 减去目标 DEF */
    PHYSICAL,
    /** 魔法伤害 — 减去目标 MDEF (= DEF/2) */
    MAGIC
}
