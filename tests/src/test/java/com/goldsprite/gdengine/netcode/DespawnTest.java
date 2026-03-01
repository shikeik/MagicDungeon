package com.goldsprite.gdengine.netcode;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.goldsprite.CLogAssert;
import com.goldsprite.GdxTestRunner;

/**
 * TDD 驱动：验证 Server 调用 despawn 后，本地移除实体并广播 DespawnPacket(0x12)，
 * Client 收到后自动清理本地副本。
 */
@RunWith(GdxTestRunner.class)
public class DespawnTest {

    private static class SimpleBehaviour extends NetworkBehaviour {
        public NetworkVariable<Float> x = new NetworkVariable<>(0f);
    }

    /**
     * 测试1：Server despawn 后，Server 本地注册表应移除该实体，Client 端也应移除。
     */
    @Test
    public void testDespawnRemovesBothSides() {
        System.out.println("======= [TDD] Despawn 双端清理测试 =======");

        // 搭建双端
        NetworkManager serverManager = new NetworkManager();
        LocalMemoryTransport serverTransport = new LocalMemoryTransport(true);
        serverManager.setTransport(serverTransport);

        NetworkManager clientManager = new NetworkManager();
        LocalMemoryTransport clientTransport = new LocalMemoryTransport(false);
        clientManager.setTransport(clientTransport);
        serverTransport.connectToPeer(clientTransport);

        int PREFAB_ID = 1;
        NetworkPrefabFactory factory = () -> {
            NetworkObject obj = new NetworkObject();
            obj.addComponent(new SimpleBehaviour());
            return obj;
        };
        serverManager.registerPrefab(PREFAB_ID, factory);
        clientManager.registerPrefab(PREFAB_ID, factory);

        // Spawn
        NetworkObject serverObj = serverManager.spawnWithPrefab(PREFAB_ID);
        int netId = (int) serverObj.getNetworkId();

        CLogAssert.assertTrue("Spawn 后 Server 应持有该实体", serverManager.getNetworkObject(netId) != null);
        CLogAssert.assertTrue("Spawn 后 Client 应自动派生该实体", clientManager.getNetworkObject(netId) != null);

        // Despawn
        serverManager.despawn(netId);

        CLogAssert.assertTrue("Despawn 后 Server 应移除该实体", serverManager.getNetworkObject(netId) == null);
        CLogAssert.assertTrue("Despawn 后 Client 应自动移除该实体", clientManager.getNetworkObject(netId) == null);

        System.out.println("======= [TDD] Despawn 双端清理通过 =======");
    }

    /**
     * 测试2：Despawn 后，剩余实体的 tick 同步不受影响。
     */
    @Test
    public void testDespawnDoesNotAffectOtherEntities() {
        System.out.println("======= [TDD] Despawn 不影响其它实体测试 =======");

        NetworkManager serverManager = new NetworkManager();
        LocalMemoryTransport serverTransport = new LocalMemoryTransport(true);
        serverManager.setTransport(serverTransport);

        NetworkManager clientManager = new NetworkManager();
        LocalMemoryTransport clientTransport = new LocalMemoryTransport(false);
        clientManager.setTransport(clientTransport);
        serverTransport.connectToPeer(clientTransport);

        int PREFAB_ID = 1;
        NetworkPrefabFactory factory = () -> {
            NetworkObject obj = new NetworkObject();
            obj.addComponent(new SimpleBehaviour());
            return obj;
        };
        serverManager.registerPrefab(PREFAB_ID, factory);
        clientManager.registerPrefab(PREFAB_ID, factory);

        // Spawn 两个实体
        NetworkObject obj1 = serverManager.spawnWithPrefab(PREFAB_ID);
        NetworkObject obj2 = serverManager.spawnWithPrefab(PREFAB_ID);
        int id1 = (int) obj1.getNetworkId();
        int id2 = (int) obj2.getNetworkId();

        // Despawn 第一个
        serverManager.despawn(id1);

        CLogAssert.assertTrue("实体1 应已被移除 (Server)", serverManager.getNetworkObject(id1) == null);
        CLogAssert.assertTrue("实体2 应仍然存在 (Server)", serverManager.getNetworkObject(id2) != null);
        CLogAssert.assertTrue("实体1 应已被移除 (Client)", clientManager.getNetworkObject(id1) == null);
        CLogAssert.assertTrue("实体2 应仍然存在 (Client)", clientManager.getNetworkObject(id2) != null);

        // 修改实体2 的状态并 tick
        SimpleBehaviour serverLogic2 = (SimpleBehaviour) obj2.getBehaviours().get(0);
        serverLogic2.x.setValue(999f);
        serverManager.tick();

        SimpleBehaviour clientLogic2 = (SimpleBehaviour) clientManager.getNetworkObject(id2).getBehaviours().get(0);
        CLogAssert.assertEquals("Despawn 后实体2 的同步应正常工作", 999f, clientLogic2.x.getValue());

        System.out.println("======= [TDD] Despawn 不影响其它实体通过 =======");
    }
}
