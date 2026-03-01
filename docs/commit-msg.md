feat: P5+P6 统一联机对战屏幕 + 多客户端动态生成

- 新增 NetcodeTankOnlineScreen: 合并原 Server/Client 双屏为统一入口
- CONFIG 状态: Tab切换角色(Server/Client), [1]编辑IP, [2]编辑端口, Enter启动
- WAITING 状态: Server等待客户端连接, Client等待SpawnPacket
- PLAYING 状态: Server驱动Tick+物理+碰撞, Client发送ServerRpc输入
- 多客户端支持: clientTanks Map动态管理, 连接时自动Spawn, 6色轮换
- Host模式: Server自身坦克(ownerClientId=-1)由WASD+J控制
- 碰撞泛化: 遍历所有坦克而非硬编码p1/p2
- TestSelectionScreen 添加"Netcode坦克联机对战"菜单项, 旧入口标记(旧)
- 编译通过, 91测试全绿(100%通过率)