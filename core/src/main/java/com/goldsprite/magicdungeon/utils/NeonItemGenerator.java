package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格物品生成器
 * 替代 SpriteGenerator 中的 createItem 和 drawItemToPixmap
 */
public class NeonItemGenerator {

    public static Texture createItem(String name) {
        int size = 128; // Default item size
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawItem(batch, size, name);
        });
        return region == null ? null : region.getTexture();
    }

    public static void drawItem(NeonBatch batch, float size, String name) {
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
            // Default generic item
            drawGenericItem(batch, size, name);
        }
    }

    // --- Specific Generators ---

    private static void drawPotion(NeonBatch batch, float size, String name) {
        Color liquid = Color.RED;
        if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
        if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;

        // Flask Body (Bottom Circle)
        // SpriteGenerator: cx=128, cy=170, r=70
        drawCirclePix(batch, size, 128, 170, 70, liquid);
        
        // Neck
        // SpriteGenerator: x=108, y=60, w=40, h=110
        drawRectPix(batch, size, 108, 60, 40, 110, liquid.cpy().mul(0.8f));
        
        // Rim
        // SpriteGenerator: x=100, y=50, w=56, h=15
        drawRectPix(batch, size, 100, 50, 56, 15, Color.valueOf("#cccccc"));
        
        // Cork
        // SpriteGenerator: x=113, y=30, w=30, h=20
        drawRectPix(batch, size, 113, 30, 30, 20, Color.valueOf("#8d6e63"));

        // Highlight
        drawCirclePix(batch, size, 150, 150, 10, new Color(1f, 1f, 1f, 0.4f));
    }

    private static void drawSword(NeonBatch batch, float size, String name) {
        Color bladeColor = Color.LIGHT_GRAY;
        if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
        if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");

        // NeonBatch supports rotation, so we can draw the sword at an angle cleanly.
        // But SpriteGenerator draws it diagonally using lines.
        // Let's replicate the diagonal placement.
        
        // Handle
        // Line from 60,200 to 85,175. Length ~35. Angle -45 deg.
        // Let's use rotated rects for cleaner look.
        
        float cx = 128; 
        float cy = 128;
        
        // Let's define the sword upright then rotate it?
        // Or just map the coordinates from SpriteGenerator approx.
        
        // Handle: 60,200 to 85,175
        // Using drawLine from NeonBatch for simplicity (it supports width)
        drawLinePix(batch, size, 60, 200, 85, 175, 16, Color.valueOf("#3e2723"));
        drawCirclePix(batch, size, 55, 205, 12, Color.GOLD); // Pommel

        // Guard
        // Line from 65,155 to 105,195
        drawLinePix(batch, size, 65, 155, 105, 195, 20, Color.valueOf("#5d4037"));
        drawCirclePix(batch, size, 65, 155, 10, Color.GOLD);
        drawCirclePix(batch, size, 105, 195, 10, Color.GOLD);

        // Blade
        // From 90,170 to 210,50
        drawLinePix(batch, size, 90, 170, 210, 50, 40, bladeColor);
        
        // Edge (Highlight)
        drawLinePix(batch, size, 90, 170, 210, 50, 32, bladeColor.cpy().mul(1.2f));
        
        // Blood Groove
        drawLinePix(batch, size, 95, 165, 205, 55, 6, bladeColor.cpy().mul(0.7f));
        
        // Tip (Triangular approximation)
        // 210,50 is the end of the main rect.
        // Extend to 230,30
        // We can draw a triangle or a tapered line
        // NeonBatch doesn't have drawLinePix, I'll implement it.
    }

    private static void drawShield(NeonBatch batch, float size, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.GRAY : Color.valueOf("#5d4037");
        Color border = isIron ? Color.LIGHT_GRAY : Color.valueOf("#8d6e63");

        // Main Body: Rect + Circle
        drawRectPix(batch, size, 50, 50, 156, 120, c);
        drawCirclePix(batch, size, 128, 170, 78, c);

        // Texture (Wood grain)
        if (!isIron) {
            Color woodDark = Color.valueOf("#3e2723");
            for(int i=1; i<5; i++) {
                drawRectPix(batch, size, 50 + i*30, 50, 4, 180, woodDark);
            }
        }

        // Border
        int bThick = 10;
        drawRectPix(batch, size, 45, 45, 166, bThick, border); // Top
        drawRectPix(batch, size, 45, 45, bThick, 140, border); // Left
        drawRectPix(batch, size, 201, 45, bThick, 140, border); // Right
        
        // Bottom Curve Border
        drawCirclePix(batch, size, 128, 230, 10, border);
        drawCirclePix(batch, size, 80, 210, 10, border);
        drawCirclePix(batch, size, 176, 210, 10, border);

        // Boss
        drawCirclePix(batch, size, 128, 120, 30, border);
    }

    private static void drawHelmet(NeonBatch batch, float size, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.LIGHT_GRAY : Color.valueOf("#5d4037");
        Color trim = isIron ? Color.GRAY : Color.valueOf("#3e2723");

        // Dome
        drawCirclePix(batch, size, 128, 128, 70, c);
        
        // Mask bottom of circle to make it a helmet shape?
        // SpriteGenerator draws a transparent rect to cut the bottom.
        // NeonBatch doesn't support subtraction easily.
        // Instead, we can draw the bottom part with background color? No, transparency is needed.
        // Better: Draw a chord/segment or just cover it with the "Rim" which is rectangular.
        
        // Rim/Guard
        drawRectPix(batch, size, 50, 150, 156, 20, trim);
        
        // Top Spike
        drawRectPix(batch, size, 123, 40, 10, 30, trim);
        
        if (isIron) {
             drawRectPix(batch, size, 126, 100, 4, 50, Color.BLACK);
             drawRectPix(batch, size, 90, 120, 76, 4, Color.BLACK);
        }
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
        boolean isLeather = name.contains("Leather") || name.contains("皮");
        Color base = isLeather ? Color.valueOf("#8d6e63") : Color.LIGHT_GRAY;
        Color trim = isLeather ? Color.valueOf("#5d4037") : Color.GRAY;

        // Chest
        drawRectPix(batch, size, 70, 60, 116, 80, base);
        // Stomach
        drawRectPix(batch, size, 75, 145, 106, 25, base);
        drawRectPix(batch, size, 75, 175, 106, 20, base);

        // Straps
        drawRectPix(batch, size, 60, 100, 136, 10, trim);
        drawRectPix(batch, size, 120, 60, 16, 140, trim);

        // Shoulders
        drawCirclePix(batch, size, 60, 70, 25, trim);
        drawCirclePix(batch, size, 196, 70, 25, trim);
    }

    private static void drawAxe(NeonBatch batch, float size, String name) {
        // Handle
        drawLinePix(batch, size, 180, 40, 60, 200, 16, Color.valueOf("#3e2723"));
        
        // Axe Head
        float cx = 165;
        float cy = 60;
        Color metal = Color.LIGHT_GRAY;
        Color edge = Color.WHITE;

        // Central Block
        drawRectPix(batch, size, cx - 15, cy - 25, 30, 50, Color.GRAY);
        drawCirclePix(batch, size, cx, cy, 8, Color.BLACK);

        // Left Blade (Simulated with rects/triangles or polygon)
        // NeonBatch supports polygon
        // Left Blade
        float[] leftBlade = new float[] {
            cx - 15, cy - 15, // Inner Top
            cx - 15, cy + 15, // Inner Bottom
            cx - 70, cy + 40, // Outer Bottom
            cx - 60, cy,      // Outer Mid
            cx - 70, cy - 40  // Outer Top
        };
        // Need to map coordinates? No, drawPolygon takes raw float[].
        // But we need to map to UV.
        // Let's implement drawPolygonPix helper.
        drawPolygonPix(batch, size, leftBlade, metal);
        
        // Right Blade
        float[] rightBlade = new float[] {
            cx + 15, cy - 15,
            cx + 15, cy + 15,
            cx + 70, cy + 40,
            cx + 60, cy,
            cx + 70, cy - 40
        };
        drawPolygonPix(batch, size, rightBlade, metal);
    }

    private static void drawWand(NeonBatch batch, float size, String name) {
        // Shaft
        drawLinePix(batch, size, 80, 200, 180, 60, 12, Color.valueOf("#5d4037"));
        
        // Crystal
        drawCirclePix(batch, size, 180, 60, 25, Color.CYAN);
        drawCirclePix(batch, size, 180, 60, 15, Color.WHITE);
        
        // Glow
        drawCirclePix(batch, size, 180, 60, 40, new Color(0, 1, 1, 0.3f));
    }

    private static void drawScroll(NeonBatch batch, float size, String name) {
        Color paper = Color.valueOf("#fff9c4");
        // Left Page
        drawRectPix(batch, size, 40, 80, 88, 100, paper);
        // Right Page
        drawRectPix(batch, size, 128, 80, 88, 100, paper);
        // Spine
        drawRectPix(batch, size, 126, 80, 4, 100, Color.valueOf("#5d4037"));
        
        // Text lines
        for(int i=0; i<6; i++) {
            drawRectPix(batch, size, 50, 90 + i*12, 60, 2, Color.BLACK);
            drawRectPix(batch, size, 140, 90 + i*12, 60, 2, Color.BLACK);
        }
    }

    private static void drawRing(NeonBatch batch, float size, String name) {
        boolean isPower = name.contains("Power") || name.contains("力量");
        Color bandColor = isPower ? Color.GOLD : Color.LIGHT_GRAY;
        
        // Ring Band (Donut)
        // Draw outer circle, then inner circle with background color?
        // No, NeonBatch doesn't support clearing/masking easily in single pass without stencil.
        // We can draw a thick stroke circle.
        // NeonBatch.drawCircle with filled=false and strokeWidth.
        
        // Map center and radius
        float base = 256f;
        float u = 128 / base;
        float v = 1.0f - 128 / base;
        float r = 50 / base * size;
        float stroke = (15 / base) * size; // Thickness
        
        batch.drawCircle(u * size, v * size, r, stroke, bandColor, 32, false);
        
        // Gem
        Color gemColor = isPower ? Color.RED : Color.CYAN;
        drawRectPix(batch, size, 110, 60, 36, 36, gemColor);
    }

    private static void drawNecklace(NeonBatch batch, float size, String name) {
        // Chain (Circle stroke)
        float base = 256f;
        float u = 128 / base;
        float v = 1.0f - 100 / base; // Center Y=100
        float r = 70 / base * size;
        float stroke = (4 / base) * size;
        
        batch.drawCircle(u * size, v * size, r, stroke, Color.GOLD, 32, false);
        
        // Pendant
        Color gemColor = Color.RED;
        if(name.contains("Blue") || name.contains("蓝")) gemColor = Color.BLUE;
        
        drawRectPix(batch, size, 118, 170, 20, 30, Color.GOLD);
        drawCirclePix(batch, size, 128, 185, 12, gemColor);
    }

    private static void drawBracelet(NeonBatch batch, float size, String name) {
        // Bracelet (Circle stroke)
        float base = 256f;
        float u = 128 / base;
        float v = 1.0f - 128 / base;
        float r = 60 / base * size;
        float stroke = (15 / base) * size;
        
        batch.drawCircle(u * size, v * size, r, stroke, Color.GOLD, 32, false);
        
        // Decoration
        drawCirclePix(batch, size, 128, 68, 8, Color.RED);
        drawCirclePix(batch, size, 128, 188, 8, Color.RED);
    }

    private static void drawCoin(NeonBatch batch, float size, String name) {
        drawCirclePix(batch, size, 128, 128, 80, Color.ORANGE);
        drawCirclePix(batch, size, 128, 128, 60, Color.GOLD);
        drawCirclePix(batch, size, 110, 110, 10, new Color(1f, 1f, 1f, 0.6f)); // Shine
    }

    private static void drawGenericItem(NeonBatch batch, float size, String name) {
        // Draw a question mark box
        drawRectPix(batch, size, 64, 64, 128, 128, Color.DARK_GRAY);
        drawRectPix(batch, size, 120, 80, 16, 60, Color.WHITE);
        drawCirclePix(batch, size, 128, 160, 10, Color.WHITE);
    }

    // --- Helper Methods ---

    /**
     * Map 256x256 Top-Left coordinates to NeonBatch Size Bottom-Left coordinates.
     */
    private static void drawRectPix(NeonBatch batch, float size, float px, float py, float w, float h, Color color) {
        float base = 256f;
        float u = px / base;
        float v = 1.0f - (py + h) / base; // Flip Y
        float uw = w / base;
        float vh = h / base;

        batch.drawRect(u * size, v * size, uw * size, vh * size, 0, 0, color, true);
    }

    private static void drawCirclePix(NeonBatch batch, float size, float px, float py, float r, Color color) {
        float base = 256f;
        float u = px / base;
        float v = 1.0f - (py) / base; // Circle center. Y is center.
        // Wait, for circle, py is center Y.
        // In Pixmap (Top-Left), larger Y is lower.
        // In GL (Bottom-Left), larger Y is higher.
        // v = 1.0 - (py / base).
        
        float ur = r / base;
        
        batch.drawCircle(u * size, v * size, ur * size, 0, color, 32, true);
    }

    private static void drawLinePix(NeonBatch batch, float size, float x1, float y1, float x2, float y2, float width, Color color) {
        float base = 256f;
        float u1 = x1 / base;
        float v1 = 1.0f - y1 / base;
        float u2 = x2 / base;
        float v2 = 1.0f - y2 / base;
        
        // Width scaling?
        float w = (width / base) * size;

        batch.drawLine(u1 * size, v1 * size, u2 * size, v2 * size, w, color);
    }

    private static void drawPolygonPix(NeonBatch batch, float size, float[] vertices, Color color) {
        float base = 256f;
        float[] mapped = new float[vertices.length];
        
        for(int i=0; i<vertices.length; i+=2) {
            float px = vertices[i];
            float py = vertices[i+1];
            
            float u = px / base;
            float v = 1.0f - py / base;
            
            mapped[i] = u * size;
            mapped[i+1] = v * size;
        }
        
        batch.drawPolygon(mapped, vertices.length / 2, 0, color, true);
    }
}
