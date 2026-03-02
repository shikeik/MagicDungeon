fix(netcode): 地图种子改为 Server 权威下发，修复直连/手动加入时墙体不同步

- 根因: 双端各自猜测地图种子, 直连路径 roomName 不一致导致 hashCode 不同
- TankBehaviour: 新增 @ClientRpc rpcSyncMapSeed(int) + volatile 接收字段
- OnlineScreen Server: 新客户端连入时通过 hostTank 广播地图种子
- OnlineScreen Client: 删除自行猜测种子, 新增 checkMapSeedSync() 每帧检查并重建地图