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
        Color armor = Color.valueOf("#3366cc");
        Color darkArmor = Color.valueOf("#224488");
        Color helmet = Color.valueOf("#aaaaaa");
        Color legs = Color.valueOf("#333333");
        
        // Legs
        drawRect(p, 80, 180, 40, 76, legs);
        drawRect(p, 136, 180, 40, 76, legs);
        
        // Body (Armor)
        drawRect(p, 60, 90, 136, 100, armor);
        // Chest Plate Detail
        drawRect(p, 80, 100, 96, 60, darkArmor);
        
        // Head
        drawRect(p, 70, 20, 116, 80, skin);
        
        // Helmet
        drawRect(p, 65, 10, 126, 40, helmet); // Top
        drawRect(p, 65, 10, 20, 90, helmet); // Side L
        drawRect(p, 171, 10, 20, 90, helmet); // Side R
        
        // Eyes
        drawRect(p, 90, 50, 15, 15, Color.BLACK);
        drawRect(p, 151, 50, 15, 15, Color.BLACK);
        // Pupils
        drawRect(p, 98, 52, 5, 5, Color.WHITE);
        drawRect(p, 159, 52, 5, 5, Color.WHITE);

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
            drawRect(p, 100, 160, 56, 80, Color.valueOf("#dddddd"));
            for(int i=0; i<4; i++) {
                drawRect(p, 80, 160 + i*20, 96, 10, Color.valueOf("#eeeeee"));
            }
            
        } else if (type.equals("ORC")) {
            // Body
            drawRect(p, 50, 50, 156, 180, Color.valueOf("#448822"));
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
        
        if (name.contains("Potion") || name.contains("Elixir")) {
            Color liquid = Color.RED;
            if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
            if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;
            
            // Flask Body (Bottom Circle)
            drawCircle(p, 128, 170, 70, liquid);
            // Neck
            drawRect(p, 108, 60, 40, 110, liquid);
            // Rim
            drawRect(p, 100, 50, 56, 15, Color.valueOf("#cccccc"));
            // Cork
            drawRect(p, 113, 30, 30, 20, Color.valueOf("#8d6e63"));
            
            // Highlight / Reflection
            p.setColor(new Color(1f, 1f, 1f, 0.4f));
            p.fillRectangle(140, 70, 10, 80);
            p.fillCircle(150, 150, 10);
            
        } else if (name.contains("Sword") || name.contains("Blade")) {
            // Blade
            Color bladeColor = Color.LIGHT_GRAY;
            if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
            if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");
            
            // Diagonal Blade is hard with rects, let's draw straight for simplicity or use lines
            // Let's draw vertical sword
            drawRect(p, 118, 20, 20, 180, bladeColor);
            // Point
            p.setColor(bladeColor);
            p.fillTriangle(118, 20, 138, 20, 128, 5);
            
            // Crossguard
            drawRect(p, 88, 200, 80, 10, Color.valueOf("#5d4037"));
            // Handle
            drawRect(p, 123, 210, 10, 40, Color.valueOf("#3e2723"));
            // Pommel
            drawCircle(p, 128, 250, 8, Color.GOLD);

        } else if (name.contains("Shield") || name.contains("盾")) {
            Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
            p.setColor(c);
            p.fillRectangle(50, 50, 156, 180);
            p.setColor(Color.DARK_GRAY);
            p.drawRectangle(50, 50, 156, 180);
            p.drawRectangle(60, 60, 136, 160);
            
        } else if (name.contains("Coin") || name.contains("金币")) {
            drawCircle(p, 128, 128, 80, Color.GOLD);
            drawCircle(p, 128, 128, 60, Color.YELLOW);
            p.setColor(Color.ORANGE);
            p.fillRectangle(110, 80, 36, 96); // "$" ish or just a bar
            
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
