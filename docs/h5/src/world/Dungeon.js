import { MapGenerator } from './MapGenerator.js';
import { TILE_TYPE } from './Tile.js';

export class Dungeon {
    constructor(width = 50, height = 50) {
        this.width = width;
        this.height = height;
        this.level = 1;
        this.map = [];
        this.startPos = { x: 0, y: 0 };
        this.generate();
    }

    generate() {
        const generator = new MapGenerator(this.width, this.height);
        const result = generator.generate();
        this.map = result.grid;
        this.startPos = result.start;
    }

    getTile(x, y) {
        if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
            return null;
        }
        return this.map[y][x];
    }

    isWalkable(x, y) {
        const tile = this.getTile(x, y);
        return tile && tile.walkable;
    }

    getRandomWalkableTile() {
        let attempts = 0;
        while (attempts < 1000) {
            const x = Math.floor(Math.random() * this.width);
            const y = Math.floor(Math.random() * this.height);
            if (this.isWalkable(x, y)) {
                return { x, y };
            }
            attempts++;
        }
        return null;
    }
}
