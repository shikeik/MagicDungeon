export const TILE_TYPE = {
    WALL: 0,
    FLOOR: 1,
    DOOR: 2,
    STAIRS_DOWN: 3
};

export class Tile {
    constructor(type = TILE_TYPE.WALL) {
        this.type = type;
        this.visible = false; // Fog of War
        this.discovered = false;
        this.walkable = type !== TILE_TYPE.WALL;
    }
}
