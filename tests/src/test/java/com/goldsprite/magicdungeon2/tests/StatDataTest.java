package com.goldsprite.magicdungeon2.tests;

import org.junit.Test;

import com.goldsprite.magicdungeon2.CLogAssert;
import com.goldsprite.magicdungeon2.core.stats.StatCalculator;
import com.goldsprite.magicdungeon2.core.stats.StatData;
import com.goldsprite.magicdungeon2.core.stats.StatType;

/**
 * 属性系统单元测试。
 * <p>
 * 验证：属性点数公式、属性计算流水线、MDEF 隐性属性、ASP/MOV 上限。
 */
public class StatDataTest {

    // ========== 属性点数公式 ==========

    @Test
    public void 测试_0级总点数为4() {
        CLogAssert.assertEquals("0级总属性点 = 4", 4, StatCalculator.totalPoints(0));
    }

    @Test
    public void 测试_1级总点数为12() {
        // 1级 = 8固定 + 4自由 = 12
        CLogAssert.assertEquals("1级总属性点 = 12", 12, StatCalculator.totalPoints(1));
    }

    @Test
    public void 测试_5级总点数为44() {
        // 5级 = 8×5 + 4 = 44
        CLogAssert.assertEquals("5级总属性点 = 44", 44, StatCalculator.totalPoints(5));
    }

    @Test
    public void 测试_固定点数每项() {
        CLogAssert.assertEquals("0级每项固定点 = 1", 1, StatCalculator.fixedPointsPerStat(0));
        CLogAssert.assertEquals("1级每项固定点 = 2", 2, StatCalculator.fixedPointsPerStat(1));
        CLogAssert.assertEquals("10级每项固定点 = 11", 11, StatCalculator.fixedPointsPerStat(10));
    }

    @Test
    public void 测试_自由点总额() {
        CLogAssert.assertEquals("0级自由点 = 0", 0, StatCalculator.totalFreePoints(0));
        CLogAssert.assertEquals("1级自由点 = 4", 4, StatCalculator.totalFreePoints(1));
        CLogAssert.assertEquals("5级自由点 = 20", 20, StatCalculator.totalFreePoints(5));
    }

    // ========== StatData 基础属性计算 ==========

    @Test
    public void 测试_0级初始属性值() {
        StatData data = new StatData();
        data.setLevel(0);
        // 0级：固定1点/每项 => HP=20, MP=10, ATK=1, DEF=1
        CLogAssert.assertEquals("0级 HP = 20", 20f, data.getHP());
        CLogAssert.assertEquals("0级 MP = 10", 10f, data.getMP());
        CLogAssert.assertEquals("0级 ATK = 1", 1f, data.getATK());
        CLogAssert.assertEquals("0级 DEF = 1", 1f, data.getDEF());
    }

    @Test
    public void 测试_1级无自由加点属性值() {
        StatData data = new StatData();
        data.setLevel(1);
        // 1级：固定2点/每项 => HP=40, MP=20, ATK=2, DEF=2
        CLogAssert.assertEquals("1级 HP = 40", 40f, data.getHP());
        CLogAssert.assertEquals("1级 MP = 20", 20f, data.getMP());
        CLogAssert.assertEquals("1级 ATK = 2", 2f, data.getATK());
        CLogAssert.assertEquals("1级 DEF = 2", 2f, data.getDEF());
    }

    @Test
    public void 测试_自由加点效果() {
        StatData data = new StatData();
        data.setLevel(1); // 4点自由点可用
        data.addFreePoints(StatType.ATK, 2);
        data.addFreePoints(StatType.DEF, 2);
        // ATK = (2固定 + 2自由) × 1 = 4
        // DEF = (2固定 + 2自由) × 1 = 4
        CLogAssert.assertEquals("1级+2自由ATK = 4", 4f, data.getATK());
        CLogAssert.assertEquals("1级+2自由DEF = 4", 4f, data.getDEF());
        CLogAssert.assertEquals("剩余自由点 = 0", 0, data.getRemainingFreePoints());
    }

    @Test
    public void 测试_自由点余额计算() {
        StatData data = new StatData();
        data.setLevel(3); // 12点自由点可用
        data.addFreePoints(StatType.HP, 3);
        data.addFreePoints(StatType.ATK, 2);
        CLogAssert.assertEquals("已使用自由点 = 5", 5, data.getUsedFreePoints());
        CLogAssert.assertEquals("剩余自由点 = 7", 7, data.getRemainingFreePoints());
    }

    // ========== MDEF 隐性属性 ==========

    @Test
    public void 测试_MDEF等于DEF除2() {
        StatData data = new StatData();
        data.setLevel(5);
        // DEF = (5+1) × 1 = 6 => MDEF = 3.0
        CLogAssert.assertEquals("5级 MDEF = 3.0", 3.0f, data.getMDEF());

        data.addFreePoints(StatType.DEF, 3);
        // DEF = (6 + 3) × 1 = 9 => MDEF = 4.5
        CLogAssert.assertEquals("5级+3自由DEF MDEF = 4.5", 4.5f, data.getMDEF());
    }

    // ========== 装备与百分比 ==========

    @Test
    public void 测试_装备固定加成() {
        StatData data = new StatData();
        data.setLevel(0);
        data.setEquipFixed(StatType.ATK, 5f);
        // ATK = (1×1 + 5) × 1 = 6
        CLogAssert.assertEquals("0级+5装备ATK = 6", 6f, data.getATK());
    }

    @Test
    public void 测试_百分比增益() {
        StatData data = new StatData();
        data.setLevel(0);
        data.setPercentBonus(StatType.ATK, 0.5f); // +50%
        // ATK = 1 × 1.5 = 1.5
        CLogAssert.assertEquals("0级+50%ATK = 1.5", 1.5f, data.getATK());
    }

    @Test
    public void 测试_装备加百分比组合() {
        StatData data = new StatData();
        data.setLevel(1);
        data.addFreePoints(StatType.ATK, 2);
        data.setEquipFixed(StatType.ATK, 3f);
        data.setPercentBonus(StatType.ATK, 0.2f); // +20%
        // ATK = ((2+2)×1 + 3) × 1.2 = 7 × 1.2 = 8.4
        CLogAssert.assertEquals("复合ATK = 8.4", 8.4f, data.getATK(), 0.001f);
    }

    // ========== ASP / MOV 上限 ==========

    @Test
    public void 测试_ASP初始值为100百分号() {
        StatData data = new StatData();
        CLogAssert.assertEquals("ASP 初始 = 1.0", 1.0f, data.getASP());
    }

    @Test
    public void 测试_ASP上限300百分号() {
        StatData data = new StatData();
        // 装备提供 +5.0（远超上限）
        data.setEquipFixed(StatType.ASP, 5.0f);
        // 应被裁剪到 3.0
        CLogAssert.assertEquals("ASP 上限 = 3.0", 3.0f, data.getASP());
    }

    @Test
    public void 测试_ASP突破上限加成() {
        StatData data = new StatData();
        data.setEquipFixed(StatType.ASP, 2.0f); // 基础1.0+2.0=3.0 到上限
        data.setUncappedBonus(StatType.ASP, 0.5f); // 突破+0.5
        // 3.0 + 0.5 = 3.5
        CLogAssert.assertEquals("ASP 突破 = 3.5", 3.5f, data.getASP());
    }

    @Test
    public void 测试_MOV与ASP独立() {
        StatData data = new StatData();
        data.setEquipFixed(StatType.MOV, 1.0f);
        data.setPercentBonus(StatType.MOV, 0.5f);
        // MOV = min((1.0 + 1.0) × (1 + 0.5), 3.0) = min(3.0, 3.0) = 3.0
        CLogAssert.assertEquals("MOV = 3.0", 3.0f, data.getMOV());
        // ASP 不受影响
        CLogAssert.assertEquals("ASP 不变 = 1.0", 1.0f, data.getASP());
    }

    // ========== 冷却计算 ==========

    @Test
    public void 测试_攻击冷却() {
        float cd = StatCalculator.attackCooldown(1.0f, 1.0f);
        CLogAssert.assertEquals("100% ASP CD = 1.0s", 1.0f, cd);

        cd = StatCalculator.attackCooldown(1.0f, 2.0f);
        CLogAssert.assertEquals("200% ASP CD = 0.5s", 0.5f, cd);
    }

    @Test
    public void 测试_移动冷却() {
        float cd = StatCalculator.moveCooldown(1.0f, 3.0f);
        // 300% MOV => CD = 1.0/3.0 ≈ 0.333
        CLogAssert.assertTrue("300% MOV CD ≈ 0.333",
            Math.abs(cd - 1.0f / 3.0f) < 0.001f);
    }

    // ========== toString ==========

    @Test
    public void 测试_toString不崩溃() {
        StatData data = new StatData();
        data.setLevel(5);
        String s = data.toString();
        CLogAssert.assertTrue("toString 包含 Lv.5", s.contains("Lv.5"));
        System.out.println(s);
    }

    // ========== 自由点溢出校验 ==========

    @Test
    public void 测试_自由点分配余额不足应失败() {
        StatData data = new StatData();
        data.setLevel(1); // 4点自由点
        // 尝试加 5 点，应该失败
        boolean success = data.addFreePoints(StatType.ATK, 5);
        CLogAssert.assertFalse("余额不足应拒绝分配", success);
        CLogAssert.assertEquals("ATK点数应不变", 0, data.getFreePoints(StatType.ATK));
    }

    @Test
    public void 测试_手动设置溢出validate检测() {
        StatData data = new StatData();
        data.setLevel(1); // 4点
        data.setFreePoints(StatType.HP, 10); // 存档篡改场景
        CLogAssert.assertFalse("自由点超限应被检测", data.validate());
    }

    @Test
    public void 测试_正常数据validate通过() {
        StatData data = new StatData();
        data.setLevel(3); // 12点
        data.addFreePoints(StatType.ATK, 5);
        data.addFreePoints(StatType.DEF, 5);
        CLogAssert.assertTrue("正常数据应通过校验", data.validate());
    }

    // ========== 百分比增益叠加 ==========

    @Test
    public void 测试_装备加百分比加自由点全管线() {
        StatData data = new StatData();
        data.setLevel(2); // 固定点 = 3
        data.addFreePoints(StatType.ATK, 2);
        data.setEquipFixed(StatType.ATK, 5f);
        data.setPercentBonus(StatType.ATK, 0.5f); // +50%
        // ATK = ((3+2)×1 + 5) × (1 + 0.5) = 10 × 1.5 = 15
        CLogAssert.assertEquals("全管线ATK = 15", 15f, data.getATK());
    }
}
