package com.goldsprite.magicdungeon2.tests;

import org.junit.Test;

import com.goldsprite.CLogAssert;
import com.goldsprite.magicdungeon2.core.combat.CombatEngine;
import com.goldsprite.magicdungeon2.core.combat.WeaponRange;

/**
 * 战斗引擎单元测试。
 * <p>
 * 验证：物理/魔法伤害公式、无保底机制、穿透衰减。
 */
public class CombatEngineTest {

    // ========== 物理伤害 ==========

    @Test
    public void 测试_物理伤害基础() {
        // ATK=10, DEF=3 => 7
        CLogAssert.assertEquals("10-3=7", 7f, CombatEngine.calcPhysicalDamage(10, 3));
    }

    @Test
    public void 测试_物理伤害无保底() {
        // ATK=5, DEF=10 => 0（不是负数）
        CLogAssert.assertEquals("5-10=0(无保底)", 0f, CombatEngine.calcPhysicalDamage(5, 10));
    }

    @Test
    public void 测试_物理伤害刚好相等() {
        CLogAssert.assertEquals("10-10=0", 0f, CombatEngine.calcPhysicalDamage(10, 10));
    }

    @Test
    public void 测试_防御高1点即无伤() {
        // 魔塔核心：DEF > ATK 就是 0
        CLogAssert.assertEquals("10-11=0", 0f, CombatEngine.calcPhysicalDamage(10, 11));
    }

    // ========== 魔法伤害 ==========

    @Test
    public void 测试_魔法伤害基础() {
        // magATK=10, MDEF=3 => 10-3=7
        CLogAssert.assertEquals("魔法10 vs MDEF3 = 7", 7f,
            CombatEngine.calcMagicDamage(10, 3));
    }

    @Test
    public void 测试_魔法伤害比物理更容易破防() {
        // DEF=10 => MDEF=5（调用方计算）
        // 物理 ATK=8: 8-10=0（破不了防）
        // 魔法 ATK=8: 8-5=3（能破防）
        CLogAssert.assertEquals("物理 8 vs DEF10 = 0", 0f,
            CombatEngine.calcPhysicalDamage(8, 10));
        CLogAssert.assertEquals("魔法 8 vs MDEF5 = 3", 3f,
            CombatEngine.calcMagicDamage(8, 5));
    }

    @Test
    public void 测试_魔法MDEF精确计算() {
        // MDEF=3.5（调用方由 DEF=7 计算得出）
        // magATK=5 => 5-3.5=1.5
        CLogAssert.assertEquals("魔法5 vs MDEF3.5 = 1.5", 1.5f,
            CombatEngine.calcMagicDamage(5, 3.5f));
    }

    // ========== 穿透衰减 ==========

    @Test
    public void 测试_穿透第1个目标无衰减() {
        CLogAssert.assertEquals("第1个 100%", 100f, CombatEngine.calcPierceDamage(100f, 0));
    }

    @Test
    public void 测试_穿透第2个目标70百分比() {
        CLogAssert.assertEquals("第2个 70%", 70f, CombatEngine.calcPierceDamage(100f, 1), 0.01f);
    }

    @Test
    public void 测试_穿透第3个目标49百分比() {
        CLogAssert.assertEquals("第3个 49%", 49f, CombatEngine.calcPierceDamage(100f, 2), 0.01f);
    }

    @Test
    public void 测试_穿透第4个目标约34百分比() {
        float dmg = CombatEngine.calcPierceDamage(100f, 3);
        CLogAssert.assertTrue("第4个 ≈ 34%", dmg >= 33f && dmg <= 35f);
    }

    @Test
    public void 测试_穿透全路径伤害数组() {
        float[] damages = CombatEngine.calcPierceAllDamages(100f, 4);
        CLogAssert.assertEquals("路径长度 = 4", 4, damages.length);
        CLogAssert.assertEquals("第1个 = 100", 100f, damages[0]);
        CLogAssert.assertEquals("第2个 ≈ 70", 70f, damages[1], 0.01f);
        CLogAssert.assertEquals("第3个 ≈ 49", 49f, damages[2], 0.01f);
        CLogAssert.assertTrue("第4个 ≈ 34", damages[3] >= 33f && damages[3] <= 35f);
    }

    // ========== 武器范围枚举 ==========

    @Test
    public void 测试_近战属性() {
        CLogAssert.assertEquals("近战范围 = 1", 1, WeaponRange.MELEE.range);
        CLogAssert.assertFalse("近战 无穿透", WeaponRange.MELEE.piercing);
    }

    @Test
    public void 测试_长柄属性() {
        CLogAssert.assertEquals("长柄范围 = 2", 2, WeaponRange.POLEARM.range);
        CLogAssert.assertTrue("长柄 可穿透", WeaponRange.POLEARM.piercing);
    }

    @Test
    public void 测试_能量弹属性() {
        CLogAssert.assertEquals("能量弹范围 = 5", 5, WeaponRange.ENERGY.range);
        CLogAssert.assertFalse("能量弹 无穿透", WeaponRange.ENERGY.piercing);
    }

    @Test
    public void 测试_箭矢属性() {
        CLogAssert.assertEquals("箭矢范围 = 5", 5, WeaponRange.ARROW.range);
        CLogAssert.assertTrue("箭矢 可穿透", WeaponRange.ARROW.piercing);
    }

    // ========== 穿透精度与阈值 ==========

    @Test
    public void 测试_穿透深层衰减归零() {
        // 100 × 0.7^20 ≈ 0.0798 < 0.1 → 应归零
        float dmg = CombatEngine.calcPierceDamage(100f, 20);
        CLogAssert.assertEquals("第21个目标伤害应归零", 0f, dmg, 0.001f);
    }

    @Test
    public void 测试_穿透阈值边界保留() {
        // 100 × 0.7^19 ≈ 0.114 > 0.1 → 应保留
        float dmg = CombatEngine.calcPierceDamage(100f, 19);
        CLogAssert.assertTrue("第20个目标伤害应保留", dmg > 0.1f);
    }

    @Test
    public void 测试_穿透第1目标不受阈值影响() {
        // 第1个目标即使基础伤害 < 0.5 也应原样返回
        float dmg = CombatEngine.calcPierceDamage(0.3f, 0);
        CLogAssert.assertEquals("第1目标不受阈值影响", 0.3f, dmg, 0.001f);
    }
}
