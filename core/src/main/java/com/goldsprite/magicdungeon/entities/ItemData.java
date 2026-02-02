package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum ItemData {
    // Weapons
    RUSTY_SWORD("Rusty Sword", ItemType.WEAPON, 5, 0, 0, Color.TAN),
    IRON_SWORD("Iron Sword", ItemType.WEAPON, 10, 0, 0, Color.LIGHT_GRAY),
    STEEL_AXE("Steel Axe", ItemType.WEAPON, 15, 0, 0, Color.GRAY),
    MAGIC_WAND("Magic Wand", ItemType.WEAPON, 20, 0, 0, Color.MAGENTA),
    LEGENDARY_BLADE("Legendary Blade", ItemType.WEAPON, 50, 0, 0, Color.GOLD),

    // Armor
    LEATHER_ARMOR("Leather Armor", ItemType.ARMOR, 0, 2, 0, Color.BROWN),
    IRON_MAIL("Iron Mail", ItemType.ARMOR, 0, 5, 0, Color.LIGHT_GRAY),
    PLATE_ARMOR("Plate Armor", ItemType.ARMOR, 0, 10, 0, Color.GRAY),

    // Potions
    HEALTH_POTION("Health Potion", ItemType.POTION, 0, 0, 20, Color.RED),
    MANA_POTION("Mana Potion", ItemType.POTION, 0, 0, 20, Color.BLUE);

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
