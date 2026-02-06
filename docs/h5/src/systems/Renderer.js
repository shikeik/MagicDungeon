import { TILE_SIZE } from '../utils/Constants.js';
import { ASSETS } from '../utils/SpriteGenerator.js';
import { TILE_TYPE } from '../world/Tile.js';

export class Renderer {
    constructor(canvasId) {
        this.canvas = document.getElementById(canvasId);
        this.ctx = this.canvas.getContext('2d');
        this.resize();
        window.addEventListener('resize', () => this.resize());

        // Camera position
        this.camera = { x: 0, y: 0 };
    }

    resize() {
        this.canvas.width = window.innerWidth;
        this.canvas.height = window.innerHeight;
        this.ctx.imageSmoothingEnabled = false; // Pixel art look
    }

    clear() {
        this.ctx.fillStyle = '#111';
        this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    }

    // Follow a target (e.g., player)
    updateCamera(target) {
        if (!target) return;

        let tx = target.x * TILE_SIZE;
        let ty = target.y * TILE_SIZE;

        if (target.visualX !== undefined) tx = target.visualX;
        if (target.visualY !== undefined) ty = target.visualY;

        // Center the camera on the target
        this.camera.x = tx - this.canvas.width / 2 + TILE_SIZE / 2;
        this.camera.y = ty - this.canvas.height / 2 + TILE_SIZE / 2;
    }

    drawMap(dungeon) {
        // Calculate visible range based on camera
        const startX = Math.floor(this.camera.x / TILE_SIZE);
        const startY = Math.floor(this.camera.y / TILE_SIZE);
        const endX = startX + Math.ceil(this.canvas.width / TILE_SIZE) + 1;
        const endY = startY + Math.ceil(this.canvas.height / TILE_SIZE) + 1;

        for (let y = startY; y < endY; y++) {
            for (let x = startX; x < endX; x++) {
                const tile = dungeon.getTile(x, y);
                if (tile) {
                    let sprite = null;
                    if (tile.type === TILE_TYPE.WALL) sprite = ASSETS.TILES.WALL;
                    else if (tile.type === TILE_TYPE.FLOOR) sprite = ASSETS.TILES.FLOOR;
                    else if (tile.type === TILE_TYPE.Stairs_Down) sprite = ASSETS.TILES.STAIRS;

                    if (sprite) {
                        this.drawSprite(sprite, x, y);
                    } else {
                        // Fallback
                        let color = '#333';
                        if (tile.type === TILE_TYPE.FLOOR) color = '#888';
                        this.drawSprite(color, x, y);
                    }
                }
            }
        }
    }

    drawSprite(sprite, x, y, size = TILE_SIZE, color = 'white') {
        // Apply camera offset
        const screenX = Math.floor(x * size - this.camera.x);
        const screenY = Math.floor(y * size - this.camera.y);

        // Cull if out of screen
        if (screenX < -size || screenX > this.canvas.width ||
            screenY < -size || screenY > this.canvas.height) return;

        if (typeof sprite === 'string') {
            // Draw colored rect for now if sprite is a color string
            this.ctx.fillStyle = sprite;
            this.ctx.fillRect(screenX, screenY, size, size);
        } else if (sprite instanceof Image || sprite instanceof HTMLCanvasElement) {
             this.ctx.drawImage(sprite, screenX, screenY, size, size);
        }
    }

    drawText(text, x, y, color = 'white', fontSize = 16) {
         this.ctx.fillStyle = color;
         this.ctx.font = `${fontSize}px 'Courier New'`;
         this.ctx.fillText(text, x, y);
    }
}
