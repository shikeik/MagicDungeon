# 视觉与界面模块: View_UIModule (HUD & Menus)

## 1. 模块职责
负责管理游戏的所有用户界面元素，包括 战斗HUD、菜单系统、悬浮信息 (Floating Text) 以及对话框。此模块仅负责 **显示** 和 **用户输入响应 (UI层)**，不包含游戏核心逻辑。

## 2. 核心界面组件

### 2.1 战斗 HUD (Heads-Up Display)
*   **状态栏**:
    *   HP 条 (红): 显示当前/最大生命值。
    *   MP 条 (蓝): 显示当前/最大魔法值。
    *   XP 条 (黄/紫): 显示当前经验进度。
*   **快捷栏 (Hotbar)**:
    *   显示当前绑定的 技能/物品 (如 1-5 键位)。
    *   显示冷却遮罩 (Cooldown Overlay)。
*   **小地图 (Minimap)**:
    *   显示已探索区域、玩家位置、重要标记 (楼梯、Boss)。
*   **战斗日志 (Message Log)**:
    *   滚动显示战斗信息 ("你对史莱姆造成了 5 点伤害")。

### 2.2 菜单系统 (Menus)
*   **角色面板 (Character Sheet)**:
    *   显示详细属性数值 (STR, DEX, INT...)。
    *   显示装备槽位 (拖拽更换装备)。
*   **背包界面 (Inventory Grid)**:
    *   网格显示物品图标。
    *   提供 丢弃/使用 按钮。
    *   **物品详情**: 悬浮显示 (Tooltip) 物品属性对比。
*   **设置 (Settings)**:
    *   音量调节、全屏切换、按键绑定。

### 2.3 动态飘字 (Floating Text)
*   **伤害数字**:
    *   玩家受击 (红色)。
    *   怪物受击 (白色)。
    *   暴击 (黄色/放大)。
    *   闪避/格挡 (灰色 "Miss"/"Block")。
*   **状态提示**:
    *   升级 ("Level Up!")。
    *   获得物品 ("+ 金币")。

### 2.4 对话框/引导 (Dialogue)
*   **小精灵头像**: 伴随提示文本出现。
*   **打字机效果**: 文本逐字显示。
*   **选项**: 简单的 Yes/No 选择 (如 "是否进入下一层？")。

## 3. 渲染架构 (Architecture)

采用 **MVC (Model-View-Controller)** 变体或者 **观察者模式 (Observer Pattern)**：
*   **Model**: `Core/World/System` 中的数据。
*   **View**: `LibGDX Scene2D` 或自定义 UI 渲染器。
*   **Controller**: `View_InputModule` 转发事件。
*   **Observer**: UI 组件监听数据变化事件 (如 HP 变化 -> 更新血条)。

## 4. 接口设计 (API Draft)

```java
interface IUIManager {
    // 界面开关
    void openWindow(WindowType type);
    void closeWindow(WindowType type);
    boolean isWindowOpen(WindowType type);
    
    // 动态显示
    void showFloatingText(String text, Position pos, Color color);
    void showDialogue(String speaker, String content);
    
    // 更新通知 (通常由事件系统触发)
    void onPlayerStatChange(Stats stats);
    void onInventoryUpdate(List<Item> items);
}

enum WindowType {
    Inventory,
    Character,
    Settings,
    Shop,
    GameOver
}
```
