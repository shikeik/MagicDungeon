package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum ItemData {
    // Weapons
    Rusty_Sword("生锈的剑", ItemType.WEAPON, 5, 0, 0, Color.TAN),
    Iron_Sword("铁剑", ItemType.WEAPON, 10, 0, 0, Color.LIGHT_GRAY),
    Steel_Axe("钢斧", ItemType.WEAPON, 15, 0, 0, Color.GRAY),
    Magic_Wand("魔法魔杖", ItemType.WEAPON, 20, 0, 0, Color.MAGENTA),
    Legendary_Blade("传奇之刃", ItemType.WEAPON, 50, 0, 0, Color.GOLD),

    // Armor
    Leather_Armor("皮甲", ItemType.ARMOR, 0, 2, 0, Color.BROWN),
    Iron_Mail("铁甲", ItemType.ARMOR, 0, 5, 0, Color.LIGHT_GRAY),
    Plate_Armor("板甲", ItemType.ARMOR, 0, 10, 0, Color.GRAY),

    // Potions
    Health_Potion("生命药水", ItemType.POTION, 0, 0, 20, Color.RED),
    Mana_Potion("魔法药水", ItemType.POTION, 0, 0, 20, Color.BLUE),
    Elixir("万能药", ItemType.POTION, 0, 0, 50, Color.PURPLE),
    
    // Rings
    Ring_Of_Power("力量戒指", ItemType.WEAPON, 2, 0, 0, Color.ORANGE),
    Ring_Of_Defense("防御戒指", ItemType.ARMOR, 0, 2, 0, Color.CYAN),
    
    // Shields
    Wooden_Shield("木盾", ItemType.ARMOR, 0, 3, 0, Color.BROWN),
    Iron_Shield("铁盾", ItemType.ARMOR, 0, 8, 0, Color.LIGHT_GRAY),
    
    // Misc
    Gold_Coin("金币", ItemType.POTION, 0, 0, 0, Color.YELLOW),
    Magic_Scroll("魔法卷轴", ItemType.WEAPON, 5, 0, 0, Color.WHITE);

    public final String name;
    public final ItemType type;
    public final int atk;
    public final int def;
    public final int heal;
    public final Color color;

    ItemData(String name, ItemType type, int atk, int def, int heal, Color color) {
        this.name = name;
        this.type = type;
        this.atk = atk;
        this.def = def;
        this.heal = heal;
        this.color = color;
    }
}
