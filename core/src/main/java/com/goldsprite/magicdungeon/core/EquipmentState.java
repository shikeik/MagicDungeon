package com.goldsprite.magicdungeon.core;

import com.goldsprite.magicdungeon.entities.InventoryItem;

public class EquipmentState {
	public InventoryItem mainHand;
	public InventoryItem offHand;
	public InventoryItem helmet;
	public InventoryItem armor;
	public InventoryItem boots;
	public InventoryItem[] accessories;
	
	public EquipmentState() {}
	
	public EquipmentState(InventoryItem mainHand, InventoryItem offHand, InventoryItem helmet, InventoryItem armor, InventoryItem boots, InventoryItem[] accessories) {
		this.mainHand = mainHand;
		this.offHand = offHand;
		this.helmet = helmet;
		this.armor = armor;
		this.boots = boots;
		this.accessories = accessories;
	}
}
