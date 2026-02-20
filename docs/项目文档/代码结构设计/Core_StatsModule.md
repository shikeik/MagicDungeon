# 核心逻辑模块: StatsModule (属性系统)

## 1. 模块职责
负责管理游戏中所有实体（玩家、怪物）的数值属性、成长计算及状态变化。此模块不依赖任何图形库，仅处理纯数据逻辑。

## 2. 核心数据结构

### 2.1 基础属性定义 (Attributes)
采用六大核心属性构建数值体系。

| 属性名称 | 缩写代码 | 说明 | 初始值 (基准) | 成长规则 |
| :--- | :--- | :--- | :--- | :--- |
| **生命值** | `HP` | 生存容错上限 | 20 | 1点属性点 = 20 HP |
| **魔法值** | `MP` | 技能/破防资源 | 10 | 1点属性点 = 10 MP |
| **攻击力** | `ATK` | 物理伤害基础 | 1 | 1点属性点 = 1 ATK |
| **防御力** | `DEF` | 物理/魔法防御基础 | 1 | 1点属性点 = 1 DEF |
| **攻击速度** | `ASP` | 动作冷却缩减比例 | 100% | 不可加点，仅装备/道具提升 (上限300%) |
| **移动速度** | `MOV` | 移动冷却缩减比例 | 100% | 不可加点，仅装备/道具提升 (上限300%) |

*   **隐性属性**:
    *   `MDEF` (魔法防御): 固定为 `DEF / 2` (向下取整)。

### 2.2 属性计算流水线 (Stat Pipeline)
最终属性值的计算通过即时流水线完成，确保增益叠加逻辑统一。
Formula: `Final = (Base + LevelFixed + Allocated + EquipFixed) * (1 + BuffPercent)`

1.  **Base (基础值)**: 实体初始值 (如玩家 Lv1 为 20/10/1/1)。
2.  **LevelFixed (等级固定成长)**: 等级带来的自动提升 (每级全属性+1对应值)。
3.  **Allocated (自由加点)**: 玩家分配的自由点数带来的提升。
4.  **EquipFixed (装备固定值)**: 装备直接提供的数值 (如 `铁剑: ATK +5`)。
5.  **BuffPercent (百分比增益)**: 药水/被动技能的百分比加成 (如 `狂暴: ATK +10%`)。
6.  **Limit (上限裁剪)**: 针对 ASP/MOV 进行 300% 上限截断。

## 3. 成长系统 (Growth & Experience)

### 3.1 经验值机制 (XP Accumulation)
*   **累积制**: 记录玩家从 0 级至今获得的**总经验值 (TotalXP)**，不设"当前等级经验槽"。
*   **等级推导**: `Level = f(TotalXP)`。等级是总经验值的函数，而非独立存储的变量。
*   **升级公式**:
    *   $ \text{RequiredXP}(L) = 100 \times 1.2^{(L-1)} $ (升到下一级所需增量)。
    *   推导总经验阈值表以确定当前等级。

### 3.2 升级奖励
每升 1 级，玩家获得：
1.  **固定成长**: HP/MP/ATK/DEF 各自动增加 **1点** 对应数值。
2.  **自由点数**: 获得 **4点** 自由属性点 (Potential Points)，存储在 `freePoints` 池中，可随时分配。

### 3.3 死亡惩罚 (Death Penalty)
*   **经验掉落**: 死亡时扣除 **当前总经验值的 20%**。
*   **等级滑落 (De-leveling)**: 若扣除后的总经验值低于当前等级阈值，**等级自动降低**，相关的固定成长和自由点数（若已分配则需处理回退逻辑，通常优先扣除自由点或设为负债）将根据新等级重新计算。
*   **怪物掉落**: 怪物死亡掉落自身携带经验的 **10%**。

## 4. 接口设计 (API Draft)

```java
interface IStatSystem {
    // 获取最终属性值
    float getFinalStat(StatType type);
    
    // 经验与等级
    void addExperience(long amount);
    void applyDeathPenalty(); // 扣除20%总经验
    int getCurrentLevel();
    long getTotalExperience();
    
    // 加点
    int getFreePoints();
    boolean allocatePoint(StatType type);
    
    // 状态检查
    boolean isDead();
    void reduceHp(float amount);
    void reduceMp(float amount);
}
```
