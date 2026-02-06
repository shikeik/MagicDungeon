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
        
        // Base Stone - Lighter
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#444444")); // Previously #222222
        
        // Big tiles pattern (2x2)
        int size = TEX_SIZE / 2;
        p.setColor(Color.valueOf("#4d4d4d"));
        p.drawRectangle(0, 0, size, size);
        p.drawRectangle(size, size, size, size);
        p.drawRectangle(size, 0, size, size);
        p.drawRectangle(0, size, size, size);

        // Noise/Gravel
        p.setColor(Color.valueOf("#555555"));
        for (int i = 0; i < 200; i++) {
            int x = MathUtils.random(TEX_SIZE);
            int y = MathUtils.random(TEX_SIZE);
            int r = MathUtils.random(1, 3);
            p.fillCircle(x, y, r);
        }
        
        // Cracks
        p.setColor(Color.valueOf("#333333"));
        for (int i=0; i<5; i++) {
             int x1 = MathUtils.random(TEX_SIZE);
             int y1 = MathUtils.random(TEX_SIZE);
             int x2 = x1 + MathUtils.random(-30, 30);
             int y2 = y1 + MathUtils.random(-30, 30);
             p.drawLine(x1, y1, x2, y2);
        }

        return toTexture(p);
    }

    public static Texture createStairs(boolean up) {
        Pixmap p = createPixmap();
        
        // No background for UP stairs as requested ("无背景")? 
        // Or usually stairs have floor background. Assuming transparent if "无背景" means just the stairs.
        // But for game logic, it usually sits on floor. Let's make it sit on floor color for DOWN, 
        // and maybe transparent/floor for UP. Let's stick to floor background for consistency unless transparent is strictly needed.
        // User said "无背景" for UP stairs.
        
        if (!up) {
            // STAIRS DOWN: Trapezoid Perspective (梯形)
            // Background Black
            drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.BLACK);
            
            // Draw stairs from Top (Light) to Bottom (Dark/Black)
            // Top is wide, Bottom is narrow
            
            int steps = 8;
            for(int i=0; i<steps; i++) {
                // Interpolate color from Grey to Black
                float t = (float)i / (steps-1);
                Color c = new Color(0.5f * (1-t), 0.5f * (1-t), 0.5f * (1-t), 1f);
                
                // Width decreases
                int topW = TEX_SIZE - 20;
                int botW = 100;
                int currentW = (int)(topW - (topW - botW) * t);
                int h = 30 - i * 2; // Height decreases slightly
                
                int x = (TEX_SIZE - currentW) / 2;
                int y = i * 35; // Position
                
                drawRect(p, x, y, currentW, h, c);
            }
            
        } else {
            // STAIRS UP: Top Small, Bottom Large (Perspective going up)
            // "无背景" -> Transparent background
            // "阶梯上小下大" -> Pyramid shape going up
            
            // Steps
            int steps = 6;
            for(int i=0; i<steps; i++) {
                int width = 60 + i * 30; // Top is small (60), Bottom is large
                int height = 30;
                int x = (TEX_SIZE - width) / 2;
                int y = 40 + i * 30; // Start from top-ish
                
                // Step top
                drawRect(p, x, y, width, height, Color.valueOf("#8d6e63")); // Wood/Stone
                // Step front highlight
                drawRect(p, x, y, width, 5, Color.valueOf("#a1887f"));
            }
        }
        
        return toTexture(p);
    }
    
    // For compatibility if needed, though we should update calls
    public static Texture createStairs() {
        return createStairs(false); // Default to down
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
        Color legs = Color.valueOf("#8d6e63"); // Brown Pants (Contrast with floor)
        Color boots = Color.valueOf("#3E2723");

        // 1. Legs (Centered)
        // User asked for big shoes.
        
        int startY = 30; // Shift down
        
        // Legs
        drawRect(p, 90, 180, 25, 60, legs);
        drawRect(p, 141, 180, 25, 60, legs);
        
        // Boots (Big Shoes - Even Bigger)
        drawRect(p, 75, 230, 55, 30, boots); // Bigger Width 55
        drawRect(p, 126, 230, 55, 30, boots);

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

        // 3. Head (Smaller)
        // Old: 96x80. New: 76x64.
        int headW = 76;
        int headH = 64;
        int headX = 128 - headW/2;
        int headY = 36;
        
        drawRect(p, headX, headY, headW, headH, skin);
        
        // 4. Helmet (Smaller)
        // Top Dome
        drawRect(p, headX - 5, headY - 10, headW + 10, 30, helmet);
        // Visor / Sides
        drawRect(p, headX - 5, headY + 10, 15, headH, darkHelmet);
        drawRect(p, headX + headW - 10, headY + 10, 15, headH, darkHelmet);
        // Center Crest
        drawRect(p, 128 - 5, headY - 20, 10, 20, Color.RED);
        
        // 5. Face Details
        // Eyes
        int eyeY = headY + 30;
        drawRect(p, 128 - 20, eyeY, 12, 12, Color.BLACK);
        drawRect(p, 128 + 8, eyeY, 12, 12, Color.BLACK);
        drawRect(p, 128 - 18, eyeY + 2, 4, 4, Color.WHITE);
        drawRect(p, 128 + 10, eyeY + 2, 4, 4, Color.WHITE);
        
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
            
            // Diagonal Sword (Broadsword, thicker)
            // Handle (Bottom Left)
            drawLine(p, 60, 200, 85, 175, 16, Color.valueOf("#3e2723")); // Thicker Handle
            drawCircle(p, 55, 205, 12, Color.GOLD); // Bigger Pommel
            
            // Guard (Perpendicular to blade)
            // Blade direction vector: (1, -1). Perpendicular: (1, 1).
            // Center around (85, 175)
            drawLine(p, 65, 155, 105, 195, 20, Color.valueOf("#5d4037")); // Thick Guard
            
            // Blade (Wide)
            int bladeWidth = 24; // Thicker
            drawLine(p, 90, 170, 210, 50, bladeWidth, bladeColor);
            
            // Tip (Triangle)
            // Hard to draw rotated triangle with current tools. 
            // Just draw a line that tapers? Or manually draw lines to form tip.
            // Let's draw a few lines of decreasing length at tip
            drawLine(p, 210, 50, 220, 40, 10, bladeColor);
            
            // Blood Groove (Fuller)
            drawLine(p, 95, 165, 205, 55, 6, bladeColor.cpy().mul(0.7f)); // Darker center line

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
             // Handle (Thick) - Diagonal
             // Top Right to Bottom Left
             drawLine(p, 180, 40, 60, 200, 16, Color.valueOf("#5d4037"));
             
             // Head Position around (160, 60)
             // Double Bit Moon Shape
             p.setColor(Color.LIGHT_GRAY);
             // Left Blade (Crescent)
             // Draw circle and cut out? Or lines.
             // Simple: 2 Triangles but curved?
             // Let's use circles for curve
             p.fillCircle(140, 80, 50); // Left big circle
             p.fillCircle(180, 40, 50); // Right big circle
             
             // Cutout to make it crescent/axe-like attached to handle?
             // This is tricky with simple primitives.
             // Let's use rects/triangles but better placed.
             
             // Re-draw handle on top later
             
             // Left Blade
             p.setColor(Color.LIGHT_GRAY);
             // Top part
             p.fillTriangle(160, 60, 100, 20, 130, 100);
             // Bottom part
             p.fillTriangle(160, 60, 130, 100, 200, 100); // This is messy.
             
             // Let's try simple "Butterfly" shape for axe head at (160, 60)
             p.setColor(Color.LIGHT_GRAY);
             // Left wing
             p.fillTriangle(160, 60, 110, 20, 110, 100); 
             // Right wing
             p.fillTriangle(160, 60, 210, 20, 210, 100);
             
             // Add curve illusion by drawing smaller dark circles?
             p.setColor(new Color(0,0,0,0)); // Transparent? No.
             // Just stick to geometric butterfly for now, better than triangle.
             
             // Redraw handle
             drawLine(p, 180, 40, 60, 200, 16, Color.valueOf("#5d4037"));
             
        } else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
             // Staff - Diagonal
             drawLine(p, 80, 200, 180, 60, 12, Color.valueOf("#5d4037"));
             
             // Head Gem
             drawGradientCircle(p, 180, 60, 25, Color.MAGENTA, Color.PURPLE);
             // Ornaments (Gold Rings)
             p.setColor(Color.GOLD);
             p.setBlending(Pixmap.Blending.None); // Overwrite
             // Ring around gem?
             // Just some dots
             drawCircle(p, 180, 60, 30, new Color(1, 0.84f, 0, 0.5f)); // Halo
             
             // Glow
             p.setBlending(Pixmap.Blending.SourceOver);
             p.setColor(new Color(1f, 0f, 1f, 0.4f));
             p.fillCircle(180, 60, 40);
             
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
