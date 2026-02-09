package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public enum ItemData {
	// Weapons (Main Hand)
	Rusty_Sword("生锈的剑", ItemType.MAIN_HAND, 5, 0, 0, Color.TAN),
	Iron_Sword("铁剑", ItemType.MAIN_HAND, 10, 0, 0, Color.LIGHT_GRAY),
	Steel_Axe("钢斧", ItemType.MAIN_HAND, 15, 0, 0, Color.GRAY),
	Magic_Wand("魔法魔杖", ItemType.MAIN_HAND, 20, 0, 0, Color.MAGENTA),
	Legendary_Blade("传奇之刃", ItemType.MAIN_HAND, 50, 0, 0, Color.GOLD),

	// Armor (Chest)
	Leather_Armor("皮甲", ItemType.ARMOR, 0, 2, 0, Color.BROWN),
	Iron_Mail("铁甲", ItemType.ARMOR, 0, 5, 0, Color.LIGHT_GRAY),
	Plate_Armor("板甲", ItemType.ARMOR, 0, 10, 0, Color.GRAY),

	// Helmets
	Leather_Helmet("皮帽", ItemType.HELMET, 0, 1, 0, Color.BROWN),
	Iron_Helmet("铁盔", ItemType.HELMET, 0, 3, 0, Color.LIGHT_GRAY),
	
	// Boots
	Leather_Boots("皮靴", ItemType.BOOTS, 0, 1, 0, Color.BROWN),
	Iron_Boots("铁靴", ItemType.BOOTS, 0, 2, 0, Color.GRAY),

	// Off-Hand (Shields, Books)
	Wooden_Shield("木盾", ItemType.OFF_HAND, 0, 3, 0, Color.BROWN),
	Iron_Shield("铁盾", ItemType.OFF_HAND, 0, 8, 0, Color.LIGHT_GRAY),
	Magic_Scroll("魔法卷轴", ItemType.OFF_HAND, 5, 0, 0, Color.WHITE),

	// Accessories (Necklace, Ring, Bracelet)
	Ring_Of_Power("力量戒指", ItemType.ACCESSORY, 2, 0, 0, 0, Color.ORANGE),
	Ring_Of_Defense("防御戒指", ItemType.ACCESSORY, 0, 2, 0, 0, Color.CYAN),
	Ruby_Necklace("红宝石项链", ItemType.ACCESSORY, 3, 0, 5, 0, Color.RED),
	Sapphire_Necklace("蓝宝石项链", ItemType.ACCESSORY, 0, 3, 5, 0, Color.BLUE),
	Gold_Bracelet("金手环", ItemType.ACCESSORY, 1, 1, 0, 0, Color.GOLD),
	Crystal_Amulet("水晶护身符", ItemType.ACCESSORY, 0, 0, 0, 2, Color.CYAN), // New Mana Regen Item

	// Potions
	Health_Potion("生命药水", ItemType.POTION, 0, 0, 20, 0, Color.RED),
	Mana_Potion("魔法药水", ItemType.POTION, 0, 0, 0, 20, Color.BLUE), // Mana potion gives instant mana, treated differently usually, but let's keep consistent signature
	Elixir("万能药", ItemType.POTION, 0, 0, 50, 50, Color.PURPLE),
	
	// Misc
	Gold_Coin("金币", ItemType.ETC, 0, 0, 0, 0, Color.YELLOW);

	public final String name;
	public final ItemType type;
	public final int atk;
	public final int def;
	public final int heal; // HP Regen or Instant Heal
	public final int manaRegen; // Mana Regen or Instant Mana
	public final Color color;

	ItemData(String name, ItemType type, int atk, int def, int heal, int manaRegen, Color color) {
		this.name = name;
		this.type = type;
		this.atk = atk;
		this.def = def;
		this.heal = heal;
		this.manaRegen = manaRegen;
		this.color = color;
	}

	// Legacy constructor for compatibility if needed, or just update all enums
	ItemData(String name, ItemType type, int atk, int def, int heal, Color color) {
		this(name, type, atk, def, heal, 0, color);
	}
}
