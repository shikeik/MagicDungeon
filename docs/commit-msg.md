fix(netcode): 修复帧率不匹配导致移动速度异常+回扯, 新增客户端预测模式

- pendingMoveX/Y 改为持续摇杆状态(不清零), 服务端每帧应用最后已知方向
- 客户端始终发送方向(含 0,0 停止), 解决高帧率服务端移动被稀释到 25% 的问题
- NetworkVariable 新增 clientPrediction 模式: 反序列化不覆盖本地值, >60px 才硬拉
- 客户端预测加入本地碰撞(clampToBoundary + pushOutOfWalls)
- 彻底删除 disableSmoothForLocalPlayer/enableSmoothForRemotePlayer 残留代码