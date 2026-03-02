package com.goldsprite.gdengine.netcode;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.goldsprite.CLogAssert;
import com.goldsprite.GdxTestRunner;

/**
 * TDD 驱动：验证地图碰撞工具方法（AABB 碰撞、边界 Clamp、墙体生成一致性）。
 * 这些方法提取自 NetcodeTankOnlineScreen 以便单元测试。
 */
@RunWith(GdxTestRunner.class)
public class MapCollisionTest {

    // ========== AABB 矩形重叠检测 ==========

    /**
     * 测试1: 两个矩形重叠
     */
    @Test
    public void testRectOverlap_overlapping() {
        System.out.println("======= [TDD] AABB 重叠检测: 重叠 =======");
        CLogAssert.assertTrue("两矩形应重叠",
            MapCollisionUtils.rectOverlap(0, 0, 30, 30, 15, 15, 30, 30));
        System.out.println("======= [TDD] AABB 重叠检测: 重叠 通过 =======");
    }

    /**
     * 测试2: 两个矩形不重叠
     */
    @Test
    public void testRectOverlap_noOverlap() {
        System.out.println("======= [TDD] AABB 重叠检测: 不重叠 =======");
        CLogAssert.assertFalse("两矩形不应重叠",
            MapCollisionUtils.rectOverlap(0, 0, 30, 30, 50, 50, 30, 30));
        System.out.println("======= [TDD] AABB 重叠检测: 不重叠 通过 =======");
    }

    /**
     * 测试3: 边缘刚好接触（不算重叠）
     */
    @Test
    public void testRectOverlap_edgeTouch() {
        System.out.println("======= [TDD] AABB 重叠检测: 边缘接触 =======");
        CLogAssert.assertFalse("边缘接触不算重叠",
            MapCollisionUtils.rectOverlap(0, 0, 30, 30, 30, 0, 30, 30));
        System.out.println("======= [TDD] AABB 重叠检测: 边缘接触 通过 =======");
    }

    // ========== 边界 Clamp ==========

    /**
     * 测试4: 坐标在边界内不变
     */
    @Test
    public void testClampToBoundary_insideBounds() {
        System.out.println("======= [TDD] 边界 Clamp: 边界内 =======");
        float[] pos = MapCollisionUtils.clampToBoundary(500, 400, 15, 2000, 1500);
        CLogAssert.assertEquals("X 不变", 500f, pos[0], 0.001f);
        CLogAssert.assertEquals("Y 不变", 400f, pos[1], 0.001f);
        System.out.println("======= [TDD] 边界 Clamp: 边界内 通过 =======");
    }

    /**
     * 测试5: 坐标超出左下角
     */
    @Test
    public void testClampToBoundary_outOfBoundsMin() {
        System.out.println("======= [TDD] 边界 Clamp: 超出最小值 =======");
        float[] pos = MapCollisionUtils.clampToBoundary(-10, -20, 15, 2000, 1500);
        CLogAssert.assertEquals("X 应被限制到 halfSize", 15f, pos[0], 0.001f);
        CLogAssert.assertEquals("Y 应被限制到 halfSize", 15f, pos[1], 0.001f);
        System.out.println("======= [TDD] 边界 Clamp: 超出最小值 通过 =======");
    }

    /**
     * 测试6: 坐标超出右上角
     */
    @Test
    public void testClampToBoundary_outOfBoundsMax() {
        System.out.println("======= [TDD] 边界 Clamp: 超出最大值 =======");
        float[] pos = MapCollisionUtils.clampToBoundary(2100, 1600, 15, 2000, 1500);
        CLogAssert.assertEquals("X 应被限制到 MAP_WIDTH - halfSize", 1985f, pos[0], 0.001f);
        CLogAssert.assertEquals("Y 应被限制到 MAP_HEIGHT - halfSize", 1485f, pos[1], 0.001f);
        System.out.println("======= [TDD] 边界 Clamp: 超出最大值 通过 =======");
    }

    // ========== 墙体生成一致性 ==========

    /**
     * 测试7: 相同种子生成相同墙体布局
     */
    @Test
    public void testWallGeneration_sameSeed() {
        System.out.println("======= [TDD] 墙体生成: 相同种子 =======");
        float[][] walls1 = MapCollisionUtils.generateWalls(12345, 2000, 1500);
        float[][] walls2 = MapCollisionUtils.generateWalls(12345, 2000, 1500);

        CLogAssert.assertEquals("墙体数量应一致", walls1.length, walls2.length);
        for (int i = 0; i < walls1.length; i++) {
            CLogAssert.assertEquals("墙体 " + i + " X 应一致", walls1[i][0], walls2[i][0], 0.001f);
            CLogAssert.assertEquals("墙体 " + i + " Y 应一致", walls1[i][1], walls2[i][1], 0.001f);
            CLogAssert.assertEquals("墙体 " + i + " W 应一致", walls1[i][2], walls2[i][2], 0.001f);
            CLogAssert.assertEquals("墙体 " + i + " H 应一致", walls1[i][3], walls2[i][3], 0.001f);
        }
        System.out.println("======= [TDD] 墙体生成: 相同种子 通过 =======");
    }

    /**
     * 测试8: 不同种子生成不同布局
     */
    @Test
    public void testWallGeneration_differentSeed() {
        System.out.println("======= [TDD] 墙体生成: 不同种子 =======");
        float[][] walls1 = MapCollisionUtils.generateWalls(12345, 2000, 1500);
        float[][] walls2 = MapCollisionUtils.generateWalls(54321, 2000, 1500);

        // 极小概率相同，但不同种子大概率布局不同
        boolean allSame = walls1.length == walls2.length;
        if (allSame) {
            for (int i = 0; i < walls1.length; i++) {
                if (Math.abs(walls1[i][0] - walls2[i][0]) > 1f) {
                    allSame = false;
                    break;
                }
            }
        }
        CLogAssert.assertFalse("不同种子应生成不同布局", allSame);
        System.out.println("======= [TDD] 墙体生成: 不同种子 通过 =======");
    }

    /**
     * 测试9: 墙体数量在 5~10 范围内
     */
    @Test
    public void testWallGeneration_countRange() {
        System.out.println("======= [TDD] 墙体生成: 数量范围 =======");
        // 测试多个种子，验证墙体数量
        for (int seed = 0; seed < 100; seed++) {
            float[][] walls = MapCollisionUtils.generateWalls(seed, 2000, 1500);
            CLogAssert.assertTrue("墙体数量应 >= 5, seed=" + seed, walls.length >= 5);
            CLogAssert.assertTrue("墙体数量应 <= 10, seed=" + seed, walls.length <= 10);
        }
        System.out.println("======= [TDD] 墙体生成: 数量范围 通过 =======");
    }

    /**
     * 测试10: 所有墙体在地图边界内（含 margin）
     */
    @Test
    public void testWallGeneration_withinBounds() {
        System.out.println("======= [TDD] 墙体生成: 边界内 =======");
        float mapW = 2000, mapH = 1500;
        float[][] walls = MapCollisionUtils.generateWalls(42, mapW, mapH);
        for (int i = 0; i < walls.length; i++) {
            float x = walls[i][0], y = walls[i][1], w = walls[i][2], h = walls[i][3];
            CLogAssert.assertTrue("墙体 " + i + " 左边界 >= 0", x >= 0);
            CLogAssert.assertTrue("墙体 " + i + " 下边界 >= 0", y >= 0);
            CLogAssert.assertTrue("墙体 " + i + " 右边界 <= 地图宽度", x + w <= mapW);
            CLogAssert.assertTrue("墙体 " + i + " 上边界 <= 地图高度", y + h <= mapH);
        }
        System.out.println("======= [TDD] 墙体生成: 边界内 通过 =======");
    }
}
