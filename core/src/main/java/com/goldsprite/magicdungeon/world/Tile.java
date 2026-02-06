package com.goldsprite.magicdungeon.world;

public class Tile {
    public TileType type;
    public boolean visible;
    public boolean discovered;
    public boolean walkable;

    public Tile(TileType type) {
        this.type = type;
        this.visible = false;
        this.discovered = false;
        this.walkable = (type != TileType.Wall);
    }
}
