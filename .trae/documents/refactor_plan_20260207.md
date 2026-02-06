# 2026-02-07 重构策划案

## 1. 概览
本策划案旨在优化 `MagicDungeon` 项目的代码质量和开发体验，主要涉及相机控制器的文档完善、预览场景的交互优化以及纹理管理器的接口统一。

## 2. 任务列表

### 2.1 优化 SimpleCameraController
*   **目标**: 为 `SimpleCameraController.java` 添加详细的中文 Javadoc 注释，解释其核心逻辑（坐标映射、激活条件、手势与鼠标处理等），便于团队成员理解和使用。
*   **执行**:
    *   对类、接口、方法添加标准 Javadoc。
    *   解释 `CoordinateMapper` 和 `activationCondition` 的作用。

### 2.2 升级 TexturePreviewScreen
*   **目标**: 将 `TexturePreviewScreen.java` 中的简易输入处理替换为功能更强大的 `SimpleCameraController`，支持更流畅的平移和缩放操作。
*   **执行**:
    *   移除原有的 `InputAdapter` 匿名内部类。
    *   实例化 `SimpleCameraController` 并绑定到 `camera`。
    *   设置 `InputProcessor`。

### 2.3 重构 TextureManager
*   **目标**: 统一材质获取接口，消除必须依赖枚举类型的限制，支持统一的 String Key 获取方式。
*   **现状**:
    *   Tiles 使用英文枚举名 (e.g., "WALL")。
    *   Monsters 使用中文显示名 (e.g., "史莱姆")。
    *   Items 使用中文显示名 (e.g., "生锈的剑")。
    *   获取方式分散 (`getTile`, `getMonster`, `getItem`)。
*   **方案**:
    *   在 `loadAll` 中，为 Monster 和 Item 同时注册 **英文枚举名** 和 **中文显示名** 作为 Key。
    *   确保 `get(String key)` 可以通过英文 ID (如 "SLIME", "RUSTY_SWORD") 获取资源。
    *   保留 `getTile`, `getMonster` 等辅助方法以维持兼容性，但推荐直接使用 `get(String key)`。
    *   更新 `TexturePreviewScreen` 中的调用方式，展示统一接口的便利性。

## 3. 验收标准
1.  `SimpleCameraController` 包含清晰的中文文档。
2.  `TexturePreviewScreen` 操作流畅，支持鼠标拖拽和平滑缩放。
3.  `TextureManager` 支持通过 `tm.get("SLIME")` 和 `tm.get("史莱姆")` 获取同一张纹理。
4.  代码编译通过，运行无异常。
