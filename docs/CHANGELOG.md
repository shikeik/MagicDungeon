# 更新日志 (Changelog)

本项目的所有显著更改都将记录在此文件中。
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，并且本项目遵循 [语义化版本控制 (Semantic Versioning)](https://semver.org/lang/zh-CN/)。

## [0.8.2] - 2026-03-03

### 修复 (Fixed)

- **帧率不匹配导致移动速度异常 + 严重回扯**：PC 240fps / Android 60fps 下，坦克实际速度只有预期的 25% 甚至更低
  - 根因：`serverTickTanks()` 每次消费 `pendingMoveX/Y` 后清零，而客户端每帧只发 1 次 RPC。服务端高帧率下，仅收到 RPC 那 1 帧有输入，其余帧 pendingMove=0，移动被大幅稀释
  - 修复：`pendingMoveX/Y` 改为**持续摇杆状态**（不清零），服务端每帧都应用最后已知方向
  - 客户端始终发送方向（含 `0,0` 停止指令），确保松手后服务端立刻停止

- **客户端预测被服务端频繁拉回**：正常延迟下本地玩家仍出现明显回扯
  - 修复：`NetworkVariable` 新增 `clientPrediction` 模式 — 反序列化不直接覆盖本地值，仅在偏差 >60px 时硬拉，微偏差通过 `reconcileTick` 漂移修正
  - 客户端预测加入本地碰撞（`clampToBoundary` + `pushOutOfWalls`），预测路径更贴近服务端

- **彻底移除位置同步插值**：删除 `disableSmoothForLocalPlayer()` / `enableSmoothForRemotePlayer()` 残留代码

---

## [0.8.1] - 2026-03-03

### 修复 (Fixed)

- **地图障碍渲染不同步**：直连 / 手动输入加入时，客户端墙体布局与服务端不一致
  - 根因：地图种子由双端各自猜测（直连路径 roomName 不同 → hashCode 不同 → 墙体布局完全不同）
  - 修复：Server 通过 `rpcSyncMapSeed` ClientRpc 权威下发种子，Client 不再自行生成，等待收到后重建地图
  - 新增 `TankBehaviour.rpcSyncMapSeed(int)` + `checkMapSeedSync()` 每帧检查

---

## [0.8.0] - 2026-03-03

### 重构 (Refactored)

- **提取 TankGameLogic 共享逻辑层**：Sandbox（纯内存）和 Online（真 UDP）场景共用同一套游戏规则
  - 新增 `TankGameLogic.java` (198行)：`serverTick()` 统一驱动移动/开火/死亡/子弹/碰撞，`clientReconcileTick()` + `clientBulletTick()` 统一客户端逻辑
  - 所有坦克输入（Host/Remote/Sandbox）统一为 `pendingMoveX/Y` + `pendingFire` 的缓存-消费模式
  - `normalizeDir()` 收拢为 `TankGameLogic` 静态方法，删除两处重复定义
  - SandboxScreen: 281→187 行 (↓33%)，OnlineScreen: 571→500 行 (↓12%)

### 修复 (Fixed)

- **本地玩家嵌墙/穿墙**：客户端本地玩家关闭 x/y 平滑插值，服务端修正值（已 pushOutOfWalls）直接生效
  - 根因：插值沿直线路径过渡，完全忽略墙体碰撞，导致坦克视觉上嵌入墙中再滑出
  - 修复：`TankGameLogic.disableSmoothForLocalPlayer()` 关闭本地玩家 x/y 的 `smoothReconciliation`，远端玩家保留插值

---

## [0.7.9] - 2026-03-03

### 修复 (Fixed)

- **房主 ESC 返回大厅后房间仍残留在 Presence 列表**
  - 根因: `replaceScreen()` 只 hide 大厅屏幕（压栈），不 dispose，`lobbyManager` 保持 Presence 连接且房间仍处于发布状态
  - 修复: SupabaseLobbyScreen 重写 `show()`，从游戏屏幕返回时自动调用 `lobbyManager.unpublishRoom()` 并清空 `myRoomInfo`，恢复建房按钮状态

- **[严重] 客户端断开后重连房间无法同步位置和子弹（实体 Spawn 正常但 RPC 全失效）**
  - 根因 1: `ReliableUdpTransport` 服务器端仅有一个全局 `ReceiveSequenceTracker`，所有客户端共享同一序列号空间。重连客户端的可靠包从 seq=0 重新开始，但追踪器记住了旧客户端的 `highestAccepted`，导致新客户端的所有 ServerRpc（移动/开火）被判定为「旧包」而静默丢弃
  - 根因 2: `UdpSocketTransport.broadcast()` 向所有历史客户端地址（含已断开的）发包，浪费带宽且向死地址发送数据
  - 修复: 引入 `Map<Integer, ReceiveSequenceTracker> clientReceiveTrackers` 按 clientId 隔离序列号追踪；`broadcast()` 仅向 `activeClientIds` 发送；客户端连接/断开/心跳超时时同步创建/清理追踪器状态

- **跨平台变量注册顺序不一致导致安卓-PC联机数据全错位**
  - `Class.getDeclaredFields()` 在不同 JVM（HotSpot vs Android ART）返回字段顺序不同
  - 导致 varIndex 错位：PC 端 `varIndex=0` 是 x 坐标，安卓端 `varIndex=0` 可能是 color
  - 表现为：HP=几千万、颜色乱闪、只有炮管没身体、双端完全无法同步
  - 修复：`autoRegisterNetworkVariables()` 按字段名字母序排序后再注册，保证一致

- **NetBuffer 防御性安全升级**
  - 写入端自动扩容（初始 1024B，按需翻倍，上限 64KB），消除 BufferOverflowException
  - `readString()` 增加长度上限校验（MAX_STRING_LENGTH=4096），防止损坏数据导致 OOM 崩溃
  - 所有 read 方法增加 `remaining()` 前置检查，不足时抛出诊断异常 `NetBufferUnderflowException`
  - 新增 `remaining()` 工具方法

- **NetworkManager 反序列化崩溃防护**
  - `onReceiveData()` 增加 try-catch 包裹，单个损坏封包不再杀死 IO 接收线程
  - 状态同步包 `modifiedCount` 增加上限校验（≤256），拦截垃圾数据
  - 未找到实体时降级为静默跳过（Unreliable 先于 Spawn 到达属正常现象）

- **ReliableUdpTransport 接收线程保护**
  - `onRawReceive()` 增加 try-catch，防止异常杀死接收线程
  - UdpSocketTransport 接收循环内 `processReceivedData()` 增加异常隔离

- **ReceiveSequenceTracker 公网乱序包丢弃修复**
  - 旧实现仅用"最高水位"判断，公网乱序包（如 seq=3 先到, seq=2 后到）会被永久丢弃
  - 新实现使用 256 位滑动窗口 + 位域去重，允许窗口内任意乱序包被正确接收
  - 修复安卓公网连 PC 时可靠包（Spawn/RPC）静默丢失的问题

### 新增 (Added)

- **NetworkManager 可配置 Tick Rate**
  - 新增 `setTickRate(hz)` / `getTickRate()` / `getTickInterval()` API
  - 默认 60Hz，支持 1~128Hz 范围动态调整
  - 采用累加器模式 `tick(delta)` 替代旧的每帧 `tick()`，频率稳定不受帧率波动影响
  - 保留无参 `tick()` 向后兼容

- **NetworkVariable 平滑调和模式 (Server Reconciliation)**
  - 新增 `enableSmooth(speed, snapDist, tolerance)` 启用平滑调和
  - 服务端状态到达后不再硬覆盖本地预测值，而是通过指数衰减插值平滑过渡
  - 解决客户端预测被服务端拉回/扯着走的手感问题（即使 0 延迟回环也不再抖动）
  - 三个可调参数：收敛速度、硬拉回阈值、吸附容差
  - 新增 `reconcileTick(delta)` 驱动每帧调和插值
  - 新增 `getServerValue()` 用于调试查看权威值

### 改进 (Changed)

- TankBehaviour 的 x/y 坐标默认启用平滑调和 `enableSmooth(10f, 50f, 0.5f)`
- NetcodeTankOnlineScreen 服务端逻辑改用 `manager.tick(delta)` 累加器模式
- NetcodeTankOnlineScreen 客户端每帧驱动 `reconcileTick()` 消化调和插值

---

## [0.7.0] - 2025-07-16

### 新增 (Added)

- **双端对战基建 (Netcode v2)**
  - TransportReceiveCallback 携带 clientId，支持识别发送方
  - 连接事件回调接口 NetworkConnectionListener（onClientConnected / onClientDisconnected）
  - UDP 握手升级：Server 回复 [0xFE×4][clientId]，Client 自动获取分配 ID
  - NetworkObject 增加 ownerClientId 字段（-1=Server, >=0=Client）
  - NetworkManager.spawnWithPrefab(prefabId, ownerClientId) 支持所有权指定
  - SpawnPacket 协议扩展：携带 ownerClientId，Client 自动标记 isLocalPlayer
  - TankBehaviour 增加 @ServerRpc rpcMoveInput / rpcFireInput 输入通道
- **统一联机对战屏幕 NetcodeTankOnlineScreen**
  - 合并原 Server/Client 双屏为单一入口
  - CONFIG 阶段：Tab 切换角色、[1] 编辑 IP、[2] 编辑端口、Enter 启动
  - 多客户端动态 Spawn：连接时自动生成坦克，6 色轮换
  - Host 模式：Server 自身坦克由 WASD+J 控制
  - 碰撞泛化：遍历所有坦克而非硬编码 p1/p2
  - TestSelectionScreen 添加「Netcode坦克联机对战」菜单项

### 验收标准

- ✅ 双端对战：两进程各操控一辆坦克互射，命中扣血
- ✅ IP 可配：UI 面板输入局域网 IP 后能跨机器联机
- ✅ 多客户端：支持 3+ 客户端同时连入独立坦克
- ✅ 向后兼容：纯内存沙盒正常运行，91 测试全绿
- ✅ 零硬编码：碰撞检测、坦克数量由运行时数据驱动

---

## [0.4.0] - 2026-02-22

### 新增 (Added)

- **虚拟摇杆增强**
  - 菱形方向指示器，进入扇区时点亮对应方向
  - 可配置 `stickHalfAngle` 扇区半角（默认 45°，全覆盖）
  - 扩展矩形触摸区域，提升移动端操控体验
  - VirtualJoystick 和 Touchpad 重写为自定义 Widget
- **核心系统接入**（`SimpleGameScreen` 对接核心数据模型）
  - 等级系统: 击杀获取经验、自动升级、升级自动分配属性点
  - 死亡惩罚系统: 经验损失 20%，死亡覆盖层 UI 显示详情
  - 重生系统: R 键重生，地图/敌人重置，保留玩家进度
  - 视口统一: 世界相机 ExtendViewport(400,400) + UI 分离
  - MP 系统: 自然回复 + 魔法攻击消耗，MP 不足时短冷却
- **三阶段架构重构**（`SimpleGameScreen` 871 行 → 345 行，减少 60%）
  - `GameEntity`: 从 Entity 内部类提取为独立顶级类
  - `DamagePopup`: 伤害飘字数据类
  - `EnemyDefs`: 敌人/玩家属性配置工厂
  - `GameConfig`: 20+ 命名常量（替代魔法数字）
  - `CombatHelper`: 统一战斗扫描/伤害/击杀，CombatListener 回调接口
  - `GrowthHelper`: 经验/升级/死亡惩罚/重生逻辑，GrowthListener 回调接口
  - `EnemyAI`: 敌人 AI 更新循环（冷却/追击/徘徊/碰撞检测）
  - `GameRenderer`: 渲染器，通过 GameState 只读接口与逻辑解耦
- **输入系统重构**
  - `InputManager`: 统一键盘 WASD + 手柄 + 虚拟摇杆轴输入
  - atan2 角度扇区检测替代简单阈值判定，修复方向偏移 Bug
  - 消除 `isPressed(MOVE_*)` 和 `getAxis()` 数据/显示不一致问题
- **其他**
  - `DLog` 控制台输出支持 ANSI 颜色
  - 统一日志输出为 `DLog`
  - 纹理重绘与第二转场效果
  - 无等待渐变转场 (`playOverlayFade`)

### 修复 (Fixed)

- 修复摇杆四向判定偏移（菱形分区 → atan2 角度检测）
- 修复 `isPressed(MOVE_*)` 内部 `isAxisMappedAction` 简单阈值拦截导致方向不一致
- 修复 `stickHalfAngle` 在 Screen 和 VirtualJoystick 之间不同步
- 修正主类名称以匹配新包结构

### 变更 (Changed)

- `SimpleGameScreen` 全面重构: God Object 拆分为 8 个职责单一的类
- `Entity` 标记为 `@Deprecated`，由 `GameEntity` 替代
- 移动输入统一为 `getAxis()` + atan2 路径，移除 `isPressed(MOVE_*)` 用于移动
- `VirtualJoystick` 从继承 Touchpad 改为自定义 Widget

---

## [0.3.0] - 2026-02-21

### 新增 (Added)

- **JSON 数据驱动绘制系统** — 完全替代硬编码纹理生成器
  - `AIDrawPlan` / `AIDrawCommand`: JSON 绘制计划与命令数据结构
  - `AIDrawMethodRegistry`: 反射扫描 NeonBatch 方法，MethodHandle 动态调用
  - `AIDrawExecutor`: JSON 文本 → TextureRegion 执行引擎，支持 FBO 导出
- **JSON 纹理管理器与资源工具**
  - `TextureManager`: JSON 驱动纹理管理器，自动扫描 `ai_draw_cmds/`，按需生成并缓存
  - `AssetUtils`: assets.txt 索引扫描器，按目录前缀批量获取资源路径
  - `TextureExporter`: FBO → PNG 纹理导出工具
- **20 套 JSON 绘制计划** (数据驱动所有游戏纹理)
  - 角色: player
  - 怪物: slime, skeleton, bat, wolf, orc, boss_dragon
  - 图块: wall, floor, door, stairs_down, stairs_up, torch, pillar
  - 物品: health_potion, mana_potion, sword, shield, coin, chest
- **纹理预览测试界面**
  - `TexturePreviewScreen`: 网格预览所有 JSON 绘制纹理
  - `TestSelectionScreen` 新增入口注册

### 变更 (Changed)

- 移除所有硬编码纹理生成器 (NeonSpriteGenerator / NeonItemGenerator / NeonTileGenerator)
- 纹理生成全面转向 JSON 绘制指令驱动，支持热更新

---

## [0.2.0] - 2026-02-21

### 新增 (Added)

- 核心数据模型系统 — 6 个源文件 + 4 个测试文件，共 72 个单元测试
  - `StatType`: 6 大属性枚举 (HP/MP/ATK/DEF/ASP/MOV)，含 `valuePerPoint` 和 `isAllocatable`
  - `StatData`: 属性容器与统一计算流水线，支持装备/百分比/突破加成
  - `StatCalculator`: 等级点数公式 (`fixedPointsPerStat`, `totalFreePoints`, `totalPoints`)
  - `CombatEngine`: 物理/魔法/穿透伤害引擎，穿透衰减 0.7，最低阈值 0.1
  - `GrowthCalculator`: 经验-等级公式 (`100×1.2^(L-1)`)，返回 `long`，全链路溢出保护
  - `DeathPenalty`: 死亡惩罚系统 (经验 20%/金币 50%/装备 2-5 件)，自由点循环均匀扣除
- 文档: 重构系统设计书 `2_0_系统设计书.md`，以实际代码为权威同步

### 修复 (Fixed)

- `StatData.addFreePoints` 增加余额校验与不可加点属性拒绝，返回 `boolean`
- `StatData.validate()` 校验自由点分配不超限
- `CombatEngine` 穿透衰减伤害低于 0.1 时归零（首目标豁免）
- `GrowthCalculator.xpForNextLevel` 从 `int` 改为 `long`，防止高等级 (>95) 溢出
- `GrowthCalculator` 全链路溢出保护 (`totalXpForLevel`/`levelFromXp`/`xpProgress`)

### 变更 (Changed)

- 属性与伤害系统 `int` 全面改为 `float`
- `CombatEngine.calcMagicDamage` 改由调用方传入 MDEF（不内部计算 DEF/2）
- ASP/MOV 统一为乘法管线: `min((base+equip)×(1+pct), 3.0) + uncapped`
- 死亡惩罚自由点扣除改为循环均匀分配算法（每轮对有余额属性均匀扣减）
- `StatData` 点数公式全部委托 `StatCalculator`，消除重复
- `GrowthCalculator.totalXpForLevel` 累加优化为局部变量，避免逐级调用

---

## [0.1.2] - 2026-02-20

### 新增 (Added)

- 新增 `magicdungeon2.BuildConfig` 独立包名配置
- 新增 `globalFileNameAndContentReplace.sh` 全局文件和内容替换脚本

### 修复 (Fixed)

### 变更 (Changed)

- 项目重命名: MagicDungeon → MagicDungeon2
- 版本重置: 0.9.0 → 0.1.2
- 包名更新: com.goldsprite.magicdungeon → com.goldsprite.magicdungeon2
- 同步更新所有构建配置、运行配置、文档、脚本中的项目名引用
