# 视觉与输入模块: View_InputModule (Controller & Event)

## 1. 模块职责
负责监听玩家的物理输入 (键盘、鼠标)，将其映射为 **游戏逻辑指令 (Commands)**，并分发给对应的系统执行。

## 2. 输入映射 (Input Mapping)

### 2.1 键盘控制 (Keyboard)
*   **移动 (WASD / Arrows)**:
    *   `Press`: 单次移动一格。
    *   `Hold`: 连续移动 (设置重复延迟)。
*   **交互 (Space / Enter)**:
    *   攻击面前的怪物。
    *   开启面前的箱子/门。
    *   拾取脚下的物品。
*   **功能键**:
    *   `I / B`: 打开背包。
    *   `C`: 打开角色面板。
    *   `Esc`: 暂停/设置。
    *   `1-9`: 使用快捷栏物品/技能。

### 2.2 鼠标控制 (Mouse - Optional)
*   **左键点击 (Left Click)**:
    *   **地面**: 移动到该位置 (自动寻路)。
    *   **怪物**: 移动并攻击。
    *   **UI**: 触发按钮/拖拽物品。
*   **右键点击 (Right Click)**:
    *   **物品**: 快速使用/装备。
    *   **怪物**: 查看详细信息 (Inspect)。
*   **悬停 (Hover)**:
    *   显示 Tooltip。

## 3. 指令模式 (Command Pattern)

为了解耦输入与逻辑，使用指令模式：
1.  **InputHandler**: 监听 `Gdx.input`。
2.  **CommandFactory**: 根据输入生成 `Command` 对象。
3.  **CommandQueue**: 将指令排队 (处理动作缓冲)。
4.  **Executor**: `World` 或 `Entity` 执行指令。

### 核心指令类型
*   `MoveCommand(Direction dir)`
*   `AttackCommand(Target target)`
*   `InteractCommand(Target object)`
*   `UseItemCommand(int slotIndex)`

## 4. 接口设计 (API Draft)

```java
interface IInputSystem {
    // 输入处理
    void processInput(float deltaTime);
    
    // 模式切换
    void setInputMode(InputMode mode); // Gameplay, UI, Cutscene
    
    // 键位绑定
    void bindKey(int keyParams, GameAction action);
    
    // 鼠标射线检测 (用于点击地图实体)
    Entity getEntityUnderMouse(int screenX, int screenY);
}

enum InputMode {
    Gameplay, // 控制角色
    Menu,     // 控制UI
    Dialog    // 控制对话推进
}
```
