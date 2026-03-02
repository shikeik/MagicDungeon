package com.goldsprite.gdengine.netcode;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.badlogic.gdx.math.Vector2;
import com.goldsprite.GdxTestRunner;
import com.goldsprite.magicdungeon2.screens.tests.netcode.TankGameMap;

/**
 * TankGameMap 单元测试：墙体生成、边界约束、碰撞检测。
 */
@RunWith(GdxTestRunner.class)
public class TankGameMapTest {

    private TankGameMap map;

    @Before
    public void setUp() {
        map = new TankGameMap(2000f, 1500f);
    }

    // ══════════════ 基本属性 ══════════════

    @Test
    public void testMapDimensions() {
        System.out.println("======= [TDD] TankGameMap: 基本属性 =======");
        assertEquals(2000f, map.getMapWidth(), 0.001f);
        assertEquals(1500f, map.getMapHeight(), 0.001f);
        assertTrue("初始墙体列表应为空", map.getWalls().isEmpty());
        System.out.println("======= [TDD] TankGameMap: 基本属性 通过 =======");
    }

    // ══════════════ 墙体生成 ══════════════

    @Test
    public void testGenerateWalls_sameSeedProducesSameWalls() {
        System.out.println("======= [TDD] TankGameMap: 同种子一致性 =======");
        map.generateWalls(42L);
        int count1 = map.getWalls().size();
        float[] first1 = map.getWalls().get(0).clone();

        map.generateWalls(42L);
        int count2 = map.getWalls().size();
        float[] first2 = map.getWalls().get(0);

        assertEquals("同种子应生成相同数量墙体", count1, count2);
        assertArrayEquals("同种子第一面墙应完全相同", first1, first2, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 同种子一致性 通过 =======");
    }

    @Test
    public void testGenerateWalls_differentSeedProducesDifferent() {
        System.out.println("======= [TDD] TankGameMap: 不同种子差异性 =======");
        map.generateWalls(42L);
        float[] first42 = map.getWalls().get(0).clone();

        map.generateWalls(999L);
        float[] first999 = map.getWalls().get(0);

        // 不同种子大概率不同（允许极小概率巧合，但实际上几乎不可能相同）
        boolean different = false;
        for (int i = 0; i < 4; i++) {
            if (Math.abs(first42[i] - first999[i]) > 0.01f) {
                different = true;
                break;
            }
        }
        assertTrue("不同种子应生成不同墙体布局", different);
        System.out.println("======= [TDD] TankGameMap: 不同种子差异性 通过 =======");
    }

    @Test
    public void testGenerateWalls_notEmpty() {
        System.out.println("======= [TDD] TankGameMap: 墙体非空 =======");
        map.generateWalls(123L);
        assertFalse("生成后墙体列表不应为空", map.getWalls().isEmpty());
        System.out.println("======= [TDD] TankGameMap: 墙体非空 通过 =======");
    }

    @Test
    public void testClear() {
        System.out.println("======= [TDD] TankGameMap: clear =======");
        map.generateWalls(42L);
        assertFalse(map.getWalls().isEmpty());
        map.clear();
        assertTrue("clear后墙体应为空", map.getWalls().isEmpty());
        assertEquals("clear后种子应重置", 0L, map.getMapSeed());
        System.out.println("======= [TDD] TankGameMap: clear 通过 =======");
    }

    // ══════════════ 边界约束 ══════════════

    @Test
    public void testClampToBoundary_insideBounds() {
        System.out.println("======= [TDD] TankGameMap: 边界内不变 =======");
        Vector2 result = map.clampToBoundary(100f, 200f, 15f);
        assertEquals(100f, result.x, 0.001f);
        assertEquals(200f, result.y, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 边界内不变 通过 =======");
    }

    @Test
    public void testClampToBoundary_leftEdge() {
        System.out.println("======= [TDD] TankGameMap: 左边界clamp =======");
        Vector2 result = map.clampToBoundary(-5f, 100f, 15f);
        assertEquals("应clamp到左边界", 15f, result.x, 0.001f);
        assertEquals(100f, result.y, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 左边界clamp 通过 =======");
    }

    @Test
    public void testClampToBoundary_rightEdge() {
        System.out.println("======= [TDD] TankGameMap: 右边界clamp =======");
        Vector2 result = map.clampToBoundary(2010f, 100f, 15f);
        assertEquals("应clamp到右边界", 2000f - 15f, result.x, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 右边界clamp 通过 =======");
    }

    @Test
    public void testClampToBoundary_bottomEdge() {
        System.out.println("======= [TDD] TankGameMap: 下边界clamp =======");
        Vector2 result = map.clampToBoundary(100f, -10f, 15f);
        assertEquals("应clamp到下边界", 15f, result.y, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 下边界clamp 通过 =======");
    }

    @Test
    public void testClampToBoundary_topEdge() {
        System.out.println("======= [TDD] TankGameMap: 上边界clamp =======");
        Vector2 result = map.clampToBoundary(100f, 1600f, 15f);
        assertEquals("应clamp到上边界", 1500f - 15f, result.y, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 上边界clamp 通过 =======");
    }

    // ══════════════ 越界检测 ══════════════

    @Test
    public void testIsOutOfBounds() {
        System.out.println("======= [TDD] TankGameMap: 越界检测 =======");
        assertFalse("地图中心不越界", map.isOutOfBounds(1000f, 750f));
        assertTrue("X负值越界", map.isOutOfBounds(-1f, 750f));
        assertTrue("X超宽越界", map.isOutOfBounds(2001f, 750f));
        assertTrue("Y负值越界", map.isOutOfBounds(1000f, -1f));
        assertTrue("Y超高越界", map.isOutOfBounds(1000f, 1501f));
        // 边缘值
        assertFalse("X=0不越界", map.isOutOfBounds(0f, 750f));
        assertFalse("X=mapWidth不越界", map.isOutOfBounds(2000f, 750f));
        System.out.println("======= [TDD] TankGameMap: 越界检测 通过 =======");
    }

    // ══════════════ 子弹墙体碰撞 ══════════════

    @Test
    public void testBulletHitsWall_noWalls() {
        System.out.println("======= [TDD] TankGameMap: 无墙不命中 =======");
        assertFalse("无墙体时不应命中", map.bulletHitsWall(100f, 100f, 8f));
        System.out.println("======= [TDD] TankGameMap: 无墙不命中 通过 =======");
    }

    @Test
    public void testBulletHitsWall_hitsExistingWall() {
        System.out.println("======= [TDD] TankGameMap: 命中墙体 =======");
        map.generateWalls(42L);
        // 取第一面墙的中心来检测
        float[] wall = map.getWalls().get(0);
        float wallCenterX = wall[0] + wall[2] / 2f;
        float wallCenterY = wall[1] + wall[3] / 2f;
        assertTrue("子弹在墙体中心应命中", map.bulletHitsWall(wallCenterX, wallCenterY, 8f));
        System.out.println("======= [TDD] TankGameMap: 命中墙体 通过 =======");
    }

    // ══════════════ 推出墙体 ══════════════

    @Test
    public void testPushOutOfWalls_noWalls() {
        System.out.println("======= [TDD] TankGameMap: 无墙不推 =======");
        Vector2 result = map.pushOutOfWalls(100f, 100f, 15f);
        assertEquals(100f, result.x, 0.001f);
        assertEquals(100f, result.y, 0.001f);
        System.out.println("======= [TDD] TankGameMap: 无墙不推 通过 =======");
    }

    @Test
    public void testPushOutOfWalls_pushesOutOfWall() {
        System.out.println("======= [TDD] TankGameMap: 推出墙体 =======");
        map.generateWalls(42L);
        // 将坦克放在第一面墙的中心
        float[] wall = map.getWalls().get(0);
        float wallCenterX = wall[0] + wall[2] / 2f;
        float wallCenterY = wall[1] + wall[3] / 2f;

        Vector2 result = map.pushOutOfWalls(wallCenterX, wallCenterY, 15f);

        // 推出后坦克不应再与该墙重叠
        float tLeft = result.x - 15f, tBottom = result.y - 15f;
        float tRight = result.x + 15f, tTop = result.y + 15f;
        boolean stillOverlap = (tLeft < wall[0] + wall[2] && tRight > wall[0]
            && tBottom < wall[1] + wall[3] && tTop > wall[1]);
        assertFalse("推出后坦克不应与墙体重叠", stillOverlap);
        System.out.println("======= [TDD] TankGameMap: 推出墙体 通过 =======");
    }
}
