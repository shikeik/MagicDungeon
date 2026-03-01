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
    
    // 预制体工厂注册表（prefabId -> factory）
    private final Map<Integer, NetworkPrefabFactory> prefabRegistry = new HashMap<>();
    
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
     * 注册预制体工厂，Server 和 Client 两端都需要注册相同的 prefabId 对应的工厂。
     */
    public void registerPrefab(int prefabId, NetworkPrefabFactory factory) {
        prefabRegistry.put(prefabId, factory);
    }

    /**
     * 生成一个新的网络对象，并分配网络ID（旧接口，保留兼容）
     */
    public void spawn(NetworkObject obj) {
        if (transport == null || !transport.isServer()) {
            throw new IllegalStateException("只有服务器可以(或者在此模式下) Spawn 对象！");
        }
        int assignedId = nextNetworkId++;
        obj.setNetworkId(assignedId);
        networkObjects.put(assignedId, obj);
        // 为所有 Behaviour 绑定 Manager 引用
        for (NetworkBehaviour b : obj.getBehaviours()) {
            b.setManager(this);
        }
    }

    /**
     * 通过预制体ID创建并生成网络对象。
     * Server 端会创建本地实例，并即时广播 SpawnPacket(0x11) 给所有客户端，
     * 客户端收到后会通过相同 prefabId 的工厂自动派生本地副本。
     */
    public NetworkObject spawnWithPrefab(int prefabId) {
        if (transport == null || !transport.isServer()) {
            throw new IllegalStateException("只有服务器可以 Spawn 对象！");
        }
        NetworkPrefabFactory factory = prefabRegistry.get(prefabId);
        if (factory == null) {
            throw new IllegalArgumentException("未注册的预制体ID: " + prefabId);
        }

        // Server 端本地创建
        NetworkObject obj = factory.create();
        int assignedId = nextNetworkId++;
        obj.setNetworkId(assignedId);
        obj.setPrefabId(prefabId);
        networkObjects.put(assignedId, obj);
        // 为所有 Behaviour 绑定 Manager 引用
        for (NetworkBehaviour b : obj.getBehaviours()) {
            b.setManager(this);
        }

        // 广播 SpawnPacket 给所有客户端
        byte[] spawnPacket = buildSpawnPacket(assignedId, prefabId);
        transport.broadcast(spawnPacket);

        return obj;
    }

    /**
     * 构建 Spawn 封包：[0x11][networkId][prefabId]
     */
    private byte[] buildSpawnPacket(int networkId, int prefabId) {
        NetBuffer buffer = new NetBuffer();
        buffer.writeInt(0x11); // SpawnPacket 魔法头
        buffer.writeInt(networkId);
        buffer.writeInt(prefabId);
        return buffer.toByteArray();
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
        
        int dirtyCount = obj.countDirtyVariables();
        buffer.writeInt(dirtyCount); 
        
        // 遍历所有变量，如果被标记为脏则进行序列化
        java.util.List<NetworkVariable<?>> vars = obj.getNetworkVariables();
        for (int i = 0; i < vars.size(); i++) {
            NetworkVariable<?> var = vars.get(i);
            if (var.isDirty()) {
                // 写入当前变量的标识位 (Index)
                buffer.writeInt(i);
                // 将变量具体的二进制数值序列化到 buffer 中
                var.serialize(buffer);
            }
        }
        
        return buffer.toByteArray();
    }

    /**
     * Transport 收到字节流后回调此方法。
     * 是从字节码还原为内存逻辑对象的必经之路。
     */
    public void onReceiveData(byte[] payload) {
        NetBuffer inBuffer = new NetBuffer(payload);
        int packetType = inBuffer.readInt();
        
        // SpawnPacket: 客户端收到后自动通过预制体工厂派生本地实体
        if (packetType == 0x11) {
            int netId = inBuffer.readInt();
            int prefabId = inBuffer.readInt();
            
            NetworkPrefabFactory factory = prefabRegistry.get(prefabId);
            if (factory == null) {
                System.err.println("[NetworkManager] Client 收到 SpawnPacket 但未注册对应的预制体工厂: prefabId=" + prefabId);
                return;
            }
            
            // 通过工厂创建本地副本
            NetworkObject localObj = factory.create();
            localObj.setNetworkId(netId);
            localObj.setPrefabId(prefabId);
            // 为所有 Behaviour 绑定 Manager 引用
            for (NetworkBehaviour b : localObj.getBehaviours()) {
                b.setManager(this);
            }
            networkObjects.put(netId, localObj);
            
            System.out.println("[NetworkManager] Client 自动派生实体成功: netId=" + netId + ", prefabId=" + prefabId);
            return;
        }
        
        // RPC 包: ServerRpc(0x20) 或 ClientRpc(0x21)
        if (packetType == 0x20 || packetType == 0x21) {
            handleRpcPacket(inBuffer);
            return;
        }
        
        // 状态同步包
        if (packetType == 0x10) {
            int netId = inBuffer.readInt();
            int modifiedCount = inBuffer.readInt();
            
            NetworkObject localObj = networkObjects.get(netId);
            if (localObj == null) {
                System.err.println("[NetworkManager] 本地找不到对应的实体, 无法执行同步更新: ID=" + netId);
                return;
            }
            
            java.util.List<NetworkVariable<?>> vars = localObj.getNetworkVariables();
            for (int i = 0; i < modifiedCount; i++) {
                int varIndex = inBuffer.readInt();
                if (varIndex >= 0 && varIndex < vars.size()) {
                    NetworkVariable<?> var = vars.get(varIndex);
                    // 执行反序列化，覆盖本地值
                    var.deserialize(inBuffer);
                } else {
                    System.err.println("[NetworkManager] 反序列化失败，越界的变量索引: " + varIndex);
                    // 由于协议强顺序性，一旦发现反序列化索引错乱，后面整个包的读取都将失效，必须直接阻断或者丢弃。
                    break; 
                }
            }
        }
    }
    
    /**
     * 构建并发送 RPC 封包。
     * 封包格式: [packetType(0x20/0x21)][netId][behaviourIndex][methodNameLength][methodNameBytes][argCount][arg1Type][arg1Value]...
     * @param packetType 0x20=ServerRpc, 0x21=ClientRpc
     */
    public void sendRpcPacket(int packetType, int netId, int behaviourIndex, String methodName, Object... args) {
        NetBuffer buffer = new NetBuffer();
        buffer.writeInt(packetType);
        buffer.writeInt(netId);
        buffer.writeInt(behaviourIndex);
        buffer.writeString(methodName);
        
        // 序列化参数
        buffer.writeInt(args.length);
        for (Object arg : args) {
            serializeRpcArg(buffer, arg);
        }
        
        byte[] payload = buffer.toByteArray();
        
        if (packetType == 0x20) {
            // ServerRpc: Client -> Server
            if (transport != null) {
                transport.sendToServer(payload);
            }
        } else if (packetType == 0x21) {
            // ClientRpc: Server -> Client
            if (transport != null) {
                transport.broadcast(payload);
            }
        }
    }

    /**
     * 将单个 RPC 参数序列化到 buffer 中。
     * 类型标记: 1=int, 2=float, 3=boolean, 4=String
     */
    private void serializeRpcArg(NetBuffer buffer, Object arg) {
        if (arg instanceof Integer) {
            buffer.writeInt(1);
            buffer.writeInt((Integer) arg);
        } else if (arg instanceof Float) {
            buffer.writeInt(2);
            buffer.writeFloat((Float) arg);
        } else if (arg instanceof Boolean) {
            buffer.writeInt(3);
            buffer.writeBoolean((Boolean) arg);
        } else if (arg instanceof String) {
            buffer.writeInt(4);
            buffer.writeString((String) arg);
        } else {
            throw new IllegalArgumentException("RPC 暂不支持此参数类型: " + (arg != null ? arg.getClass() : "null"));
        }
    }

    /**
     * 从 buffer 中反序列化单个 RPC 参数
     */
    private Object deserializeRpcArg(NetBuffer buffer) {
        int typeTag = buffer.readInt();
        switch (typeTag) {
            case 1: return buffer.readInt();
            case 2: return buffer.readFloat();
            case 3: return buffer.readBoolean();
            case 4: return buffer.readString();
            default: throw new IllegalArgumentException("未知的 RPC 参数类型标记: " + typeTag);
        }
    }

    /**
     * 处理接收到的 RPC 封包，反射调用目标方法
     */
    private void handleRpcPacket(NetBuffer inBuffer) {
        int netId = inBuffer.readInt();
        int behaviourIndex = inBuffer.readInt();
        String methodName = inBuffer.readString();
        int argCount = inBuffer.readInt();
        
        Object[] args = new Object[argCount];
        for (int i = 0; i < argCount; i++) {
            args[i] = deserializeRpcArg(inBuffer);
        }
        
        // 在本地找到对应的实体和行为组件
        NetworkObject localObj = networkObjects.get(netId);
        if (localObj == null) {
            System.err.println("[NetworkManager] RPC 目标实体不存在: netId=" + netId);
            return;
        }
        
        java.util.List<NetworkBehaviour> behaviours = localObj.getBehaviours();
        if (behaviourIndex < 0 || behaviourIndex >= behaviours.size()) {
            System.err.println("[NetworkManager] RPC 目标行为组件索引越界: " + behaviourIndex);
            return;
        }
        
        NetworkBehaviour target = behaviours.get(behaviourIndex);
        
        // 通过反射找到并调用目标方法
        try {
            // 构建参数类型数组用于精确匹配方法
            Class<?>[] paramTypes = new Class<?>[argCount];
            for (int i = 0; i < argCount; i++) {
                if (args[i] instanceof Integer) paramTypes[i] = int.class;
                else if (args[i] instanceof Float) paramTypes[i] = float.class;
                else if (args[i] instanceof Boolean) paramTypes[i] = boolean.class;
                else if (args[i] instanceof String) paramTypes[i] = String.class;
                else paramTypes[i] = args[i].getClass();
            }
            
            java.lang.reflect.Method method = target.getClass().getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (Exception e) {
            System.err.println("[NetworkManager] RPC 反射调用失败: " + methodName + " -> " + e.getMessage());
            e.printStackTrace();
        }
    }

    public NetworkObject getNetworkObject(int id) {
        return networkObjects.get(id);
    }
}
