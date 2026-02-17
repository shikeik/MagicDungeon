package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格物品生成器
 * 替代 SpriteGenerator 中的 createItem 和 drawItemToPixmap
 */
public class NeonItemGenerator {

    private static final float REF_SIZE = 256f;

    public static Texture createItem(String name) {
        int size = 256; 
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawItem(batch, size, name);
        });
        return region == null ? null : region.getTexture();
    }

    public static void drawItem(NeonBatch batch, float size, String name) {
        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        float scale = size / REF_SIZE;
        if (scale != 1f) {
            batch.getTransformMatrix().scale(scale, scale, 1f);
        }

        try {
            drawItemImpl(batch, name);
        } finally {
            batch.setTransformMatrix(oldTransform);
        }
    }

    private static void drawItemImpl(NeonBatch batch, String name) {
        float size = REF_SIZE;

        if (name.contains("Potion") || name.contains("Elixir") || name.contains("药水") || name.contains("万能药")) {
            drawPotion(batch, size, name);
        } else if (name.contains("Sword") || name.contains("Blade") || name.contains("剑") || name.contains("刃")) {
            drawSword(batch, size, name);
        } else if (name.contains("Shield") || name.contains("盾")) {
            drawShield(batch, size, name);
        } else if (name.contains("Helmet") || name.contains("Hat") || name.contains("帽") || name.contains("盔")) {
            drawHelmet(batch, size, name);
        } else if (name.contains("Boots") || name.contains("Shoes") || name.contains("靴") || name.contains("鞋")) {
            drawBoots(batch, size, name);
        } else if (name.contains("Armor") || name.contains("Mail") || name.contains("甲")) {
            drawArmor(batch, size, name);
        } else if (name.contains("Axe") || name.contains("斧")) {
            drawAxe(batch, size, name);
        } else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
            drawWand(batch, size, name);
        } else if (name.contains("Scroll") || name.contains("卷轴") || name.contains("Book") || name.contains("书")) {
            drawScroll(batch, size, name);
        } else if (name.contains("Ring") || name.contains("戒指")) {
            drawRing(batch, size, name);
        } else if (name.contains("Necklace") || name.contains("项链")) {
            drawNecklace(batch, size, name);
        } else if (name.contains("Bracelet") || name.contains("手环")) {
            drawBracelet(batch, size, name);
        } else if (name.contains("Coin") || name.contains("金币")) {
            drawCoin(batch, size, name);
        } else {
            drawGenericItem(batch, size, name);
        }
    }

    // --- Specific Generators (All use size = REF_SIZE = 256) ---

    private static void drawPotion(NeonBatch batch, float size, String name) {
        Color liquid = Color.RED;
        if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
        if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;

        // Based on SpriteGenerator coordinates (approx)
        // Body: 128, 170, 70
        drawCirclePix(batch, size, 128, 170, 70, liquid);
        
        // Neck: 108, 60, 40, 110
        drawRectPix(batch, size, 108, 60, 40, 110, liquid.cpy().mul(0.8f));
        
        // Rim: 100, 50, 56, 15
        drawRectPix(batch, size, 100, 50, 56, 15, Color.valueOf("#cccccc"));
        
        // Cork: 113, 30, 30, 20
        drawRectPix(batch, size, 113, 30, 30, 20, Color.valueOf("#8d6e63"));

        // Highlight: 150, 150, 10
        drawCirclePix(batch, size, 150, 150, 10, new Color(1f, 1f, 1f, 0.4f));
    }

    private static void drawSword(NeonBatch batch, float size, String name) {
        Color bladeColor = Color.LIGHT_GRAY;
        if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
        if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");

        // Handle
        drawRectPix(batch, size, 60, 200, 24, 16, Color.valueOf("#3e2723"), 45); 
        
        // Blade (Diagonal)
        drawLinePix(batch, size, 60, 200, 200, 60, 20, bladeColor);
        
        // Guard
        drawLinePix(batch, size, 50, 190, 90, 230, 8, Color.valueOf("#5d4037"));
        
        // Hilt
        drawLinePix(batch, size, 30, 230, 50, 210, 8, Color.valueOf("#3e2723"));
    }

    private static void drawShield(NeonBatch batch, float size, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.GRAY : Color.valueOf("#5d4037");
        Color border = isIron ? Color.LIGHT_GRAY : Color.valueOf("#8d6e63");

        drawRectPix(batch, size, 50, 50, 156, 120, c);
        drawCirclePix(batch, size, 128, 170, 78, c);

        int bThick = 10;
        drawRectPix(batch, size, 45, 45, 166, bThick, border); // Top
        drawRectPix(batch, size, 45, 45, bThick, 140, border); // Left
        drawRectPix(batch, size, 201, 45, bThick, 140, border); // Right
        
        drawCirclePix(batch, size, 128, 230, 10, border);
        drawCirclePix(batch, size, 128, 120, 30, border);
    }

    private static void drawHelmet(NeonBatch batch, float size, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.LIGHT_GRAY : Color.valueOf("#5d4037");
        Color trim = isIron ? Color.GRAY : Color.valueOf("#3e2723");

        drawCirclePix(batch, size, 128, 128, 70, c);
        drawRectPix(batch, size, 50, 150, 156, 20, trim);
        drawRectPix(batch, size, 123, 40, 10, 30, trim);
    }

    private static void drawBoots(NeonBatch batch, float size, String name) {
        Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
        Color trim = name.contains("Iron") ? Color.LIGHT_GRAY : Color.valueOf("#3e2723");

        // Left Boot
        drawRectPix(batch, size, 60, 80, 50, 100, c);
        drawRectPix(batch, size, 40, 160, 70, 40, c);
        drawRectPix(batch, size, 60, 80, 50, 10, trim);

        // Right Boot
        drawRectPix(batch, size, 140, 80, 50, 100, c);
        drawRectPix(batch, size, 140, 160, 70, 40, c);
        drawRectPix(batch, size, 140, 80, 50, 10, trim);
    }

    private static void drawArmor(NeonBatch batch, float size, String name) {
        Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
        Color trim = Color.DARK_GRAY;

        drawRectPix(batch, size, 68, 60, 120, 140, c);
        drawCirclePix(batch, size, 60, 80, 30, trim);
        drawCirclePix(batch, size, 196, 80, 30, trim);
        drawRectPix(batch, size, 88, 80, 80, 80, c.cpy().mul(1.2f));
    }

    private static void drawAxe(NeonBatch batch, float size, String name) {
        drawLinePix(batch, size, 60, 200, 180, 80, 12, Color.valueOf("#3e2723"));
        drawCirclePix(batch, size, 180, 80, 40, Color.GRAY);
        drawCirclePix(batch, size, 170, 90, 30, Color.DARK_GRAY);
    }

    private static void drawWand(NeonBatch batch, float size, String name) {
        drawLinePix(batch, size, 60, 200, 180, 80, 8, Color.valueOf("#5d4037"));
        drawCirclePix(batch, size, 180, 80, 16, Color.CYAN);
        drawCirclePix(batch, size, 180, 80, 8, Color.WHITE);
    }

    private static void drawScroll(NeonBatch batch, float size, String name) {
        Color paper = Color.valueOf("#fff9c4");
        drawCirclePix(batch, size, 80, 80, 20, paper.cpy().mul(0.9f));
        drawCirclePix(batch, size, 80, 180, 20, paper.cpy().mul(0.9f));
        drawRectPix(batch, size, 80, 80, 100, 100, paper);
        for(int i=0; i<3; i++) {
            drawRectPix(batch, size, 100, 100 + i*20, 60, 4, Color.BLACK);
        }
    }

    private static void drawRing(NeonBatch batch, float size, String name) {
        drawCirclePix(batch, size, 128, 128, 40, Color.GOLD);
        drawCirclePix(batch, size, 128, 128, 30, Color.BLACK); 
        drawCirclePix(batch, size, 128, 88, 16, Color.RED);
    }

    private static void drawNecklace(NeonBatch batch, float size, String name) {
        drawLinePix(batch, size, 80, 60, 128, 160, 4, Color.GOLD);
        drawLinePix(batch, size, 176, 60, 128, 160, 4, Color.GOLD);
        drawCirclePix(batch, size, 128, 160, 20, Color.BLUE);
    }

    private static void drawBracelet(NeonBatch batch, float size, String name) {
        drawCirclePix(batch, size, 128, 128, 50, Color.GOLD);
        drawCirclePix(batch, size, 128, 128, 40, Color.BLACK);
    }

    private static void drawCoin(NeonBatch batch, float size, String name) {
        drawCirclePix(batch, size, 128, 128, 60, Color.GOLD);
        drawRectPix(batch, size, 108, 108, 40, 40, Color.YELLOW);
    }

    private static void drawGenericItem(NeonBatch batch, float size, String name) {
        drawCirclePix(batch, size, 128, 128, 80, Color.GRAY);
        drawRectPix(batch, size, 120, 80, 16, 60, Color.WHITE);
        drawRectPix(batch, size, 120, 160, 16, 16, Color.WHITE);
    }

    // --- Helpers (Top-Left Origin) ---

    private static void drawRectPix(NeonBatch batch, float totalSize, float x, float y, float w, float h, Color color) {
        drawRectPix(batch, totalSize, x, y, w, h, color, 0);
    }

    private static void drawRectPix(NeonBatch batch, float totalSize, float x, float y, float w, float h, Color color, float rotationDeg) {
        float glY = totalSize - y - h;
        // NeonBatch.drawRect rotates around center.
        // Signature: drawRect(x, y, width, height, rotationDeg, lineWidth, color, filled)
        batch.drawRect(x, glY, w, h, rotationDeg, 0, color, true);
    }

    private static void drawCirclePix(NeonBatch batch, float totalSize, float cx, float cy, float r, Color color) {
        float glCy = totalSize - cy;
        // Signature: drawCircle(x, y, radius, lineWidth, color, segments, filled)
        batch.drawCircle(cx, glCy, r, 0, color, 32, true);
    }
    
    private static void drawLinePix(NeonBatch batch, float totalSize, float x1, float y1, float x2, float y2, float width, Color color) {
        float glY1 = totalSize - y1;
        float glY2 = totalSize - y2;
        // Signature: drawLine(x1, y1, x2, y2, width, color)
        batch.drawLine(x1, glY1, x2, glY2, width, color);
    }
}
