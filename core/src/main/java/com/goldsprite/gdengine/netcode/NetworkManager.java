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
                // （在这个沙盒模型中，我们先直接打印或记录下来，后面补上序列化方法）
                byte[] payload = serializeObjectState(entry.getKey(), obj);
                transport.broadcast(payload);
                // 重置所有对象的脏检查位
                obj.clearAllDirtyVariables();
            }
        }
    }
    
    /**
     * 将带有脏变标志的对象转成字节（占位，之后需要真实序列化）
     */
    private byte[] serializeObjectState(int networkId, NetworkObject obj) {
        // [4 byte NetworkId] + [n byte Object 状态数据]
        // 现阶段用一个最简长度代表，后面会用ByteBuffer或者DataOutputStream
        return new byte[]{ (byte)networkId }; 
    }
    
    public NetworkObject getNetworkObject(int id) {
        return networkObjects.get(id);
    }
}
