package com.goldsprite.gdengine.netcode;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.goldsprite.CLogAssert;
import com.goldsprite.GdxTestRunner;

@RunWith(GdxTestRunner.class)
public class DualEndpointIntegrationTest {

    private static class DummyPlayerBehaviour extends NetworkBehaviour {
        public NetworkVariable<Float> hp = new NetworkVariable<>(100f);
    }

    @Test
    public void testServerToClientStateSync() {
        System.out.println("======= 开始双端自动同步TDD驱动测试 =======");

        // ------------- 1. 搭建 Server -------------
        NetworkManager serverManager = new NetworkManager();
        LocalMemoryTransport serverTransport = new LocalMemoryTransport(true);
        serverTransport.setManager(serverManager);
        serverManager.setTransport(serverTransport);

        // ------------- 2. 搭建 Client -------------
        NetworkManager clientManager = new NetworkManager();
        LocalMemoryTransport clientTransport = new LocalMemoryTransport(false);
        clientTransport.setManager(clientManager);
        clientManager.setTransport(clientTransport);

        serverTransport.connectToPeer(clientTransport);

        // ------------- 3. 业务层准备：Server在场景里创建一个坦克 -------------
        NetworkObject serverPlayer = new NetworkObject(1);
        DummyPlayerBehaviour serverLogic = new DummyPlayerBehaviour();
        serverPlayer.addComponent(serverLogic);
        serverManager.spawn(serverPlayer);

        // 为客户端也伪造好这个副本接收端
        NetworkObject clientPlayerMock = new NetworkObject(1);
        DummyPlayerBehaviour clientLogic = new DummyPlayerBehaviour();
        clientPlayerMock.addComponent(clientLogic);
        
        try {
            java.lang.reflect.Field field = NetworkManager.class.getDeclaredField("networkObjects");
            field.setAccessible(true);
            java.util.Map<Integer, NetworkObject> map = (java.util.Map<Integer, NetworkObject>) field.get(clientManager);
            map.put(1, clientPlayerMock);
            
            java.lang.reflect.Field bfield = NetworkObject.class.getDeclaredField("behaviours");
            bfield.setAccessible(true);
            java.util.List<NetworkBehaviour> bs = (java.util.List<NetworkBehaviour>) bfield.get(clientPlayerMock);
            for(NetworkBehaviour b : bs) b.internalAttach(clientPlayerMock);
        } catch (Exception e) {}

        // ------------- 4. 驱动 Server 状态同步 -------------
        int expectedPreRecv = clientTransport.messagesReceived;
        
        serverLogic.hp.setValue(80f);
        System.out.println("扣除了Server上的血量，当前Server HP: " + serverLogic.hp.getValue());

        // 执行网络管理同步主循环，此时产生Dirty应该被序列化发包
        serverManager.tick();

        int afterRecv = clientTransport.messagesReceived;
        CLogAssert.assertTrue("当Server数据更新时必须能收到包", afterRecv > expectedPreRecv);   
        
        System.out.println("收到包之前客户端的HP: " + clientLogic.hp.getValue());

        clientManager.tick(); // 触发网络层解包与应用

        CLogAssert.assertEquals("自动同步流：Client的数据应该被同步，从而与Server的数据保持严格一致", 80f, clientLogic.hp.getValue());

        System.out.println("======= 测试双端自动同步完成 =======");
    }
}
