# 背包格重构方案

## 1. 现状分析
当前 `GameHUD` 中的背包系统实现较为简单，存在以下问题：
*   **代码耦合**: 背包格子的创建逻辑直接写在 `updateInventory` 方法中，缺乏封装，难以维护和复用。
*   **布局局限**: 使用简单的 `VisTable` 添加图片和文字，缺乏层叠能力（Stack），导致无法方便地在图标上叠加状态角标（如"已装备"标识）。
*   **交互缺失**: 仅支持点击操作，缺乏鼠标悬浮提示（Tooltip），玩家无法查看物品详细信息（如属性、类型）。
*   **视觉反馈弱**: 已装备状态仅通过改变背景边框颜色表示，不够直观。

## 2. 需求定义
根据用户指示，本次重构需满足以下需求：

### 2.1 结构封装
*   将背包格子逻辑封装为 `GameHUD` 的内部类 `InventorySlot`。
*   该类应独立管理自身的 UI 状态和数据更新。

### 2.2 布局优化
*   使用 `Stack` 布局作为核心容器，确保 UI 元素可以层叠显示。
*   层级结构应为：背景 -> 物品图标 -> 状态角标(右上角)。

### 2.3 视觉元素
*   **背景格**: 统一的格子底图。
*   **物品 Icon**: 显示物品图片。
*   **右上角标**: 当物品被装备时，在右上角显示 "E" 字样（Equipped）；未装备时不显示。

### 2.4 交互增强 (Tooltip)
*   **通用 Tooltip**: 封装一个通用的 Tooltip 控件（类似 Toast 风格）。
*   **物品信息**: 鼠标悬浮在格子上时，显示物品详细信息：
    *   物品名称
    *   物品类型
    *   物品属性
    *   是否已装备状态

## 3. 技术方案

### 3.1 InventorySlot 类设计
```java
private class InventorySlot extends VisTable {
    private Stack stack;
    private Image bgImage;
    private Image iconImage;
    private VisLabel equipBadge; // "E"
    
    public InventorySlot(InventoryItem item) {
        // 初始化 UI
        // 设置 Tooltip
    }
    
    public void setItem(InventoryItem item) {
        // 更新图标、角标状态
    }
}
```

### 3.2 Tooltip 实现
使用 VisUI 的 `Tooltip` 组件，自定义其 Content Table。
*   创建一个辅助方法或类 `UIToolkit.createItemTooltip(Item item)` 返回配置好的 Tooltip。
*   内容布局：
    *   Title: 物品名称 (颜色区分稀有度)
    *   Type: 物品类型
    *   Stats: 攻击/防御力等
    *   Status: "已装备" (如果有)

### 3.3 实施步骤
1.  **定义内部类**: 在 `GameHUD` 中定义 `InventorySlot`。
2.  **实现 UI 布局**: 使用 `Stack` 组装背景、图标、角标。
3.  **实现 Tooltip**: 编写构建 Tooltip 内容的逻辑。
4.  **替换旧逻辑**: 修改 `updateInventory` 方法，使用新的 `InventorySlot` 替代原有的构建代码。
