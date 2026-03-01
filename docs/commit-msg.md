feat: v0.6.5 Despawn网络广播 + 跨进程UDP坦克沙盒

- 新增 Despawn 机制: NetworkManager.despawn(netId) + 0x12 封包双端清理
- 新增 DespawnTest (2个TDD用例, 全绿)
- 新增跨进程 UDP 坦克沙盒: NetcodeUdpServerScreen + NetcodeUdpClientScreen
  - Server 状态机: WAITING_FOR_CLIENT → PLAYING (等待握手后 Spawn)
  - Client 状态机: CONNECTING → PLAYING (轮询 SpawnPacket 到达)
- 提取共享类消除重复: Bullet / TankBehaviour / TankSandboxUtils
- 重构 NetcodeTankSandboxScreen 使用共享工具类
- 新增查询 API: getNetworkObjectCount / getAllNetworkObjects / getClientCount
- 修复 registerPrefab 时序: 必须在 connect/startServer 之前注册, 避免异步收包先于工厂
- TestSelectionScreen 新增「Netcode UDP服务端」「Netcode UDP客户端」入口
- 版本升级 0.6.4 → 0.6.5, 报表 #16