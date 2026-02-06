package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum ItemData {
    // Weapons
    RUSTY_SWORD("生锈的剑", ItemType.WEAPON, 5, 0, 0, Color.TAN),
    IRON_SWORD("铁剑", ItemType.WEAPON, 10, 0, 0, Color.LIGHT_GRAY),
    STEEL_AXE("钢斧", ItemType.WEAPON, 15, 0, 0, Color.GRAY),
    MAGIC_WAND("魔法魔杖", ItemType.WEAPON, 20, 0, 0, Color.MAGENTA),
    LEGENDARY_BLADE("传奇之刃", ItemType.WEAPON, 50, 0, 0, Color.GOLD),

    // Armor
    LEATHER_ARMOR("皮甲", ItemType.ARMOR, 0, 2, 0, Color.BROWN),
    IRON_MAIL("铁甲", ItemType.ARMOR, 0, 5, 0, Color.LIGHT_GRAY),
    PLATE_ARMOR("板甲", ItemType.ARMOR, 0, 10, 0, Color.GRAY),

    // Potions
    HEALTH_POTION("生命药水", ItemType.POTION, 0, 0, 20, Color.RED),
    MANA_POTION("魔法药水", ItemType.POTION, 0, 0, 20, Color.BLUE),
    ELIXIR("万能药", ItemType.POTION, 0, 0, 50, Color.PURPLE),
    
    // Rings
    RING_OF_POWER("力量戒指", ItemType.WEAPON, 2, 0, 0, Color.ORANGE),
    RING_OF_DEFENSE("防御戒指", ItemType.ARMOR, 0, 2, 0, Color.CYAN),
    
    // Shields
    WOODEN_SHIELD("木盾", ItemType.ARMOR, 0, 3, 0, Color.BROWN),
    IRON_SHIELD("铁盾", ItemType.ARMOR, 0, 8, 0, Color.LIGHT_GRAY),
    
    // Misc
    GOLD_COIN("金币", ItemType.POTION, 0, 0, 0, Color.YELLOW),
    MAGIC_SCROLL("魔法卷轴", ItemType.WEAPON, 5, 0, 0, Color.WHITE);

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
