package com.goldsprite.gdengine.netcode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.GdxTestRunner;
import com.goldsprite.magicdungeon2.screens.tests.netcode.Bullet;
import com.goldsprite.magicdungeon2.screens.tests.netcode.BulletSystem;
import com.goldsprite.magicdungeon2.screens.tests.netcode.TankBehaviour;
import com.goldsprite.magicdungeon2.screens.tests.netcode.TankGameMap;

/**
 * BulletSystem 单元测试：子弹物理、墙体碰撞、坦克碰撞、越界消除。
 */
@RunWith(GdxTestRunner.class)
public class BulletSystemTest {

    private BulletSystem bulletSystem;
    private TankGameMap map;
    private Map<Integer, TankBehaviour> tanks;

    @Before
    public void setUp() {
        bulletSystem = new BulletSystem();
        map = new TankGameMap(2000f, 1500f);
        // 不生成墙体，方便基础测试
        tanks = new HashMap<>();
    }

    /** 辅助：创建一个简单的坦克 */
    private TankBehaviour createTank(float x, float y, float rot) {
        TankBehaviour tank = new TankBehaviour();
        tank.x.setValue(x);
        tank.y.setValue(y);
        tank.rot.setValue(rot);
        tank.color.setValue(Color.RED);
        tank.hp.setValue(4);
        tank.isDead.setValue(false);
        return tank;
    }

    // ══════════════ 基本属性 ══════════════

    @Test
    public void testInitialState() {
        System.out.println("======= [TDD] BulletSystem: 初始状态 =======");
        assertTrue("初始Server子弹应为空", bulletSystem.getServerBullets().isEmpty());
        assertTrue("初始Client子弹应为空", bulletSystem.getClientBullets().isEmpty());
        assertEquals("初始ID应为1", 1, bulletSystem.getNextBulletId());
        System.out.println("======= [TDD] BulletSystem: 初始状态 通过 =======");
    }

    @Test
    public void testClear() {
        System.out.println("======= [TDD] BulletSystem: clear =======");
        TankBehaviour tank = createTank(500f, 500f, 0f);
        tanks.put(-1, tank);
        bulletSystem.createBullet(tank, -1);
        assertFalse(bulletSystem.getServerBullets().isEmpty());

        bulletSystem.clear();
        assertTrue("clear后Server子弹应为空", bulletSystem.getServerBullets().isEmpty());
        assertEquals("clear后ID应重置为1", 1, bulletSystem.getNextBulletId());
        System.out.println("======= [TDD] BulletSystem: clear 通过 =======");
    }

    // ══════════════ 子弹生成 ══════════════

    @Test
    public void testSpawnBullet() {
        System.out.println("======= [TDD] BulletSystem: 子弹生成 =======");
        TankBehaviour tank = createTank(500f, 500f, 0f);
        tanks.put(-1, tank);

        Bullet b = bulletSystem.createBullet(tank, -1);
        assertNotNull("应返回子弹对象", b);
        assertEquals("ownerId应为-1", -1, b.ownerId);
        assertEquals("bulletId应为1", 1, b.bulletId);
        assertEquals("Server子弹列表长度应为1", 1, bulletSystem.getServerBullets().size());
        assertEquals("下一个ID应为2", 2, bulletSystem.getNextBulletId());
        System.out.println("======= [TDD] BulletSystem: 子弹生成 通过 =======");
    }

    @Test
    public void testSpawnMultipleBullets() {
        System.out.println("======= [TDD] BulletSystem: 多次生成 =======");
        TankBehaviour tank = createTank(500f, 500f, 90f);
        tanks.put(-1, tank);

        bulletSystem.createBullet(tank, -1);
        bulletSystem.createBullet(tank, -1);
        bulletSystem.createBullet(tank, -1);

        assertEquals("应有3颗子弹", 3, bulletSystem.getServerBullets().size());
        assertEquals("下一个ID应为4", 4, bulletSystem.getNextBulletId());
        System.out.println("======= [TDD] BulletSystem: 多次生成 通过 =======");
    }

    // ══════════════ 子弹物理 ══════════════

    @Test
    public void testBulletMovement() {
        System.out.println("======= [TDD] BulletSystem: 子弹运动 =======");
        TankBehaviour tank = createTank(500f, 500f, 0f); // 朝右
        tanks.put(-1, tank);

        bulletSystem.createBullet(tank, -1);
        Bullet b = bulletSystem.getServerBullets().get(0);
        float startX = b.x;

        // 模拟一帧 (0.016s ≈ 60fps)
        bulletSystem.updateServerBullets(0.016f, map, tanks);

        // 子弹应该向右移动
        if (!bulletSystem.getServerBullets().isEmpty()) {
            assertTrue("子弹应向右移动", bulletSystem.getServerBullets().get(0).x > startX);
        }
        System.out.println("======= [TDD] BulletSystem: 子弹运动 通过 =======");
    }

    // ══════════════ 越界消除 ══════════════

    @Test
    public void testBulletOutOfBoundsRemoval() {
        System.out.println("======= [TDD] BulletSystem: 越界消除 =======");
        TankBehaviour tank = createTank(1990f, 750f, 0f); // 接近右边界，朝右
        tanks.put(-1, tank);

        bulletSystem.createBullet(tank, -1);
        assertFalse(bulletSystem.getServerBullets().isEmpty());

        // 多帧更新直到子弹越界
        for (int i = 0; i < 100; i++) {
            bulletSystem.updateServerBullets(0.1f, map, tanks);
        }
        assertTrue("子弹应因越界被移除", bulletSystem.getServerBullets().isEmpty());
        System.out.println("======= [TDD] BulletSystem: 越界消除 通过 =======");
    }

    // ══════════════ 墙体碰撞 ══════════════

    @Test
    public void testBulletWallCollision() {
        System.out.println("======= [TDD] BulletSystem: 墙体碰撞 =======");
        // 生成墙体
        map.generateWalls(42L);
        assertFalse("应有墙体", map.getWalls().isEmpty());

        // 在地图中心生成坦克，朝向第一面墙
        float[] wall = map.getWalls().get(0);
        float wallCX = wall[0] + wall[2] / 2f;
        float wallCY = wall[1] + wall[3] / 2f;

        // 在墙旁边生成坦克，朝墙射击
        float tankX = wallCX - 100f;
        float tankY = wallCY;
        TankBehaviour tank = createTank(tankX, tankY, 0f); // 朝右
        tanks.put(-1, tank);

        bulletSystem.createBullet(tank, -1);
        int initialCount = bulletSystem.getServerBullets().size();
        assertEquals(1, initialCount);

        // 多帧更新让子弹飞向墙体
        for (int i = 0; i < 200; i++) {
            bulletSystem.updateServerBullets(0.016f, map, tanks);
            if (bulletSystem.getServerBullets().isEmpty()) break;
        }
        // 子弹应因墙体碰撞或越界被移除
        assertTrue("子弹最终应被移除", bulletSystem.getServerBullets().isEmpty());
        System.out.println("======= [TDD] BulletSystem: 墙体碰撞 通过 =======");
    }

    // ══════════════ 坦克碰撞 ══════════════

    @Test
    public void testBulletHitsTank() {
        System.out.println("======= [TDD] BulletSystem: 命中坦克 =======");
        // 射手在左侧
        TankBehaviour shooter = createTank(200f, 500f, 0f); // 朝右
        tanks.put(-1, shooter);

        // 目标在右侧
        TankBehaviour target = createTank(260f, 500f, 180f);
        tanks.put(1, target);

        int hpBefore = target.hp.getValue();
        bulletSystem.createBullet(shooter, -1);

        // 多帧更新让子弹飞向目标
        List<BulletSystem.BulletHitEvent> allHits = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            List<BulletSystem.BulletHitEvent> hits = bulletSystem.updateServerBullets(0.016f, map, tanks);
            allHits.addAll(hits);
            if (bulletSystem.getServerBullets().isEmpty()) break;
        }

        // 应该命中目标并扣血
        assertFalse("应有命中事件", allHits.isEmpty());
        assertEquals("命中目标应为玩家1", 1, allHits.get(0).hitTankOwnerId);
        assertEquals("射手应为-1", -1, allHits.get(0).shooterOwnerId);
        assertTrue("目标HP应减少", target.hp.getValue() < hpBefore);
        System.out.println("======= [TDD] BulletSystem: 命中坦克 通过 =======");
    }

    @Test
    public void testBulletDoesNotHitSelf() {
        System.out.println("======= [TDD] BulletSystem: 不打自己 =======");
        // 射手朝右
        TankBehaviour shooter = createTank(500f, 500f, 0f);
        tanks.put(-1, shooter);

        int hpBefore = shooter.hp.getValue();
        bulletSystem.createBullet(shooter, -1);

        // 多帧更新
        for (int i = 0; i < 100; i++) {
            bulletSystem.updateServerBullets(0.016f, map, tanks);
        }

        assertEquals("射手HP不应减少", hpBefore, (int) shooter.hp.getValue());
        System.out.println("======= [TDD] BulletSystem: 不打自己 通过 =======");
    }

    @Test
    public void testBulletDoesNotHitDeadTank() {
        System.out.println("======= [TDD] BulletSystem: 不打死亡坦克 =======");
        TankBehaviour shooter = createTank(200f, 500f, 0f);
        tanks.put(-1, shooter);

        TankBehaviour deadTank = createTank(260f, 500f, 180f);
        deadTank.isDead.setValue(true);
        tanks.put(1, deadTank);

        int hpBefore = deadTank.hp.getValue();
        bulletSystem.createBullet(shooter, -1);

        for (int i = 0; i < 100; i++) {
            List<BulletSystem.BulletHitEvent> hits = bulletSystem.updateServerBullets(0.016f, map, tanks);
            assertTrue("不应有命中死亡坦克的事件", hits.isEmpty());
        }

        assertEquals("死亡坦克HP不应变化", hpBefore, (int) deadTank.hp.getValue());
        System.out.println("======= [TDD] BulletSystem: 不打死亡坦克 通过 =======");
    }
}
