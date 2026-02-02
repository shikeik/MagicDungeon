import { Input } from './Input.js';
import { Renderer } from '../systems/Renderer.js';
import { GAME_STATE } from '../utils/Constants.js';
import { Dungeon } from '../world/Dungeon.js';
import { Player } from '../entities/Player.js';
import { Monster, MONSTER_TYPES } from '../entities/Monster.js';
import { Item, ITEMS, ITEM_TYPE } from '../entities/Item.js';
import { SaveSystem } from '../systems/SaveSystem.js';
import { AchievementSystem } from '../systems/AchievementSystem.js';
import { SkillSystem, SKILLS } from '../systems/SkillSystem.js';
import { AudioSystem } from '../systems/AudioSystem.js';
import { initAssets, ASSETS } from '../utils/SpriteGenerator.js';

import { TILE_SIZE } from '../utils/Constants.js';
import { TILE_TYPE } from '../world/Tile.js';

export class Game {
    constructor() {
        // Init assets first
        initAssets();
        
        this.renderer = new Renderer('game-canvas');
        this.input = new Input();
        this.audio = new AudioSystem();
        this.state = GAME_STATE.MENU;
        this.lastTime = 0;
        
        this.monsters = [];
        this.items = [];
        this.player = null;
        this.map = null;
        this.log = [];
        this.level = 1;
        
        this.achievements = new AchievementSystem();
        this.skillSystem = null;

        // UI Binding
        this.bindUI();
    }

    bindUI() {
        document.getElementById('start-btn').addEventListener('click', () => {
            this.startGame();
        });
        document.getElementById('load-btn').addEventListener('click', () => {
            this.loadGame();
        });
        
        // New UI bindings
        document.getElementById('help-btn')?.addEventListener('click', () => {
            document.getElementById('help-modal').style.display = 'flex';
        });
        
        document.getElementById('inventory-btn')?.addEventListener('click', () => {
            this.updateInventoryUI();
            document.getElementById('inventory-modal').style.display = 'flex';
        });
        
        document.getElementById('save-btn')?.addEventListener('click', () => {
            this.saveGame();
        });
        
        document.querySelectorAll('.close-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                e.target.closest('.modal').style.display = 'none';
            });
        });
    }


    saveGame() {
        if (!this.player || !this.map) return;
        
        const saveData = {
            player: {
                stats: this.player.stats,
                inventory: this.player.inventory,
                equipment: this.player.equipment,
                x: this.player.x,
                y: this.player.y
            },
            level: this.level,
            // For full save we need map data, monsters, items. 
            // Simplified: Save player and restart level generation logic or simplified state
            seed: Date.now() // Ideally use a seed for map gen
        };
        
        if (SaveSystem.save('dungeon_save', saveData)) {
            this.logMessage("Game Saved!");
            alert("Game Saved Successfully!"); // Explicit feedback
        } else {
            this.logMessage("Save Failed!");
            alert("Save Failed!");
        }
    }

    loadGame() {
        const data = SaveSystem.load('dungeon_save');
        if (!data) {
            this.logMessage("No save found!");
            return;
        }

        // Start game at the saved level
        this.startGame(data.level || 1);

        // Restore stats
        if (this.player) {
            this.player.stats = data.player.stats;
            this.player.inventory = data.player.inventory;
            this.player.equipment = data.player.equipment;
            
            // Restore equipment references to inventory items
            // Since inventory is loaded from JSON, objects are new. 
            // We need to link equipment back to the inventory items if possible, 
            // or just rely on the data being correct.
            // Simplified: Re-assigning stats is enough for logic, but for equipment UI 
            // we might need to check if the exact object references matter.
            // In equip(), we store reference. Here we loaded copies. 
            // So this.player.equipment.weapon is NOT in this.player.inventory array.
            // Fix: Find the matching item in inventory and link it.
            
            if (this.player.equipment.weapon) {
                const w = this.player.inventory.find(i => i.data.name === this.player.equipment.weapon.data.name);
                if (w) this.player.equipment.weapon = w;
            }
            if (this.player.equipment.armor) {
                const a = this.player.inventory.find(i => i.data.name === this.player.equipment.armor.data.name);
                if (a) this.player.equipment.armor = a;
            }
        }
        this.logMessage("Game Loaded (Level " + this.level + ")");
        this.updateInventoryUI();
    }

    startGame(startLevel = 1) {
        document.getElementById('main-menu').style.display = 'none';
        document.getElementById('hud').style.display = 'block';
        this.state = GAME_STATE.PLAYING;
        this.level = startLevel;
        
        this.initLevel();
    }

    initLevel() {
        this.map = new Dungeon(50, 50, this.level);
        // If player exists, keep stats but reset pos
        if (!this.player) {
            this.player = new Player(this.map.startPos.x, this.map.startPos.y);
            this.skillSystem = new SkillSystem(this.player);
            this.player.stats.skillPoints = 1;
            this.skillSystem.unlock(SKILLS.HEAL.id);
        } else {
            this.player.x = this.map.startPos.x;
            this.player.y = this.map.startPos.y;
            this.player.visualX = this.player.x * TILE_SIZE;
            this.player.visualY = this.player.y * TILE_SIZE;
        }
        
        // Spawn Monsters (More/Stronger based on level)
        this.monsters = [];
        const monsterCount = 10 + Math.floor(this.level * 2);
        for (let i = 0; i < monsterCount; i++) {
            const pos = this.map.getRandomWalkableTile();
            if (pos && (pos.x !== this.player.x || pos.y !== this.player.y)) {
                const types = Object.values(MONSTER_TYPES);
                // Simple difficulty scaling: filter out weak monsters in later levels or just random
                const type = types[Math.floor(Math.random() * types.length)];
                const m = new Monster(pos.x, pos.y, type);
                // Scale stats
                m.maxHp += (this.level - 1) * 5;
                m.hp = m.maxHp;
                m.atk += (this.level - 1) * 2;
                this.monsters.push(m);
            }
        }

        // Spawn Items
        this.items = [];
        for (let i = 0; i < 5; i++) {
            const pos = this.map.getRandomWalkableTile();
            if (pos) {
                const itemTypes = Object.values(ITEMS);
                const itemType = itemTypes[Math.floor(Math.random() * itemTypes.length)];
                this.items.push(new Item(pos.x, pos.y, itemType));
            }
        }

        this.renderer.updateCamera(this.player);
        this.logMessage(`Entered Level ${this.level}`);
        console.log(`Level ${this.level} Started`);
    }

    nextLevel() {
        this.level++;
        this.logMessage(`Descending to Level ${this.level}...`);
        this.audio.playLevelUp(); // Re-use sound for now
        this.initLevel();
        
        // Auto Save
        this.saveGame();
        this.logMessage("Auto-saved game.");
    }

    logMessage(msg) {
        console.log(msg);
        this.log.push(msg);
        if (this.log.length > 10) this.log.shift();
        
        const logEl = document.getElementById('game-log');
        if (logEl) {
            logEl.innerHTML = this.log.slice().reverse().map(m => `<div>${m}</div>`).join('');
        }
    }

    start() {
        requestAnimationFrame((time) => this.loop(time));
    }

    loop(time) {
        const dt = (time - this.lastTime) / 1000;
        this.lastTime = time;

        this.update(dt);
        this.render();

        requestAnimationFrame((t) => this.loop(t));
    }

    update(dt) {
        this.input.update();

        if (this.state === GAME_STATE.PLAYING) {
            if (this.player) {
                // Visual Updates
                this.player.updateVisuals(dt);
                this.monsters.forEach(m => m.updateVisuals(dt));

                // Skills update
                if (this.skillSystem) this.skillSystem.update(dt);
                
                // Skill input (Space to Heal)
                if (this.input.isKeyPressed('Space')) {
                     if (this.skillSystem.use(SKILLS.HEAL.id)) {
                         this.logMessage("Used Heal!");
                     } else {
                         this.logMessage("Heal not ready or locked.");
                     }
                }

                const action = this.player.update(dt, this.input, this.map, this.monsters);
                if (action) {
                    if (action.type === 'attack') {
                        this.handleAttack(this.player, action.target, action.damage);
                    } else if (action.type === 'move') {
                        // Check for item pickup
                        const itemIndex = this.items.findIndex(i => i.x === action.x && i.y === action.y);
                        if (itemIndex !== -1) {
                            const item = this.items[itemIndex];
                            this.items.splice(itemIndex, 1);
                            this.player.inventory.push(item);
                            this.logMessage(`Picked up ${item.data.name}`);
                            this.audio.playItem();
                            
                            // Auto equip for simplicity if better or first
                            this.player.equip(item);
                        }
                        
                        // Check for stairs
                        const tile = this.map.getTile(action.x, action.y);
                        if (tile && tile.type === TILE_TYPE.STAIRS_DOWN) {
                             this.logMessage("You found the stairs! Press SPACE or click Next Level to descend.");
                             // For now, auto descend or simple check
                             // Let's auto descend for seamlessness or maybe wait for input?
                             // User asked "How to enter next level", implying manual or automatic.
                             // Let's make it automatic upon stepping on it for now to answer "How".
                             this.nextLevel();
                        }
                    }
                }
                
                // Update Monsters
                this.monsters.forEach(m => {
                    const result = m.update(dt, this.player, this.map, this.monsters);
                    if (result && result.type === 'attack') {
                        this.handleAttack(m, this.player, result.damage);
                    }
                });

                // Remove dead monsters
                this.monsters = this.monsters.filter(m => m.hp > 0);

                this.renderer.updateCamera(this.player);
                this.updateUI();
            }
        }
    }

    handleAttack(attacker, target, damage) {
        // Bump Animation
        const dx = target.x - attacker.x;
        const dy = target.y - attacker.y;
        attacker.triggerBump(dx, dy);

        target.hp -= damage;
        this.audio.playAttack();
        this.logMessage(`${attacker.name || attacker.constructor.name} attacks ${target.name || target.constructor.name} for ${damage} damage!`);
        
        if (target === this.player) {
            // Visual feedback for player damage
            const overlay = document.getElementById('damage-overlay');
            if (overlay) {
                overlay.style.opacity = 0.5;
                setTimeout(() => overlay.style.opacity = 0, 200);
            }
        }

        if (target.hp <= 0) {
            this.logMessage(`${target.name || target.constructor.name} dies!`);
            
            if (target === this.player) {
                this.state = GAME_STATE.GAME_OVER;
                alert("Game Over!");
                location.reload();
            } else {
                // Monster died
                if (attacker === this.player) {
                    this.player.stats.kills++;
                    this.player.stats.xp += 10; // Flat XP for now
                    if (this.player.stats.xp >= this.player.stats.level * 50) {
                        this.player.stats.level++;
                        this.player.stats.xp = 0;
                        this.player.stats.maxHp += 10;
                        this.player.stats.hp = this.player.stats.maxHp;
                        this.player.stats.atk += 2;
                        this.player.stats.skillPoints++;
                        this.logMessage(`Level Up! You are now level ${this.player.stats.level}`);
                        this.audio.playLevelUp();
                    }
                    
                    // Check Achievements
                    const newAch = this.achievements.check(this.player.stats);
                    newAch.forEach(ach => this.logMessage(`ACHIEVEMENT: ${ach.name}`));
                }
            }
        }
    }

    updateInventoryUI() {
        const list = document.getElementById('inventory-list');
        if (!list || !this.player) return;
        
        const typeNames = ['Weapon', 'Armor', 'Potion', 'Key'];

        list.innerHTML = '';
        this.player.inventory.forEach(item => {
            const div = document.createElement('div');
            div.className = 'inventory-item';
            
            const isEquipped = (this.player.equipment.weapon === item) || (this.player.equipment.armor === item);
            if (isEquipped) {
                div.classList.add('equipped');
                div.style.borderColor = '#ffd700'; // Gold border for equipped
            }

            const typeName = typeNames[item.data.type] || 'Unknown';

            div.innerHTML = `
                <span class="item-name">${item.data.name} ${isEquipped ? '<span style="color:#ffd700">(E)</span>' : ''}</span>
                <span class="item-type">${typeName}</span>
            `;
            // Simple click to equip/use
            div.addEventListener('click', () => {
                this.player.equip(item);
                const action = item.data.type === ITEM_TYPE.POTION ? 'Used' : 'Equipped';
                this.logMessage(`${action} ${item.data.name}`);
                this.updateInventoryUI(); // Refresh
            });
            list.appendChild(div);
        });
        
        // Stats
        const stats = document.getElementById('player-stats-detail');
        if (stats) {
            stats.innerHTML = `
                Level: ${this.player.stats.level}<br>
                HP: ${this.player.stats.hp}/${this.player.stats.maxHp}<br>
                ATK: ${this.player.stats.atk}<br>
                DEF: ${this.player.stats.def}<br>
                XP: ${this.player.stats.xp}
            `;
        }
    }

    updateUI() {
        if (this.player) {
            document.getElementById('hp-val').innerText = this.player.stats.hp;
            document.getElementById('hp-max').innerText = this.player.stats.maxHp;
            document.getElementById('level-val').innerText = this.player.stats.level;
        }
    }

    render() {
        this.renderer.clear();

        if (this.state === GAME_STATE.PLAYING && this.map) {
            this.renderer.drawMap(this.map);
            
            // Draw Items
            this.items.forEach(i => {
                let sprite = null;
                if (i.data.type === ITEM_TYPE.WEAPON) sprite = ASSETS.ITEMS.WEAPON;
                else if (i.data.type === ITEM_TYPE.ARMOR) sprite = ASSETS.ITEMS.ARMOR;
                else if (i.data.type === ITEM_TYPE.POTION) sprite = ASSETS.ITEMS.POTION;
                else sprite = i.color;
                
                this.renderer.drawSprite(sprite, i.x, i.y); 
            });

            // Draw Monsters
            this.monsters.forEach(m => {
                let sprite = null;
                if (m.name === 'Slime') sprite = ASSETS.CHARACTERS.SLIME;
                else if (m.name === 'Skeleton') sprite = ASSETS.CHARACTERS.SKELETON;
                else if (m.name === 'Orc') sprite = ASSETS.CHARACTERS.ORC;
                else if (m.name === 'Bat') sprite = ASSETS.CHARACTERS.BAT;
                else if (m.name === 'Dragon') sprite = ASSETS.CHARACTERS.BOSS;
                else sprite = m.color;

                const drawX = (m.visualX + m.bumpX) / TILE_SIZE;
                const drawY = (m.visualY + m.bumpY) / TILE_SIZE;
                this.renderer.drawSprite(sprite, drawX, drawY);
            });

            if (this.player) {
                const drawX = (this.player.visualX + this.player.bumpX) / TILE_SIZE;
                const drawY = (this.player.visualY + this.player.bumpY) / TILE_SIZE;
                this.renderer.drawSprite(ASSETS.CHARACTERS.PLAYER, drawX, drawY);
            }
        }
    }
}
