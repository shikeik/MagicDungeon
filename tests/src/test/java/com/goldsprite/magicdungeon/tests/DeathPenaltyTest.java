package com.goldsprite.magicdungeon2.tests;

import org.junit.Test;

import com.goldsprite.magicdungeon2.CLogAssert;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty;
import com.goldsprite.magicdungeon2.core.growth.DeathPenalty.DeathResult;
import com.goldsprite.magicdungeon2.core.growth.GrowthCalculator;
import com.goldsprite.magicdungeon2.core.stats.StatData;
import com.goldsprite.magicdungeon2.core.stats.StatType;

/**
 * 死亡惩罚单元测试。
 * <p>
 * 验证：经验掉落20%、等级滑落、自由点平均扣除、金币惩罚。
 */
public class DeathPenaltyTest {

    // ========== 经验惩罚 ==========

    @Test
    public void 测试_经验掉落20百分比() {
        DeathResult r = DeathPenalty.calcPenalty(1000, 0);
        CLogAssert.assertEquals("损失经验 = 200", 200L, r.xpLost);
        CLogAssert.assertEquals("剩余经验 = 800", 800L, r.xpAfter);
    }

    @Test
    public void 测试_0经验死亡不崩溃() {
        DeathResult r = DeathPenalty.calcPenalty(0, 0);
        CLogAssert.assertEquals("0经验损失 = 0", 0L, r.xpLost);
        CLogAssert.assertEquals("等级依旧 = 0", 0, r.levelAfter);
    }

    @Test
    public void 测试_死亡可能导致降级() {
        // 2级需要 183 总经验，假设刚好 183
        long xp = GrowthCalculator.totalXpForLevel(2); // 183
        CLogAssert.assertEquals("2级前置经验 = 183", 183L, xp);

        DeathResult r = DeathPenalty.calcPenalty(xp, 0);
        CLogAssert.assertEquals("死前等级 = 2", 2, r.levelBefore);
        // 183 × 0.8 = 146.4 → 146
        // 1级需要 83，2级需要 183 => 146 > 83 => 仍为1级
        CLogAssert.assertEquals("死后等级 = 1", 1, r.levelAfter);
    }

    @Test
    public void 测试_高经验不降级() {
        // 有大量溢出经验
        long xp = GrowthCalculator.totalXpForLevel(5) + 500;
        int expectedLevel = GrowthCalculator.levelFromXp(xp);
        DeathResult r = DeathPenalty.calcPenalty(xp, 0);
        CLogAssert.assertEquals("死前等级", expectedLevel, r.levelBefore);
        // 80% 仍然够维持较高等级，不会降超过1级
        CLogAssert.assertTrue("高溢出经验不降级",
            r.levelAfter >= r.levelBefore - 1);
    }

    // ========== 金币惩罚 ==========

    @Test
    public void 测试_金币掉落50百分比() {
        DeathResult r = DeathPenalty.calcPenalty(0, 1000);
        CLogAssert.assertEquals("掉落金币 = 500", 500, r.goldDropped);
    }

    @Test
    public void 测试_可拾回金币40百分比() {
        DeathResult r = DeathPenalty.calcPenalty(0, 1000);
        // 掉落500, 永久损失60%=300, 可拾回40%=200
        CLogAssert.assertEquals("永久损失 = 300", 300, r.goldPermanentLost);
        CLogAssert.assertEquals("可拾回 = 200", 200, r.goldRecoverable);
    }

    @Test
    public void 测试_0金币不崩溃() {
        DeathResult r = DeathPenalty.calcPenalty(0, 0);
        CLogAssert.assertEquals("0金币掉落 = 0", 0, r.goldDropped);
    }

    // ========== 自由点平均扣除 ==========

    @Test
    public void 测试_降级自由点均匀扣除() {
        StatData data = new StatData();
        data.setLevel(5);
        // 5级总自由点 = 20，平均分4项，每项5点
        data.setFreePoints(StatType.HP, 5);
        data.setFreePoints(StatType.MP, 5);
        data.setFreePoints(StatType.ATK, 5);
        data.setFreePoints(StatType.DEF, 5);

        // 降到3级，损失自由点 = (5-3)×4 = 8, 平均每项扣2
        DeathPenalty.applyLevelLoss(data, 5, 3);

        CLogAssert.assertEquals("降级后等级 = 3", 3, data.getLevel());
        CLogAssert.assertEquals("HP自由点 = 3", 3, data.getFreePoints(StatType.HP));
        CLogAssert.assertEquals("MP自由点 = 3", 3, data.getFreePoints(StatType.MP));
        CLogAssert.assertEquals("ATK自由点 = 3", 3, data.getFreePoints(StatType.ATK));
        CLogAssert.assertEquals("DEF自由点 = 3", 3, data.getFreePoints(StatType.DEF));
    }

    @Test
    public void 测试_降级自由点不足时扣到0() {
        StatData data = new StatData();
        data.setLevel(3);
        // 只在ATK加了全部12点自由点
        data.setFreePoints(StatType.ATK, 12);

        // 降到0级，损失自由点 = 3×4 = 12, 平均每项扣3
        // HP/MP/DEF 自由点 = 0，扣不了，只会设为0
        DeathPenalty.applyLevelLoss(data, 3, 0);

        CLogAssert.assertEquals("降级后等级 = 0", 0, data.getLevel());
        CLogAssert.assertEquals("HP自由点 = 0", 0, data.getFreePoints(StatType.HP));
        CLogAssert.assertEquals("ATK自由点 = 9", 9, data.getFreePoints(StatType.ATK));
    }

    @Test
    public void 测试_不降级不扣点() {
        StatData data = new StatData();
        data.setLevel(5);
        data.setFreePoints(StatType.ATK, 10);

        DeathPenalty.applyLevelLoss(data, 5, 5);

        CLogAssert.assertEquals("等级不变 = 5", 5, data.getLevel());
        CLogAssert.assertEquals("ATK自由点不变 = 10", 10, data.getFreePoints(StatType.ATK));
    }
}
