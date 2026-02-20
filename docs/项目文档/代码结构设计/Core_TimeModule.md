# 核心时间模块: Core_TimeModule (Action & Cooldown)

## 1. 模块职责
负责管理游戏中的时间流逝、动作冷却 (Global Cooldown) 和移动冷却。这是实现半回合制/即时制混合玩法的核心。

## 2. 核心机制

### 2.1 时间切片 (Time Slicing)
*   **单位**: 逻辑帧 (Logic Frame) 或 毫秒 (ms)。
*   **最小间隔**: 0.05s ~ 0.1s (根据攻速/移速上限 300% 推算，极限约为 0.1s 一次动作)。
*   **积累制**: 并非简单的倒计时，而是 `进度条积累 (Accumulation)` 模式。
    *   $Progress += Speed \times \Delta T$
    *   当 $Progress \ge Threshold$ (如 100) 时，允许动作。

### 2.2 冷却类型

#### 移动冷却 (Movement Cooldown)
*   **基准**: 初始移动间隔 (Base Move Interval) 例如 0.5s。
*   **公式**: $ActualInterval = BaseInterval \div (1 + MOV\%)$。
*   **上限**: MOV 300% -> 间隔缩短至 $1/4$，即 0.125s。

#### 攻击冷却 (Attack Cooldown)
*   **基准**: 武器基础攻速 (Weapon Base Interval)。
*   **公式**: $ActualInterval = BaseInterval \div (1 + ASP\%)$。
*   **上限**: ASP 300% -> 间隔缩短至 $1/4$。
*   **武器差异**:
    *   匕首 (Dagger): 基础 0.4s -> 极限 0.1s。
    *   重锤 (Hammer): 基础 1.2s -> 极限 0.3s。

## 3. 动作队列 (Action Queue)
*   **缓冲**: 允许玩家在冷却未好时预输入指令 (Pre-input)。
*   **优先级**: 
    1.  移动指令 (若移动冷却就绪)。
    2.  攻击指令 (若攻击范围内有敌人且冷却就绪)。
    3.  待机 (Idle)。

## 4. 接口设计 (API Draft)

```java
interface ITimeManager {
    // 全局时间更新
    void update(float deltaTime);
    
    // 实体时间状态
    boolean isReadyToMove(Entity entity);
    boolean isReadyToAttack(Entity entity);
    
    // 动作执行后重置
    void consumeMove_Charge(Entity entity);
    void consumeAttack_Charge(Entity entity);
    
    // 获取冷却进度 (用于UI显示)
    float getMoveProgress(Entity entity); // 0.0 ~ 1.0
    float getAttackProgress(Entity entity); // 0.0 ~ 1.0
}
```
