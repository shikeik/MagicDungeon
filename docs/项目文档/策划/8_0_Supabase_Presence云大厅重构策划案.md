# 8_0 Supabase Realtime Presence 云大厅重构策划案

> 生成日期: 2026-03-02  
> 项目: MagicDungeon2  
> 关联版本: Netcode v0.7.2+  
> 前置文档: `7_0_Supabase大厅与房间管理策划案.md`、`18_v0.7.1_Supabase云大厅实装报表.md`

---

## 一、背景与问题分析

### 1.1 现状回顾

v0.7.1 已实装的云大厅采用 **"REST 表操作 + 轮询 + 心跳"** 架构：

| 模块 | 当前实现方式 | 存在问题 |
|---|---|---|
| `SupabaseClient` | HTTP REST (POST/GET/PATCH/DELETE) 操作 `rooms` 表 | 每次操作都是一次完整的 HTTP 往返，延迟 200~500ms |
| `RoomManager` 心跳 | 每 15 秒 `PATCH last_ping` | 持续的数据库写 I/O；房主崩溃后需 30s+ 才被视为死亡 |
| `SupabaseLobbyScreen` 列表刷新 | 每 10 秒 `GET /rooms` 轮询 | 非实时，用户体验滞后；浪费免费配额 |
| `joinRoom()` | 仅更新状态文本 `setStatus(...)` | **空壳**，未发起任何 UDP 连接 |
| `createAndJoinRoom()` | 仅调用 `RoomManager.createRoom()` | **空壳**，建房后未启动 `NetworkManager` 监听 |
| 幽灵房间清理 | 依赖 `pg_cron` 每分钟清扫 | 最坏情况下死房存活 90 秒，严重影响大厅质量 |
| UI 反馈 | 无 Loading 遮罩、无超时提示、无重试 | 用户无法感知操作进度和错误原因 |

### 1.2 为什么要迁移到 Realtime Presence？

Supabase 提供了基于 WebSocket 的 **Realtime Presence** 机制，专为"谁在线、谁在哪个房间"这类瞬态状态设计：

| 维度 | REST 表操作 (当前) | Realtime Presence (目标) |
|---|---|---|
| **离线检测** | 30~60 秒（心跳超时 + pg_cron 周期） | **毫秒级**（WebSocket 断连即触发） |
| **列表更新** | 10 秒轮询，最差延迟 10 秒 | **实时推送**，join/leave 事件即刻到达 |
| **数据库负载** | 每间隔心跳一次 PATCH + 每间隔列表一次 GET | **零数据库 I/O**，纯内存状态同步 |
| **幽灵房间** | 需要 pg_cron / Edge Function 后台清理 | **自动消失**，无需任何清理逻辑 |
| **免费额度** | API 请求次数受限（50万/月） | 200 并发连接 + 200 万消息/月 |
| **代码复杂度** | HTTP 请求 + JSON 序列化 + Timer 心跳 | Channel 订阅 + track/untrack |

**结论：Presence 在延迟、可靠性、资源开销三个核心指标上全面碾压 REST 轮询方案，迁移势在必行。**

---

## 二、目标架构设计

### 2.1 新的网络拓扑

```text
       [房主 (Host)]                                  [客户端 (Client)]
            │                                               │
            │ 1. channel.track({房间信息})                   │ 3. channel.on('presence','sync')
            │    WebSocket 长连接                             │    WebSocket 长连接
            ▼                                               ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                    Supabase Realtime Server                      │
   │             Channel: "game_lobby"                                │
   │     Presence State (纯内存): { user_key: {房间元数据} }           │
   └──────────────────────────────────────────────────────────────────┘
            │                                               │
            │ 2. 房主同时启动 UDP Server                      │ 4. 读取 host_ip:host_port
            │    NetworkManager.startHost()                  │    发起 UDP 握手
            └───────────────────────────────────────────────┘
```

**关键变化**：Supabase 的角色从"数据库"退化为"纯信令中继"，不再有任何持久化写操作。

### 2.2 模块职责重新划分

```text
[SupabaseLobbyScreen]  ← UI 层，监听 Presence 事件渲染房间列表
        │
        ├── [PresenceLobbyManager]  ← 新核心，封装 Channel + Presence 协议
        │       ├── track()     → 发布/更新自己的房间信息
        │       ├── untrack()   → 下线/销毁房间
        │       └── sync 事件   → 获取实时全量在线房间列表
        │
        ├── [PublicIPResolver]  ← 保留，房主建房前获取公网 IP
        │
        └── [NetworkManager]  ← 已有的 Netcode 框架
                ├── startHost()  → 房主建房后立即启动 UDP 监听
                └── startClient(ip, port)  → 客户端加入后发起 UDP 连接
```

### 2.3 废弃与保留清单

| 模块 | 处置 | 原因 |
|---|---|---|
| `SupabaseClient.java` | **废弃** | REST 操作不再需要；若项目其他地方需要 Supabase REST，可保留但从大厅链路移除 |
| `RoomManager.java` | **废弃**，由 `PresenceLobbyManager` 取代 | 心跳 Timer、HTTP CRUD 全部不需要 |
| `RoomModel.java` | **重构** → `PresenceRoomInfo` | 移除数据库字段 (`id`, `last_ping`, `created_at`)，保留纯业务字段 |
| `SupabaseConfig.java` | **保留并扩展** | 新增 Realtime WebSocket 端点配置 |
| `PublicIPResolver.java` | **保留** | 房主仍需获取公网 IP |
| `SupabaseLobbyScreen.java` | **大幅重构** | 从轮询驱动改为事件驱动 |

---

## 三、详细设计

### 3.1 数据模型：PresenceRoomInfo

不再是数据库行映射，而是 Presence Track 时发送的 JSON 载荷：

```java
/**
 * Presence 频道中，每个房主发布的房间元数据
 */
public class PresenceRoomInfo {
    /** 房间显示名 */
    public String roomName;
    /** 房主公网 IP */
    public String hostIp;
    /** 房主 UDP 监听端口 */
    public int hostPort;
    /** 当前玩家数 */
    public int currentPlayers;
    /** 最大玩家数 */
    public int maxPlayers;
    /** 房间状态: "waiting" | "playing" | "full" */
    public String status;
}
```

### 3.2 核心类：PresenceLobbyManager

#### 职责
- 建立并维护到 Supabase Realtime Server 的 WebSocket 长连接
- 管理 Presence Channel 的 `track` / `untrack` 生命周期
- 将 `join`、`leave`、`sync` 事件转发给 UI 层

#### 关键接口设计

```java
public class PresenceLobbyManager {

    /** 频道名——所有大厅用户共享同一个频道 */
    private static final String CHANNEL_NAME = "game_lobby";

    // === 回调接口 ===

    /** 房间列表同步回调，每次有人加入/离开都会触发 */
    public interface OnRoomsSyncListener {
        void onSync(List<PresenceRoomInfo> allRooms);
    }

    /** 单个玩家加入事件 */
    public interface OnPlayerJoinListener {
        void onJoin(PresenceRoomInfo room);
    }

    /** 单个玩家离开事件 */
    public interface OnPlayerLeaveListener {
        void onLeave(PresenceRoomInfo room);
    }

    // === 核心方法 ===

    /** 连接到大厅频道并开始监听 Presence 事件 */
    public void connect(OnRoomsSyncListener syncListener,
                        OnPlayerJoinListener joinListener,
                        OnPlayerLeaveListener leaveListener);

    /** 房主：发布自己的房间信息到 Presence（类似建房） */
    public void publishRoom(PresenceRoomInfo info);

    /** 房主：更新房间信息（如人数变化、状态变化） */
    public void updateRoom(PresenceRoomInfo info);

    /** 房主：取消发布（离开大厅 / 销毁房间） */
    public void unpublishRoom();

    /** 断开 WebSocket 连接，释放资源 */
    public void disconnect();
}
```

#### Presence 协议流程

```text
房主建房:
  1. PublicIPResolver.resolvePublicIP() → 获得 hostIp
  2. NetworkManager.startHost(port)    → 启动 UDP 服务端
  3. PresenceLobbyManager.publishRoom({roomName, hostIp, port, 1, 6, "waiting"})
     → WebSocket 发送 track payload
     → 所有已连接客户端的 sync 回调被触发，列表刷新

客户端浏览大厅:
  1. PresenceLobbyManager.connect(syncListener, joinListener, leaveListener)
     → WebSocket 建立连接
     → 立即收到一次 sync 事件，包含当前所有在线房间

客户端加入房间:
  1. 从 PresenceRoomInfo 中取出 hostIp + hostPort
  2. NetworkManager.startClient(hostIp, hostPort)
     → 底层 UdpSocketTransport 发起 0xFF 握手
  3. 握手成功 → ScreenManager 跳转到游戏场景
  4. 握手超时(5s) → 提示"房间不可达"

房主异常断线:
  1. WebSocket 连接断开
  2. Supabase 自动从 Presence 中移除该房主
  3. 所有客户端的 leave 回调被触发 → 该房间从列表中消失(毫秒级)
```

### 3.3 LibGDX WebSocket 实现方案

LibGDX 原生 `Net` 模块仅支持 HTTP，不支持 WebSocket。需要额外的适配方案：

#### 方案 A: gdx-websocket 第三方库（推荐）

使用社区维护的 `com.github.AriasaProp:gdx-websocket` 或类似库，提供跨平台（Desktop + Android + Web）的 WebSocket 支持。

```groovy
// build.gradle (core)
dependencies {
    implementation "com.github.AriasaProp:gdx-websocket:1.x.x"
}
```

#### 方案 B: Java 原生 WebSocket（仅 Desktop/Android）

使用 JDK 11+ 的 `java.net.http.WebSocket` 或 OkHttp 的 WebSocket 客户端。缺点是 GWT/Web 端需要另写适配。

#### 方案 C: 走 Supabase REST + Realtime HTTP 长轮询

如果 WebSocket 集成困难，可使用 Supabase 的 HTTP 长轮询（Long Polling）模式作为降级方案，但实时性和性能不如原生 WebSocket。

**本案推荐方案 A**，原因：跨平台一致性好，社区活跃，API 简洁。具体选型可在技术预研阶段确认。

### 3.4 Supabase Realtime 协议要点

Supabase Realtime 使用的是 **Phoenix Channel** 协议（基于 WebSocket），关键交互如下：

```text
# 1. 建立 WebSocket 连接
ws://PROJECT_REF.supabase.co/realtime/v1/websocket?apikey=PUBLISHABLE_KEY

# 2. 加入频道 (JSON message)
{
  "topic": "realtime:game_lobby",
  "event": "phx_join",
  "payload": {},
  "ref": "1"
}

# 3. Track Presence (房主发布房间)
{
  "topic": "realtime:game_lobby",
  "event": "presence",
  "payload": {
    "type": "track",
    "key": "unique_user_key",
    "state": {
      "roomName": "大马猴的坦克战场",
      "hostIp": "123.45.67.89",
      "hostPort": 20000,
      "currentPlayers": 1,
      "maxPlayers": 6,
      "status": "waiting"
    }
  },
  "ref": "2"
}

# 4. 接收 Presence Sync (服务端推送)
{
  "topic": "realtime:game_lobby",
  "event": "presence_state",
  "payload": {
    "unique_user_key_1": { "metas": [{ ...roomInfo... }] },
    "unique_user_key_2": { "metas": [{ ...roomInfo... }] }
  }
}

# 5. 接收 Presence Diff (有人加入/离开)
{
  "topic": "realtime:game_lobby",
  "event": "presence_diff",
  "payload": {
    "joins": { "new_user_key": { "metas": [...] } },
    "leaves": { "left_user_key": { "metas": [...] } }
  }
}
```

### 3.5 SupabaseLobbyScreen 重构设计

#### 从轮询驱动到事件驱动

| 生命周期 | 旧实现 | 新实现 |
|---|---|---|
| `create()` | `new RoomManager()` + 手动 `fetchRooms()` | `new PresenceLobbyManager()` + `connect(syncListener)` |
| `render0()` | 10 秒计时器 → `refreshRoomList()` | **移除定时器**，列表由 `sync` 事件自动更新 |
| `dispose()` | `roomManager.destroyRoom()` | `presenceLobbyManager.disconnect()` |
| 建房按钮 | `RoomManager.createRoom()` → 仅设状态文本 | `resolveIP` → `NetworkManager.startHost()` → `publishRoom()` → **跳转游戏** |
| 加入按钮 | `setStatus("连接中...")` → 无后续 | `NetworkManager.startClient(ip,port)` → Loading → 超时/成功 |

#### 新增 UI 元素

1. **Loading 遮罩层**：建房/加入期间覆盖全屏的半透明遮罩 + 旋转动画 + "连接中..." 文本
2. **房间状态标签**：每行显示房间状态（等待中 / 游戏中 / 已满），已满/进行中的房间"加入"按钮灰化
3. **连接状态指示器**：顶栏显示 WebSocket 连接状态图标（🟢已连接 / 🟡连接中 / 🔴断开）
4. **自动重连提示**：WebSocket 断连后自动尝试重连，并显示倒计时
5. **房间详情弹窗**：点击房间行可展开显示 host_ip (部分隐藏)、延迟估算等信息

---

## 四、开发阶段规划

### Phase 1: WebSocket 基础设施（预计 1~2 天）

| 任务 | 说明 |
|---|---|
| 1.1 技术选型与依赖引入 | 评估 gdx-websocket / OkHttp-WebSocket / Java原生，选定方案并加入 Gradle 依赖 |
| 1.2 WebSocket 适配层封装 | 封装 `RealtimeWebSocket` 类，屏蔽底层库差异，提供 `connect/send/onMessage/disconnect` |
| 1.3 Phoenix Channel 协议实现 | 实现 `phx_join`、heartbeat (`phx_heartbeat` 每30s)、Presence track/untrack 的 JSON 消息编解码 |
| 1.4 连通性验证 | 编写独立测试用例，在 `tests` 模块中连接 Supabase Realtime 并成功打印 `presence_state` |

### Phase 2: PresenceLobbyManager（预计 1~2 天）

| 任务 | 说明 |
|---|---|
| 2.1 新建 `PresenceRoomInfo` | 纯 POJO，字段见 3.1 节 |
| 2.2 新建 `PresenceLobbyManager` | 封装 Phase 1 的 WebSocket + Channel，暴露 `connect/publishRoom/updateRoom/unpublishRoom/disconnect` |
| 2.3 事件分发机制 | 解析 `presence_state` 和 `presence_diff` 消息，构建房间列表并回调 `OnRoomsSyncListener` |
| 2.4 异常处理 | WebSocket 断连自动重连（指数退避，最大间隔 30 秒）；JSON 解析错误容错 |

### Phase 3: SupabaseLobbyScreen 重构（预计 1~2 天）

| 任务 | 说明 |
|---|---|
| 3.1 移除旧的轮询逻辑 | 删除 `autoRefreshTimer`、旧 `RoomManager` 依赖 |
| 3.2 接入 PresenceLobbyManager | 在 `create()` 中 `connect()`，在 `dispose()` 中 `disconnect()` |
| 3.3 事件驱动刷新列表 | `OnRoomsSyncListener.onSync()` → `Gdx.app.postRunnable()` → `rebuildRoomList()` |
| 3.4 建房流程打通 | 获取 IP → 启动 `NetworkManager.startHost()` → `publishRoom()` → 跳转 `NetcodeTankOnlineScreen` |
| 3.5 加入流程打通 | 读取 `hostIp:hostPort` → `NetworkManager.startClient()` → Loading 遮罩 → 超时 5s / 成功跳转 |
| 3.6 新增 UI 元素 | Loading 遮罩、房间状态标签、连接状态指示器 |

### Phase 4: 集成测试与清理（预计 1 天）

| 任务 | 说明 |
|---|---|
| 4.1 双端联调 | 桌面端 A 建房 → 桌面端 B 刷到并加入 → 确认 UDP 握手成功 → 进入坦克大战 |
| 4.2 异常场景测试 | 房主 Alt+F4 → 客户端列表是否秒级消失；网络断开重连后是否恢复 |
| 4.3 废弃代码清理 | 标记/移除旧的 `SupabaseClient`、`RoomManager`（或移入 legacy 包）、`rooms` 表的 pg_cron 任务 |
| 4.4 文档更新 | 更新版本报表、CHANGELOG、TODO |

---

## 五、风险与降级策略

| 风险 | 影响 | 降级方案 |
|---|---|---|
| LibGDX 缺乏成熟的跨平台 WebSocket 库 | WebSocket 连接不稳定或在 Android/GWT 上不可用 | 回退到 REST 长轮询模式（HTTP Keep-Alive + `GET /realtime/...`） |
| Supabase Realtime 免费版有 200 并发上限 | 同时在线超过 200 人后无法连接 | 项目初期绰绰有余；若后期需要可升级 Pro ($25/月) |
| NAT 穿透依然无法保证（Presence 解决的是信令问题而非打洞问题） | 部分玩家能看到房间但加不进去 | 保持当前策略：提示用户配置端口转发；后续迭代考虑 TURN 中继或 STUN 打洞 |
| Phoenix Channel 协议需要自行实现心跳和重连 | 实现复杂度上升 | WebSocket 心跳是固定的 `phx_heartbeat` JSON，逻辑简单；重连使用指数退避即可 |

---

## 六、Supabase 侧配置变更

### 6.1 Realtime 启用

在 Supabase Dashboard → Settings → Realtime 中确认 Presence 功能已开启（免费版默认启用）。

### 6.2 rooms 表处理

迁移完成并稳定后，`rooms` 表及其 `pg_cron` 任务可以彻底删除：

```sql
-- 清除定时任务
SELECT cron.unschedule('delete-old-rooms');

-- 删除 rooms 表（确认不再使用后）
DROP TABLE IF EXISTS rooms;
```

### 6.3 RLS 策略

Presence 走 WebSocket 不经过 REST API，不需要针对表的 RLS 策略。但 WebSocket 连接本身受 `apikey` + `JWT` 验证保护，免费版的 Anon Key 已足够。

---

## 七、后续迭代展望

| 迭代 | 内容 |
|---|---|
| **v0.7.3** | 本策划案的实装——Presence 云大厅上线 |
| **v0.7.4** | UDP NAT 穿透方案调研（STUN/TURN），降低对端口转发的依赖 |
| **v0.7.5** | 大厅内聊天功能（复用 Realtime Channel 的 broadcast 消息，实现房间内文字聊天） |
| **v0.8.0** | 匹配系统（自动匹配空闲房间 / 按区域就近匹配） |

---

## 附录 A: 新旧方案对比一览

```text
                    旧方案 (REST 表操作)              新方案 (Realtime Presence)
                    ========================          ============================
建房              POST /rooms                       channel.track({...})
心跳              Timer 15s → PATCH last_ping       WebSocket 自带 (phx_heartbeat)
刷新列表           Timer 10s → GET /rooms            sync 事件自动推送
幽灵清理           pg_cron 每分钟 DELETE              自动消失 (WebSocket 断连)
离线检测           30~60 秒                          毫秒级
销毁房间           DELETE /rooms?id=eq.xxx           channel.untrack() 或直接断连
数据库负载         高 (持续读写)                       零
代码行数           ~400 行 (Client+Manager+Model)     ~200 行 (PresenceLobbyManager)
```
