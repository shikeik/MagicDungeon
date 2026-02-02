import { Entity } from './Entity.js';
import { ITEM_TYPE } from './Item.js';

export class Player extends Entity {
    constructor(x, y) {
        super(x, y, '#0f0'); // Green player
        this.stats = {
            hp: 100,
            maxHp: 100,
            level: 1,
            xp: 0,
            atk: 5,
            def: 0,
            kills: 0,
            skillPoints: 0
        };
        this.inventory = [];
        this.skills = [];
        this.equipment = {
            weapon: null,
            armor: null
        };
        
        this.moveTimer = 0;
        this.moveDelay = 0.12; // Movement speed
        
        // Ensure visual speed matches logic speed
        this.visualSpeed = 32 / this.moveDelay; // 32 pixels in moveDelay seconds
    }

    equip(item) {
        if (item.data.type === ITEM_TYPE.WEAPON) {
            this.equipment.weapon = item;
            this.updateStats();
        } else if (item.data.type === ITEM_TYPE.ARMOR) {
            this.equipment.armor = item;
            this.updateStats();
        } else if (item.data.type === ITEM_TYPE.POTION) {
            this.usePotion(item);
        }
    }
    
    usePotion(item) {
        if (item.data.heal) {
            this.stats.hp = Math.min(this.stats.maxHp, this.stats.hp + item.data.heal);
            // Remove from inventory
            const index = this.inventory.indexOf(item);
            if (index > -1) {
                this.inventory.splice(index, 1);
            }
        }
    }

    updateStats() {
        let atk = 5; // Base
        let def = 0;
        
        if (this.equipment.weapon) atk += this.equipment.weapon.data.atk;
        if (this.equipment.armor) def += this.equipment.armor.data.def;
        
        this.stats.atk = atk;
        this.stats.def = def;
    }

    update(dt, input, map, monsters, items) {
        this.moveTimer -= dt;
        if (this.moveTimer > 0) return null;

        const axis = input.getAxis();
        if (axis.x !== 0 || axis.y !== 0) {
            // Prevent diagonal movement
            if (axis.x !== 0 && axis.y !== 0) {
                // Prioritize X or Y? Let's just zero out Y if X is present for simplicity, or keep Y if X is 0
                // Or better: check which key was pressed last? 
                // Since we don't track history here, let's just pick one.
                // Usually prioritize horizontal or vertical based on game feel.
                // Let's zero out Y to prioritize X movement, or vice versa.
                axis.y = 0;
            }

            const nextX = this.x + axis.x;
            const nextY = this.y + axis.y;

            // Check for monsters
            const targetMonster = monsters.find(m => m.x === nextX && m.y === nextY && m.hp > 0);
            if (targetMonster) {
                this.moveTimer = this.moveDelay;
                return { type: 'attack', target: targetMonster, damage: this.stats.atk };
            }

            if (map.isWalkable(nextX, nextY)) {
                this.x = nextX;
                this.y = nextY;
                this.moveTimer = this.moveDelay;
                
                // Check for items
                // This will be handled by Game loop checking overlap, or here.
                // Let's return a move event.
                return { type: 'move', x: this.x, y: this.y };
            }
        }
        return null;
    }
}
