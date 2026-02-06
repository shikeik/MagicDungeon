package com.goldsprite.magicdungeon.entities;

import com.badlogic.gdx.graphics.Color;

public class Item {
	public int x;
	public int y;
	public InventoryItem item; // Replaced ItemData with InventoryItem

	// Visual Position (Pixels)
	public float visualX;
	public float visualY;

	public Item(int x, int y, InventoryItem item) {
		this.x = x;
		this.y = y;
		this.item = item;

		this.visualX = x * 32; 
		this.visualY = y * 32;
	}
}
