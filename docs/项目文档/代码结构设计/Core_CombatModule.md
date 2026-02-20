# 核心战斗模块: CombatModule (Combat Mechanic)

## 1. 模块职责
负责管理游戏中所有实体间的伤害判定、物理/魔法攻击转换、范围计算及穿透衰减逻辑。此模块不处理任何UI或渲染。

## 2. 核心伤害公式 (Combat Cycle)

### 2.1 物理伤害 (Physical Damage)
*   **攻防判定**: `ATK > DEF` 则破防。
*   **伤害值**: `Max(0, Attacker.ATK - Target.DEF)`
*   **无保底机制**: 无 1点保底，高防御可实现物理无伤。

### 2.2 魔法伤害 (Magic Damage)
*   **计算公式**: `MDEF = DEF / 2` (向下取整)。
*   **伤害值**: `Max(0, Attacker.ATK - Target.MDEF)`
*   **触发方式**:
    1.  **主动技能**: 消耗 `MP` 可释放，无视普攻冷却。
    2.  **法杖武器**: 普通攻击直接转为魔法伤害，但动作后摇(ASP)极大。

### 2.3 穿透与衰减 (Penetration Decay)
*   **伤害列表 (Damage Chain)**: 攻击可以穿透多个目标。
*   **衰减系数**: 每穿透一个目标，伤害衰减 **30%**。
*   **公式**: $ \text{Damage}(n) = \text{BaseDamage} \times 0.7^{(n-1)} $
    *   n=1: 100% 伤害
    *   n=2: 70% 伤害
    *   n=3: 49% 伤害

## 3. 攻击范围类型 (Range & AOE)

### 3.1 近战 (Melee)
*   **范围**: 邻接的 4 个格子。
*   **特性**: 不可穿透。仅击中一个目标即停止。

### 3.2 长柄 (Spear)
*   **范围**: 正前方直线 2 个格子。
*   **特性**: **全穿透**。对该直线上的所有实体依次造成伤害 (每多一个单位衰减 30%)。

### 3.3 远程 - 能量弹 (Magic Bolt)
*   **范围**: 正前方直线 5 个格子。
*   **特性**: **不穿透**。击中第一个障碍物或实体即消失。

### 3.4 远程 - 箭矢 (Arrow)
*   **范围**: 正前方直线 5 个格子。
*   **特性**: **全穿透**。可贯穿该直线上所有实体，每多一个单位衰减 30%。

## 4. 接口设计 (API Draft)

```java
interface ICombatEngine {
    // 发起一次攻击判定
    void performAttack(Entity attacker, Grid targetGrid, WeaponType weapon);
    
    // 计算单次伤害（不执行，仅预测）
    float calculateDamage(Entity attacker, Entity defender, int pierceIndex, DamageType type);
    
    // 范围判定
    List<Grid> getAttackRange(Entity attacker, WeaponType weapon);
}

enum DamageType {
    PHYSICAL, 
    MAGIC
}

enum WeaponType {
    MELEE(1, false),      // 1格不穿透
    SPEAR(2, true),       // 2格全穿透
    MAGIC_BOLT(5, false), // 5格不穿透
    ARROW(5, true);       // 5格全穿透
}
```
