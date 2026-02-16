package com.goldsprite.magicdungeon.vfx;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class NeonParticle {
    public float x, y;
    public float vx, vy;
    public float life, maxLife;
    public float size;
    public Color color;
    public boolean dead;
    
    // 渲染类型
    public enum Type {
        CIRCLE, RECT, LINE
    }
    public Type type = Type.CIRCLE;

    public NeonParticle(float x, float y, float size, Color color, float life) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = color.cpy();
        this.life = life;
        this.maxLife = life;
        this.dead = false;
    }

    public void update(float dt) {
        life -= dt;
        if (life <= 0) {
            dead = true;
            return;
        }
        
        x += vx * dt;
        y += vy * dt;
        
        // 简单的阻尼效果，模拟空气阻力
        vx *= 0.95f;
        vy *= 0.95f;
    }

    public void render(NeonBatch batch) {
        float alpha = life / maxLife;
        color.a = alpha;
        
        if (type == Type.CIRCLE) {
            // 简单的实心圆
            batch.drawCircle(x, y, size * (life / maxLife), 0, color, 8, true);
        } else if (type == Type.RECT) {
            float s = size * (life / maxLife);
            batch.drawRect(x - s/2, y - s/2, s, s, 0, 0, color, true);
        }
    }
}
