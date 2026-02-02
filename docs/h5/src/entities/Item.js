export const ITEM_TYPE = {
    WEAPON: 0,
    ARMOR: 1,
    POTION: 2,
    KEY: 3
};

export const ITEMS = {
    // Weapons
    RUSTY_SWORD: { name: 'Rusty Sword', type: ITEM_TYPE.WEAPON, atk: 5, color: '#cd7f32' },
    IRON_SWORD: { name: 'Iron Sword', type: ITEM_TYPE.WEAPON, atk: 10, color: '#aaa' },
    STEEL_AXE: { name: 'Steel Axe', type: ITEM_TYPE.WEAPON, atk: 15, color: '#888' },
    MAGIC_WAND: { name: 'Magic Wand', type: ITEM_TYPE.WEAPON, atk: 20, color: '#f0f' },
    LEGENDARY_BLADE: { name: 'Legendary Blade', type: ITEM_TYPE.WEAPON, atk: 50, color: '#ffd700' },
    
    // Armor
    LEATHER_ARMOR: { name: 'Leather Armor', type: ITEM_TYPE.ARMOR, def: 2, color: '#8b4513' },
    IRON_MAIL: { name: 'Iron Mail', type: ITEM_TYPE.ARMOR, def: 5, color: '#aaa' },
    PLATE_ARMOR: { name: 'Plate Armor', type: ITEM_TYPE.ARMOR, def: 10, color: '#888' },
    
    // Potions
    HEALTH_POTION: { name: 'Health Potion', type: ITEM_TYPE.POTION, heal: 20, color: '#f00' },
    MANA_POTION: { name: 'Mana Potion', type: ITEM_TYPE.POTION, heal: 20, color: '#00f' }
};

export class Item {
    constructor(x, y, type) {
        this.x = x;
        this.y = y;
        this.data = type;
        this.color = type.color;
    }
}
