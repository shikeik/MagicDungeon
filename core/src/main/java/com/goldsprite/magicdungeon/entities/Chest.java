package com.goldsprite.magicdungeon.entities;

import java.util.ArrayList;
import java.util.List;
import com.goldsprite.magicdungeon.utils.Constants;

public class Chest {
	public int x;
	public int y;
	public List<InventoryItem> items;
	public boolean isOpen;

	// Visual Position (Pixels)
	public float visualX;
	public float visualY;

	public Chest(int x, int y) {
		this.x = x;
		this.y = y;
		this.items = new ArrayList<>();
		this.isOpen = false;

		this.visualX = x * Constants.TILE_SIZE; 
		this.visualY = y * Constants.TILE_SIZE;
	}
	
	public void addItem(InventoryItem item) {
		this.items.add(item);
	}
}
