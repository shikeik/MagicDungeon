package com.goldsprite.magicdungeon2.core.combat;

/**
 * 战斗引擎 — 纯数值计算，不依赖任何图形库。
 * <p>
 * 核心公式（魔塔式无保底）：
 * <ul>
 *   <li>物理伤害 = max(0, ATK - DEF)</li>
 *   <li>魔法伤害 = max(0, ATK_mag - MDEF)，调用方传入 MDEF</li>
 *   <li>穿透衰减 = D_base × 0.7^(n-1)</li>
 * </ul>
 */
public final class CombatEngine {

    /** 穿透衰减系数（每多穿透一个目标保留 70% 伤害） */
    public static final float PIERCE_DECAY = 0.7f;

    /** 最小有效伤害阈值，穿透衰减后低于此值视为 0 */
    public static final float MIN_DAMAGE_THRESHOLD = 0.1f;

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
     * @deprecated 与 calcDamage 完全等价，游戏中未使用。直接调用 {@link #calcDamage} 即可。
     */
    @Deprecated
    public static float calcPhysicalDamage(float atk, float def) {
        return calcDamage(atk, def);
    }

    /**
     * 计算魔法伤害。
     * <p>
     * 调用方需自行传入 MDEF（通常由 {@code StatData.getMDEF()} 获得），
     * 本方法不再内部计算 DEF/2，以避免双重除法陷阱。
     *
     * @param magAtk 攻击方的魔法攻击力
     * @param mdef 防御方的魔法防御力（MDEF）
     * @return 伤害值
     * @deprecated 尚未接入游戏循环。无魔法伤害系统。
     */
    @Deprecated
    public static float calcMagicDamage(float magAtk, float mdef) {
        return calcDamage(magAtk, mdef);
    }

    /**
     * 计算穿透衰减后的伤害。
     * <p>
     * 衰减后低于 {@link #MIN_DAMAGE_THRESHOLD} 的伤害归零，
     * 避免浮点精度带来的“灰尘伤害”。
     *
     * @param baseDamage 基础伤害（对第1个目标的伤害）
     * @param targetIndex 目标序号（从0开始，0=第1个目标）
     * @return 衰减后的伤害
     * @deprecated 尚未接入游戏循环。无穿透武器系统。
     */
    @Deprecated
    public static float calcPierceDamage(float baseDamage, int targetIndex) {
        if (targetIndex <= 0) return baseDamage;
        float dmg = (float) (baseDamage * Math.pow(PIERCE_DECAY, targetIndex));
        return dmg < MIN_DAMAGE_THRESHOLD ? 0f : dmg;
    }

    /**
     * 计算穿透路径上所有目标的伤害数组。
     *
     * @param baseDamage 基础伤害
     * @param targetCount 路径上的目标数量
     * @return 每个目标受到的伤害数组
     * @deprecated 尚未接入游戏循环。无穿透武器系统。
     */
    @Deprecated
    public static float[] calcPierceAllDamages(float baseDamage, int targetCount) {
        float[] damages = new float[targetCount];
        for (int i = 0; i < targetCount; i++) {
            damages[i] = calcPierceDamage(baseDamage, i);
        }
        return damages;
    }
}
