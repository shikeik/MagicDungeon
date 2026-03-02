refactor(netcode): 重构 SupabaseLobbyScreen 从轮询驱动到 Presence 事件驱动

- SupabaseLobbyScreen: 移除 RoomManager 和 10s 轮询定时器，接入 PresenceLobbyManager
- SupabaseLobbyScreen: 建房流程打通 (获取IP→publishRoom→跳转游戏)
- SupabaseLobbyScreen: 加入流程打通 (读取hostIp:port→跳转游戏)
- SupabaseLobbyScreen: 新增连接状态指示器、房间状态标签、按钮禁用态
- NetcodeTankOnlineScreen: 新增 preConfigureAsHost/preConfigureAsClient 静态预配置接口
- NetcodeTankOnlineScreen: create() 中检测预配置，跳过 CONFIG 阶段直接启动网络