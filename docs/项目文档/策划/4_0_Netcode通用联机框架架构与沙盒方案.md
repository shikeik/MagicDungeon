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

## 4. 靶场设计：2D俯视角坦克大战 (Netcode Sandbox)

为了彻底隔绝业务逻辑与底层核心框架，我们绝对不能在魔法地牢里测试。需要在 `examples` 模块中开发一个名为 **"Netcode 坦克大战"** (NetTank) 的极简沙盒。
**核心原则：在这个沙盒中，你只允许调用 `NetworkBehaviour` 提供的方法，严禁直接碰 Socket 和 Packet 组件！**

*   **所在位置**：`examples/src/com/goldsprite/examples/nettank/`
*   **视觉表现**：抛弃传统 ShapeRenderer，直接使用引擎内置的 `NeonBatch` (GScreen自带实例) 绘制发光几何体。
    *   🟦 蓝色发光方块 = 本地玩家 (`IsLocalPlayer == true`)
    *   🟥 红色发光方块 = 网络其他玩家 (`IsLocalPlayer == false`)
    *   🟡 黄色发光圆球 = 场景中自动按路线移动的NPC敌人
    *   ⚪ 白色实心发光小圆 = 发射的子弹
*   **核心测试用例映射表**：
    1.  **进出房间测试 (NetworkPrefab & Spawn/Despawn)**
        *   **逻辑**：Server 启动后分配 `NetworkId`，客户端连入后 Server 执行 `NetworkManager.Spawn(PlayerPrefab)`。
        *   **验收**：A端掉线，B端能立刻看到对应方块消失，没有任何空指针和内存泄漏。
    2.  **绝对服务端权威移动机制 (NetworkVariable<Vector2>)**
        *   **逻辑**：玩家按下 W 键，不直接本地移动坐标，而是触发按键的状态发送（或者简单的 ClientPrediction）。我们优先实现 Server权威移动：Server 收到摇杆输入，修改 `NetworkVariable<Vector2> Position`，Client根据 `isDirty` 被动同步并**平滑插值 (Interpolation)** 移动。
        *   **验收**：在 200ms Ping 的模拟延迟下，方块仍能平滑移动而不是鬼畜闪现。
    3.  **瞬时事件与特效同步 (RPC 系统)**
        *   **逻辑**：点击鼠标，调用 `@ServerRpc requestFire()`。服务器计算冷却，若合法则 `Spawn` 一个子弹实体（具有物理碰撞）。子弹碰到墙壁后，调用 `@ClientRpc playExplosionFX(x, y)` 让所有客户端在本地绘制爆炸粒子。
        *   **验收**：业务层代码像调用普通方法一样，感知不到网络包的存在。
    4.  **状态同步系统与生命周期测试 (NetworkVariable<Integer> / <String>)**
        *   **逻辑**：方块上方显示名字与血量（如 `Player1 [100/100]`）。受到攻击时，仅 Server 扣血。`m_health` 发生变化自动下发。
        *   **重生机制**：当血量降为 0 时，触发死亡逻辑，Server 设置该对象为隐藏状态，开始 3秒 倒计时，倒计时结束后重新在随机出生点 Spawn 并恢复状态。
        *   **验收**：无需写任何更新血量的网络协议，只要值改变，所有客户端UI自动刷新，并能正确处理死亡倒计时重生表现。

---

## 5. 质量保证体系 (测试驱动开发战略)

抛弃以前 "出了Bug才靠断点打Log来猜" 的模式。这套核心通用框架必须有 **严密的单元测试和自动化测试** 支撑，保证底层磐石般坚固。

整个质量保证分为三层金字塔：

### ① 纯业务无关测试 (Headless JUnit 单元测试)
对于底层序列化、连接管理，完全不需要 LibGDX 引擎启动。在项目的 `tests/` 目录中：
*   **`NetworkVariableTest.java`**: 
    测试脏标记机制，在模拟的内存流中序列化 `NetworkVariable<Integer>`，修改其值，断言其序列化前后的值与 `isDirty` 状态是否严格一致。
*   **`RpcMethodReflectionTest.java`**: 
    测试方法上的 `@ServerRpc` 拦截机制。断言通过反射能否正确提取方法签名和参数类型列表。
*   **`LocalTransportTest.java`**: 
    抛弃物理网卡，写一个 `MemoryTransport` 存放于内存中的收发队列，用于模拟瞬间极高兵量的压测和丢包模拟。

### ② 依赖引擎环境测试 (GdxTestRunner)
需要 `Vector2`, `Entity`, `Lifecycle` 的集成测试。
*   **`NetObjectSpawnTest.java`**: 
    使用 GdxTestRunner。启动一个本地 Server 实例和两个本地 Client 实例。Server 端执行 `Spawn()`，并 Assert Client1, Client2 中的 `EntityManager` 是否在接下来几帧内正确反序列化并生成了该实体的镜像对象。

### ③ 暴力压力测试 (AutoTest Bot 自动化集群)
在开发到 Iteration 3 之后，我们需要提供极其暴力的回归测试来验证其真正可用：
*   **`HeadlessBotClient.java`**: 
    编写一个脱离UI渲染的纯算力僵尸客户端。
*   **混沌猴子测试 (Chaos Monkey bot tests)**: 
    启动 1 个服务器，同时在后台用多线程启动 50 个 `HeadlessBotClient`，每个 Bot 每秒随机向四面八方疯狂移动、疯狂发射 `@ServerRpc` 的射击请求，并在期间随机执行 Socket 断开连接（模拟杀进程拔网线）。
*   **断言目标**：
    持续运行 10 分钟。服务器**必须不能卡死、不能抛出非受检异常、不报 ConcurrentModificationException，内存不泄露**。如果不满足，框架不准合并到 `main` 分支。

---

## 6. 开发里程碑与 Iteration 计划

*   **Iteration 1: 基建与单元测试架设**
    *   搭建 `gdengine-netcode` 模块（完全独立）。搭建 `tests` 测试用例池。
    *   搭建 `NetTank` 靶场空壳。实现 Headless 环境下 Server/Client 互通。
*   **Iteration 2: 实体生成与管理 (Spawn/Despawn + GdxTestRunner)**
    *   实现全局 `NetworkObjectId` 生成系统，通过集成测试验证 Client 可复制 Server 实体。
*   **Iteration 3: 核心同步三板斧 (状态与过程同步)**
    *   引入 `NetworkVariable<T>` 及自动打包。
    *   使用注解（或硬编码映射字典）跑通 `ServerRpc` / `ClientRpc`。
    *   靶场能愉快地看到子弹互射和血量同步。
*   **Iteration 4: 上混沌猴子压测与防抖调雷**
    *   写 BotClient 并发压测，消灭所有并发锁和网络时序问题。
*   **Iteration 5: 回归与降维打击**
    *   靶场毕业后，重构魔法地牢的庞杂通信问题。