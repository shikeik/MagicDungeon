# 7_0 Supabase 大厅与房间管理策划案

> 生成日期: 2026-03-02
> 项目: MagicDungeon2 
> 关联版本: Netcode v0.7.0 以上

---

## 一、 引言与目标

目前，网络构架（v0.7.0）已实现了完善的 UDP 双端对战及局域网/公网直连（需手动输入 IP 和端口）。为了提升联机体验，我们计划引入 **Serverless 无服务端大厅** 机制。

**目标**：
1. 废弃“手动输入 IP 和端口”的硬核模式，改为直观的**房间列表**。
2. 利用 **Supabase** (PostgreSQL + REST API) 作为大厅数据中心。
3. 房主通过本地端口转发/公网 IP 提供游戏底层 UDP 服务，Supabase 仅负责“登记薄”功能，零服务器成本。

---

## 二、 架构与网络拓扑

采用 **大厅服务 (HTTP) + 游戏服务 (UDP)** 分离的混合 P2P 架构设计：

```text
       [房主 (Host)]                                  [客户端 (Client)]
            │                                               │
            │ 1. POST /rooms (注册房间)                     │ 4. GET /rooms (拉取列表)
            │ 2. PATCH /rooms (心跳延期)                    │
            ▼                                               ▼
   ┌──────────────────────────────────────────────────────────────────┐
   │                           Supabase 云端                           │
   │  [Rooms 表]: id | name | host_ip | udp_port | player_count | 状态  │
   └──────────────────────────────────────────────────────────────────┘
            │
            │ 3. P2P 游戏数据交互 (UDP)                     │ 5. 发起 UDP 握手
            └───────────────────────────────────────────────┘
```

---

## 三、 数据库设计 (Supabase Schema)

需要在 Supabase 新建数据表 `rooms`，字段设计如下：

| 字段名 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID / int8 | 房间唯一标识 (Primary Key，默认自动生成) |
| `room_name` | text | 房间名称，供玩家辨认 (如："大马猴的坦克战场") |
| `host_ip` | text | 房主的公网 IPv4/IPv6 地址 |
| `host_port` | int4 | 房主开放监听的 UDP 端口 |
| `player_count`| int4 | 当前房间内的人数 |
| `max_players` | int4 | 房间最大人数限制（默认 6） |
| `last_ping` | timestamp | 最后一次心跳时间（UTC） |
| `created_at` | timestamp | 房间创建时间 |

**安全规则 (RLS)**:
*   开启 `Row Level Security`，仅允许使用 Anon 密匙进行 `SELECT`, `INSERT`, `UPDATE`, `DELETE`。

---

## 四、 核心功能模块设计

针对 Libgdx 客户端，需要新增以下几个子模块：

### 1. 公网 IP 获知模块 (Public IP Resolver)
*   **需求**：房主建房前，必须知道自己的公网 IP 才能注册给 Supabase。
*   **实现**：使用 `Gdx.net.sendHttpRequest` 请求 `https://api.ipify.org`，获取返回的纯文本 IP 字符串并缓存在内存中。

### 2. Supabase HTTP 框架 (SupabaseClient)
*   **需求**：封装对 Supabase REST API 的基础调用。
*   **实现**：
    *   读取配置文件中的 `SUPABASE_URL` 和 `SUPABASE_PUBLISHABLE_KEY`。
    *   封装统一的 `GET`, `POST`, `PATCH`, `DELETE` 方法，通过 `Gdx.net.HttpRequest` 实现非阻塞异步网络请求。
    *   自动填充 `apikey` 和 `Authorization: Bearer` 请求头。

### 3. 大厅管理器 (RoomManager / LobbyService)
*   **建房 (Create Room)**：房主确定端口，获取公网 IP 后，生成房间名，调用 `POST` 存入数据库，返回记录的 `id`。然后启动 `NetworkManager` 和 UDP 监听。
*   **拉取列表 (Fetch Rooms)**：客户端发出 `GET` 请求，使用过滤参数 `last_ping=gte.now()-30s` (只查过去 30 秒内有心跳的房间)，反序列化为 Java 对象列表展现到 UI。
*   **销毁房间 (Destroy Room)**：房主正常退出时，通过 HTTP `DELETE` 移除自己的房间记录。

### 4. 心跳保活机制 (Heartbeat Task)
*   **痛点**：房主游戏崩溃或强退（如 Alt+F4），无法发出 `DELETE` 请求，会导致大厅出现“死房间”。
*   **解决方案 (客户端驱动)**：
    *   房主成功建房后，启动一个定时器（Libgdx `Timer.schedule`），每 **15 秒**触发一次心跳。
    *   心跳行为：向 Supabase 发出 `PATCH` 请求，更新自身 `id` 的 `last_ping` 为 `now()`。
    *   如果客户端看到某个房间的 `last_ping` 落后当前时间超过 30 秒，说明心跳已断，房主离线，刷新时自动视为死房。

---

## 五、 UI 与交互流程设计

新增场景 `LobbyScreen.java`（取代当前直接进 `NetcodeTankOnlineScreen` 的逻辑）：

1.  **左侧：房间列表区域**
    *   显示：房间名、IP(可选隐藏)、人数进度条 (2/6)、延迟(Ping值)。
    *   底部：[刷新列表] 按钮。
2.  **右侧：控制台面板**
    *   玩家昵称输入框（保存至 `Preferences`）。
    *   [创建新房间] 按钮 -> 弹出建房配置框 -> 获取公网 IP -> 通知 Supabase -> 成功后跳转进入 `NetcodeTankOnlineScreen` (Host模式)。
3.  **连接过渡**
    *   玩家选中列表中某行，点击 [加入房间]。
    *   UI 盖上一层 "Connecting..." 遮罩。
    *   直接读取选中记录的 `host_ip` 和 `host_port`，走底层 `UdpSocketTransport` 协议进行 `0xFF` 握手。
    *   若 5 秒未收到 `0xFE` 握手回复，连接超时，提示 “房间不可达，请检查房主端口转发情况”。

---

## 六、 阶段开发路线规划

为保证 v0.7.0 版本的稳定性能够平滑过渡，建议分为三个阶段开发：

*   **Phase 1: 基础设施构建 (Supabase 接入)**
    *   在 Supabase 控制台建立项目与表结构。
    *   开发 `SupabaseClient` 和 `PublicIPResolver` (获取 IP 模块)。
    *   编写少量独立测试用例验证连通性和增删改查。
*   **Phase 2: 房间管理器与心跳核心**
    *   开发 `RoomManager` 业务封装（Model 与 JSON 映射）。
    *   实现后台 15 秒定时心跳逻辑与退出时的销毁清理。
*   **Phase 3: GUI 面板与集成**
    *   利用 Scene2D 开发大厅 UI（`LobbyScreen`）。
    *   将旧的“手动直连输入框”改为内部隐藏参数传递。
    *   梳理切换流程：Menu -> Lobby -> Gaming，及各种 ESC/Disconnect 返回大厅的链路清理。

---

## 七、 异常与风控说明

1.  **公网与 NAT 穿透**：由于底层是纯 UDP 套接字，本架构*不提供*任何打洞服务。房主的网络必须具备公网 IP 并由其自行在路由器上设置了（Virtual Server / Port Forward）UDP 端口映射！如果玩家反馈“别人能看见我的房间但加不进来”，100% 是 NAT 拦截导致。
2.  **安全性防爆破**：大厅本质是纯前端直接操作数据库，建议在 Supabase 的 API 设置页设定每分钟的请求频控 (Rate Limiting)，防止有人恶意狂刷新建房间 API 把大厅列表塞满垃圾数据。