# 背包 Tooltip 系统分析与重构方案 (v2)

## 1. 现状分析 (Updated)

当前背包系统存在三种物品信息展示方式（键盘焦点旁、鼠标悬浮、左下角操作反馈），导致视觉冗余和体验割裂。特别是在混合操作时（如鼠标悬浮在键盘选中的格子上），会出现两个 Tooltip 重叠的情况。

## 2. 核心需求变更

根据最新讨论，重构方案不再采用“固定面板”，而是**统一的跟随式 Tooltip**，并引入**输入模式切换**机制。

### 2.1 核心目标
1.  **单一数据源与显示实例**：全局同一时间只能显示一个 Tooltip。
2.  **输入模式互斥**：
    *   **键盘/手柄模式 (Keyboard Mode)**: 触发导航时进入。**隐藏鼠标光标**。Tooltip 显示在焦点格子旁边。
    *   **鼠标模式 (Mouse Mode)**: 检测到鼠标移动时进入。**显示鼠标光标**，并**取消键盘焦点**。Tooltip 跟随鼠标悬浮显示。

## 3. 技术实现方案

### 3.1 统一 Tooltip 管理器 (`TooltipManager`)

不再依赖 VisUI 自带的 `Tooltip` 类（它是基于 Listener 自动管理的，难以控制互斥），而是手动管理一个全局的 `TooltipTable`。

*   **位置**: `GameHUD` 类中。
*   **成员**:
    *   `VisTable currentTooltip`: 当前显示的 Tooltip 实例。
    *   `InputMode currentInputMode`: 枚举 `MOUSE`, `KEYBOARD`。
*   **方法**:
    *   `showTooltip(Actor target, InventoryItem item)`: 根据当前模式计算位置并显示。
    *   `hideTooltip()`: 移除当前 Tooltip。

### 3.2 输入模式切换逻辑

我们需要在 `GameHUD` 或 `InputManager` 中监听输入事件来切换状态。

#### A. 进入键盘模式 (Keyboard Mode)
*   **触发条件**: 
    *   检测到 `InputAction.UI_UP/DOWN/LEFT/RIGHT` 或 `TAB` 等导航按键。
*   **执行动作**:
    1.  `currentInputMode = KEYBOARD`。
    2.  **隐藏鼠标**: `Gdx.input.setCursorCatched(true)` (或将光标移出屏幕/设置透明图标)。
    3.  **恢复焦点**: 如果之前有选中的格子，重新高亮它；或者默认选中第一个。
    4.  **显示 Tooltip**: 立即在当前焦点格子旁显示 Tooltip。

#### B. 进入鼠标模式 (Mouse Mode)
*   **触发条件**: 
    *   检测到 `mouseMoved` 或 `touchDown` 事件。
*   **执行动作**:
    1.  `currentInputMode = MOUSE`。
    2.  **显示鼠标**: `Gdx.input.setCursorCatched(false)`。
    3.  **取消焦点**: 调用 `inventoryDialog.clearFocus()`，移除所有格子的焦点高亮边框。
    4.  **Tooltip 更新**: 
        *   隐藏原本的焦点 Tooltip。
        *   如果鼠标此刻正悬停在某个格子上（通过 `hit` 检测或 `InputListener`），显示该格子的 Tooltip。

### 3.3 UI 组件改造

#### `InventorySlot` & `EquipmentSlot`
1.  **移除 VisUI Tooltip**: 删除构造函数中的 `new Tooltip.Builder(...)`。
2.  **添加鼠标监听**:
    ```java
    addListener(new InputListener() {
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            if (currentInputMode == MOUSE) {
                gameHUD.showTooltip(thisSlot, item);
            }
        }
        public void exit(...) {
            gameHUD.hideTooltip();
        }
    });
    ```
3.  **修改焦点逻辑**:
    *   `setFocused(boolean focused)`: 仅当 `focused == true` 且 `currentInputMode == KEYBOARD` 时，才显示焦点边框和 Tooltip。

#### `InventoryDialog`
1.  **增加 `clearFocus()`**: 遍历所有 Slot，调用 `setFocused(false)`。
2.  **输入监听**: 在 `act` 方法或全局 `InputProcessor` 中，加入对鼠标移动的检测，触发模式切换。

### 3.4 边界情况处理
*   **点击操作**: 无论是鼠标点击还是键盘确认，执行装备/使用逻辑后，应刷新 Tooltip 内容（因为物品可能变了或消失了）。
*   **窗口关闭**: 关闭背包时，Tooltip 必须强制隐藏。

## 4. 实施步骤 (Task List)

1.  **GameHUD 改造**:
    *   添加 `InputMode` 枚举与状态变量。
    *   实现 `updateInputMode(InputMode newMode)` 方法，处理光标显隐和焦点清除。
    *   重构 `showTooltip` 方法，支持两种定位模式（Target右侧 vs 鼠标跟随）。
2.  **输入监听接入**:
    *   在 `GameHUD.act()` 或 `InputManager` 中添加鼠标移动检测逻辑。
3.  **Slot 类清理**:
    *   移除旧的 `Tooltip` 代码。
    *   添加新的鼠标 `enter/exit` 监听，对接 `GameHUD` 的 Tooltip 方法。
4.  **InventoryDialog 适配**:
    *   实现 `clearFocus()`。
    *   在键盘导航逻辑中，强制切换回 `KEYBOARD` 模式。

---
*本文档由 AI 助手生成，用于指导 MagicDungeon2 项目背包系统的 UI 重构 (v2)。*
