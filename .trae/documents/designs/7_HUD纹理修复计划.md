# HUD 材质丢失问题分析与修复策划案

## 1. 问题现状分析

当前游戏中，HUD（`GameHUD`）的背包界面无法显示物品图标，导致数据看起来为空。经过代码分析，发现存在以下问题链：

1.  **资源键值不匹配**：
    *   **TextureManager**：在加载资源时，使用 `ItemData` 枚举的**英文名称**（如 `Rusty_Sword`）作为 Key 存储材质。
    *   **GameScreen**：在初始化 HUD 时，构建了一个 `itemTexMap`，但使用了 `ItemData.name` 字段（**中文名称**，如 `生锈的剑`）作为 Key 去 `TextureManager` 查询。
    *   **结果**：`TextureManager.get("生锈的剑")` 返回 `null`，导致传递给 HUD 的 Map 中所有材质都为 `null`。

2.  **查询方式错误**：
    *   **GameHUD**：在 `InventorySlot` 类中，尝试使用 `item.data.name()`（**英文名称**）去查询上述传入的 Map。
    *   **结果**：Map 中存储的 Key 是中文名，而查询用的是英文名，且 Map 中的 Value 本身就是 `null`。这导致双重错误，图标无法显示。

3.  **架构冗余**：
    *   `TextureManager` 已经是一个全局单例，负责管理所有材质。
    *   `GameHUD` 没有必要自己维护一个 `itemTextures` Map，更不需要由 `GameScreen` 负责注入。这种依赖注入增加了不必要的耦合，且容易导致上述的 Key 不一致问题。

## 2. 修复方案

为了彻底解决问题并优化代码结构，采取以下修复步骤：

### 2.1 重构 GameHUD

*   **移除冗余字段**：删除 `private Map<String, Texture> itemTextures;` 及其 Setter 方法 `setItemTextures`。
*   **直接对接管理器**：在 `InventorySlot` 内部类中，不再从 `itemTextures` 获取材质，而是直接调用 `TextureManager.getInstance().getItem(item.data.name())`。

### 2.2 清理 GameScreen

*   **移除无效代码**：删除 `create()` 方法中构建 `itemTexMap` 并传递给 `hud` 的相关代码块。

## 3. 执行计划

1.  修改 `core/src/main/java/com/goldsprite/magicdungeon/ui/GameHUD.java`。
2.  修改 `core/src/main/java/com/goldsprite/magicdungeon/core/screens/GameScreen.java`。
3.  验证修复结果（代码逻辑检查）。

## 4. 预期结果

*   HUD 背包界面将正确显示所有物品的图标。
*   消除了 `GameScreen` 和 `GameHUD` 之间关于材质传递的耦合。
*   修复了中英文 Key 不匹配导致的逻辑错误。
