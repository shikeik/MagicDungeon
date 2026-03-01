package com.goldsprite.gdengine.netcode;

import java.util.HashMap;
import java.util.Map;

/**
 * 网络层管理器，负责统筹所有的 NetworkObject 生命周期（Spawn / Despawn），
 * 并在每一帧（Net Tick）中汇总脏数据，通过 Transport 接口将数据序列化下发到物理网络层。
 */
public class NetworkManager {
    
    // 网络对象注册表（使用统一分配的网络ID作为Key）
    private final Map<Integer, NetworkObject> networkObjects = new HashMap<>();
    
    // 自增网络ID分配器
    private int nextNetworkId = 1;
    
    // 传输层
    private Transport transport;

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    /**
     * 生成一个新的网络对象，并分配网络ID
     */
    public void spawn(NetworkObject obj) {
        if (transport == null || !transport.isServer()) {
            throw new IllegalStateException("只有服务器可以(或者在此模式下) Spawn 对象！");
        }
        int assignedId = nextNetworkId++;
        networkObjects.put(assignedId, obj);
        // 这里理论上要向所有客户端发送一条 SpawnRPC 消息...（简化处理，之后在数据包机制实现）
    }

    /**
     * 服务器专用心跳机制
     */
    public void tick() {
        if (transport == null || !transport.isServer()) return;

        for (Map.Entry<Integer, NetworkObject> entry : networkObjects.entrySet()) {
            NetworkObject obj = entry.getValue();
            int dirties = obj.countDirtyVariables();
            if (dirties > 0) {
                // 如果发现该实体有脏数据，将其打包序列化发送给客户端。
                byte[] payload = serializeObjectState(entry.getKey(), obj);
                transport.broadcast(payload);
                // 重置所有对象的脏检查位
                obj.clearAllDirtyVariables();
            }
        }
    }
    
    /**
     * 将带有脏变标志的对象转成字节
     */
    private byte[] serializeObjectState(int networkId, NetworkObject obj) {
        NetBuffer buffer = new NetBuffer();
        // 封包头：先写一个魔法数字标识这是一个【状态同步包】 (暂定0x10表示StatusSync)
        buffer.writeInt(0x10);
        buffer.writeInt(networkId);
        // 这里暂时只是象征性地写入实体发生脏变的数据条数，完整版本需要对每个 NetworkVariable 进行序列化映射出去了。
        buffer.writeInt(obj.countDirtyVariables()); 
        
        return buffer.toByteArray();
    }

    /**
     * Transport 收到字节流后回调此方法。
     * 是从字节码还原为内存逻辑对象的必经之路。
     */
    public void onReceiveData(byte[] payload) {
        NetBuffer inBuffer = new NetBuffer(payload);
        int packetType = inBuffer.readInt();
        
        // 如果是状态更新包
        if (packetType == 0x10) {
            int netId = inBuffer.readInt();
            int modifiedCount = inBuffer.readInt();
            // 在实际代码里，这里会把具体改变的 float / int 更新到对应的 NetworkVirtual 里
            System.out.println("[NetworkManager] 接收到同步包! 实体ID: " + netId + " 变更变量数: " + modifiedCount);
            
            // 假设我们找得到本地副本，这里要进行数据覆盖（供TDD验证接收回调畅通）
            NetworkObject localObj = networkObjects.get(netId);
            if (localObj != null) {
                // ... 模拟应用了这些变更
            }
        }
    }
    
    public NetworkObject getNetworkObject(int id) {
        return networkObjects.get(id);
    }
}
