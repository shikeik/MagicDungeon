import { Entity } from './Entity.js';

export const MONSTER_TYPES = {
    SLIME: { name: 'Slime', hp: 20, atk: 5, color: '#00f', speed: 1.0 },
    SKELETON: { name: 'Skeleton', hp: 40, atk: 10, color: '#eee', speed: 0.8 },
    ORC: { name: 'Orc', hp: 60, atk: 15, color: '#0f0', speed: 0.6 },
    BAT: { name: 'Bat', hp: 10, atk: 3, color: '#555', speed: 0.4 },
    BOSS: { name: 'Dragon', hp: 200, atk: 30, color: '#f00', speed: 0.5 }
};

export class Monster extends Entity {
    constructor(x, y, type = MONSTER_TYPES.SLIME) {
        super(x, y, type.color);
        this.name = type.name;
        this.maxHp = type.hp;
        this.hp = type.hp;
        this.atk = type.atk;
        this.moveDelay = type.speed;
        this.moveTimer = 0;
        
        // Monsters move slower visually to match their logic speed, but not too slow
        // Cap visual speed to avoid looking like floating
        this.visualSpeed = Math.max(32 / this.moveDelay, 64); 
    }

    update(dt, player, map, otherMonsters) {
        if (this.hp <= 0) return;

        this.moveTimer -= dt;
        if (this.moveTimer > 0) return;

        // Simple chase logic
        const dx = player.x - this.x;
        const dy = player.y - this.y;
        const dist = Math.sqrt(dx*dx + dy*dy);

        if (dist < 10) { // Aggro range
            let nextX = this.x;
            let nextY = this.y;

            if (Math.abs(dx) > Math.abs(dy)) {
                nextX += dx > 0 ? 1 : -1;
            } else {
                nextY += dy > 0 ? 1 : -1;
            }

            // Check collision with map
            if (map.isWalkable(nextX, nextY)) {
                // Check collision with player
                if (nextX === player.x && nextY === player.y) {
                    // Attack Player!
                    // Return attack info or handle it
                    this.moveTimer = this.moveDelay;
                    return { type: 'attack', damage: this.atk };
                }

                // Check collision with other monsters
                let blocked = false;
                for (const m of otherMonsters) {
                    if (m !== this && m.hp > 0 && m.x === nextX && m.y === nextY) {
                        blocked = true;
                        break;
                    }
                }

                if (!blocked) {
                    this.x = nextX;
                    this.y = nextY;
                }
            }
            this.moveTimer = this.moveDelay;
        }
    }
}
