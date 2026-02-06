package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class SpriteGenerator {
    // High resolution texture size
    private static final int TEX_SIZE = 256;

    private static Pixmap createPixmap() {
        Pixmap p = new Pixmap(TEX_SIZE, TEX_SIZE, Pixmap.Format.RGBA8888);
        p.setColor(0, 0, 0, 0);
        p.fill();
        return p;
    }

    private static Texture toTexture(Pixmap pixmap) {
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // --- Helper Drawing Methods ---

    private static void drawRect(Pixmap p, int x, int y, int w, int h, Color c) {
        p.setColor(c);
        p.fillRectangle(x, y, w, h);
    }

    private static void drawCircle(Pixmap p, int x, int y, int r, Color c) {
        p.setColor(c);
        p.fillCircle(x, y, r);
    }
    
    // Draw a gradient circle (simulated by concentric circles)
    private static void drawGradientCircle(Pixmap p, int x, int y, int r, Color centerColor, Color edgeColor) {
        for (int i = r; i > 0; i--) {
            float t = (float) i / r;
            p.setColor(edgeColor.cpy().lerp(centerColor, 1 - t));
            p.fillCircle(x, y, i);
        }
    }

    // Draw a diagonal line with thickness
    private static void drawLine(Pixmap p, int x1, int y1, int x2, int y2, int thickness, Color c) {
        p.setColor(c);
        // Simple implementation: draw multiple lines
        for(int i = -thickness/2; i <= thickness/2; i++) {
            p.drawLine(x1 + i, y1, x2 + i, y2);
            p.drawLine(x1, y1 + i, x2, y2 + i);
        }
    }
    
    // Add noise to the whole pixmap or a region
    private static void applyNoise(Pixmap p, float intensity) {
        // This is slow for 256x256 in Java loop, but acceptable for loading screen
        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                if (Math.random() < 0.3) {
                    int pixel = p.getPixel(x, y);
                    Color c = new Color(pixel);
                    float factor = 1.0f + (float)((Math.random() - 0.5) * intensity);
                    c.mul(factor);
                    p.setColor(c);
                    p.drawPixel(x, y);
                }
            }
        }
    }

    // --- Tile Generators ---

    public static Texture createWall() {
        Pixmap p = createPixmap();
        
        // Background (Mortar)
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#2a2a2a"));

        // Bricks
        int rows = 4;
        int cols = 4;
        int brickH = TEX_SIZE / rows;
        int brickW = TEX_SIZE / cols;
        int gap = 4;

        Color brickColor = Color.valueOf("#555555");
        Color highlight = Color.valueOf("#666666");
        Color shadow = Color.valueOf("#444444");

        for (int row = 0; row < rows; row++) {
            int offset = (row % 2 == 0) ? 0 : brickW / 2;
            for (int col = -1; col <= cols; col++) {
                int x = col * brickW + offset + gap;
                int y = row * brickH + gap;
                int w = brickW - gap * 2;
                int h = brickH - gap * 2;

                // Main Brick
                drawRect(p, x, y, w, h, brickColor);
                
                // Bevel (Highlight Top/Left)
                drawRect(p, x, y, w, 4, highlight);
                drawRect(p, x, y, 4, h, highlight);
                
                // Bevel (Shadow Bottom/Right)
                drawRect(p, x, y + h - 4, w, 4, shadow);
                drawRect(p, x + w - 4, y, 4, h, shadow);
            }
        }
        
        applyNoise(p, 0.1f);
        return toTexture(p);
    }

    public static Texture createFloor() {
        Pixmap p = createPixmap();
        
        // Base Stone
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#222222"));
        
        // Big tiles pattern (2x2)
        int size = TEX_SIZE / 2;
        p.setColor(Color.valueOf("#262626"));
        p.drawRectangle(0, 0, size, size);
        p.drawRectangle(size, size, size, size);
        p.drawRectangle(size, 0, size, size);
        p.drawRectangle(0, size, size, size);

        // Noise/Gravel
        p.setColor(Color.valueOf("#333333"));
        for (int i = 0; i < 200; i++) {
            int x = MathUtils.random(TEX_SIZE);
            int y = MathUtils.random(TEX_SIZE);
            int r = MathUtils.random(1, 3);
            p.fillCircle(x, y, r);
        }
        
        // Cracks
        p.setColor(Color.valueOf("#111111"));
        for (int i=0; i<5; i++) {
             int x1 = MathUtils.random(TEX_SIZE);
             int y1 = MathUtils.random(TEX_SIZE);
             int x2 = x1 + MathUtils.random(-30, 30);
             int y2 = y1 + MathUtils.random(-30, 30);
             p.drawLine(x1, y1, x2, y2);
        }

        return toTexture(p);
    }

    public static Texture createStairs() {
        Pixmap p = createPixmap();
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#111111"));
        
        int steps = 6;
        int stepH = TEX_SIZE / steps;
        
        for (int i = 0; i < steps; i++) {
            int y = i * stepH;
            // Step Top
            drawRect(p, 20, y, TEX_SIZE - 40, stepH - 10, Color.valueOf("#444444"));
            // Step Front (Shadow)
            drawRect(p, 20, y + stepH - 10, TEX_SIZE - 40, 10, Color.valueOf("#222222"));
        }
        return toTexture(p);
    }

    public static Texture createDoor() {
        Pixmap p = createPixmap();
        
        // Frame
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#3e2723"));
        drawRect(p, 20, 20, TEX_SIZE - 40, TEX_SIZE - 20, Color.valueOf("#5d4037")); // Inner Door
        
        // Wood Planks
        p.setColor(Color.valueOf("#4e342e"));
        for (int i=1; i<4; i++) {
            int x = 20 + i * ((TEX_SIZE - 40) / 4);
            p.fillRectangle(x, 20, 4, TEX_SIZE - 20);
        }
        
        // Knob
        drawCircle(p, 40, TEX_SIZE / 2, 12, Color.GOLD);
        drawCircle(p, 40, TEX_SIZE / 2, 8, Color.YELLOW);
        
        return toTexture(p);
    }

    // --- Character Generators ---

    public static Texture createPlayer() {
        Pixmap p = createPixmap();
        
        // Colors
        Color skin = Color.valueOf("#ffccaa");
        Color armor = Color.valueOf("#2196F3"); // Brighter Blue
        Color darkArmor = Color.valueOf("#1565C0");
        Color gold = Color.GOLD;
        Color helmet = Color.valueOf("#CFD8DC"); // Light Silver
        Color darkHelmet = Color.valueOf("#90A4AE");
        Color legs = Color.valueOf("#424242");
        Color boots = Color.valueOf("#3E2723");

        // 1. Legs (Centered)
        drawRect(p, 90, 180, 25, 60, legs);
        drawRect(p, 141, 180, 25, 60, legs);
        // Boots
        drawRect(p, 85, 240, 35, 16, boots);
        drawRect(p, 136, 240, 35, 16, boots);

        // 2. Body (Armor)
        // Main Chest
        drawRect(p, 70, 100, 116, 90, armor);
        // Shoulder Pads
        drawRect(p, 50, 90, 30, 40, darkArmor);
        drawRect(p, 176, 90, 30, 40, darkArmor);
        // Chest Plate Detail (Gold Trim)
        drawRect(p, 80, 110, 96, 70, darkArmor);
        drawRect(p, 118, 110, 20, 70, gold); // Center strip
        
        // Belt
        drawRect(p, 70, 180, 116, 15, Color.valueOf("#3e2723"));
        drawRect(p, 118, 180, 20, 15, Color.GOLD); // Buckle

        // 3. Head
        drawRect(p, 80, 30, 96, 80, skin);
        
        // 4. Helmet
        // Top Dome
        drawRect(p, 75, 10, 106, 40, helmet);
        // Visor / Sides
        drawRect(p, 70, 40, 20, 80, darkHelmet);
        drawRect(p, 166, 40, 20, 80, darkHelmet);
        // Center Crest (Red Plume?)
        drawRect(p, 123, 0, 10, 20, Color.RED);
        
        // 5. Face Details
        // Eyes (Anime style bigger eyes)
        drawRect(p, 100, 60, 12, 12, Color.BLACK);
        drawRect(p, 144, 60, 12, 12, Color.BLACK);
        drawRect(p, 102, 62, 4, 4, Color.WHITE);
        drawRect(p, 146, 62, 4, 4, Color.WHITE);
        
        return toTexture(p);
    }

    public static Texture createMonster(String type) {
        Pixmap p = createPixmap();
        
        if (type.equals("SLIME")) {
            // Slime Body
            p.setColor(Color.valueOf("#44aaff"));
            p.fillCircle(128, 140, 100);
            p.fillRectangle(28, 140, 200, 80);
            
            // Highlight (Glossy)
            p.setColor(Color.valueOf("#88ccff"));
            p.fillCircle(160, 100, 30);
            
            // Eyes
            drawCircle(p, 90, 130, 20, Color.WHITE);
            drawCircle(p, 166, 130, 20, Color.WHITE);
            drawCircle(p, 90, 130, 8, Color.BLACK);
            drawCircle(p, 166, 130, 8, Color.BLACK);
            
        } else if (type.equals("SKELETON")) {
            // Skull
            drawRect(p, 80, 40, 96, 80, Color.valueOf("#eeeeee"));
            // Jaw
            drawRect(p, 90, 120, 76, 30, Color.valueOf("#dddddd"));
            // Eyes
            drawRect(p, 90, 60, 25, 25, Color.BLACK);
            drawRect(p, 141, 60, 25, 25, Color.BLACK);
            // Ribs
            drawRect(p, 100, 160, 56, 60, Color.valueOf("#dddddd"));
            for(int i=0; i<3; i++) {
                drawRect(p, 80, 160 + i*20, 96, 10, Color.valueOf("#eeeeee"));
            }
            // Legs (Fix: Higher up to avoid cut-off)
            drawRect(p, 90, 220, 15, 30, Color.valueOf("#dddddd"));
            drawRect(p, 151, 220, 15, 30, Color.valueOf("#dddddd"));
            // Feet
            drawRect(p, 85, 250, 25, 6, Color.valueOf("#aaaaaa"));
            drawRect(p, 146, 250, 25, 6, Color.valueOf("#aaaaaa"));
            
        } else if (type.equals("ORC")) {
            // Body
            drawRect(p, 50, 50, 156, 180, Color.valueOf("#448822"));
            // Leather Armor
            drawRect(p, 60, 180, 136, 40, Color.valueOf("#5d4037")); // Belt
            drawRect(p, 70, 60, 116, 100, Color.valueOf("#795548")); // Chest
            // Tusks
            drawRect(p, 80, 140, 15, 40, Color.WHITE);
            drawRect(p, 161, 140, 15, 40, Color.WHITE);
            // Eyes (Angry)
            drawRect(p, 80, 80, 30, 10, Color.RED);
            drawRect(p, 146, 80, 30, 10, Color.RED);
            
        } else if (type.equals("BAT")) {
            // Wings
            p.setColor(Color.DARK_GRAY);
            p.fillTriangle(128, 128, 20, 40, 50, 180);
            p.fillTriangle(128, 128, 236, 40, 206, 180);
            // Body
            drawCircle(p, 128, 128, 40, Color.BLACK);
            // Eyes
            drawCircle(p, 118, 120, 8, Color.YELLOW);
            drawCircle(p, 138, 120, 8, Color.YELLOW);

        } else if (type.equals("BOSS")) {
            // Dragon Head
            p.setColor(Color.valueOf("#aa0000"));
            p.fillRectangle(60, 40, 136, 120);
            // Snout
            p.fillRectangle(80, 160, 96, 60);
            // Eyes
            drawRect(p, 70, 70, 30, 20, Color.GREEN);
            drawRect(p, 156, 70, 30, 20, Color.GREEN);
            // Horns
            p.setColor(Color.YELLOW);
            p.fillTriangle(70, 40, 50, 10, 90, 40);
            p.fillTriangle(186, 40, 166, 40, 206, 10);
        }
        
        return toTexture(p);
    }

    public static Texture createItem(String name) {
        Pixmap p = createPixmap();
        
        if (name.contains("Potion") || name.contains("Elixir") || name.contains("药水") || name.contains("万能药")) {
            Color liquid = Color.RED;
            if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
            if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;
            
            // Flask Body (Bottom Circle) - Gradient
            drawGradientCircle(p, 128, 170, 70, liquid, liquid.cpy().mul(0.6f));
            
            // Neck
            drawRect(p, 108, 60, 40, 110, liquid.cpy().mul(0.8f));
            // Rim
            drawRect(p, 100, 50, 56, 15, Color.valueOf("#cccccc"));
            // Cork
            drawRect(p, 113, 30, 30, 20, Color.valueOf("#8d6e63"));
            
            // Bubbles
            p.setColor(new Color(1f, 1f, 1f, 0.3f));
            for(int i=0; i<5; i++) {
                int bx = 100 + (int)(Math.random()*56);
                int by = 130 + (int)(Math.random()*80);
                p.fillCircle(bx, by, 3 + (int)(Math.random()*4));
            }
            
            // Highlight / Reflection
            p.setColor(new Color(1f, 1f, 1f, 0.4f));
            p.fillRectangle(140, 70, 10, 80);
            p.fillCircle(150, 150, 10);
            
        } else if (name.contains("Sword") || name.contains("Blade") || name.contains("剑") || name.contains("刃")) {
            // Blade Color
            Color bladeColor = Color.LIGHT_GRAY;
            if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
            if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");
            
            // Diagonal Sword (Bottom Left to Top Right)
            // Handle
            drawLine(p, 60, 200, 90, 170, 10, Color.valueOf("#3e2723"));
            drawCircle(p, 55, 205, 8, Color.GOLD); // Pommel
            // Guard
            drawLine(p, 70, 190, 110, 150, 6, Color.valueOf("#5d4037")); // Cross part logic simplified
            drawLine(p, 75, 155, 105, 185, 14, Color.valueOf("#5d4037")); // Actual Guard
            
            // Blade
            drawLine(p, 90, 170, 200, 60, 20, bladeColor);
            // Tip
            // Simple triangle tip logic is hard with diagonals, just extend line and taper?
            // Let's just draw a thinner line at end
            drawLine(p, 200, 60, 210, 50, 10, bladeColor);
            
            // Blood Groove (Fuller)
            drawLine(p, 95, 165, 195, 65, 4, bladeColor.cpy().mul(0.7f));

        } else if (name.contains("Shield") || name.contains("盾")) {
            Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
            Color border = Color.DARK_GRAY;
            
            // Classic U-Shape Shield
            // Main body
            p.setColor(c);
            p.fillRectangle(50, 50, 156, 120); // Top part
            p.fillCircle(128, 170, 78); // Bottom curve approximation
            
            // Border
            p.setColor(border);
            // Draw lines for border (simplified)
            drawRect(p, 45, 45, 166, 10, border); // Top rim
            drawRect(p, 45, 45, 10, 120, border); // Left
            drawRect(p, 201, 45, 10, 120, border); // Right
            
            // Center Boss
            drawGradientCircle(p, 128, 120, 30, border, c);

        } else if (name.contains("Axe") || name.contains("斧")) {
             // Handle
             drawRect(p, 120, 40, 16, 180, Color.valueOf("#5d4037"));
             // Blades (Double bit)
             p.setColor(Color.LIGHT_GRAY);
             p.fillTriangle(120, 60, 60, 40, 60, 120); // Left
             p.fillTriangle(136, 60, 196, 40, 196, 120); // Right
             
        } else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
             // Staff
             drawRect(p, 124, 40, 8, 200, Color.valueOf("#5d4037"));
             // Gem
             drawGradientCircle(p, 128, 40, 20, Color.MAGENTA, Color.PURPLE);
             // Glow
             p.setColor(new Color(1f, 0f, 1f, 0.3f));
             p.fillCircle(128, 40, 30);
             
        } else if (name.contains("Scroll") || name.contains("卷轴")) {
             // Paper
             drawRect(p, 60, 60, 136, 136, Color.valueOf("#fff9c4"));
             // Rolled parts
             drawGradientCircle(p, 128, 60, 20, Color.valueOf("#fff9c4"), Color.valueOf("#cbc26d")); // Top roll
             drawGradientCircle(p, 128, 196, 20, Color.valueOf("#fff9c4"), Color.valueOf("#cbc26d")); // Bottom roll
             // Runes
             p.setColor(Color.BLACK);
             drawLine(p, 80, 100, 100, 100, 4, Color.BLACK);
             drawLine(p, 110, 100, 130, 120, 4, Color.BLACK);
             
        } else if (name.contains("Ring") || name.contains("戒指")) {
             // Band
             drawGradientCircle(p, 128, 128, 60, Color.GOLD, Color.YELLOW);
             drawCircle(p, 128, 128, 50, new Color(0,0,0,0)); // Hole (Transparent) - Actually fill with clear?
             // Pixmap doesn't support clear rect easily without blending mode.
             // Re-draw center transparent? No, just draw ring as thick circle if possible.
             // Let's just draw a filled circle and a smaller inner circle with background color?
             // No background color known.
             // Simple approach: Draw Gold Circle, then smaller Transparent Circle? 
             // Pixmap.drawCircle is outline. fillCircle is fill.
             // We need a donut.
             p.setBlending(Pixmap.Blending.None);
             p.setColor(0,0,0,0);
             p.fillCircle(128, 128, 45);
             p.setBlending(Pixmap.Blending.SourceOver);
             
             // Gem
             drawGradientCircle(p, 128, 68, 15, Color.RED, Color.FIREBRICK);
             
        } else if (name.contains("Coin") || name.contains("金币")) {
            drawGradientCircle(p, 128, 128, 80, Color.GOLD, Color.ORANGE);
            drawCircle(p, 128, 128, 60, Color.YELLOW);
            p.setColor(Color.ORANGE);
            // $ Sign
            drawRect(p, 124, 80, 8, 96, Color.ORANGE);
            
        } else if (name.contains("Armor") || name.contains("Mail") || name.contains("甲")) {
            Color ac = name.contains("Leather") || name.contains("皮") ? Color.valueOf("#8d6e63") : Color.LIGHT_GRAY;
            // Chest piece
            drawRect(p, 64, 64, 128, 128, ac);
            // Shoulders
            drawRect(p, 44, 64, 40, 40, ac.cpy().mul(0.8f));
            drawRect(p, 172, 64, 40, 40, ac.cpy().mul(0.8f));
            
        } else {
            // Default Box
            drawRect(p, 64, 64, 128, 128, Color.MAGENTA);
            drawRect(p, 80, 80, 96, 96, Color.valueOf("#aa00aa"));
            // Question Mark
            drawRect(p, 120, 100, 16, 40, Color.WHITE);
            drawRect(p, 120, 150, 16, 16, Color.WHITE);
        }

        return toTexture(p);
    }
}
