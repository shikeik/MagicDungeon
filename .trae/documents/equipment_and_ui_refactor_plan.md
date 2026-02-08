# 装备系统扩展与UI重构开发案

## 1. 核心目标
*   **统一角色渲染**：实现“所见即所得”，UI中的纸娃娃（Avatar）与场景中的玩家形象共用同一套渲染逻辑。
*   **扩展装备槽位**：建立包含主手、副手、头盔、铠甲、鞋子及3个通用饰品位的完整装备体系。
*   **重构背包界面**：采用标准RPG布局，以纸娃娃为中心，装备槽位环绕，属性面板置于下方。

## 2. 详细技术方案

### 2.1 数据层变更 (Data Layer)

#### 2.1.1 物品类型枚举 (`ItemType`)
重构枚举以支持新的装备分类：
```java
public enum ItemType {
    MAIN_HAND,  // 主手 (武器：剑、斧、魔杖)
    OFF_HAND,   // 副手 (盾牌、法球、魔法书)
    HELMET,     // 头盔
    ARMOR,      // 铠甲 (胸甲)
    BOOTS,      // 鞋子
    ACCESSORY,  // 饰品 (项链、戒指、手环通用)
    POTION,     // 药水
    ETC         // 其他材料
}
```

#### 2.1.2 玩家装备数据 (`Player.Equipment`)
扩展装备槽位定义：
```java
public class Equipment {
    public InventoryItem mainHand;      // 主手
    public InventoryItem offHand;       // 副手
    public InventoryItem helmet;        // 头盔
    public InventoryItem armor;         // 铠甲
    public InventoryItem boots;         // 鞋子
    
    // 饰品槽位 (数组或独立字段，建议使用数组方便遍历)
    public InventoryItem[] accessories = new InventoryItem[3]; 
}
```

#### 2.1.3 装备逻辑 (`Player.equip`)
*   **自动装填逻辑**：
    *   **饰品**：优先填入空槽位；若全满，则替换第一个槽位（或弹出选择，第一阶段先实现替换第一个）。
    *   **双手武器处理**（可选）：如果装备双手武器，自动卸下副手（当前暂按单手+副手设计）。
*   **属性计算**：遍历所有非空槽位累加属性。

### 2.2 视觉与资源层 (Visual & Assets)

#### 2.2.1 统一渲染管线 (`CharacterRenderer`)
为了实现“玩家场景渲染形象与纸娃娃形象统一”，我们将创建一个核心渲染器，用于动态生成角色纹理。

*   **重构 `SpriteGenerator`**：
    *   废弃静态的 `createPlayer()`。
    *   新增 `generateCharacterTexture(Equipment equipment)` 方法。
    *   **绘制图层顺序**：
        1.  **Body**: 素体（皮肤、内衣）。
        2.  **Boots**: 鞋子（若有）。
        3.  **Pants**: 裤子（根据铠甲类型可能变化）。
        4.  **Armor**: 铠甲/胸甲（若有）。
        5.  **Head/Face**: 头部、五官。
        6.  **Helmet**: 头盔（若有，覆盖头部）。
        7.  **MainHand**: 武器（绘制在**左侧**，需镜像翻转，模拟右手持握）。
        8.  **OffHand**: 副手（绘制在**右侧**，模拟左手持握）。

*   **应用场景**：
    *   **场景渲染**：玩家实体在地图上移动时，使用该动态生成的 Texture（缩放到32x32或保持原比例）。
    *   **UI渲染**：背包界面的 AvatarWidget 使用同一张 Texture（放大显示，例如 128x128）。

#### 2.2.2 缺失素材程序化生成
在 `SpriteGenerator.createItem()` 中补充以下类型的图标生成逻辑：
*   **Helmet (头盔)**: 全盔、半盔、兜帽。
*   **Boots (鞋子)**: 皮靴、铁靴。
*   **Accessory (饰品)**:
    *   **Necklace**: 链条+吊坠。
    *   **Bracelet**: 环形+宝石镶嵌。
    *   **Ring**: 现有的戒指逻辑。

### 2.3 UI 布局重构 (InventoryDialog)

#### 2.3.1 布局蓝图
采用 **左右分栏** 结构：

**左侧：角色概览 (Character Panel)**
*   **布局风格**：标准RPG“装备环绕”式布局。
*   **核心区域**：
    *   **Center**: 动态 Avatar 形象（大尺寸）。
    *   **Top**: [头盔]
    *   **Left**: [主手]
    *   **Right**: [副手]
    *   **Body**: [铠甲] (Avatar下方或重叠位置，通常UI上会有独立槽位) -> 修正为环绕：
        *   顶部居中: [头盔]
        *   中间左侧: [主手]
        *   中间居中: **Avatar**
        *   中间右侧: [副手]
        *   Avatar下方: [铠甲]
        *   更下方: [鞋子]
        *   底部行: [饰品1] [饰品2] [饰品3]
*   **底部区域**: 详细属性文本 (攻击、防御、生命、魔法、暴击等)。

**右侧：物品背包 (Inventory Panel)**
*   保持现有的网格滚动视图 (`VisScrollPane`)。
*   显示未装备的物品。

### 2.4 开发步骤

1.  **基础类库修改**: 更新 `ItemType`, `ItemData`, `Player.Equipment`。
2.  **素材生成器升级**:
    *   实现 `Necklace`, `Bracelet`, `Helmet`, `Boots` 的图标生成。
    *   实现分层角色渲染逻辑 `generateCharacterTexture`。
3.  **玩家逻辑更新**: 修改 `equip` 方法适配新槽位。
4.  **UI 重写**:
    *   创建新的 `InventoryDialog` 类。
    *   实现装备槽位 (`EquipmentSlot`) 与 背包槽位 (`InventorySlot`) 的交互（点击装备/卸下）。
    *   集成 `AvatarWidget`。
5.  **联调与验证**: 确保穿戴装备后，场景中的小人和UI中的小人同步变化。

## 3. 待确认事项
*   是否确认立即执行此方案？
