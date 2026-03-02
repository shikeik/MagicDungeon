feat(netcode): 实装 Supabase Realtime Presence 云大厅核心层

- 新增 PhoenixChannel: 封装 WebSocket + Phoenix 协议 (phx_join/heartbeat/presence track/untrack)
- 新增 PresenceLobbyManager: 替代旧 RoomManager，零数据库 I/O 的实时房间列表同步
- 新增 PresenceRoomInfo: 纯内存房间元数据模型，替代旧 RoomModel 数据库映射
- 扩展 SupabaseConfig: 新增 REALTIME_URL 和 LOBBY_CHANNEL 常量
- 新增 PresenceLobbyManagerTest: 验证 WebSocket 连接/加入频道/发布房间/同步接收
- 修复 GdxTestRunner: postRunnable 在测试环境中立即执行，解决异步回调不触发问题
- 引入 Java-WebSocket 1.5.7 依赖
- 新增策划文档: 8_0_Supabase_Presence云大厅重构策划案