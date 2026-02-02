import { TILE_SIZE } from '../utils/Constants.js';

export class Entity {
    constructor(x, y, color = 'white') {
        this.x = x;
        this.y = y;
        this.color = color;
        
        // Visual Position (Pixels)
        this.visualX = x * TILE_SIZE;
        this.visualY = y * TILE_SIZE;
        
        // Animation Offsets
        this.bumpX = 0;
        this.bumpY = 0;
    }

    updateVisuals(dt) {
        // Smooth movement interpolation (Linear)
        const targetX = this.x * TILE_SIZE;
        const targetY = this.y * TILE_SIZE;
        // Use custom visual speed or default fast speed
        const moveSpeed = this.visualSpeed || (TILE_SIZE * 10); 

        // X Axis
        if (this.visualX !== targetX) {
            const dirX = Math.sign(targetX - this.visualX);
            const dist = Math.abs(targetX - this.visualX);
            const move = moveSpeed * dt;
            
            if (move >= dist) {
                this.visualX = targetX;
            } else {
                this.visualX += dirX * move;
            }
        }

        // Y Axis
        if (this.visualY !== targetY) {
            const dirY = Math.sign(targetY - this.visualY);
            const dist = Math.abs(targetY - this.visualY);
            const move = moveSpeed * dt;
            
            if (move >= dist) {
                this.visualY = targetY;
            } else {
                this.visualY += dirY * move;
            }
        }

        // Bump animation decay
        const bumpDecay = 10;
        this.bumpX += (0 - this.bumpX) * bumpDecay * dt;
        this.bumpY += (0 - this.bumpY) * bumpDecay * dt;
        
        if (Math.abs(this.bumpX) < 0.1) this.bumpX = 0;
        if (Math.abs(this.bumpY) < 0.1) this.bumpY = 0;
    }

    triggerBump(dirX, dirY) {
        const force = TILE_SIZE * 0.5;
        this.bumpX = dirX * force;
        this.bumpY = dirY * force;
    }
}
