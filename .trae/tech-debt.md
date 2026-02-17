# 技术债与优化清单 (Tech Debt & Optimization Backlog)

> 本文件用于记录代码审查中发现的非阻断性问题、重构计划及性能优化点。
> **P0 级严重问题请直接修复，不要记录在此。**

## 待处理 (Pending)

### 高优先级 (High Priority - P1)
- [x] [P1] Core: GameScreen 资源释放缺失
  - 详情: `GameScreen` 类中创建了 `NeonBatch`, `PolygonSpriteBatch`, `SkeletonRenderer`, `TextureAtlas` (wolfAtlas), `VFXManager` 等资源，但未覆写 `dispose()` 方法进行释放，切换屏幕或退出时会导致严重的内存泄漏。
  - 提出日期: 2026-02-17
  - 完成日期: 2026-02-17

- [x] [P1] UI: GameHUD 资源释放缺失
  - 详情: `GameHUD` 类创建了 `Stage` 以及 `slotBgTexture`, `whiteTexture` 等多个纹理资源，但未提供 `dispose()` 方法供外部调用，导致资源泄漏。
  - 提出日期: 2026-02-17
  - 完成日期: 2026-02-17

### 中优先级 (Medium Priority - P2)
- [ ] [P2] Assets: Spine 资源加载硬编码
  - 详情: `GameScreen.create()` 中直接加载 `spines/wolf/exports/spine_108_02.atlas`，路径硬编码且未通过 `AssetManager` 或统一的 Loader 管理，导致资源重复加载和管理困难。
  - 提出日期: 2026-02-17

- [ ] [P2] World: WorldMapScreen 地图节点数据硬编码
  - 详情: `initNodes()` 方法中硬编码了所有地牢节点的坐标、颜色、名称等数据。建议提取到 JSON/XML 配置文件或专门的数据类中。
  - 提出日期: 2026-02-17

- [ ] [P2] UI: GameHUD 类职责过重
  - 详情: `GameHUD` 包含了 UI 构建、库存逻辑、装备逻辑、甚至 `GameOverWindow` 的实现。建议将 `GameOverWindow`, `InventoryDialog` 等拆分为独立类。
  - 提出日期: 2026-02-17

### 低优先级 (Low Priority - P3)
*(暂无)*

## 已完成 (Completed)
*(暂无)*
