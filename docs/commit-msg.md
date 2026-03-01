feat: v0.6.4 Transport策略模式重构与UDP真实传输层

- 新增 TransportReceiveCallback 统一回调接口
- Transport 接口新增 setReceiveCallback() 方法
- LocalMemoryTransport 从 manager 引用改为 receiveCallback
- NetworkManager.setTransport() 自动注册接收回调，消除手动 setManager() 接线
- 新增 UdpSocketTransport：基于 Java DatagramSocket 的 UDP 传输层
  - 握手协议 [0xFF×4]、长度前缀封包、异步守护线程接收
- 沙盒新增 USE_UDP 开关与 HUD 传输模式显示
- 新增 UdpSocketTransportTest (3个TDD测试：广播/上行/全链路Spawn+Sync)
- 全量测试 103/103 通过