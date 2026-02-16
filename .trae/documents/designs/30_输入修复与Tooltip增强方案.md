# 30_输入修复与Tooltip增强方案

## 1. 问题现状与分析

### 1.1 输入模式冲突 (Bug)
*   **现象**: 游戏启动时（进入 `GdxLauncher`），如果连接了手柄，可能导致输入逻辑卡死。表现为鼠标被锁定（进入键盘/手柄模式），但手柄操作无法控制 UI，且鼠标无法解锁。
*   **原因分析**:
    *   `InputManager` 可能因手柄摇杆的微小漂移（即使在死区内也可能触发某些逻辑，或者死区设置过小）在启动瞬间切换到了 `KEYBOARD` 模式。
    *   切换到 `KEYBOARD` 模式时，代码执行了 `Gdx.input.setCursorCatched(true)` 锁定鼠标。
    *   然而，此时 UI 焦点（Focus）可能尚未初始化（默认没有选中任何按钮或格子），导致手柄按键（依赖焦点导航）无效。
    *   玩家试图移动鼠标解锁，但可能因为 `InputManager` 的更新逻辑优先响应手柄信号，导致无法切回 `MOUSE` 模式，或者切回后立即又被手柄切走。
*   **解决思路**:
    *   **优化初始状态**: 启动时默认保持 `MOUSE` 模式，且不锁定鼠标，直到检测到明确的、大幅度的手柄/键盘操作。
    *   **焦点保护**: 当切换到 `KEYBOARD` 模式时，如果当前没有焦点元素，强制选中默认元素（如第一个按钮或背包第一格），防止“失控”。
    *   **死区调整**: 确保手柄摇杆死区足够大，防止漂移误触。

### 1.2 Tooltip 位置不跟手
*   **现象**: 鼠标唤出 Tooltip 时，位置偏移，且不跟随鼠标移动。
*   **解决思路**:
    *   **修正初始位置**: 将 Tooltip 的左上角对齐鼠标光标的右下侧（或严格对齐），符合用户习惯。
    *   **实时跟随**: 在 `GameHUD` 的渲染循环（`render` 或 `act`）中，如果当前处于 `MOUSE` 模式且有 Tooltip 显示，每帧更新 Tooltip 的坐标为当前鼠标坐标。

### 1.3 装备对比功能缺失
*   **需求**: 查看装备时，并列显示已穿戴的同部位装备。
*   **难点**: 饰品槽有 3 个，如何判定对比哪一个？
*   **解决思路**:
    *   **判定逻辑**:
        *   **普通装备** (主手/副手/头/甲/鞋): 直接获取对应槽位的物品。
        *   **饰品**:
            *   如果玩家背包里查看饰品：对比**第一个已装备的饰品**（Slot 0）。如果 Slot 0 空但 Slot 1 有，则对比 Slot 1。如果都空，不显示对比。
            *   （进阶）或者显示所有已装备饰品？（占用屏幕过大，暂不推荐）。
            *   （折中）对比逻辑优先取 `accessories[0]`，因为它是主饰品位。
    *   **UI 实现**:
        *   改造 `showTooltip` 方法。
        *   创建一个容器 Table (`HBox`)。
        *   先添加 [新物品 Tooltip]。
        *   检查是否需要对比：
            *   如果是装备类型 && 玩家已装备同类型物品 && 该已装备物品 != 当前查看物品。
            *   添加 [已装备物品 Tooltip] 到容器右侧（或左侧，视习惯而定，通常“已装备”在左，“新物品”在右，或者反之。参考暗黑类游戏，通常是对齐显示）。
            *   这里建议：[已装备] [新物品] 并排。

## 2. 详细实施方案

### 2.1 输入管理器 (`InputManager.java`)
1.  **增加焦点检查回调**: 提供一个接口或回调，当检测到手柄输入且当前无焦点时，通知 UI 系统重置焦点。
2.  **调整死区**: 检查 `axisMoved` 的阈值，确保 `0.3f` 足够（现有代码似乎已设为 0.3，需确认是否生效）。
3.  **防止启动锁死**: 在 `InputManager` 初始化后的前几帧忽略手柄输入，或者要求必须有由于按键（Button）按下才切换模式，单纯摇杆移动需要更大的阈值。

### 2.2 UI 系统 (`GameHUD.java`)

#### A. 修复输入与焦点
*   在 `act()` 或 `render()` 中，如果模式转为 `KEYBOARD` 且 `currentFocus == null`，自动触发 `resetFocus()`。
*   `resetFocus()` 逻辑：如果是背包打开，选中背包第一格；如果是主界面，选中工具栏第一个。

#### B. Tooltip 系统重构
*   **数据结构**:
    *   `showTooltip(Actor target, InventoryItem item)` 方法升级。
    *   不再直接添加 `createItemTooltipTable` 返回的 Table，而是创建一个 `containerTable`。
*   **位置更新**:
    *   在 `GameHUD.draw()` 或 `act()` 中：
        ```java
        if (currentTooltip != null && inputMode == MOUSE) {
            updateTooltipPosition();
        }
        ```
    *   `updateTooltipPosition()`: 设置 (MouseX + offset, StageHeight - MouseY - offset)。

#### C. 装备对比逻辑
*   **方法**: `private VisTable createComparisonTooltip(InventoryItem item)`
*   **逻辑**:
    ```java
    InventoryItem equipped = null;
    switch (item.data.type) {
        case MAIN_HAND: equipped = player.equipment.mainHand; break;
        // ... 其他部位
        case ACCESSORY:
            // 简单策略：对比第一个非空的饰品槽
            for (InventoryItem acc : player.equipment.accessories) {
                if (acc != null) { equipped = acc; break; }
            }
            break;
    }
    
    if (equipped != null && equipped != item) {
        // 创建并列视图
        VisTable container = new VisTable();
        VisTable equippedTip = createItemTooltipTable(equipped, true);
        VisTable newTip = createItemTooltipTable(item, false);
        
        // 标记已装备
        equippedTip.add(new VisLabel("(已装备)").color(Color.YELLOW)).row();
        
        container.add(equippedTip).top().padRight(10);
        container.add(newTip).top();
        return container;
    }
    return createItemTooltipTable(item, isEquipped);
    ```

### 3. 任务列表

1.  **输入修复**: 
    *   修改 `InputManager`，避免启动误触。
    *   修改 `GameHUD`，确保 Keyboard 模式下必有焦点。
2.  **Tooltip 增强**:
    *   实现 `updateTooltipPosition()` 跟随鼠标。
    *   实现 `createComparisonTooltip()` 支持装备对比。
    *   处理饰品对比逻辑（优先 Slot 0）。

