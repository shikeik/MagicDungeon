package com.goldsprite.gdengine.netcode;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.GdxTestRunner;
import com.goldsprite.magicdungeon2.screens.tests.netcode.TankBehaviour;
import com.goldsprite.magicdungeon2.screens.tests.netcode.TankSpawnSystem;

/**
 * TankSpawnSystem 单元测试：出生位置计算、颜色分配、坦克配置、复活功能。
 */
@RunWith(GdxTestRunner.class)
public class TankSpawnSystemTest {

    // ══════════════ 辅助方法 ══════════════

    /** 创建测试用坦克实例（不绑定 NetworkManager） */
    private TankBehaviour createTestTank() {
        TankBehaviour tank = new TankBehaviour();
        tank.hp.setValue(4);
        tank.isDead.setValue(false);
        return tank;
    }

    // ══════════════ 出生位置计算 ══════════════

    @Test
    public void testSpawnPosition_firstRow() {
        System.out.println("======= [TDD] TankSpawnSystem: 第一行出生点 =======");
        // index 0 → (150, 150)
        Vector2 p0 = TankSpawnSystem.getSpawnPosition(0);
        assertEquals("index0 X", 150f, p0.x, 0.01f);
        assertEquals("index0 Y", 150f, p0.y, 0.01f);

        // index 1 → (300, 150)
        Vector2 p1 = TankSpawnSystem.getSpawnPosition(1);
        assertEquals("index1 X", 300f, p1.x, 0.01f);
        assertEquals("index1 Y", 150f, p1.y, 0.01f);

        // index 3 → (600, 150)
        Vector2 p3 = TankSpawnSystem.getSpawnPosition(3);
        assertEquals("index3 X", 600f, p3.x, 0.01f);
        assertEquals("index3 Y", 150f, p3.y, 0.01f);

        System.out.println("======= [TDD] TankSpawnSystem: 第一行出生点 通过 =======");
    }

    @Test
    public void testSpawnPosition_secondRow() {
        System.out.println("======= [TDD] TankSpawnSystem: 第二行出生点 =======");
        // index 4 → (150, 300)，换行
        Vector2 p4 = TankSpawnSystem.getSpawnPosition(4);
        assertEquals("index4 X", 150f, p4.x, 0.01f);
        assertEquals("index4 Y", 300f, p4.y, 0.01f);

        // index 5 → (300, 300)
        Vector2 p5 = TankSpawnSystem.getSpawnPosition(5);
        assertEquals("index5 X", 300f, p5.x, 0.01f);
        assertEquals("index5 Y", 300f, p5.y, 0.01f);

        System.out.println("======= [TDD] TankSpawnSystem: 第二行出生点 通过 =======");
    }

    @Test
    public void testSpawnPosition_largeIndex() {
        System.out.println("======= [TDD] TankSpawnSystem: 大序号出生点 =======");
        // index 8 → (150, 450)，第三行第一列
        Vector2 p8 = TankSpawnSystem.getSpawnPosition(8);
        assertEquals("index8 X", 150f, p8.x, 0.01f);
        assertEquals("index8 Y", 450f, p8.y, 0.01f);

        System.out.println("======= [TDD] TankSpawnSystem: 大序号出生点 通过 =======");
    }

    // ══════════════ 颜色循环分配 ══════════════

    @Test
    public void testSpawnColor_cycle() {
        System.out.println("======= [TDD] TankSpawnSystem: 颜色循环 =======");
        // 前 6 个应分别对应 ORANGE, CYAN, LIME, MAGENTA, GOLD, SKY
        assertEquals(Color.ORANGE, TankSpawnSystem.getSpawnColor(0));
        assertEquals(Color.CYAN, TankSpawnSystem.getSpawnColor(1));
        assertEquals(Color.LIME, TankSpawnSystem.getSpawnColor(2));
        assertEquals(Color.MAGENTA, TankSpawnSystem.getSpawnColor(3));
        assertEquals(Color.GOLD, TankSpawnSystem.getSpawnColor(4));
        assertEquals(Color.SKY, TankSpawnSystem.getSpawnColor(5));
        // 第 7 个应循环回 ORANGE
        assertEquals("颜色应循环", Color.ORANGE, TankSpawnSystem.getSpawnColor(6));

        System.out.println("======= [TDD] TankSpawnSystem: 颜色循环 通过 =======");
    }

    @Test
    public void testSpawnColor_count() {
        System.out.println("======= [TDD] TankSpawnSystem: 颜色种类数 =======");
        assertEquals("应有 6 种颜色", 6, TankSpawnSystem.SPAWN_COLORS.length);
        System.out.println("======= [TDD] TankSpawnSystem: 颜色种类数 通过 =======");
    }

    // ══════════════ configureTank ══════════════

    @Test
    public void testConfigureTank_host() {
        System.out.println("======= [TDD] TankSpawnSystem: 配置Host坦克 =======");
        TankBehaviour tank = createTestTank();
        TankSpawnSystem.configureTank(tank, 0, -1);

        assertEquals("Host X 应为 150", 150f, tank.x.getValue(), 0.01f);
        assertEquals("Host Y 应为 150", 150f, tank.y.getValue(), 0.01f);
        assertEquals("Host 颜色应为 ORANGE", Color.ORANGE, tank.color.getValue());
        assertEquals("Host 名称应为 Host", "Host", tank.playerName.getValue());

        System.out.println("======= [TDD] TankSpawnSystem: 配置Host坦克 通过 =======");
    }

    @Test
    public void testConfigureTank_client() {
        System.out.println("======= [TDD] TankSpawnSystem: 配置Client坦克 =======");
        TankBehaviour tank = createTestTank();
        TankSpawnSystem.configureTank(tank, 2, 42);

        assertEquals("Client X 应为 450", 450f, tank.x.getValue(), 0.01f);
        assertEquals("Client Y 应为 150", 150f, tank.y.getValue(), 0.01f);
        assertEquals("Client 颜色应为 LIME", Color.LIME, tank.color.getValue());
        assertEquals("Client 名称应为 Player#42", "Player#42", tank.playerName.getValue());

        System.out.println("======= [TDD] TankSpawnSystem: 配置Client坦克 通过 =======");
    }

    @Test
    public void testConfigureTank_differentIndices() {
        System.out.println("======= [TDD] TankSpawnSystem: 不同序号配置 =======");
        // index 5 → (300, 300), SKY
        TankBehaviour tank = createTestTank();
        TankSpawnSystem.configureTank(tank, 5, 99);

        assertEquals("index5 X", 300f, tank.x.getValue(), 0.01f);
        assertEquals("index5 Y", 300f, tank.y.getValue(), 0.01f);
        assertEquals("index5 颜色", Color.SKY, tank.color.getValue());
        assertEquals("Player#99", tank.playerName.getValue());

        System.out.println("======= [TDD] TankSpawnSystem: 不同序号配置 通过 =======");
    }

    // ══════════════ respawnTank ══════════════

    @Test
    public void testRespawnTank_resetsState() {
        System.out.println("======= [TDD] TankSpawnSystem: 复活重置状态 =======");
        // 用 LinkedHashMap 保证插入顺序
        Map<Integer, TankBehaviour> clientTanks = new LinkedHashMap<>();

        TankBehaviour hostTank = createTestTank();
        clientTanks.put(-1, hostTank);

        TankBehaviour tank = createTestTank();
        tank.isDead.setValue(true);
        tank.hp.setValue(0);
        tank.x.setValue(999f);
        tank.y.setValue(999f);
        clientTanks.put(10, tank);

        TankSpawnSystem.respawnTank(tank, 10, clientTanks);

        assertFalse("复活后 isDead 应为 false", tank.isDead.getValue());
        assertEquals("复活后 HP 应为 4", (Integer) 4, tank.hp.getValue());
        // owner=10 在映射中是第 2 个 (index=1)，出生点 (300, 150)
        assertEquals("复活后 X", 300f, tank.x.getValue(), 0.01f);
        assertEquals("复活后 Y", 150f, tank.y.getValue(), 0.01f);
        assertEquals("复活后颜色", Color.CYAN, tank.color.getValue());

        System.out.println("======= [TDD] TankSpawnSystem: 复活重置状态 通过 =======");
    }

    @Test
    public void testRespawnTank_unknownOwner() {
        System.out.println("======= [TDD] TankSpawnSystem: 未知owner复活 =======");
        Map<Integer, TankBehaviour> clientTanks = new HashMap<>();

        TankBehaviour tank = createTestTank();
        tank.isDead.setValue(true);
        tank.hp.setValue(0);

        // owner=99 不在映射中，应回退到 index=0
        TankSpawnSystem.respawnTank(tank, 99, clientTanks);

        assertFalse("isDead 应为 false", tank.isDead.getValue());
        assertEquals("HP 应为 4", (Integer) 4, tank.hp.getValue());
        assertEquals("回退到 index0 X", 150f, tank.x.getValue(), 0.01f);
        assertEquals("回退到 index0 Y", 150f, tank.y.getValue(), 0.01f);

        System.out.println("======= [TDD] TankSpawnSystem: 未知owner复活 通过 =======");
    }
}
