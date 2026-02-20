package com.goldsprite.magicdungeon2.tests;

import com.goldsprite.magicdungeon2.CLogAssert;
import com.goldsprite.magicdungeon2.core.growth.GrowthCalculator;
import org.junit.Test;

/**
 * 经验与等级系统单元测试。
 * <p>
 * 验证：升级经验公式、累计经验、等级推导、经验进度条。
 */
public class GrowthCalculatorTest {

    // ========== 升级经验公式 ==========

    @Test
    public void 测试_0级到1级经验() {
        // floor(100 × 1.2^(-1)) = floor(83.33) = 83
        int xp = GrowthCalculator.xpForNextLevel(0);
        CLogAssert.assertEquals("0→1级需要 83 XP", 83, xp);
    }

    @Test
    public void 测试_1级到2级经验() {
        // floor(100 × 1.2^0) = 100
        CLogAssert.assertEquals("1→2级需要 100 XP", 100, GrowthCalculator.xpForNextLevel(1));
    }

    @Test
    public void 测试_2级到3级经验() {
        // floor(100 × 1.2^1) = 120
        CLogAssert.assertEquals("2→3级需要 120 XP", 120, GrowthCalculator.xpForNextLevel(2));
    }

    @Test
    public void 测试_10级到11级经验() {
        // floor(100 × 1.2^9) ≈ 515
        int xp = GrowthCalculator.xpForNextLevel(10);
        CLogAssert.assertTrue("10→11级经验约 515", Math.abs(xp - 515) <= 2);
    }

    // ========== 累计总经验 ==========

    @Test
    public void 测试_0级总经验为0() {
        CLogAssert.assertEquals("0级总经验 = 0", 0L, GrowthCalculator.totalXpForLevel(0));
    }

    @Test
    public void 测试_1级总经验() {
        // 从0到1 = 83
        CLogAssert.assertEquals("1级总经验 = 83", 83L, GrowthCalculator.totalXpForLevel(1));
    }

    @Test
    public void 测试_2级总经验() {
        // 83 + 100 = 183
        CLogAssert.assertEquals("2级总经验 = 183", 183L, GrowthCalculator.totalXpForLevel(2));
    }

    @Test
    public void 测试_3级总经验() {
        // 83 + 100 + 120 = 303
        CLogAssert.assertEquals("3级总经验 = 303", 303L, GrowthCalculator.totalXpForLevel(3));
    }

    // ========== 等级推导 ==========

    @Test
    public void 测试_0经验为0级() {
        CLogAssert.assertEquals("0 XP = 0级", 0, GrowthCalculator.levelFromXp(0));
    }

    @Test
    public void 测试_82经验仍为0级() {
        CLogAssert.assertEquals("82 XP = 0级", 0, GrowthCalculator.levelFromXp(82));
    }

    @Test
    public void 测试_83经验恰好1级() {
        CLogAssert.assertEquals("83 XP = 1级", 1, GrowthCalculator.levelFromXp(83));
    }

    @Test
    public void 测试_182经验仍为1级() {
        CLogAssert.assertEquals("182 XP = 1级", 1, GrowthCalculator.levelFromXp(182));
    }

    @Test
    public void 测试_183经验恰好2级() {
        CLogAssert.assertEquals("183 XP = 2级", 2, GrowthCalculator.levelFromXp(183));
    }

    @Test
    public void 测试_大量经验等级推导() {
        // 验证 totalXpForLevel 与 levelFromXp 的一致性
        for (int level = 0; level <= 30; level++) {
            long xp = GrowthCalculator.totalXpForLevel(level);
            int derived = GrowthCalculator.levelFromXp(xp);
            CLogAssert.assertEquals(level + "级总经验反推等级", level, derived);
        }
    }

    @Test
    public void 测试_略低于某级总经验反推为前一级() {
        for (int level = 1; level <= 20; level++) {
            long xp = GrowthCalculator.totalXpForLevel(level) - 1;
            int derived = GrowthCalculator.levelFromXp(xp);
            CLogAssert.assertEquals("低于" + level + "级1XP应为" + (level - 1) + "级",
                level - 1, derived);
        }
    }

    // ========== 经验进度 ==========

    @Test
    public void 测试_经验进度条() {
        // 1级内（83 ~ 182），假设总经验 = 120
        float[] progress = GrowthCalculator.xpProgress(120);
        // 当前级已有 = 120 - 83 = 37
        // 当前级需要 = 100
        CLogAssert.assertEquals("进度: 已有 37", 37f, progress[0]);
        CLogAssert.assertEquals("进度: 需要 100", 100f, progress[1]);
    }

    @Test
    public void 测试_恰好在等级起点的进度() {
        // 恰好 83 XP = 1级起点
        float[] progress = GrowthCalculator.xpProgress(83);
        CLogAssert.assertEquals("1级起点: 已有 0", 0f, progress[0]);
        CLogAssert.assertEquals("1级起点: 需要 100", 100f, progress[1]);
    }
}
