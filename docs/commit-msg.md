fix(netcode): 修复大厅房间残留 + 客户端重连同步全失效

- SupabaseLobbyScreen: 重写 show()，从游戏返回大厅时自动 unpublishRoom 清除残留房间
- UdpSocketTransport: broadcast() 仅向 activeClientIds 发送，不再向已断开的旧地址发包
- ReliableUdpTransport: 引入按 clientId 隔离的 ReceiveSequenceTracker，修复重连客户端可靠包被当旧包丢弃
- ReliableUdpTransport: setConnectionListener 拦截连接/断开事件，自动创建/清理追踪器
- ReliableUdpTransport: checkHeartbeatTimeouts 同步清理断开客户端的追踪器
- ReliableUdpTransport: disconnect() 清空所有按客户端隔离的状态