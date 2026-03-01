package com.goldsprite.gdengine.netcode;

import com.goldsprite.CLogAssert;
import com.goldsprite.GdxTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class DualEndpointIntegrationTest {

    // 依然用最简坦克测试它的行为托管
    private static class DummyPlayerBehaviour extends NetworkBehaviour {
        public NetworkVariable<Float> hp = new NetworkVariable<>(100f);
    }

    /**
     * 模拟两端（A为Server，B为Client）在同一JVM内存里进行双向测试的操作！
     * 将之前的底盘组件（Transform, NetBuffer, NetworkManager）整个串联。
     */
    @Test
    public void testServerToClientStateSync() {
        System.out.println("======= 开始内存级双端联机全链路测试 =======");
        
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
        
        // --- 核心！网线插上：把 Server 和 Client 连上 ---
        serverTransport.connectToPeer(clientTransport);
        
        // ------------- 3. 业务层准备：Server在场景里创建一个坦克 -------------
        NetworkObject serverPlayer = new NetworkObject(0);
        DummyPlayerBehaviour logic = new DummyPlayerBehaviour();
        serverPlayer.addComponent(logic);
        
        // 调用Spawn，由托管器分配Id = 1
        serverManager.spawn(serverPlayer);
        
        // 为客户端也伪造好这个副本接收端（在真实的系统里是由 Spawn 封包来完成的实例化）
        NetworkObject clientPlayerMock = new NetworkObject(1);
        clientManager.getNetworkObject(0); // 假装通过某种方式存住了(实际应该用暴露的缓存API，TDD暂用断言模拟表现)
        
        // ------------- 4. 驱动 Server 状态同步 -------------
        int expectedPreRecv = clientTransport.messagesReceived;
        
        // 使发生伤害扣血
        logic.hp.setValue(80f);
        
        // Server 的游戏主循环刷新
        serverManager.tick();
        
        int afterRecv = clientTransport.messagesReceived;
        CLogAssert.assertTrue("当Server发生数据改变调用tick()后，直连的Client应该从网关中收到了一条包", afterRecv > expectedPreRecv);
        
        System.out.println("======= 双端内存通信流验证完毕 =======");
    }
}
