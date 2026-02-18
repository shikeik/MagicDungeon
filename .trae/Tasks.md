# 智能任务拾取配置:
// - 目标拾取数(0则无上限): 0
// - 开始时间: 2026-02-18 10:00
// - 当前完成时间: 2026-02-18 10:30
// - 当前已耗时: 30min
// - 当前已完成数(开始时置0): 0

## Version 0.x.Refactor - 地牢与存档系统重构
> 主题: 重构数据结构与存档系统，支持目录式存档与多层地图。
> 设计文档: [.trae/documents/designs/40_地牢与存档系统重构设计.md]

### Phase 1: 核心数据与IO (Completed)
- [x] 定义 `AreaData` 数据类
- [x] 在 `MapIO` 实现 `LayerData` 与 `GameMapData` 的转换逻辑
- [x] 重构 `SaveManager` 支持目录结构与 `LayerData` 导入
    - [x] `importAssetsAreas` 支持自动转换 Legacy 格式
    - [x] 验证 `saveLayerData` / `loadLayerData`

### Phase 2: UI与流程 (Completed)
- [x] 更新 `SaveData` 增加 `maxDepth`
- [x] 重构 `GameScreen.enterDungeon` 统一使用 `SaveManager` 加载逻辑
- [x] 清理 `GameScreen` 中废弃的 `visitedLevels` 缓存逻辑
- [x] 修复 `GameScreen` 中错误的 `saveGame` 调用

## Version 0.x.Next - Neon 艺术化升级 (Completed)
> 主题: 将程序化纹理生成全面迁移至 NeonBatch 矢量绘图，提升视觉质量。
> 设计文档: [.trae/documents/designs/35_Neon生成与艺术化升级计划.md]

### Phase 1: 基础设施 (Completed)
- [x] 修复 `NeonGenTestScreen` 闪退问题
- [x] 验证 `NeonBatch` 描边与渐变能力 (参考设计文档 2.2)
- [x] 实现 `NeonGenerator` 基类框架

### Phase 1.5: 工具链优化 (Completed)
- [x] 重构 `NeonGenTestScreen` UI (参考编辑器布局)
    - [x] 实现左右分栏 (工作区/控制面板)
    - [x] 实现模式切换 (Live Vector / Baked Texture)
    - [x] 实现烘焙纹理预览参数调节 (拉伸/定长)

### Phase 2: 核心迁移 (Completed)
- [x] 重写 `createPlayer` (支持纸娃娃)
- [x] 重写 Tiles 生成 (Floor, Wall)
- [x] 重写 Items 生成 (Potion, Sword, Shield, Armor, Boots, Coin)

## 更新清单 (2026-02-17)
1. **NeonTileGenerator**:
   - 实现基于 NeonBatch 的矢量地砖生成 (Floor, Wall Tileset)。
   - 保持原有 Dual Grid 自动平铺逻辑兼容性。
2. **NeonItemGenerator**:
   - 实现矢量化物品图标生成 (Potion, Sword, Shield, Armor, Boots, Coin)。
   - 支持旋转、缩放和图层叠加，提升图标清晰度。
3. **NeonGenTestScreen**:
   - 增加 Generator Type 选择器 (Character, Wall, Floor, Item)。
   - 增加 Item Name 输入框用于测试不同物品生成。
4. **TextureManager**:
   - 全面迁移 Player, Wall, Floor, Items 至 Neon 生成管线。

## 待拾取

## 待检查
