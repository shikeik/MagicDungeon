feat: P4 TankBehaviour 增加 ServerRpc 输入通道 (rpcMoveInput + rpcFireInput)

- TankBehaviour 新增 @ServerRpc rpcMoveInput(dx, dy): Client 上报移动方向, Server 执行位移
- TankBehaviour 新增 @ServerRpc rpcFireInput(): Client 上报开火请求, Server 标记 pendingFire
- 新增 transient pendingFire 标志, 供沙盒层 update 消费
- 编译通过, 60 测试全绿