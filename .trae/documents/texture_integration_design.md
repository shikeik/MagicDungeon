# 纹理资源整合与混合系统设计方案

## 1. 背景与目标
当前游戏使用程序化生成（Procedural Generation）的方式在运行时生成纹理。为了提升视觉质量，项目中引入了基于美术素材（Assets）的纹理资源。
本方案的目标是设计一套“混合纹理系统”，使游戏能够：
1.  **优先加载美术资源**：对于已存在美术素材的物体，优先使用美术资源。
2.  **兼容生成式纹理**：对于尚未有美术素材的物体，继续使用代码生成，确保游戏功能完整。
3.  **平滑过渡**：支持在开发过程中逐步用美术资源替换生成式纹理。

## 2. 现有资源分析
项目 `assets/sprites` 目录下已包含以下图集资源：
*   **all_blocks_sheet** (`.png`, `.json`): 地形与环境图块（树、草地、沙地等）。
*   **all_entities_sheet** (`.png`, `.json`): 角色与怪物（史莱姆、兽人、骑士等）。
*   **all_items_sheet** (`.png`, `.json`): 道具与装备（剑、斧、法杖、盾牌等）。

资源格式为自定义 JSON 描述的 Grid Sheet（64x64像素网格），Key 为 `snake_case` 格式（如 `rusty_sword`），而代码中 Enum 为 `PascalCase` 或带下划线（如 `Rusty_Sword`）。

## 3. 技术架构设计

### 3.1 核心变更：引入 TextureRegion
目前 `TextureManager` 管理的是 `Texture` 对象。为了支持图集（Sprite Sheet），必须将其升级为管理 `TextureRegion`。
*   **原因**：图集是将多个小图合并在一张大图（Texture）中，每个小图只是大图的一个区域（Region）。
*   **兼容性**：LibGDX 的渲染接口（如 `Batch.draw`）和 UI 组件（如 `TextureRegionDrawable`）均完美支持 `TextureRegion`。生成式纹理生成的独立 `Texture` 也可以轻松包装为 `TextureRegion`。

### 3.2 模块设计

#### A. 资源加载器 (SpriteSheetLoader)
新增一个辅助类，负责读取 JSON 配置文件并切割纹理。
*   **输入**：`.json` 文件路径和对应的 `.png` 纹理。
*   **处理**：
    1.  解析 JSON 获取网格大小 (`gridWidth`, `gridHeight`) 和区域定义 (`regions`)。
    2.  使用 `TextureRegion.split()` 切割纹理。
    3.  建立 `Map<String, TextureRegion>`，Key 统一规范化为 **小写 (Lower Case)** 以忽略大小写差异。

#### B. 纹理管理器 (TextureManager) 重构
*   **存储结构**：将 `Map<String, Texture> textureCache` 修改为 `Map<String, TextureRegion> regionCache`。
*   **初始化流程 (`loadAll`)**：
    1.  **第一步：加载资源式纹理**。调用 `SpriteSheetLoader` 加载所有 `.sheet.json`，存入缓存。
    2.  **第二步：加载生成式纹理（补全）**。遍历 `TileType`, `MonsterType`, `ItemData`：
        *   检查缓存中是否已存在对应的 Key（将 Enum Name 转为小写后查找）。
        *   **如果存在**：跳过生成，使用美术资源。
        *   **如果不存在**：调用 `SpriteGenerator` 生成纹理，包装为 `TextureRegion` 并存入缓存。
*   **对外接口**：
    *   修改 `get(String key)` 返回 `TextureRegion`。
    *   保留 `getTexture(String key)` 但标记为 `@Deprecated`（如果必须），或者完全移除并修改调用处。
    *   **注意**：`dispose()` 方法需要修改，只销毁底层的 `Texture`（去重后），避免重复销毁。

### 3.3 键值映射策略 (Key Mapping)
为了解决资源 Key (`rusty_sword`) 与代码 Enum (`Rusty_Sword`) 不一致的问题：
*   **统一规则**：所有 Key 在存取时统一转换为 **小写**。
    *   资源加载时：`rusty_sword` -> `rusty_sword`
    *   代码获取时：`Rusty_Sword` -> `rusty_sword`
*   这样可以自动匹配，无需维护繁琐的映射表。

## 4. 实施计划 (逐步执行)

### 阶段一：基础设施 (Infrastructure)
1.  创建 `com.goldsprite.magicdungeon.assets.SpriteSheetLoader` 类。
2.  修改 `TextureManager`，引入 `TextureRegion` 支持。
3.  实现“优先加载资源，回退到生成”的逻辑。

### 阶段二：调用处适配 (Refactoring)
1.  修改 `ItemRenderer`, `GameHUD`, `GameScreen` 等渲染代码。
    *   将 `batch.draw(texture, ...)` 改为 `batch.draw(region, ...)`（通常参数兼容，无需大改）。
    *   将 `new TextureRegionDrawable(texture)` 改为 `new TextureRegionDrawable(region)`。

### 阶段三：资源验证与清理
1.  启动游戏，检查哪些物体使用了新纹理，哪些仍使用旧纹理。
2.  确认无误后，可以在 `SpriteGenerator` 中逐步注释掉已被替代的生成逻辑（可选，为了节省包体积）。

## 5. 风险控制
*   **代码变更风险**：涉及 `TextureManager` 这一核心类，影响范围广。
    *   *对策*：先在 `examples` 模块的 `MaterialPreviewScreen` 进行测试，验证加载逻辑无误后再应用到主游戏逻辑。
*   **内存管理**：图集纹理不应被频繁创建/销毁。
    *   *对策*：`TextureManager` 为单例，常驻内存，符合预期。

## 6. 待确认事项
*   是否同意修改 `TextureManager` 的返回类型为 `TextureRegion`？这是最干净的方案，但涉及修改调用处。
*   如果不同意修改返回类型，则需要通过 `Pixmap` 复制像素来创建独立 `Texture`，这会增加内存消耗和加载时间，**强烈不推荐**。

请确认以上方案，特别是关于代码重构的部分。
