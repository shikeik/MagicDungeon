# 存档系统重构方案

## 1. 现状问题分析
当前存档系统 (`SaveManager` & `GameState`) 仅保存了以下信息：
*   **全局数据**：`dungeonLevel` (当前层数), `seed` (全局种子)
*   **玩家数据**：`playerStats` (属性), `inventory` (背包)

**严重缺失的数据**：
1.  **玩家位置**：读档后玩家被重置到初始位置 (`dungeon.startPos`)，而不是存档时的位置。
2.  **当前层世界状态**：
    *   **怪物状态**：当前层剩余的怪物、怪物的位置、剩余血量均未保存。读档后会重新生成满血怪物。
    *   **物品状态**：地面上未拾取的物品未保存。读档后会重新生成随机物品。
    *   **地图状态**：虽然有种子保证地形一致，但如果未来加入可破坏墙体或已探索迷雾，当前系统无法支持。

**后果**：
玩家可以利用“保存/加载”来重置当前层的怪物状态（刷怪）、满血复活（如果重置位置安全），或者丢失已经清理的关卡进度。这不符合 Roguelike 或 RPG 的基本体验。

## 2. 重构目标
实现**完全状态保存**，读档后恢复到存档时的精确瞬间，包括：
*   玩家确切坐标。
*   当前层所有存活怪物的状态（位置、HP、类型）。
*   当前层地面所有未拾取物品的状态（位置、类型）。

## 3. 技术方案

### 3.1. 数据结构变更 (`GameState`)
扩展 `GameState` 类，包含当前关卡的完整快照。

```java
public class GameState {
    // 全局信息
    public int dungeonLevel;
    public long seed;
    
    // 玩家信息
    public PlayerStats playerStats;
    public List<InventoryItem> inventory;
    public int playerX, playerY; // 新增：玩家坐标
    
    // 世界状态快照
    public List<MonsterState> monsters; // 新增：怪物列表
    public List<ItemState> items;       // 新增：地面物品列表
    // public boolean[][] fogMap;       // (可选) 未来支持迷雾状态
}
```

### 3.2. 辅助数据类 (DTO)
为了方便 JSON 序列化，创建专门的数据传输对象（DTO）。

#### `MonsterState`
```java
public class MonsterState {
    public int x, y;
    public String typeName; // 对应 MonsterType.name()
    public int hp;
    public int maxHp;
    // 如果有 buff 或其他状态也需在此保存
}
```

#### `ItemState`
```java
public class ItemState {
    public int x, y;
    public String itemName; // 对应 ItemData.name()
}
```

### 3.3. 逻辑变更

#### `SaveManager.saveGame(...)`
*   不再只保存玩家 stats，而是从 `GameScreen` 或 `Dungeon` 获取当前的 `monsters` 和 `items` 列表。
*   将实体转换为对应的 State 对象存入 `GameState`。
*   记录 `player.x` and `player.y`。

#### `GameScreen.loadGame()`
*   **第一步**：恢复基础数据（层数、种子、玩家属性、背包）。
*   **第二步**：**仅生成地形** (`dungeon.generate()`)，**不要调用** `spawnEntities()` (因为该方法会随机生成新实体)。
*   **第三步**：**恢复实体**。
    *   设置玩家坐标 `player.x/y`。
    *   清空 `monsters` 和 `items` 列表。
    *   遍历 `GameState.monsters`，重建 `Monster` 对象并加入列表。
    *   遍历 `GameState.items`，重建 `Item` 对象并加入列表。

### 3.4. 兼容性处理
*   如果读取旧版本存档（缺少 monsters 字段），可以回退到默认的 `spawnEntities()` 逻辑，避免崩溃。

## 4. 实施步骤

1.  **定义 DTO 类**：在 `com.goldsprite.magicdungeon.core` 包下创建 `MonsterState` 和 `ItemState` (或者作为 `GameState` 的静态内部类)。
2.  **更新 `GameState`**：添加上述新字段。
3.  **更新 `SaveManager`**：修改 `saveGame` 方法，接收 `List<Monster>` 和 `List<Item>` 参数，并进行序列化转换。
4.  **更新 `GameScreen`**：
    *   修改调用 `SaveManager.saveGame` 的地方，传入实体列表。
    *   重写 `loadGame` 逻辑，实现从 `GameState` 重建实体的逻辑。
5.  **验证**：进入游戏 -> 移动并杀怪 -> 存档 -> 读档 -> 确认位置和怪物状态一致。

---
*请确认是否按照此方案进行重构。*
