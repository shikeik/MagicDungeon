# 文档规则:

- 优先级表示: 0~5*, 5*最高, (*****)

## 问题:

## 新功能:

### Netcode 联机框架后续路线 (v0.6.x)

1. **实体自动 Spawn / Despawn 派生机制** (*****)
   - 引入 PrefabId（预制体ID）概念，Server spawn 时自动向所有客户端广播 SpawnPacket(0x11)
   - Client 收到包后根据 PrefabId 自动创建实例、挂载组件并注册到 networkObjects
   - 彻底删除所有测试和沙盒中的 `getDeclaredField` 反射黑魔法

2. **RPC 机制 (Remote Procedure Call)** (****)
   - 构建 ServerRpc（客户端请求服务端）和 ClientRpc（服务端下发客户端）的瞬发事件广播流
   - 用于开火特效、音效、一次性动作等不适合用状态同步的场景

3. **真实网络介质接入 (Netty/UDP)** (***)
   - 实现 NettyTransport 继承 Transport 接口，替换 LocalMemoryTransport
   - 支持跨进程、跨公网的真实联机
