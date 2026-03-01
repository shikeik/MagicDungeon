fix: 修复UDP传输闪退(异步Spawn NPE)与中文乱码

- 修复 UDP 模式下 SpawnPacket 异步到达导致 clientManager.getNetworkObject() 返回 null 的 NPE
- 增加 UDP 模式下的轮询等待逻辑，确保 Client 完成 Spawn 派生后再获取引用
- 重建 UdpSocketTransport.java 修复中文注释乱码（文件编码损坏）
- 回退传输层切换为编译期常量 USE_UDP（运行时切换非必需，改完重启即可）
- 客户端子弹碰撞检测修复（右侧子弹穿透不消失）
- 沙盒字体改用 FontUtils.generate() 支持中文显示
- Transport 接口新增 setReceiveCallback() 方法
- LocalMemoryTransport 从 manager 引用改为 receiveCallback
- NetworkManager.setTransport() 自动注册接收回调，消除手动 setManager() 接线
- 新增 UdpSocketTransport：基于 Java DatagramSocket 的 UDP 传输层
  - 握手协议 [0xFF×4]、长度前缀封包、异步守护线程接收
- 沙盒新增 USE_UDP 开关与 HUD 传输模式显示
- 新增 UdpSocketTransportTest (3个TDD测试：广播/上行/全链路Spawn+Sync)
- 全量测试 103/103 通过