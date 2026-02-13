# 背包 Tooltip 系统分析与重构方案

## 1. 现状分析

经过对 `GameHUD.java` 及相关代码的深入分析，当前背包系统中的物品信息展示（Tooltip）存在三种不同的实现方式，导致了视觉上的冗余和交互体验的不一致。

### 1.1 三种现存的展示机制

1.  **键盘焦点 Tooltip (Focus Tooltip)**
    *   **触发条件**: 使用键盘（WASD/方向键）或手柄在背包/装备栏中移动光标，使格子获得焦点 (`setFocused(true)`) 时触发。
    *   **位置**: 动态计算，通常显示在目标格子的**右侧**。如果右侧空间不足，则翻转到左侧。
    *   **实现**: `GameHUD.showFocusTooltip(Actor target, InventoryItem item, ...)`。
    *   **代码位置**: `InventorySlot.setFocused` 和 `EquipmentSlot.setFocused` 中调用。

2.  **鼠标悬浮 Tooltip (Mouse Hover Tooltip)**
    *   **触发条件**: 鼠标指针悬浮在物品格子上时触发。
    *   **位置**: 跟随鼠标指针或显示在鼠标附近（LibGDX VisUI 默认行为）。
    *   **实现**: 使用 VisUI 的标准 `Tooltip` 组件。
    *   **代码位置**: `InventorySlot` 和 `EquipmentSlot` 的构造函数中 `new Tooltip.Builder(...)`。

3.  **操作反馈提示 (Action Feedback)**
    *   **触发条件**: 执行装备、卸下、使用物品等操作时触发。
    *   **位置**: 屏幕**左下角**（系统日志区域 `msgLabel`），用户可能感知为“中下固定位置”。
    *   **实现**: `GameHUD.showMessage(String msg)`。
    *   **内容**: 简单的文本提示，如“装备 铁剑”、“使用 红色药水”。

### 1.2 存在的问题

*   **视觉冲突与冗余**: 当用户使用鼠标悬浮在一个被键盘选中的格子上时，**Focus Tooltip** 和 **Mouse Hover Tooltip** 会同时出现。由于它们显示的内容几乎相同（都是物品详情），这造成了严重的视觉杂乱。
*   **位置不统一**: 信息的展示位置取决于输入方式（键盘 vs 鼠标），导致用户视线需要在不同区域跳跃，体验不连贯。
*   **代码逻辑分散**: Tooltip 的创建和管理分散在 `InventorySlot`、`EquipmentSlot` 和 `GameHUD` 中，维护成本高。

## 2. 解决方案：统一固定详情面板 (Fixed Detail Panel)

为了彻底解决上述问题，建议废弃所有弹出的 Tooltip，转而采用**固定区域显示物品详情**的方案。这也是许多经典 RPG 游戏（如《暗黑破坏神》、《星露谷物语》的主机版）的标准设计。

### 2.1 核心设计理念

*   **单一数据源**: 无论通过鼠标悬浮还是键盘选择，信息的展示终端只有一个。
*   **固定位置**: 在背包 UI 中开辟一块专用区域（Detail Panel），用于展示当前选中物品的详细信息。
*   **即时响应**: 焦点改变或鼠标划过时，立即更新面板内容。

### 2.2 UI 布局改造

建议调整 `InventoryDialog` 的布局，在现有两栏布局的基础上进行优化：

*   **当前布局**:
    *   左栏 (30%): 装备槽 (Equipment) + 属性 (Stats)
    *   右栏 (70%): 物品列表 (Inventory List)

*   **建议布局**:
    *   将 **属性 (Stats)** 移动到更紧凑的位置，或者与装备栏整合。
    *   在 **底部** 或 **右侧** 新增一个 **详情面板 (Detail Panel)**。
    *   或者，将右栏一分为二：上方为物品列表，下方为详情面板。

### 2.3 交互逻辑重构

1.  **移除旧机制**:
    *   删除 `InventorySlot` 和 `EquipmentSlot` 中的 `new Tooltip.Builder(...)` 代码。
    *   删除 `GameHUD` 中的 `showFocusTooltip`、`hideFocusTooltip` 方法及其调用。

2.  **引入新机制**:
    *   在 `GameHUD` 或 `InventoryDialog` 中创建一个 `updateDetailPanel(InventoryItem item)` 方法。
    *   **键盘交互**: 在 `setFocused(true)` 时，调用 `updateDetailPanel(this.item)`。
    *   **鼠标交互**: 给每个 Slot 添加 `InputListener`，监听 `enter` (鼠标进入) 事件。当鼠标进入时，调用 `updateDetailPanel(this.item)` 并可选地将该 Slot 设置为焦点（实现鼠标与键盘焦点的同步）。

### 2.4 预期效果

*   **整洁**: 界面上不再有飘忽不定的浮窗，所有信息井然有序。
*   **统一**: 无论使用手柄、键盘还是鼠标，查看物品详情的体验完全一致。
*   **高效**: 玩家可以快速浏览背包，视线只需固定在详情面板区域，无需跟随光标移动。

## 3. 具体实施步骤 (Task List)

1.  **备份代码**: 确保当前工作区代码已提交或备份。
2.  **清理旧代码**:
    *   注释掉或删除 `GameHUD.java` 中关于 `Focus Tooltip` 的逻辑。
    *   注释掉或删除 Slot 类中关于 `VisUI Tooltip` 的逻辑。
3.  **UI 布局调整**:
    *   修改 `InventoryDialog` 的构造函数。
    *   创建一个新的 `VisTable` 作为 `detailPanel`。
    *   将其放置在合适的布局位置（推荐：右侧物品列表下方，固定高度）。
4.  **实现详情渲染**:
    *   将原有的 `createItemTooltipTable` 方法逻辑迁移到 `updateDetailPanel` 中，用于填充 `detailPanel`。
5.  **绑定事件**:
    *   修改 `InventorySlot` 和 `EquipmentSlot`，在 `setFocused` 和鼠标 `enter` 事件中触发面板更新。
6.  **验证**:
    *   测试键盘导航，确认详情面板随光标更新。
    *   测试鼠标悬浮，确认详情面板随鼠标更新。
    *   确认无残留的弹出框。

---
*本文档由 AI 助手生成，用于指导 MagicDungeon 项目背包系统的 UI 重构。*
