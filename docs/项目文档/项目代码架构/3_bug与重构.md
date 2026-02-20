# MagicDungeon 代码缺陷分析 (Urgent)

本文档聚焦于当前代码库中**已经存在且极有可能导致严重后果**的缺陷，不谈未来架构，只谈眼下的生存问题。

## 1. 致命崩溃风险 (High Risk)

这些问题会导致游戏直接崩溃 (Crash)，必须立即修复。

### 1.1. 并发修改异常 (ConcurrentModificationException)
-   **位置**: `GameScreen.java` : 1033 行 (updateLogic)
    ```java
    for (Monster m : monsters) {
        int damage = m.update(...);
        if (player.stats.hp <= 0) triggerGameOver();
    }
    ```
-   **现状**: 你正在遍历 `monsters` 列表。
-   **风险**: 虽然 `m.update` 本身没有移除怪物，但如果玩家的反击逻辑（在 `m.update` 内部触发）或者其他系统（如 `vfxManager` 回调）修改了 `monsters` 列表，游戏将立即崩溃。更危险的是，如果在 `triggerGameOver` -> `enterCamp` 流程中清空了 `monsters` 列表（`monsters.clear()`），而此时循环还在进行中，**必崩无疑**。
-   **证据**: `triggerGameOver` 调用 `enterCamp`，而 `enterCamp` 会调用 `loadCurrentLevel`，后者会执行 `monsters.clear()`。虽然 `triggerGameOver` 中使用了 `Runnable` 延迟执行，但如果逻辑稍有变动改为立即执行，这就是一个地雷。

### 1.2. 存档损坏导致的启动崩溃
-   **位置**: `SaveManager.java` : 47 行, 195 行
    ```java
    return json.fromJson(type, file);
    ```
-   **现状**: 仅仅是简单包装了 `json.fromJson`。
-   **风险**: 如果玩家的 `player.json` 或 `meta.json` 因为断电、强杀进程而只写入了一半（JSON 格式错误），`json.fromJson` 会抛出 `SerializationException`。由于 `GameScreen` 在构造函数中就调用 `loadGame`，这会导致游戏**启动即崩溃**，且无法进入主菜单，玩家甚至没有机会删除损坏的存档（除非手动删文件）。这是最恶劣的用户体验。

### 1.3. 数组越界风险
-   **位置**: `GameScreen.java` : 609 行
    ```java
    MonsterType type = allowed[monsterRng.nextInt(allowed.length)];
    ```
-   **风险**: 如果 `dungeon.theme.allowedMonsters` 为空数组（虽然有判空，但如果配置错误导致长度为 0），`nextInt(0)` 会抛出异常。这是一个典型的配置依赖型 Bug。

## 2. 严重逻辑漏洞 (Logic Bugs)

这些问题不会导致崩溃，但会严重破坏游戏体验。

### 2.1. 怪物无视碰撞重叠
-   **位置**: `Monster.java` : 112 行
    ```java
    for (Monster m : otherMonsters) { ... }
    ```
-   **现状**: 怪物移动时会检查与其他怪物的位置是否重叠。
-   **风险**: 当怪物数量较多时，这种 O(N^2) 的检查方式（每个怪物每帧都遍历所有其他怪物）会导致性能急剧下降。更严重的是，如果两个怪物同时移动到同一个格子（在同一帧内），由于它们读取的是旧状态，它们会重叠在一起，变成一个“双头怪”，攻击力翻倍但看起来像一个怪。

### 2.2. 死亡惩罚逻辑漏洞
-   **位置**: `Player.java` : 315 行
    ```java
    inventory.remove(idx);
    ```
-   **现状**: 死亡时随机移除物品。
-   **风险**: 这里直接移除了 `InventoryItem` 对象。如果这个物品当前被装备着（引用在 `equipment` 中），它虽然从背包列表中消失了，但仍然显示在装备栏上，并且属性加成依然生效！玩家得到了一个“幽灵装备”，卸下后就会永久消失。
-   **修复**: 在移除前必须检查并卸下装备。

### 2.3. 存档数据不一致
-   **位置**: `GameScreen.java` : 308 行 `enterCamp`
    ```java
    boolean wasInDungeon = dungeon.level > 0;
    if (wasInDungeon) { saveCurrentLevelState(); }
    ```
-   **风险**: 如果玩家在地牢中直接强退游戏（非正常保存退出），`saveCurrentLevelState` 不会被调用。下次加载时，玩家会恢复到上一次手动保存的状态（可能在几层楼之前），但全局元数据（如 `meta.json` 中的 `currentFloor`）可能已经在进入楼层时被更新了。这会导致元数据说你在 5 层，但读取的 5 层数据却是空的或者旧的。

## 3. 资源泄露 (Resource Leaks)

### 3.1. 纹理未释放
-   **位置**: `GameScreen.java`
-   **风险**: `wolfSkeletonData` 及其关联的 `TextureAtlas` 加载后从未被显式释放。虽然它是单例管理，但在长时间运行或频繁切换存档后，可能会导致内存占用持续增加，特别是在 Android 这种内存受限的设备上。

## 4. 立即行动清单

别管什么解耦了，先修这些：

1.  **[紧急] 修复怪物更新循环**: 将 `GameScreen` 中的增强 `for` 循环改为安全的迭代方式，或者复制一份列表进行遍历。
2.  **[紧急] 装备移除检查**: 在 `Player.applyDeathPenalty` 中添加 `unequip` 逻辑。
3.  **[紧急] 存档防御**: 在 `SaveManager` 的所有 `load` 方法中加上 `try-catch`，如果失败返回 `null`，并在 `GameScreen` 中处理 `null` 的情况（例如弹窗提示并返回主菜单，而不是崩溃）。
4.  **[高] 修复怪物重叠**: 在怪物移动逻辑中，不仅要检查当前位置，还要检查“即将移动到该位置”的其他怪物（虽然这很难，但至少要防止静态重叠）。

这份文档指出了如果不修，明天就会有人骂你的 Bug。开始行动吧。
