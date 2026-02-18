package com.goldsprite.magicdungeon.screens.tests.dualgrid;

public enum TerrainType {
    AIR(null, 0),
    SAND("sprites/tilesets/sand_tiles.png", 16),
    DIRT("sprites/tilesets/dirt_tiles.png", 16),
    GRASS("sprites/tilesets/grass_tiles.png", 16),
    DUNGEON_BRICK("sprites/tilesets/dungeon_brick_tiles_32x.png", 32),
    ;

    public final int id;
    public final String texPath;
    public final int sourceSize;

    TerrainType(String texPath, int sourceSize) {
        this.id = ordinal()-1;
        this.texPath = texPath;
        this.sourceSize = sourceSize;
    }
}
