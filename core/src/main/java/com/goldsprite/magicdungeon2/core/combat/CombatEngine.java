package com.goldsprite.magicdungeon2.core.combat;

/**
 * 战斗引擎 — 纯数值计算，不依赖任何图形库。
 * <p>
 * 核心公式（魔塔式无保底）：
 * <ul>
 *   <li>物理伤害 = max(0, ATK - DEF)</li>
 *   <li>魔法伤害 = max(0, ATK_mag - MDEF)，其中 MDEF = DEF / 2</li>
 *   <li>穿透衰减 = D_base × 0.7^(n-1)</li>
 * </ul>
 */
public final class CombatEngine {

    /** 穿透衰减系数（每多穿透一个目标保留 70% 伤害） */
    public static final float PIERCE_DECAY = 0.7f;

    private CombatEngine() {}

    /**
     * 计算单次伤害（第1个目标）。
     *
     * @param attackerAtk 攻击方的攻击力（物理或魔法ATK）
     * @param defenderDef 防御方的防御力（DEF 或 MDEF）
     * @return 最终伤害值（>=0）
     */
    public static float calcDamage(float attackerAtk, float defenderDef) {
        return Math.max(0f, attackerAtk - defenderDef);
    }

    /**
     * 计算物理伤害。
     *
     * @param atk 攻击方 ATK
     * @param def 防御方 DEF
     * @return 伤害值
     */
    public static float calcPhysicalDamage(float atk, float def) {
        return calcDamage(atk, def);
    }

    /**
     * 计算魔法伤害。
     *
     * @param magAtk 攻击方的魔法攻击力
     * @param def 防御方 DEF（MDEF 将自动计算为 DEF/2）
     * @return 伤害值
     */
    public static float calcMagicDamage(float magAtk, float def) {
        float mdef = def / 2f;
        return calcDamage(magAtk, mdef);
    }

    /**
     * 计算穿透衰减后的伤害。
     *
     * @param baseDamage 基础伤害（对第1个目标的伤害）
     * @param targetIndex 目标序号（从0开始，0=第1个目标）
     * @return 衰减后的伤害
     */
    public static float calcPierceDamage(float baseDamage, int targetIndex) {
        if (targetIndex <= 0) return baseDamage;
        return (float) (baseDamage * Math.pow(PIERCE_DECAY, targetIndex));
    }

    /**
     * 计算穿透路径上所有目标的伤害数组。
     *
     * @param baseDamage 基础伤害
     * @param targetCount 路径上的目标数量
     * @return 每个目标受到的伤害数组
     */
    public static float[] calcPierceAllDamages(float baseDamage, int targetCount) {
        float[] damages = new float[targetCount];
        for (int i = 0; i < targetCount; i++) {
            damages[i] = calcPierceDamage(baseDamage, i);
        }
        return damages;
    }
}
