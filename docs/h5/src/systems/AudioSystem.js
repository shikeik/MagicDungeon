export class AudioSystem {
    constructor() {
        this.ctx = new (window.AudioContext || window.webkitAudioContext)();
        this.enabled = true;
    }

    playTone(freq, type, duration) {
        if (!this.enabled) return;
        
        // Resume context if suspended (browser policy)
        if (this.ctx.state === 'suspended') {
            this.ctx.resume();
        }

        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();

        osc.type = type;
        osc.frequency.setValueAtTime(freq, this.ctx.currentTime);
        
        gain.gain.setValueAtTime(0.1, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, this.ctx.currentTime + duration);

        osc.connect(gain);
        gain.connect(this.ctx.destination);

        osc.start();
        osc.stop(this.ctx.currentTime + duration);
    }

    playAttack() {
        this.playTone(150, 'sawtooth', 0.1);
    }

    playHit() {
        this.playTone(100, 'square', 0.2);
    }

    playMove() {
        // Very quiet click
        // this.playTone(50, 'sine', 0.05);
    }
    
    playItem() {
        this.playTone(600, 'sine', 0.3);
        setTimeout(() => this.playTone(800, 'sine', 0.3), 100);
    }
    
    playLevelUp() {
        this.playTone(400, 'sine', 0.2);
        setTimeout(() => this.playTone(500, 'sine', 0.2), 200);
        setTimeout(() => this.playTone(600, 'sine', 0.4), 400);
    }
}
