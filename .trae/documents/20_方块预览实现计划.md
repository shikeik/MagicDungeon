# 新材质预览功能开发计划

## 1. 目标
创建一个通用的测试场景 `MaterialPreviewScreen`，用于动态展示游戏中的材质纹理。第一阶段将优先展示 `all_blocks_sheet.png` 中的所有方块纹理。该场景将利用 `CustomAtlasLoader` 加载资源，并动态获取配置的纹理区域。

## 2. 资源配置
创建 `assets/sprites/all_blocks_sheet.json` 描述文件。
- **Grid 设置**: 设定 `gridWidth` 和 `gridHeight` 为 **64** (根据用户指定)。
- **Regions 定义**: 映射索引到名称 (基于纹理内容识别)。
    - Index 0: **tree** (树木 - 包含深绿色树冠和棕色树干)
    - Index 1: **grass** (草地 - 绿色背景带有各色小花)
    - Index 2: **sand** (沙地 - 黄色沙砾质感)
    - Index 3: **stone_road** (石板路 - 灰色不规则石块铺设)
    - Index 4: **portal** (传送门 - 石质拱门结构，内部黑色)

## 3. 核心代码增强
修改 `CustomAtlasLoader` 类，增加数据访问接口。
- **新增方法**: `public java.util.Set<String> getRegionNames(String imagePath)`
- **目的**: 允许外部查询指定 Atlas 中定义了哪些 Region 名称，以便在预览场景中动态遍历显示，无需硬编码列表。

## 4. 新场景实现
创建 `com.goldsprite.magicdungeon.screens.tests.MaterialPreviewScreen`。
- **继承**: `GScreen`
- **视口配置**: 使用 `ExtendViewport(1280, 720, camera)`，确保在不同分辨率下内容不失真且可视范围合理扩展。
- **功能**:
    - **初始化**: 在 `create()` 中调用 `CustomAtlasLoader.inst().getRegion(...)` 预加载 `sprites/all_blocks_sheet.png`。
    - **数据获取**: 调用 `getRegionNames` 获取所有定义的材质名称。
    - **布局**: 
        - 使用 `SpriteBatch` 绘制。
        - 自动排列材质：从左上角开始，每行显示固定数量（例如 6 个），自动换行。
        - 每个条目显示：64x64 的纹理图标 + 下方显示材质名称。
    - **交互**: 
        - 保留相机控制（拖拽平移、滚轮缩放），参考 `TexturePreviewScreen` 中的 `SimpleCameraController`。
    - **HUD**: 绘制简要操作说明文本。

## 5. 入口注册
修改 `TestSelectionScreen.java`。
- 在 `initScreenMapping` 中注册 "**新材质预览**" -> `MaterialPreviewScreen.class`。

## 6. 验证计划
- 启动游戏进入 "新材质预览"。
- 确认视口适配正常，调整窗口大小时内容无拉伸。
- 确认以下材质图标和名称正确显示：
    - tree, grass, sand, stone_road, portal
- 确认纹理清晰，无切片错误（基于 64x64 Grid）。
