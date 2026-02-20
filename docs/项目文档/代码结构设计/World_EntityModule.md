# 世界与实体模块: World_EntityModule (World System)

## 1. 模块职责
负责管理游戏世界中的所有动态实体 (玩家、怪物、巢穴)、地形生成逻辑及持久化数据结构。

## 2. 核心实体定义

### 2.1 玩家 (Player)
*   **属性**: 拥有完整的六大属性。
*   **状态**: 移动/攻击冷却独立。
*   **存档**: 等级、经验、背包一键保存。

### 2.2 怪物 (Monster)
*   **属性**: 简化版的属性 (HP/ATK/DEF)。
*   **行为**: 简单的 AI 追逐逻辑 (A*)。
*   **种类**:
    *   **普通 (Common)**: 会掉落消耗品。有概率生成巢穴。
    *   **精英 (Elite)**: 固定掉落装备。有概率生成巢穴。
    *   **唯一/Boss (Unique)**: 一次性击杀，不复活。不可生成巢穴。

### 2.3 巢穴 (Nest)
*   **生成规则**: **普通/精英怪物**有概率 (Probability) 自动生成其对应的巢穴。
    *   *解释*: 并非死亡掉落，而是怪物作为"宿主"，可能会演变为巢穴，或在地图生成时作为"怪物出生点"自然存在。
    *   *功能*: 缓慢产出 (Slowly Produce) 同类怪物。
    *   *限制*: 每个巢穴有 **最大存活数量上限** (Max Limit)，防止无限堆积。
    *   *战术*: 玩家可摧毁巢穴以消除刷怪源，或保留作为经验提款机。

## 3. 地图结构 (World Map)

### 3.1 网格系统 (Grid System)
*   **坐标**: 整数 `(x, y)`。
*   **庞大地图**: 直接在内存中加载整张地图 (`200x200` 或更大)，不分片加载。
*   **迷雾**: 仅渲染玩家视野内的区域，其余遮罩。

### 3.2 持久化状态 (Persistence)
*   **怪物**: 记录 `IsDead` 状态。非巢穴产出的怪物死亡不复活。
*   **宝箱**: 记录 `IsOpened` 状态。开启后永久开启。
*   **最高层级**: 记录 `RecordHighFloor`，用于已通关判断。
*   **巢穴**: 记录当前存活的子体数量，以及巢穴自身的生命值。

## 4. 接口设计 (API Draft)

```java
interface IWorldManager {
    // 实体管理
    void spawnEntity(EntityData data, Position pos);
    void removeEntity(Entity entity);
    
    // 巢穴逻辑
    void updateNests(float deltaTime); // 这里的 update 用于刷怪计时
    boolean canNestSpawn(NestData nest);
    
    // 地图持久化
    void saveWorldState();
    void loadWorldState();
    
    // 网格查询
    Entity getEntityAt(int gridX, int gridY);
    boolean isWalkable(int gridX, int gridY);
}

class NestData {
    MonsterType type;
    int maxSpawnLimit;
    int currentSpawnCount;
    float spawnCooldown;
}
```
