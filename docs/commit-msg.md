refactor(netcode): 提取 TankGameLogic 统一双端逻辑 + 修复本地玩家嵌墙

- 新增 TankGameLogic: Sandbox 和 Online 共用 serverTick/clientReconcileTick/clientBulletTick
- 统一输入约定: 所有坦克走 pendingMoveX/Y + pendingFire 缓存-消费模式
- SandboxScreen: 重写，全部逻辑委托 TankGameLogic (281→18​7行)
- OnlineScreen: updateServerLogic/updateClientLogic 委托 TankGameLogic (571→500行)
- 修复本地玩家嵌墙: disableSmoothForLocalPlayer 关闭本地 x/y 插值，远端保留
- 删除重复的 normalizeDir()、respawnTank() 包装方法