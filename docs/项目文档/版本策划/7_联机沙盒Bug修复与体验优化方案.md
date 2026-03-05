# 6.0 联机沙盒 Bug 修复与体验优化方案

## 一、问题清单

| # | 分类 | 现象 | 根因 |
|---|------|------|------|
| B1 | Bug | 客户端坦克移速飞快 | `rpcMoveInput` 每帧调用一次，每次用固定 `dt=1/60`；但 UDP 收包线程将多帧积压 RPC 在同一 Server 帧批量处理，导致单帧内叠加多次位移 |
| B2 | Bug | 客户端子弹击中后不消失、穿过去 | Client 仅做越界移除，无碰撞/销毁通知；Server 端销毁子弹后未同步给 Client |
| B3 | Bug | 3 进程时坦克颜色/位置不同步 | `sendExistingSpawnsToClient` 仅补发 SpawnPacket（不含状态），新客户端拿到的都是默认值(0,0,RED)。同时退出屏幕时未断开连接 / 通知 Server |
| B4 | 体验 | 窗口 Resize 时画面不刷新 | `NeonBatch.begin()` 未设置投影矩阵，没用 GScreen 自带的 `uiViewport` / `worldCamera`，resize 后坐标系失配 |

---

## 二、修复方案

### B1 — 客户端移速飞快

**方案**: 改为 Server 权威速度模型。Client 只发送归一化方向 `(dx, dy)∈{-1,0,1}`，Server 在自己的 `updateServerLogic` 里统一用 `delta` 驱动所有坦克（包括远程坦克）。

**改动点**:
1. `TankBehaviour.rpcMoveInput(dx, dy)` → 不再内部做位移，仅存储 `pendingMoveX/Y`
2. `NetcodeTankOnlineScreen.updateServerLogic()` → 遍历所有远程坦克的 `pendingMove`，用同一 `speed * delta` 执行位移
3. Client 端发送 RPC 改为 `isKeyJustPressed` 检测方向键按下/释放边沿，或者保持每帧发送但 Server 只存储最新方向

**选定**: 每帧发送方向，Server 只记录最新方向。这样最简单、延迟最低。

### B2 — 客户端子弹不消失

**方案**: Server 碰撞命中后通过 **ClientRpc** 通知所有 Client 销毁对应子弹。由于子弹本身没有 netId，改用「坦克+子弹索引」不可靠。更简洁的做法：

**选定**: 给每颗子弹分配一个自增 bulletId（Server 端全局），SpawnBullet 时携带 bulletId，HitBullet 时广播 `rpcDestroyBullet(bulletId)` 通知客户端移除。

**改动点**:
1. `Bullet` 增加 `int bulletId` 字段
2. `NetcodeTankOnlineScreen` Server 端维护 `nextBulletId` 自增器
3. `TankSandboxUtils.spawnBullet()` 接受 bulletId 参数，ClientRpc `rpcSpawnBullet` 携带 bulletId
4. 新增 `TankBehaviour.@ClientRpc rpcDestroyBullet(int bulletId)`，Client 端从 localBullets 移除
5. Server 端碰撞命中时调用 `rpcDestroyBullet`

### B3 — 多客户端颜色/位置不同步

**方案**: 在 `sendExistingSpawnsToClient` 后，再向新客户端发送所有已有实体的**完整状态快照**（全量 StatusSync）。

**改动点**:
1. `NetworkManager` 新增 `sendFullStateToClient(int clientId)` — 对所有已注册实体做全量序列化并 `sendToClient`
2. `NetcodeTankOnlineScreen.onNewClientConnected()` 调用链: `sendExistingSpawnsToClient` → `spawnTankForOwner` → `sendFullStateToClient`（确保新坦克的初始状态也被发出）
3. `NetcodeTankOnlineScreen.dispose()` 增加断开连接+清理逻辑（已有 `transport.disconnect()`，确认是否足够）

### B4 — 视口变换时画面不刷新

**方案**: 在 `renderPlaying` / `renderConfig` / `renderWaiting` 的 `neon.begin()` 前设置投影矩阵为 GScreen 的 `uiViewport.getCamera().combined`。同时确保 `render()` 调用 `super.render(delta)` 后视口已正确 apply。

**改动点**:
1. 所有 `neon.begin()` 前加 `neon.setProjectionMatrix(uiViewport.getCamera().combined)`
2. 坦克坐标系统改用 uiViewport 世界坐标（不再用 `Gdx.graphics.getWidth/Height` 做边界，用 `uiViewport.getWorldWidth/Height`）
3. 确认 `super.render(delta)` 调用了 `uiViewport.apply()`，否则手动 apply

---

## 三、实施顺序

| 步骤 | 内容 | 文件 |
|------|------|------|
| S1 | B1: 移速修复 — pendingMove + Server 权威驱动 | TankBehaviour, NetcodeTankOnlineScreen |
| S2 | B2: 子弹同步 — bulletId + rpcDestroyBullet | Bullet, TankBehaviour, TankSandboxUtils, NetcodeTankOnlineScreen |
| S3 | B3: 全量状态补发 + 断连清理 | NetworkManager, NetcodeTankOnlineScreen |
| S4 | B4: 视口投影矩阵 | NetcodeTankOnlineScreen |
| S5 | 编译 + 测试 + 提交 | — |

---

## 四、验收标准

1. ✅ Server/Client 坦克移速一致（200px/s）
2. ✅ 子弹击中后在所有端同步消失
3. ✅ 第 3 个客户端加入时能看到前 2 辆坦克的正确颜色和位置
4. ✅ 窗口 Resize 后画面正确刷新
5. ✅ 退出屏幕时断开连接，91 测试全绿
