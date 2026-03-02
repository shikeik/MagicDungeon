# Netcode v0.7.0 开发报告

> 生成日期: 2026-03-02  
> 项目: MagicDungeon2— GDEngine Netcode 模块  
> 版本: v0.7.0

---

## 一、概述

本轮开发完成了 GDEngine 自研 Netcode 网络框架的核心功能，实现了基于 UDP 的跨进程坦克联机对战沙盒。涵盖从底层传输协议、对象生命周期管理到上层游戏逻辑的全链路实现，并经过多轮 Bug 修复迭代达到可用状态。

---

## 二、架构总览

```
┌──────────────────────────────────────────────┐
│            NetcodeTankOnlineScreen            │  ← 示例层（游戏逻辑）
│   CONFIG → WAITING → PLAYING 状态机           │
├────────────────────┬─────────────────────────┤
│   TankBehaviour    │   TankSandboxUtils       │  ← 网络行为组件 + 工具
├────────────────────┴─────────────────────────┤
│              NetworkManager                   │  ← 引擎层（对象生命周期）
│  Spawn/Despawn · StatusSync · RPC · Tick      │
├──────────────────────────────────────────────┤
│            UdpSocketTransport                 │  ← 传输层（真实 UDP）
│  握手 · 广播 · 点对点 · 断开通知               │
└──────────────────────────────────────────────┘
```

### 关键类职责

| 类名 | 模块 | 职责 |
|------|------|------|
| `UdpSocketTransport` | core/netcode | UDP Socket 封装，握手/断开协议，客户端地址管理 |
| `NetworkManager` | core/netcode | 网络对象注册表，Spawn/Despawn，状态同步 Tick，RPC 分发 |
| `NetworkObject` | core/netcode | 网络实体容器，持有 NetworkVariable 列表和 Behaviour 组件 |
| `NetworkVariable<T>` | core/netcode | 泛型网络同步变量，脏标记 + 序列化/反序列化 |
| `NetworkBehaviour` | core/netcode | 网络行为组件基类，提供 RPC 发送接口 |
| `TankBehaviour` | examples/netcode | 坦克网络行为：位置/旋转/HP/颜色/名称 + RPC |
| `TankSandboxUtils` | examples/netcode | 坦克绘制、子弹生成、碰撞检测工具 |
| `NetcodeTankOnlineScreen` | examples/netcode | 统一联机屏幕（Server/Client 合一），含抽屉面板 |

---

## 三、协议设计

### 封包类型

| 魔法头 | 名称 | 方向 | 说明 |
|--------|------|------|------|
| `0xFF×4` | Handshake | C→S | 客户端连接握手 |
| `0xFE×4 + clientId` | HandshakeReply | S→C | 服务端回复分配的 clientId |
| `0xFD×4` | DisconnectNotify | C→S | 客户端主动断开通知 |
| `0x10` | StatusSync | S→C(广播) | 脏变量增量同步 |
| `0x11` | SpawnPacket | S→C(广播) | 预制体实例化广播 |
| `0x12` | DespawnPacket | S→C(广播) | 实体销毁广播 |
| `0x20` | ServerRpc | C→S | 客户端→服务端远程调用 |
| `0x21` | ClientRpc | S→C(广播) | 服务端→客户端远程调用 |

### 数据帧格式

- 普通数据帧: `[4字节长度前缀][payload]`
- 控制帧（握手/断开）: 裸 4 字节魔法标识，无长度前缀

---

## 四、功能清单

### P1: 传输层 (UdpSocketTransport)
- [x] UDP DatagramSocket 封装（Server 绑定端口 / Client 随机端口）
- [x] 异步守护线程接收循环
- [x] 发送带长度前缀的封包
- [x] 握手协议：Client 发送 `0xFF×4`，Server 记录地址并回复 `0xFE×4 + clientId`
- [x] 断开通知协议：Client 发送 `0xFD×4`，Server 触发回调
- [x] 活跃客户端追踪 (`activeClientIds` 集合)

### P2: 对象生命周期 (NetworkManager)
- [x] 预制体工厂注册 (`registerPrefab`)
- [x] Server 端 `spawnWithPrefab(prefabId, ownerClientId)` + 广播 SpawnPacket
- [x] Client 端自动派生本地副本 + `isLocalPlayer` 标记
- [x] `despawn(netId)` + 广播 DespawnPacket
- [x] `despawnByOwner(ownerClientId)` — 批量移除断开客户端的实体

### P3: 状态同步
- [x] 脏变量增量序列化 (`serializeObjectState`)
- [x] 全量状态快照补发 (`sendFullStateToClient`, `serializeFullObjectState`)
- [x] 已有实体 SpawnPacket 补发 (`sendExistingSpawnsToClient`)
- [x] 支持的数据类型: int, float, boolean, String, Color

### P4: RPC 系统
- [x] `@ServerRpc` 注解 + Client→Server 远程调用
- [x] `@ClientRpc` 注解 + Server→Client 广播调用
- [x] 反射调用：方法名 + 参数类型精确匹配
- [x] 支持参数类型: int, float, boolean, String

### P5: 游戏逻辑 (NetcodeTankOnlineScreen)
- [x] CONFIG→WAITING→PLAYING 状态机
- [x] 可配 IP / 端口输入（键盘）
- [x] Server/Client 角色切换 (Tab)
- [x] Host 模式：Server 自身坦克本地键盘控制
- [x] 多客户端动态 Spawn（6 色轮换）
- [x] Server 权威子弹物理 + 泛化碰撞检测
- [x] 死亡/复活机制
- [x] 玩家头顶名称标签 (`playerName` NetworkVariable)
- [x] 右侧抽屉式房间成员详情面板（I 键切换，带动画）

### P6: 健壮性
- [x] 端口占用优雅回退 (try-catch → configError 提示)
- [x] 线程安全断开事件 (`pendingDisconnects` 队列 + 主线程 tick 消费)
- [x] hide() 网络清理（ESC 返回不泄漏资源）
- [x] 全部日志统一使用 `DLog`（tag: "Netcode"）

---

## 五、Bug 修复历程

### 第 1 轮 (commit 503a216)

| 编号 | 问题 | 原因 | 修复 |
|------|------|------|------|
| B1 | 客户端坦克移速不一致 | Client 端没有 pendingMove 机制 | 引入 `pendingMoveX/Y` + Server delta 驱动 |
| B2 | 子弹不同步 | 缺少子弹 ID 和销毁 RPC | 添加 `bulletId` + `rpcDestroyBullet` |
| B3 | 后连入的客户端看不到已有坦克 | 缺少全量状态补发 | 添加 `sendFullStateToClient` |
| B4 | 视口坐标偏移 | 未应用 projectionMatrix | 使用 `uiViewport.getCamera().combined` |

### 第 2 轮 (commit 106a1fc)

| 编号 | 问题 | 原因 | 修复 |
|------|------|------|------|
| B5 | 重复创建资源导致渲染异常 | `show()` 被当作 `create()` | 一次性初始化移入 `create()` |
| B6 | 返回后网络连接泄漏 | `hide()` 未清理网络 | `hide()` 调用 `shutdownNetwork()` |
| B7 | 松键后坦克继续滑动 | `pendingMove` 未被消费后清零 | Server update 消费后清零 |
| B8 | 子弹销毁 RPC 从被击中坦克发出 | 客户端在错误的 localBullets 列表中查找 | 从射手坦克发送 `rpcDestroyBullet` |

### 第 3 轮 (commit de04977)

| 编号 | 问题 | 原因 | 修复 |
|------|------|------|------|
| B9 | 客户端断开后坦克残留 | 无断开通知机制 | 添加 `0xFD` 断开通知协议 + `despawnByOwner` |

### 第 4 轮 (commit 8d782b1)

| 编号 | 问题 | 原因 | 修复 |
|------|------|------|------|
| B10 | 端口占用导致崩溃 | `startServer` 无异常处理 | try-catch 回退到配置界面 + `configError` 提示 |
| B11 | 断开事件触发 ConcurrentModifyException | IO 线程直接操作 `networkObjects` | `pendingDisconnects` 队列 + 主线程 tick 消费 |

### 第 5 轮 (当前)

| 编号 | 问题 | 原因 | 修复 |
|------|------|------|------|
| B12 | 远程客户端数显示错误 | `getClientCount()` 返回历史总数 | 新增 `activeClientIds` 集合 + `getActiveClientCount()` |

---

## 六、文件清单

### 引擎层 (core/netcode)

| 文件 | 行数(约) | 说明 |
|------|----------|------|
| NetworkManager.java | ~520 | 核心管理器 |
| UdpSocketTransport.java | ~360 | UDP 传输层 |
| NetworkObject.java | ~80 | 网络实体容器 |
| NetworkVariable.java | ~90 | 泛型同步变量 |
| NetworkBehaviour.java | ~50 | 行为组件基类 |
| NetworkPrefabFactory.java | ~10 | 预制体工厂接口 |
| NetworkConnectionListener.java | ~10 | 连接事件接口 |
| Transport.java | ~30 | 传输层抽象接口 |
| NetBuffer.java | ~80 | 字节序列化工具 |
| LocalMemoryTransport.java | ~60 | 进程内测试用传输 |

### 示例层 (examples/netcode)

| 文件 | 行数(约) | 说明 |
|------|----------|------|
| NetcodeTankOnlineScreen.java | ~700 | 统一联机对战屏幕 |
| TankBehaviour.java | ~95 | 坦克网络行为 |
| TankSandboxUtils.java | ~110 | 绘制/工厂工具 |
| Bullet.java | ~20 | 子弹数据类 |

### 测试

| 文件 | 测试数 | 说明 |
|------|--------|------|
| UdpSocketTransportTest.java | 3 | UDP 传输测试 |
| SpawnAutoDerivationTest.java | 3 | Spawn 派生测试 |
| DespawnTest.java | 2 | Despawn 测试 |
| RpcInvocationTest.java | 2 | RPC 调用测试 |
| 其他 netcode 测试 | ~81 | 全部 91 测试绿 |

---

## 七、已知限制与后续计划

### 已知限制
- UDP 无可靠性保证，极端丢包场景可能状态不一致
- 未实现心跳包 / 超时检测（仅依赖主动断开通知）
- RPC 反射调用有性能开销，高频场景可考虑缓存 MethodHandle
- 客户端断开后 `clientAddresses` 保留空槽（保持 index 稳定），长时间运行可能内存膨胀

### 后续计划
- [ ] 可靠 UDP（重传机制、序列号）
- [ ] 心跳包 + 超时断开
- [ ] 网络延迟补偿 / 客户端预测
- [ ] 房间系统（创建/加入/列表）
- [ ] 更丰富的游戏内容（道具、地图、多武器）
