package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.InventoryItem;

public class EquipmentState {
	public InventoryItem weapon;
	public InventoryItem armor;
	
	public EquipmentState() {}
	
	public EquipmentState(InventoryItem weapon, InventoryItem armor) {
		this.weapon = weapon;
		this.armor = armor;
	}
}
