package com.goldsprite.magicdungeon.world;

public enum TileType {
	Air, // 0
	Wall,
	Floor,
	Door,
	Stairs_Down,
	Stairs_Up,
	Tree,
	Grass,
	Sand,
	StonePath,
	Dungeon_Entrance,
	Pillar,
	Torch,
	Window,
	Dirt;

    public static TileType fromId(int id) {
        if (id < 0 || id >= values().length) return Air;
        return values()[id];
    }
    
    public int getId() {
        return this.ordinal();
    }
}
