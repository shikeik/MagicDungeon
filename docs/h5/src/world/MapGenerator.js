import { Tile, TILE_TYPE } from './Tile.js';

export class MapGenerator {
    constructor(width, height) {
        this.width = width;
        this.height = height;
    }

    generate() {
        const map = [];
        const rooms = [];

        // Initialize with walls
        for (let y = 0; y < this.height; y++) {
            const row = [];
            for (let x = 0; x < this.width; x++) {
                row.push(new Tile(TILE_TYPE.WALL));
            }
            map.push(row);
        }

        // Generate Rooms
        const maxRooms = 15;
        const minSize = 6;
        const maxSize = 12;

        for (let i = 0; i < maxRooms; i++) {
            const w = Math.floor(Math.random() * (maxSize - minSize + 1)) + minSize;
            const h = Math.floor(Math.random() * (maxSize - minSize + 1)) + minSize;
            const x = Math.floor(Math.random() * (this.width - w - 2)) + 1;
            const y = Math.floor(Math.random() * (this.height - h - 2)) + 1;

            const newRoom = { x, y, w, h };

            // Check collision with other rooms
            let failed = false;
            for (const other of rooms) {
                if (newRoom.x <= other.x + other.w && newRoom.x + newRoom.w >= other.x &&
                    newRoom.y <= other.y + other.h && newRoom.y + newRoom.h >= other.y) {
                    failed = true;
                    break;
                }
            }

            if (!failed) {
                this.createRoom(map, newRoom);
                
                if (rooms.length > 0) {
                    const prevRoom = rooms[rooms.length - 1];
                    const prevCenter = {
                        x: Math.floor(prevRoom.x + prevRoom.w / 2),
                        y: Math.floor(prevRoom.y + prevRoom.h / 2)
                    };
                    const newCenter = {
                        x: Math.floor(newRoom.x + newRoom.w / 2),
                        y: Math.floor(newRoom.y + newRoom.h / 2)
                    };

                    // Coin flip for corridor direction
                    if (Math.random() > 0.5) {
                        this.createHCorridor(map, prevCenter.x, newCenter.x, prevCenter.y);
                        this.createVCorridor(map, prevCenter.y, newCenter.y, newCenter.x);
                    } else {
                        this.createVCorridor(map, prevCenter.y, newCenter.y, prevCenter.x);
                        this.createHCorridor(map, prevCenter.x, newCenter.x, newCenter.y);
                    }
                }

                rooms.push(newRoom);
            }
        }

        // Place Stairs in the last room
        const lastRoom = rooms[rooms.length - 1];
        const stairsX = Math.floor(lastRoom.x + lastRoom.w / 2);
        const stairsY = Math.floor(lastRoom.y + lastRoom.h / 2);
        map[stairsY][stairsX].type = TILE_TYPE.STAIRS_DOWN;

        // Start position
        const startRoom = rooms[0];
        const startX = Math.floor(startRoom.x + startRoom.w / 2);
        const startY = Math.floor(startRoom.y + startRoom.h / 2);

        return { grid: map, start: { x: startX, y: startY } };
    }

    createRoom(map, room) {
        for (let y = room.y; y < room.y + room.h; y++) {
            for (let x = room.x; x < room.x + room.w; x++) {
                map[y][x].type = TILE_TYPE.FLOOR;
                map[y][x].walkable = true;
            }
        }
    }

    createHCorridor(map, x1, x2, y) {
        for (let x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            map[y][x].type = TILE_TYPE.FLOOR;
            map[y][x].walkable = true;
        }
    }

    createVCorridor(map, y1, y2, x) {
        for (let y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
            map[y][x].type = TILE_TYPE.FLOOR;
            map[y][x].walkable = true;
        }
    }
}
