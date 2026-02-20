# 系统物品模块: System_InventoryModule (Item & Equipment)

## 1. 模块职责
负责管理游戏内所有物品的定义、生成、掉落、背包存储及装备逻辑。

## 2. 核心实体定义

### 2.1 物品分类 (Item Types)

#### 消耗品 (Consumable)
*   **血瓶 (Potion)**: 恢复 HP。
    *   *堆叠*: 可堆叠 (Stackable)。
    *   *使用*: 快捷键直接使用，或背包内点击。
*   **魔瓶 (Mana Potion)**: 恢复 MP。
*   **钥匙 (Yellow/Blue/Red Key)**: 对应开启不同颜色的门/箱子。

#### 装备 (Equipment)
*   **部位**: 
    *   武器 (Weapon): 提供 ATK, ASP, Range。
    *   防具 (Armor): 提供 DEF, HP, MOV。
    *   饰品 (Accessory): 特殊属性 (如吸血、反伤、经验加成)。
*   **品质**: 
    *   普通 (White)
    *   稀有 (Blue)
    *   史诗 (Purple)
    *   传说 (Orange)
*   **词条 (Affix)**: 装备生成时随机附带的属性加成 (由 `Core_StatsModule` 定义属性)。

### 2.2 掉落机制 (Drop System)

#### 掉落表 (Loot Table)
*   **普通怪物**: 
    *   30% 掉落金币 (Gold)。
    *   10% 掉落消耗品。
    *   1% 掉落普通装备。
*   **精英怪物**:
    *   100% 掉落稀有+ 装备。
    *   20% 掉落特殊材料/钥匙。
*   **Boss**:
    *   100% 掉落传说装备。
    *   100% 掉落通关凭证。

#### 巢穴掉落 (Nest Drop)
*   巢穴被摧毁时，计算为一次 "相当于精英怪" 的掉落。

## 3. 背包管理 (Inventory Management)
*   **容量**: 固定格数 (如 20 格)。
*   **操作**: 
    *   拾取 (Pick up): 自动拾取金币/钥匙；点击拾取装备/药水。
    *   丢弃 (Drop): 从背包移除。
    *   装备/卸下 (Equip/Unequip): 改变角色属性。

## 4. 接口设计 (API Draft)

```java
interface IInventoryManager {
    // 物品生成
    Item createRandomItem(int level, Rarity rarity);
    
    // 背包操作
    boolean addItem(Item item);
    boolean removeItem(Item item);
    boolean useItem(int slotIndex, Entity target);
    
    // 装备操作
    void equipItem(int slotIndex);
    void unequipItem(EquipmentSlot slot);
    
    // 查询
    List<Item> getInventoryItems();
    Item getEquippedItem(EquipmentSlot slot);
}

class ItemData {
    String id;
    String name;
    String description;
    ItemType type;
    Map<StatType, Float> statModifiers; // 属性加成
}
```
