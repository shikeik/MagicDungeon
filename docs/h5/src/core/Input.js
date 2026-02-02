import { KEYS } from '../utils/Constants.js';

export class Input {
    constructor() {
        this.keys = {};
        this.downKeys = {}; // Keys pressed this frame
        this.touchStart = { x: 0, y: 0 };
        this.touchEnd = { x: 0, y: 0 };
        this.isTouching = false;

        window.addEventListener('keydown', (e) => {
            this.keys[e.code] = true;
            this.downKeys[e.code] = true;
        });

        window.addEventListener('keyup', (e) => {
            this.keys[e.code] = false;
        });

        // Simple touch controls for mobile (virtual joystick logic can be added later)
        window.addEventListener('touchstart', (e) => {
            this.isTouching = true;
            this.touchStart.x = e.touches[0].clientX;
            this.touchStart.y = e.touches[0].clientY;
        }, { passive: false });

        window.addEventListener('touchend', (e) => {
            this.isTouching = false;
        });
        
        window.addEventListener('touchmove', (e) => {
            if(this.isTouching) {
                 this.touchEnd.x = e.touches[0].clientX;
                 this.touchEnd.y = e.touches[0].clientY;
            }
        }, { passive: false });
    }

    isKeyDown(keyCode) {
        return !!this.keys[keyCode];
    }

    isKeyPressed(keyCode) {
        return !!this.downKeys[keyCode];
    }

    update() {
        this.downKeys = {}; // Reset one-shot keys
    }
    
    // Get virtual axis from touch or keys
    getAxis() {
        let x = 0;
        let y = 0;

        if (this.isKeyDown(KEYS.W) || this.isKeyDown(KEYS.ARROW_UP)) y = -1;
        if (this.isKeyDown(KEYS.S) || this.isKeyDown(KEYS.ARROW_DOWN)) y = 1;
        if (this.isKeyDown(KEYS.A) || this.isKeyDown(KEYS.ARROW_LEFT)) x = -1;
        if (this.isKeyDown(KEYS.D) || this.isKeyDown(KEYS.ARROW_RIGHT)) x = 1;

        // Simple touch logic: if drag is significant, override keys
        if (this.isTouching) {
             const dx = this.touchEnd.x - this.touchStart.x;
             const dy = this.touchEnd.y - this.touchStart.y;
             if (Math.abs(dx) > 30 || Math.abs(dy) > 30) {
                 if (Math.abs(dx) > Math.abs(dy)) {
                     x = dx > 0 ? 1 : -1;
                     y = 0;
                 } else {
                     x = 0;
                     y = dy > 0 ? 1 : -1;
                 }
             }
        }

        return { x, y };
    }
}
