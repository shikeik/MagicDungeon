package com.goldsprite.magicdungeon.vfx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class TextParticle extends NeonParticle {
    public String text;
    private static final GlyphLayout layout = new GlyphLayout();

    public TextParticle(float x, float y, String text, Color color, float life) {
        super(x, y, 0, color, life);
        this.text = text;
        this.vy = 40; // 向上漂浮速度
    }

    @Override
    public void render(NeonBatch batch) {
        // Do nothing for NeonBatch
    }

    public void render(SpriteBatch batch, BitmapFont font) {
        float alpha = life / maxLife;
        // 保持原来的颜色，只修改透明度 (注意：font.setColor 会修改全局状态，最好存一下或者确保之后重置)
        // 但这里为了性能，假设外部会重置
        font.setColor(color.r, color.g, color.b, alpha);
        
        // 计算布局以居中
        layout.setText(font, text);
        font.draw(batch, text, x - layout.width / 2, y + layout.height / 2);
    }
}
