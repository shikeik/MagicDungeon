feat: P1~P3 引擎层双端对战基建 — clientId传播 + 连接事件 + 所有权模型

- TransportReceiveCallback.onReceiveData 增加 clientId 参数，Server 收包可识别来源
- UdpSocketTransport 握手升级: Server 回复 [0xFE×4][clientId]，Client 自动缓存分配的 clientId
- 新增 NetworkConnectionListener 接口 (onClientConnected / onClientDisconnected)
- Transport 接口新增 setConnectionListener()，UdpSocket / LocalMemory 均实现
- NetworkManager.setTransport() 自动挂载连接监听，Client 端自动设置 localClientId
- NetworkManager 新增 setConnectionListener() 供游戏层监听连接事件
- NetworkObject 新增 ownerClientId 字段 (-1=Server拥有, >=0=Client拥有)
- spawnWithPrefab 新增重载 spawnWithPrefab(prefabId, ownerClientId)
- SpawnPacket 格式扩展: [0x11][netId][prefabId][ownerClientId]
- Client 收到 SpawnPacket 时自动判定 isLocalPlayer (ownerClientId == localClientId)
- 适配 UdpSocketTransportTest lambda 签名 + NetworkManagerTest MockTransport
- 全量 60 测试用例通过