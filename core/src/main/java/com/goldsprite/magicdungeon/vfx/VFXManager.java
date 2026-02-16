package com.goldsprite.magicdungeon.vfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class VFXManager {
    private Array<NeonParticle> particles = new Array<>();

    public void update(float dt) {
        for (int i = particles.size - 1; i >= 0; i--) {
            NeonParticle p = particles.get(i);
            p.update(dt);
            if (p.dead) {
                particles.removeIndex(i);
            }
        }
    }

    public void render(NeonBatch batch) {
        for (NeonParticle p : particles) {
            p.render(batch);
        }
    }

    public void spawn(NeonParticle p) {
        particles.add(p);
    }
    
    // 简单的爆炸效果
    public void spawnExplosion(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            float size = MathUtils.random(2f, 5f);
            float life = MathUtils.random(0.3f, 0.6f);
            NeonParticle p = new NeonParticle(x, y, size, color, life);
            float angle = MathUtils.random(0, MathUtils.PI2);
            float speed = MathUtils.random(20f, 60f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed;
            spawn(p);
        }
    }

    // 漂浮文字效果 (简单版，用粒子模拟或者直接用字体绘制)
    // 这里暂时不处理字体，字体通常由 GameHUD 处理
}
