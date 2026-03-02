# 可靠 UDP 传输层 & 地图墙体/边界 策划文档

> 创建日期: 2026-03-02  
> 项目: MagicDungeon2 — GDEngine Netcode 模块  
> 前置版本: v0.7.4 (客户端预测 + 旧代码清理)

---

## 一、可靠 UDP (Reliable UDP) 传输层

### 1.1 背景

当前 `UdpSocketTransport` 是纯 UDP 传输，存在以下问题：

- **丢包无感知**: UDP 不保证到达，RPC 调用（如开火、Spawn/Despawn）一旦丢包，客户端状态就会永久不一致。
- **乱序**: 状态同步包可能乱序到达，旧数据覆盖新数据导致"回弹"。
- **无重传**: 关键封包（SpawnPacket、DespawnPacket、RPC）丢失后无法恢复。

### 1.2 方案设计

在现有 `UdpSocketTransport` 之上新增 **`ReliableUdpTransport`** 层（装饰器模式），对外仍实现 `Transport` 接口，内部包装原始 UDP 传输并添加可靠性保障。

#### 封包协议

```
┌──────────────┬──────────────┬──────────────────┬─────────────┐
│ channelType  │ sequenceNum  │  ackNum          │  payload    │
│  (1 byte)    │  (2 bytes)   │  (2 bytes)       │  (N bytes)  │
└──────────────┴──────────────┴──────────────────┴─────────────┘
```

- **channelType**: 
  - `0x00` = Unreliable（原始 UDP，无序列号/确认）
  - `0x01` = Reliable（带序列号，需要 ACK 确认，会重传）
- **sequenceNum**: 发送方递增的 16-bit 序列号（0~65535 循环）
- **ackNum**: 最近收到的对方序列号，用于确认
- **payload**: 原始业务数据

#### 通道分类

| 数据类型 | 通道 | 原因 |
|---|---|---|
| 状态同步 (0x10) | Unreliable | 高频发送，丢一帧无影响，下帧会覆盖 |
| Spawn (0x11) | Reliable | 丢失会导致实体永远不出现 |
| Despawn (0x12) | Reliable | 丢失会导致实体永远不消失 |
| ServerRpc (0x20) | Reliable | 开火等操作不可丢失 |
| ClientRpc (0x21) | Reliable | 子弹生成/销毁通知不可丢失 |
| ACK (0x02) | Unreliable | 确认包自身不需要确认 |

#### 重传机制

- 发送 Reliable 包后存入 `pendingSentPackets` 缓冲区（按 seqNum 索引）
- 收到 ACK 后从缓冲区移除
- 每帧检查超时（默认 200ms），超时则重传，最多重传 5 次
- 5 次重传仍无 ACK 视为断连

#### 序列号与乱序处理

- 接收方维护 `expectedSeqNum`，收到序列号 < expected 的包直接丢弃（旧包）
- 使用 16-bit 循环序列号，通过 `(a - b + 65536) % 65536 < 32768` 判断 a 是否新于 b

### 1.3 类设计

```
Transport (接口)
├── UdpSocketTransport (原始 UDP，不变)
└── ReliableUdpTransport (新增，装饰器)
      ├── 内部持有 UdpSocketTransport
      ├── 发送端: seqNum 分配、pendingSentPackets 缓冲
      ├── 接收端: expectedSeqNum、去重、ACK 回复
      └── tick(): 超时重传检查（需每帧调用）
```

### 1.4 TDD 测试计划

1. **序列号循环测试**: 验证 seqNum 从 65535 → 0 的正确循环
2. **可靠发送与 ACK 测试**: 发送 Reliable 包，模拟收到 ACK，验证从缓冲区移除
3. **重传测试**: 模拟 ACK 超时，验证自动重传触发
4. **乱序丢弃测试**: 验证旧序列号的包被正确丢弃
5. **Unreliable 通道测试**: 验证 Unreliable 包不参与序列号/ACK 机制
6. **集成测试**: 通过真实 UDP 验证 Reliable 通道端到端可靠性

---

## 二、地图墙体与边界

### 2.1 背景

当前战场无任何障碍物和边界：
- 坦克可以无限移动到任何方向
- 子弹越界的判定使用 `uiViewport` 尺寸（屏幕大小），不合理
- 战场缺乏战术元素

### 2.2 方案设计

#### 地图边界

- 定义一个矩形区域作为战场边界，例如 `(0, 0) ~ (2000, 1500)`
- 使用线框矩形（`NeonBatch.drawRect(..., filled=false)`）渲染可见边界
- 坦克移动和子弹越界均以此边界为准
- Server 端权威执行边界碰撞（Clamp 坐标）

#### 随机墙体

- 地图加载时（Server 端）随机生成 5~10 个矩形墙体
- 墙体参数: `(x, y, width, height)`，使用线框矩形渲染
- 墙体信息通过 NetworkVariable 或初始化时 RPC 同步给客户端
- 碰撞检测: AABB 矩形碰撞（坦克 30x30，子弹 8x8）

#### 碰撞规则

| 碰撞对 | 行为 |
|---|---|
| 坦克 vs 边界 | 坐标 Clamp，不可穿出 |
| 坦克 vs 墙体 | 推回（回退本帧位移） |
| 子弹 vs 边界 | 销毁子弹 |
| 子弹 vs 墙体 | 销毁子弹 |

### 2.3 数据结构

```java
/** 矩形墙体 */
public class Wall {
    float x, y, width, height;  // 左下角 (x,y) + 尺寸
}

/** 地图配置 */
public class MapConfig {
    float boundaryWidth = 2000f;
    float boundaryHeight = 1500f;
    List<Wall> walls;  // 随机生成的墙体
}
```

### 2.4 同步策略

墙体在 Server 创建房间时随机生成，通过以下方式同步：
- 使用随机种子（`roomSeed`）方案: Server 和 Client 使用相同种子独立生成相同墙体布局
- 种子通过现有 PresenceRoomInfo 的扩展字段传递，或在握手后通过自定义 RPC 下发

### 2.5 TDD 测试计划

1. **边界 Clamp 测试**: 验证坐标超出边界时正确被限制
2. **AABB 碰撞测试**: 验证坦克-墙体、子弹-墙体矩形碰撞检测
3. **墙体生成测试**: 验证相同种子生成相同布局
4. **坦克推回测试**: 验证碰墙后位移回退
5. **子弹墙体销毁测试**: 验证子弹撞墙后被正确移除

---

## 三、实施顺序

1. **Phase 1**: 可靠 UDP 传输层（ReliableUdpTransport）
   - TDD 测试先行
   - 实现序列号、ACK、重传
   - 集成到现有 NetworkManager

2. **Phase 2**: 地图边界与墙体
   - TDD 测试先行
   - 实现 MapConfig + Wall 数据结构
   - 实现碰撞检测工具方法
   - 集成到 NetcodeTankOnlineScreen
   - 渲染墙体和边界线框

3. **Phase 3**: 提交 & 报表
