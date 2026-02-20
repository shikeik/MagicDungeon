# 系统引导模块: System_GuidanceModule (Fairy & Tutorial)

## 1. 模块职责
负责游戏内的引导逻辑，主要是 "小精灵" (Fairy) 的智能提示系统，以及新手教程的流程控制。

## 2. 小精灵系统 (Fairy System)

### 2.1 核心逻辑 - 智能判断 (Smart Hinting)
小精灵不是简单的脚本对话，而是根据玩家当前状态 (State) 动态生成建议。

*   **状态监测**:
    *   `Map Knowledge`: 玩家是否探索了通往下一层的楼梯？
    *   `Stat Check`: 玩家当前的攻防能否无伤击杀当前区域的怪物？
    *   `Resource Check`: 玩家是否有足够的钥匙开启面前的门？

### 2.2 提示场景 (Scenarios)

1.  **打不过提示 (Too Weak)**:
    *   *触发*: 玩家尝试攻击一个计算结果为 "必死" 或 "高战损" 的怪物。
    *   *Fairy*: "小心！你的攻击力不足以穿透它的护甲，建议先去寻找 [力量宝石] 或提升等级。"

2.  **迷路提示 (Lost)**:
    *   *触发*: 玩家在同一层停留时间过长且未找到出口。
    *   *Fairy*: "我感觉到楼梯在 [东北方向]，也许那是通往下一层的路。"

3.  **资源警告 (Low Resource)**:
    *   *触发*: 玩家生命值低于 20% 且背包有药水未使用。
    *   *Fairy*: "你看起来很虚弱，快喝下 [红药水/血瓶]！"

4.  **巢穴预警 (Nest Warning)**:
    *   *触发*: 玩家靠近一个正在快速刷怪的巢穴。
    *   *Fairy*: "前方有强烈的魔力波动！那个 [巢穴] 正在源源不断地召唤怪物，如果不摧毁它，怪物会无穷无尽！"

## 3. 教程流程 (Tutorial Pipeline)

*   **阶段 1: 移动与战斗**: 限制玩家只能移动和攻击木桩。
*   **阶段 2: 属性成长**: 强制玩家拾取宝石，展示属性变化。
*   **阶段 3: 物品使用**: 给予扣血，强制喝药。
*   **阶段 4: 巢穴机制**: 生成一个不攻击的巢穴，让玩家体验摧毁巢穴停止刷怪的过程。

## 4. 接口设计 (API Draft)

```java
interface IGuidanceSystem {
    // 状态更新
    void onPlayerMove(Position pos);
    void onCombatStart(Entity target);
    void onInventoryChange();
    
    // 获取建议
    String getCurrentHint();
    
    // 显示控制
    boolean shouldShowPopupA();
    void dismissPopup();
}

enum HintType {
    CombatRisky,
    Pathfinding,
    ResourceUsage,
    NestMechanic
}
```
