import { TILE_SIZE } from './Constants.js';

export const ASSETS = {
    TILES: {},
    CHARACTERS: {},
    ITEMS: {}
};

class SpriteGenerator {
    constructor() {
        this.canvas = document.createElement('canvas');
        this.ctx = this.canvas.getContext('2d');
        this.canvas.width = TILE_SIZE;
        this.canvas.height = TILE_SIZE;
    }

    clear() {
        this.ctx.clearRect(0, 0, TILE_SIZE, TILE_SIZE);
    }

    generateImage() {
        const img = new Image();
        img.src = this.canvas.toDataURL();
        return img;
    }

    // --- Tile Generators ---

    createWall() {
        this.clear();
        const ctx = this.ctx;
        // Base color
        ctx.fillStyle = '#444';
        ctx.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        
        // Bricks
        ctx.fillStyle = '#333';
        const brickH = 8;
        for(let y=0; y<TILE_SIZE; y+=brickH) {
            const offset = (y/brickH) % 2 === 0 ? 0 : 8;
            for(let x=-8; x<TILE_SIZE; x+=16) {
                ctx.fillRect(x + offset, y, 15, brickH-1);
            }
        }
        return this.generateImage();
    }

    createFloor() {
        this.clear();
        const ctx = this.ctx;
        ctx.fillStyle = '#222';
        ctx.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        
        // Noise/Gravel
        ctx.fillStyle = '#2a2a2a';
        for(let i=0; i<20; i++) {
            const x = Math.floor(Math.random() * TILE_SIZE);
            const y = Math.floor(Math.random() * TILE_SIZE);
            ctx.fillRect(x, y, 2, 2);
        }
        return this.generateImage();
    }

    createStairs() {
        this.clear();
        const ctx = this.ctx;
        ctx.fillStyle = '#222';
        ctx.fillRect(0, 0, TILE_SIZE, TILE_SIZE);
        
        ctx.fillStyle = '#666';
        for(let i=0; i<TILE_SIZE; i+=4) {
            ctx.fillRect(4, i, TILE_SIZE-8, 2);
        }
        return this.generateImage();
    }

    // --- Character Generators ---

    createPlayer() {
        this.clear();
        const ctx = this.ctx;
        
        // Head
        ctx.fillStyle = '#fcc'; // Skin
        ctx.fillRect(10, 4, 12, 10);
        
        // Helmet/Hair
        ctx.fillStyle = '#aaa';
        ctx.fillRect(10, 2, 12, 4);
        ctx.fillRect(8, 4, 2, 8);
        ctx.fillRect(22, 4, 2, 8);

        // Body (Armor)
        ctx.fillStyle = '#88f';
        ctx.fillRect(8, 14, 16, 12);
        
        // Legs
        ctx.fillStyle = '#444';
        ctx.fillRect(10, 26, 4, 6);
        ctx.fillRect(18, 26, 4, 6);

        // Eyes
        ctx.fillStyle = '#000';
        ctx.fillRect(12, 8, 2, 2);
        ctx.fillRect(18, 8, 2, 2);

        return this.generateImage();
    }

    createMonster(type) {
        this.clear();
        const ctx = this.ctx;
        
        if (type === 'SLIME') {
            ctx.fillStyle = '#00f';
            ctx.beginPath();
            ctx.arc(16, 20, 10, Math.PI, 0); // Top half
            ctx.lineTo(26, 28);
            ctx.lineTo(6, 28);
            ctx.fill();
            
            // Eyes
            ctx.fillStyle = '#fff';
            ctx.fillRect(12, 18, 3, 3);
            ctx.fillRect(18, 18, 3, 3);
            ctx.fillStyle = '#000';
            ctx.fillRect(13, 19, 1, 1);
            ctx.fillRect(19, 19, 1, 1);
        } else if (type === 'SKELETON') {
            ctx.fillStyle = '#eee';
            // Skull
            ctx.fillRect(10, 4, 12, 10);
            // Ribs
            ctx.fillRect(12, 16, 8, 10);
            // Legs
            ctx.fillRect(10, 26, 3, 6);
            ctx.fillRect(19, 26, 3, 6);
            // Eyes
            ctx.fillStyle = '#000';
            ctx.fillRect(12, 7, 3, 3);
            ctx.fillRect(17, 7, 3, 3);
        } else if (type === 'ORC') {
            ctx.fillStyle = '#0a0';
            ctx.fillRect(6, 6, 20, 22);
            // Tusks
            ctx.fillStyle = '#fff';
            ctx.fillRect(10, 20, 2, 4);
            ctx.fillRect(20, 20, 2, 4);
             // Eyes
             ctx.fillStyle = '#f00';
             ctx.fillRect(10, 10, 4, 2);
             ctx.fillRect(18, 10, 4, 2);
        } else if (type === 'BAT') {
             ctx.fillStyle = '#555';
             // Wings
             ctx.beginPath();
             ctx.moveTo(16, 16);
             ctx.lineTo(4, 10);
             ctx.lineTo(8, 22);
             ctx.lineTo(16, 22);
             ctx.lineTo(24, 22);
             ctx.lineTo(28, 10);
             ctx.fill();
             // Eyes
             ctx.fillStyle = '#ff0';
             ctx.fillRect(14, 18, 1, 1);
             ctx.fillRect(17, 18, 1, 1);
        } else if (type === 'BOSS') {
             // Dragon
             // Wings
             ctx.fillStyle = '#800'; // Dark Red Wings
             ctx.beginPath();
             ctx.moveTo(16, 16);
             ctx.lineTo(2, 6);
             ctx.lineTo(10, 20);
             ctx.lineTo(16, 24);
             ctx.lineTo(22, 20);
             ctx.lineTo(30, 6);
             ctx.fill();

             // Body
             ctx.fillStyle = '#d00'; // Bright Red
             ctx.fillRect(12, 10, 8, 16);
             
             // Head
             ctx.fillStyle = '#f00';
             ctx.fillRect(10, 6, 12, 10);
             
             // Horns
             ctx.fillStyle = '#ff0';
             ctx.fillRect(10, 2, 2, 6);
             ctx.fillRect(20, 2, 2, 6);

             // Eyes
             ctx.fillStyle = '#0f0'; // Green eyes
             ctx.fillRect(11, 10, 2, 2);
             ctx.fillRect(19, 10, 2, 2);

             // Tail
             ctx.fillStyle = '#d00';
             ctx.beginPath();
             ctx.moveTo(16, 26);
             ctx.lineTo(12, 30);
             ctx.lineTo(20, 30);
             ctx.fill();
        } else {
             // Generic blob
             ctx.fillStyle = '#f00';
             ctx.fillRect(8, 8, 16, 16);
        }

        return this.generateImage();
    }

    // --- Item Generators ---

    createItem(type) {
        this.clear();
        const ctx = this.ctx;

        if (type === 'WEAPON') {
            ctx.save();
            ctx.translate(16, 16);
            ctx.rotate(Math.PI / 4);
            ctx.translate(-16, -16);
            
            // Blade
            ctx.fillStyle = '#aaa';
            ctx.fillRect(14, 4, 4, 18);
            // Hilt
            ctx.fillStyle = '#630';
            ctx.fillRect(14, 22, 4, 6);
            // Guard
            ctx.fillStyle = '#gold';
            ctx.fillRect(10, 22, 12, 2);
            
            ctx.restore();
        } else if (type === 'ARMOR') {
            ctx.fillStyle = '#888';
            // Body
            ctx.fillRect(8, 8, 16, 18);
            // Shoulders
            ctx.fillRect(4, 8, 4, 6);
            ctx.fillRect(24, 8, 4, 6);
        } else if (type === 'POTION') {
            // Bottle
            ctx.fillStyle = '#eee';
            ctx.fillRect(12, 8, 8, 4); // Neck
            ctx.fillStyle = '#f00'; // Liquid (default red)
            ctx.beginPath();
            ctx.arc(16, 20, 8, 0, Math.PI * 2);
            ctx.fill();
        } else {
            ctx.fillStyle = '#ff0';
            ctx.beginPath();
            ctx.arc(16, 16, 6, 0, Math.PI * 2);
            ctx.fill();
        }

        return this.generateImage();
    }
}

export const initAssets = () => {
    const gen = new SpriteGenerator();

    ASSETS.TILES.WALL = gen.createWall();
    ASSETS.TILES.FLOOR = gen.createFloor();
    ASSETS.TILES.STAIRS = gen.createStairs();

    ASSETS.CHARACTERS.PLAYER = gen.createPlayer();
    
    ASSETS.CHARACTERS.SLIME = gen.createMonster('SLIME');
    ASSETS.CHARACTERS.SKELETON = gen.createMonster('SKELETON');
    ASSETS.CHARACTERS.ORC = gen.createMonster('ORC');
    ASSETS.CHARACTERS.BAT = gen.createMonster('BAT');
    ASSETS.CHARACTERS.BOSS = gen.createMonster('BOSS');

    ASSETS.ITEMS.WEAPON = gen.createItem('WEAPON');
    ASSETS.ITEMS.ARMOR = gen.createItem('ARMOR');
    ASSETS.ITEMS.POTION = gen.createItem('POTION');
    ASSETS.ITEMS.KEY = gen.createItem('KEY');
    
    console.log("Assets Generated");
};
