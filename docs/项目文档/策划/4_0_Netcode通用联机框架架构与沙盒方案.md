# 通用 Netcode 联机框架与沙盒靶场开发方案

## 1. 背景与痛点分析
当前业务逻辑、核心引擎重构、三端适配以及联机同步的代码完全耦合在一起，导致修改难度指数级上升。
**分而治之**是必须的：我们将提炼出一套独立的、类似 **Unity Netcode for GameObjects (NGO)** 的高层联机框架。这样上层业务只需关心给组件挂载 `NetworkBehaviour`，而不需要去处理底层的发包、接包、序列化和状态缓存逻辑。

---

## 2. 架构决策：写在哪里？

**结论：不要直接在 `TestNetty` 项目里面写。**

*   **TestNetty 定位 = Transport Layer (传输层)**：它类似于 Unity 的 `UnityTransport (UTP)` 或 ENet。它的职责应该保持纯粹：管理 Socket、UDP发包/收包、心跳保活、字节流加密解密。它对"游戏"、"玩家"、"实体"概念应该**一无所知**。
*   **Netcode 框架定位 = High-Level Network Layer (高层网络层)**：应该在 `GDEngine` 中开辟一个单独的模块（例如 `gdengine-netcode` 包），它作为 TestNetty 和 游戏实体 之间的桥梁。

**层级图示：**
```text
[游戏业务 (MagicDungeon2 / 靶场小游戏)] 
        ↓ 调用、继承
[GD Netcode (NetworkBehaviour, NetworkVariable, RPCs)]  <- 我们要开发的新框架！
        ↓ 字节打包、RPC映射
[TestNetty (Client/Server 实例, Channel, DatagramPacket)] <- 已有的底层传输
```

---

## 3. 核心设计：参考 Unity NGO 

为了快速上手并保证通用性，前期我们需要以下几个最核心的概念映射：

### ① NetworkManager (网络管理器)
全局单例枢纽。负责维护当前的网络拓扑结构架构。
*   提供 `startHost()`, `startClient()`, `startServer()` 接口。
*   管理局域网内所有活跃的 `NetworkObject` 的查表（Dictionary）。

### ② NetworkObject (网络物体)
挂载在 LibGDX Entity 上的通行证对象。
*   拥有全网唯一的 `NetworkId` (通常由 Server 统一分配)。
*   处理网络层面的 `Spawn()` (在所有端生成) 和 `Despawn()` (在所有端销毁)。

### ③ NetworkBehaviour (网络逻辑组件)
游戏业务逻辑继承的基类。提供类似于 NGO 的：
*   `IsServer`, `IsClient`, `IsOwner`, `IsLocalPlayer` 的权限判断。
*   提供对应的生命周期钩子，例如 `OnNetworkSpawn()`。

### ④ NetworkVariable (网络变量 / 状态同步)
基于泛型的自动同步变量。例如由服务器权威管理的玩家血量：`NetworkVariable<Integer> m_Health`.
*   内部记录 `Value` 和 `isDirty`（脏标记）。
*   在 LateUpdate 中，如果 `isDirty == true`，打包该变量并同步给 Clients。
*   **初期目标**：实现类似 `NetworkVariable<Vector2> position`，实现移动插值。

### ⑤ RPC (远程过程调用 / 事件同步)
代替目前手动写 Enum、Packet、注册 Subscriber 的噩梦！
*   **ServerRpc**: 客户端请求服务器执行（如：我也要开火，请求验证）。
*   **ClientRpc**: 服务器广播给客户端执行（如：播放某爆炸特效）。
*   *技术实现*：我们将通过反射+注解（或简化的硬编码方法映射）来实现 RPC 的调度，把方法名/ID和参数序列化发送。

---

## 4. 靶场设计：沙盒小游戏 (Netcode Sandbox)

为了不受现有复杂代码（地牢生成、攻击计算、怪物AI等）的干扰，我们需要一个 **极简的靶场小游戏** 来验证这套框架。

*   **所在位置**：`examples` 模块中新建一个包 `com.goldsprite.examples.netcodesandbox`，和一个纯享版的入口 `SandboxGameScreen`。
*   **视觉表现**：纯色方块/圆形（使用 ShapeRenderer 或 简单的白块贴图+染色）。
*   **核心玩法与验证点**：
    1.  **进出房间测验 (Spawn/Despawn)**
        *   每个玩家加入时，服务器 `Spawn` 一个玩家方块赋予 `NetworkId`。
        *   退出时，方块被立刻 `Despawn`。
    2.  **移动同步测验 (NetworkVariable <Transform>)**
        *   玩家 WASD 移动方块。通过 `NetworkVariable` 测试：位置插值平滑、Client Prediction 表现。
    3.  **RPC 测验 (发射子弹与减血)**
        *   鼠标点击：调用 `ServerRpc_FireBullet(x, y)`。
        *   服务器生成一个子弹实体（也是 `NetworkObject` 并 `Spawn`）。
        *   子弹触碰别人：服务器判定伤害，减少其 `NetworkVariable<Int> hp`，并触发 `ClientRpc_PlayExplosion()`。

---

## 5. 开发里程碑与 Iteration 计划

*   **Iteration 1: 基础设施架设**
    *   搭建 `GDNetcode` 模块（独立于魔法地牢的具体业务）。
    *   实现 `NetworkManager` 对 `TestNetty` Server/Client 的封装。
    *   创建 `SandboxLobbyScreen` ，跑通双端建立连接的基层通讯。
*   **Iteration 2: 实体与生成机制**
    *   实现 `NetworkObject` 和全局 ID 生成器。
    *   实现服务端的 `Spawn` 并把状态推给刚加入的 Client。
    *   在靶场中测试玩家实体的出现和消失。
*   **Iteration 3: 状态与动作同步**
    *   实现 `NetworkVariable` 接口及其泛型打包。
    *   在靶场中验证玩家方块的基础移动。
    *   实现基础的 `ServerRpc` 与 `ClientRpc`。
*   **Iteration 4: 框架回迁**
    *   靶场完全跑通后，将 `MagicDungeon2` 的复杂战局基于 `GDNetcode` 进行一次降维打击式的重构。