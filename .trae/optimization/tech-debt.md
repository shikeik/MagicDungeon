# 技术债与优化清单 (Tech Debt & Optimization Backlog)

> 本文件用于记录代码审查中发现的问题、重构计划及性能优化点。所有条目需严格按照优先级标准分类。

## 优先级标准定义 (Priority Standards)

| 级别 | 定义 (Definition) | 判定标准 (Criteria) | 响应时效 (Response) | 代码示例 (Example) |
| :--- | :--- | :--- | :--- | :--- |
| **P0** | **阻断性 (Critical)** | 系统崩溃、无法启动、核心数据丢失、无法构建、严重安全漏洞。 | **立即修复**，阻断后续开发。 | `throw new RuntimeException("Game Crash");` 导致启动失败。 |
| **P1** | **严重 (High)** | 核心功能受损、严重内存泄漏、严重性能掉帧(<30FPS)、主要逻辑错误。 | **当前版本/迭代必须修复**。 | `texture = new Texture(...)` 但从未调用 `dispose()`。 |
| **P2** | **重要 (Medium)** | 代码结构混乱(难以维护)、硬编码、次要功能缺陷、缺乏错误处理。 | **列入计划**，后续迭代修复。 | `if (id == 1001) { ... }` (Magic Number/硬编码)。 |
| **P3** | **优化 (Low)** | 代码风格问题、注释缺失、命名不规范、微小UI/UX瑕疵、建议性重构。 | **Backlog**，有空闲时修复。 | `int a = 1;` (命名不规范) 或 缺少 JavaDoc。 |

## 维护规则 (Maintenance Rules)

1. **分类原则**: 所有新增条目必须根据上述标准标记 P0-P3。
2. **完成归档**: 
   - 当任务标记为完成 (`[x]`) 后，**必须**将其从 "待处理 (Pending)" 区域移动到 "已完成 (Completed)" 区域。
   - 必须记录 "完成日期"。
3. **定期审查**: 每周审查一次清单，升级或降级任务优先级。

---

## 待处理 (Pending)

### 严重问题 (P0)
*(暂无 - 保持代码库健康)*

### 高优先级 (High Priority - P1)
*(暂无)*

### 中优先级 (Medium Priority - P2)
- [ ] [P2] World: WorldMapScreen 地图节点数据硬编码
  - **详情**: `initNodes()` 方法中硬编码了所有地牢节点的坐标、颜色、名称等数据。
  - **建议**: 提取到 JSON/XML 配置文件或专门的数据类 (`MapConfig`) 中，实现数据驱动。
  - **提出日期**: 2026-02-17

- [ ] [P2] UI: GameHUD 类职责过重 (God Class)
  - **详情**: `GameHUD` 包含了 UI 构建、库存逻辑、装备逻辑、甚至 `GameOverWindow` 的实现，代码耦合度高。
  - **建议**: 将 `GameOverWindow`, `InventoryDialog` 等拆分为独立类，采用 MVC 或 MVP 模式解耦。
  - **提出日期**: 2026-02-17
  - **状态**: 部分完成 (已提取 `CombatLogUI`)。

- [ ] [P2] VFX: VFXManager 缺少清理方法
  - **详情**: `VFXManager` 维护了粒子列表，但未提供清理机制。
  - **建议**: 添加 `dispose()` 或 `clear()` 方法，在场景切换时彻底清理粒子引用，防止潜在的内存占用积累。
  - **提出日期**: 2026-02-17
  - **调整**: 原 P3，升级为 P2，因涉及资源引用管理。

- [ ] [P2] Utils: SpriteGenerator 严重硬编码
  - **详情**: 包含数千行绘图代码，所有颜色、坐标均为 Magic Numbers。
  - **建议**: 引入 `ThemeConfig` 提取关键参数。
  - **提出日期**: 2026-02-17
  - **状态**: 部分完成 (已引入 `ThemeConfig` 并应用于部分生成器)。

### 低优先级 (Low Priority - P3)
- [ ] [P3] Core: GameScreen CameraController 逻辑完善
  - **详情**: `GameScreen.initViewport` 中 `SimpleCameraController` 的 activationCondition 有个 TODO 需要实现。
  - **建议**: 实现 `activationCondition`，仅在光标落在 UI 区域外时激活相机控制。
  - **提出日期**: 2026-02-17

## 已完成 (Completed)

- [x] [P1] Utils: TextureExporter 资源泄漏
  - **详情**: `run()` 方法中创建了 `SpriteBatch` 和 `FrameBuffer`，但方法结束时未调用 `dispose()`，导致严重显存泄漏。
  - **解决**: 使用 `try-finally` 块确保资源释放。
  - **完成日期**: 2026-02-17

- [x] [P1] Core: GameScreen 资源释放缺失
  - **详情**: `GameScreen` 类中创建了 `NeonBatch`, `PolygonSpriteBatch`, `SkeletonRenderer`, `TextureAtlas` (wolfAtlas), `VFXManager` 等资源，但未覆写 `dispose()` 方法进行释放。
  - **解决**: 已实现 `dispose()` 方法。
  - **完成日期**: 2026-02-17

- [x] [P1] UI: GameHUD 资源释放缺失
  - **详情**: `GameHUD` 类创建了 `Stage` 以及 `slotBgTexture`, `whiteTexture` 等多个纹理资源，但未提供 `dispose()` 方法。
  - **解决**: 已实现 `dispose()` 方法。
  - **完成日期**: 2026-02-17

- [x] [P2] Core: GameScreen 渲染批次切换频繁
  - **详情**: 在 `render()` 循环中多次切换 `batch.begin()/end()` 和 `polyBatch.begin()/end()`。
  - **解决**: 优化渲染顺序，合并批次调用。
  - **完成日期**: 2026-02-17

- [x] [P2] VFX: VFXManager 混合渲染批次
  - **详情**: 同时持有 `render(NeonBatch)` 和 `renderText(SpriteBatch)`，导致外部调用混乱。
  - **解决**: 统一使用 `Batch` 接口。
  - **完成日期**: 2026-02-17

- [x] [P3] Tests: ScrollLayoutTestScreen 位置不当
  - **详情**: 测试代码混在 `screens` 包中。
  - **解决**: 移动到 `tests` 包。
  - **完成日期**: 2026-02-17

- [x] [P2] Assets: Spine 资源加载硬编码
  - **详情**: `GameScreen.create()` 中直接加载 `spines/wolf/exports/spine_108_02.atlas`，路径硬编码且未通过 `AssetManager` 或统一的 Loader 管理。
  - **解决**: 已重构为使用 `AssetManager` 管理，或移除了硬编码加载。
  - **提出日期**: 2026-02-17
  - **完成日期**: 2026-02-17
