# 2026-02-07 第二次重构策划案

## 1. 概览
本次重构主要针对代码规范、枚举命名、纹理管理以及预览工具进行优化。目标是消除编译错误，规范化命名（采用 Mmm_Mmm 风格），简化纹理预览逻辑，并清理代码中的全限定名引用。

## 2. 任务列表

### 2.1 修复编译错误
*   **SpriteGenerator**: `Color.SILVER` 在 LibGDX 中不存在。
    *   **方案**: 替换为 `Color.LIGHT_GRAY` 或自定义银色常量。

### 2.2 重构枚举命名 (Mmm_Mmm 风格)
*   **MonsterType**: `SLIME` -> `Slime`, `SKELETON` -> `Skeleton`, ...
*   **ItemData**: `Rusty_Sword` -> `Rusty_Sword`, `Iron_Sword` -> `Iron_Sword`, ...
*   **TileType**: `WALL` -> `Wall`, `FLOOR` -> `Floor`, `Stairs_Down` -> `Stairs_Down`, ...
*   **执行**: 修改枚举定义，并全局搜索替换引用。

### 2.3 优化 TextureManager
*   **移除中文 Key**: 不再将物体的中文显示名（如 "史莱姆"）作为 Key 注册到纹理缓存中。
*   **统一 Key**: 仅保留枚举名（如 "Slime"）作为 Key。
*   **影响**: 确保所有获取纹理的地方都使用英文 Key。

### 2.4 重写 TexturePreviewScreen
*   **目标**: 极简代码，不再按类型分类。
*   **方案**:
    *   移除所有硬编码的分类逻辑（Tiles, Monsters, Items）。
    *   直接遍历 `TextureManager` 中的所有 Key-Value 对。
    *   网格化罗列展示所有纹理。

### 2.5 全局优化 Imports
*   **目标**: 禁止在代码块中直接使用全限定类名（如 `com.badlogic.gdx.math.Vector2`）。
*   **执行**:
    *   扫描代码库。
    *   提取全限定名。
    *   添加 `import` 语句。
    *   替换为简写类名。

## 3. 验收标准
1.  代码编译通过，无 `Color.SILVER` 错误。
2.  枚举命名符合 `Mmm_Mmm` 规范。
3.  `TextureManager` 内部 Map 不包含中文字符 Key。
4.  `TexturePreviewScreen` 能正常展示所有纹理。
5.  代码中无不必要的全限定名引用。
