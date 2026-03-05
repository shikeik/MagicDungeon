# 持久化系统升级方案

## 1. 现状与问题

### 1.1 装备数据丢失
当前 `GameState` 仅保存了 `playerStats` (基础属性) 和 `inventory` (背包列表)。
**问题**：`Player.Equipment` (已装备的武器/防具) **未被保存**。
**后果**：读档后，玩家已装备的强力武器会消失（甚至没回到背包），属性也会重置回基础值，战斗力大幅下降。

### 1.2 层级数据重置
当前存档是一个单层快照，每次 `saveGame` 都会覆盖整个文件。
**问题**：
*   当玩家从第 1 层下到第 2 层，第 1 层的状态（怪物尸体、未捡物品、地图探索）就被丢弃了。
*   如果玩家之后能返回第 1 层，或者只是重新加载第 2 层，游戏都是“重新生成”的状态（虽然有种子保证地形，但状态是新的）。
*   **需求**：“进入已访问过的层不应生成层而是加载层数据”。这意味着我们需要持久化每一层的状态。

## 2. 解决方案

### 2.1 装备数据持久化
1.  **扩展 `GameState`**：
    *   新增 `equippedWeaponId` (String) 和 `equippedArmorId` (String) 字段。
    *   或者直接保存 `InventoryItem` 对象，但推荐保存引用 ID 以便与背包逻辑统一（如果装备仍在背包列表里）。
    *   *注：当前代码逻辑似乎装备后物品仍在 inventory 中？需确认逻辑。如果 `equip` 只是引用，则保存 ID 即可。如果 `equip` 是从 inventory 移出，则需独立保存 Item 数据。*
    *   *检查代码 `Player.java`*：`equip` 方法似乎只是赋值给 `equipment` 字段，并未从 `inventory` 移除（待确认 UI 逻辑）。假设装备仍在背包中，或者装备栏是独立的。为了安全起见，我们将装备视为独立槽位保存。

    **方案**：在 `GameState` 中增加 `equipment` 字段。
    ```java
    public static class EquipmentState {
        public InventoryItem weapon;
        public InventoryItem armor;
    }
    public EquipmentState equipment;
    ```

### 2.2 多层级数据管理
为了支持“已访问层”的持久化，我们需要将存档结构从“单层快照”升级为“世界存档”。

#### 数据结构变更
引入 `LevelState` 概念，用于存储单层的状态（地图种子、怪物、物品、迷雾等）。`GameState` 将管理一个 `Map<Integer, LevelState>`。

**新版 `GameState` 结构**：
```java
public class GameState {
    // === 全局玩家数据 ===
    public long globalSeed;
    public PlayerStats playerStats;
    public List<InventoryItem> inventory;
    public EquipmentState equipment; // 新增：装备
    
    // === 当前状态 ===
    public int currentLevelIndex; // 当前所在层数
    public int playerX, playerY;  // 当前层坐标
    
    // === 关卡历史数据 ===
    // Key: 层数 (1, 2, 3...), Value: 该层的状态快照
    public Map<Integer, LevelState> visitedLevels; 
}

public class LevelState {
    // 该层特有的数据
    public List<MonsterState> monsters;
    public List<ItemState> items;
    // 未来可扩展：boolean[][] exploredTiles;
}
```

#### 逻辑流程变更

**1. 进入下一层/上一层 (Level Transition)**
*   **离开当前层时**：
    *   将当前层的 `monsters` 和 `items` 状态打包成 `LevelState`。
    *   保存到 `GameState.visitedLevels.put(currentLevel, levelState)`。
*   **进入目标层时**：
    *   检查 `visitedLevels` 是否包含目标层。
    *   **有记录 (已访问)**：直接从 `LevelState` 恢复怪物和物品，**跳过** `generate()` 中的实体生成步骤（地形仍需通过种子重新生成）。
    *   **无记录 (新层)**：调用标准的 `generate()` 和 `spawnEntities()`，然后初始化新的 `LevelState`（可选，或者等离开时再存）。

**2. 存档 (Save)**
*   保存 `GameState` 时，会自动序列化 `visitedLevels` Map，从而保存所有已探索过的层级状态。

**3. 读档 (Load)**
*   恢复全局玩家数据（含装备）。
*   根据 `currentLevelIndex` 获取当前层的 `LevelState` 并恢复场景。

## 3. 实施步骤

1.  **定义 `LevelState` 类**：包含怪物和物品列表。
2.  **更新 `GameState`**：
    *   添加 `equipment` 字段。
    *   添加 `visitedLevels` 字段。
    *   移除顶层的 `monsters` 和 `items`（移入 `LevelState`，或者保留作为当前层的缓存，但在保存时归档）。建议保留顶层字段作为“当前活跃数据”，但在切换楼层时归档。
3.  **更新 `Player`**：
    *   确保装备逻辑正确（加载时能恢复装备并重新计算属性）。
4.  **更新 `GameScreen`**：
    *   修改 `nextLevel()` / `prevLevel()` 逻辑：切换前保存当前状态，切换后尝试加载历史状态。
    *   修改 `loadGame()`：支持恢复装备。
5.  **更新 `SaveManager`**：适配新的 `GameState` 结构。

---
*请确认是否按照此方案进行代码更新。*
