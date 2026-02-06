# 背包装备显示异常及系统重构分析

## 1. 问题现象
在背包界面中，当存在两个相同的装备（例如两把 `RUSTY_SWORD`）时，装备其中一把，UI 会错误地显示两把都处于"已装备"状态。

## 2. 原因分析

### 2.1 物体重复与引用同一性
目前系统中的物品数据结构使用 `ItemData` 枚举（Enum）来表示。
- **Enum 的特性**：`ItemData.RUSTY_SWORD` 在整个 Java 虚拟机中只有一个单例对象。
- **库存存储**：`Player` 的背包 `List<ItemData> inventory` 存储的是对这个单例对象的引用。当背包里有两把剑时，实际上列表里存了两个指向同一个内存地址的引用。
- **装备逻辑**：`Player` 的装备槽 `ItemData weapon` 也指向这个单例。

### 2.2 UI 渲染逻辑缺陷
在 `GameHUD` 的渲染循环中，判断物品是否装备的逻辑如下：
```java
boolean isEquipped = (player.equipment.weapon == item) || (player.equipment.armor == item);
```
由于 `item`（来自背包循环）和 `player.equipment.weapon`（当前装备）指向的是同一个 Enum 对象，无论遍历到背包里的哪一把剑，这个等式恒成立。因此，所有同类物品都会被标记为"已装备"。

## 3. 属性变更正确性分析
虽然 UI 显示错误，但属性计算逻辑目前是**正确**的。
在 `Player.updateStats()` 中：
```java
if (equipment.weapon != null) {
    baseAtk += equipment.weapon.atk;
}
```
此逻辑只根据当前装备槽中的引用来计算属性，与背包中有多少个同类物品无关。因此，玩家的实际属性值不会叠加错误。

## 4. 解决方案

为了解决 UI 识别问题，必须引入**物品实例（Item Instance）**的概念，使每个获得的物品拥有独立的身份标识（Identity），而不仅仅是共享的数据定义。

### 4.1 引入 `InventoryItem` 类
创建一个新的包装类，包含静态数据引用和唯一标识符（UUID）。

```java
public class InventoryItem {
    public String id; // 唯一标识符
    public ItemData data; // 静态数据引用
    
    public InventoryItem(ItemData data) {
        this.id = UUID.randomUUID().toString();
        this.data = data;
    }
}
```

### 4.2 系统重构计划
1.  **数据层**：
    - 新增 `InventoryItem` 类。
    - 修改 `GameState`，将 `inventory` 类型从 `List<ItemData>` 改为 `List<InventoryItem>`。
    - 修改 `Player`，将 `inventory` 和 `equipment` 中的槽位改为 `InventoryItem` 类型。

2.  **逻辑层**：
    - 修改物品拾取逻辑：捡起 `Item` 时，实例化一个新的 `InventoryItem` 并分配 UUID。
    - 修改装备逻辑：操作对象变为 `InventoryItem` 实例。

3.  **表现层 (UI)**：
    - `GameHUD` 遍历 `InventoryItem` 列表。
    - 判断装备状态时，比较 `InventoryItem` 对象的引用或 ID，从而精确区分哪一个实例被装备。

### 4.3 存档兼容性注意
由于数据结构变更（从字符串列表变为对象列表），旧的存档文件（`game_save.json`）将无法读取，需要删除旧存档重新开始游戏。
