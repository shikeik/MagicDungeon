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
        
        // Base Stone - Darker
        drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#222222"));
        
        // Big tiles pattern (2x2)
        int size = TEX_SIZE / 2;
        p.setColor(Color.valueOf("#2a2a2a"));
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

    public static Texture createStairs(boolean up) {
        Pixmap p = createPixmap();
        
        // Unify Color: Stone Grey Style for both
        Color stairColor = Color.valueOf("#555555");
        Color stairHighlight = Color.valueOf("#777777");
        Color stairShadow = Color.valueOf("#333333");
        
        // Max Width: 2/3 of TEX_SIZE (approx 170px)
        int maxWidth = (int)(TEX_SIZE * 0.66f);
        
        if (!up) {
            // STAIRS DOWN: Trapezoid Perspective
            // Background: Dark Floor
            drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#222222"));
            
            // Draw stairs from Top (Light) to Bottom (Dark)
            int steps = 8;
            for(int i=0; i<steps; i++) {
                // Interpolate color for depth
                float t = (float)i / (steps-1);
                Color c = stairColor.cpy().lerp(Color.BLACK, t * 0.8f);
                
                // Width decreases from maxWidth to ~80
                int topW = maxWidth;
                int botW = 80;
                int currentW = (int)(topW - (topW - botW) * t);
                int h = 30 - i * 2;
                
                int x = (TEX_SIZE - currentW) / 2;
                int y = i * 35 + 10; // Start a bit lower
                
                drawRect(p, x, y, currentW, h, c);
                // Highlight edge
                drawRect(p, x, y, currentW, 2, c.cpy().add(0.1f, 0.1f, 0.1f, 0));
            }
            
        } else {
            // STAIRS UP: Pyramid shape
            // Background: Transparent (or simple floor)
            
            int steps = 6;
            for(int i=0; i<steps; i++) {
                // Width increases from small to maxWidth
                int topW = 60;
                int botW = maxWidth;
                // i=0 is top, i=steps-1 is bottom
                float t = (float)i / (steps-1);
                int width = (int)(topW + (botW - topW) * t);
                
                int height = 30;
                int x = (TEX_SIZE - width) / 2;
                int y = 40 + i * 30;
                
                // Step top
                drawRect(p, x, y, width, height, stairColor);
                // Step front highlight
                drawRect(p, x, y, width, 5, stairHighlight);
                // Shadow sides
                drawRect(p, x, y, 5, height, stairShadow);
                drawRect(p, x + width - 5, y, 5, height, stairShadow);
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
        Color legs = Color.valueOf("#8d6e63"); // Brown Pants
        Color boots = Color.valueOf("#3E2723");

        // 1. Legs (Centered)
        int startY = 30; // Shift down
        
        // Legs
        drawRect(p, 90, 180, 25, 60, legs);
        drawRect(p, 141, 180, 25, 60, legs);
        
        // Boots (Bigger, Taller, Wider, Separated)
        // Center 128. Gap 10. Width 65. Height 45.
        // Left Boot: 128 - 10 - 65 = 53
        // Right Boot: 128 + 10 = 138
        int bootW = 65;
        int bootH = 45;
        int bootY = 215;
        
        // Left Boot
        drawRect(p, 53, bootY, bootW, bootH, boots);
        // Right Boot
        drawRect(p, 138, bootY, bootW, bootH, boots);
        
        // Boot Detail (Texture/Laces)
        Color bootLight = boots.cpy().mul(1.2f);
        // Soles
        drawRect(p, 53, bootY + bootH - 10, bootW, 10, Color.BLACK);
        drawRect(p, 138, bootY + bootH - 10, bootW, 10, Color.BLACK);
        // Highlights
        drawRect(p, 53 + 5, bootY + 5, 10, 20, bootLight);
        drawRect(p, 138 + 5, bootY + 5, 10, 20, bootLight);

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
            // Fix: Extend rectangle width by 1 pixel to match circle edge perfectly (28+201=229 vs 128+100=228?)
            // Actually circle boundary calculation might be slightly different. Let's add overlap.
            p.fillRectangle(28, 140, 202, 80); 
            
            // Highlight (Glossy)
            p.setColor(Color.valueOf("#88ccff"));
            p.fillCircle(160, 100, 30);
            
            // Eyes
            drawCircle(p, 90, 130, 20, Color.WHITE);
            drawCircle(p, 166, 130, 20, Color.WHITE);
            drawCircle(p, 90, 130, 8, Color.BLACK);
            drawCircle(p, 166, 130, 8, Color.BLACK);
            
        } else if (type.equals("SKELETON")) {
            // Skull (Smaller)
            int headW = 70;
            int headH = 60;
            int headX = 128 - headW/2;
            int headY = 40;
            drawRect(p, headX, headY, headW, headH, Color.valueOf("#eeeeee"));
            
            // Jaw
            drawRect(p, headX + 10, headY + headH, headW - 20, 20, Color.valueOf("#dddddd"));
            
            // Eyes
            drawRect(p, headX + 10, headY + 20, 18, 18, Color.BLACK);
            drawRect(p, headX + headW - 28, headY + 20, 18, 18, Color.BLACK);
            
            // Ribs
            drawRect(p, 100, 130, 56, 60, Color.valueOf("#dddddd"));
            for(int i=0; i<3; i++) {
                drawRect(p, 80, 130 + i*20, 96, 10, Color.valueOf("#eeeeee"));
            }
            // Legs
            drawRect(p, 90, 200, 15, 30, Color.valueOf("#dddddd"));
            drawRect(p, 151, 200, 15, 30, Color.valueOf("#dddddd"));
            
            // Feet (Bigger - match player)
            drawRect(p, 60, 230, 50, 25, Color.valueOf("#aaaaaa")); // Left
            drawRect(p, 146, 230, 50, 25, Color.valueOf("#aaaaaa")); // Right
            
        } else if (type.equals("ORC")) {
            // Body (Improved Color)
            Color skin = Color.valueOf("#558b2f"); // Olive Green
            drawRect(p, 50, 50, 156, 180, skin);
            
            // Armor (Improved)
            Color armor = Color.valueOf("#3e2723"); // Dark Leather
            Color metal = Color.GRAY;
            
            // Chest
            drawRect(p, 60, 60, 136, 100, armor);
            // Metal Plates
            drawRect(p, 70, 70, 50, 80, metal);
            drawRect(p, 136, 70, 50, 80, metal);
            
            // Belt
            drawRect(p, 50, 170, 156, 30, Color.valueOf("#212121"));
            
            // Tusks
            drawRect(p, 80, 140, 15, 40, Color.valueOf("#fffde7"));
            drawRect(p, 161, 140, 15, 40, Color.valueOf("#fffde7"));
            
            // Eyes (Angry)
            drawRect(p, 80, 80, 30, 10, Color.RED);
            drawRect(p, 146, 80, 30, 10, Color.RED);
            
        } else if (type.equals("BAT")) {
            // Wings (Dark Purple/Grey)
            p.setColor(Color.valueOf("#424242"));
            p.fillTriangle(128, 128, 20, 40, 50, 180);
            p.fillTriangle(128, 128, 236, 40, 206, 180);
            
            // Wing membranes
            p.setColor(Color.valueOf("#616161"));
            p.fillTriangle(128, 128, 30, 50, 60, 170);
            p.fillTriangle(128, 128, 226, 50, 196, 170);

            // Body (Lighter than wings)
            drawCircle(p, 128, 128, 40, Color.valueOf("#757575"));
            
            // Eyes
            drawCircle(p, 118, 120, 8, Color.YELLOW);
            drawCircle(p, 138, 120, 8, Color.YELLOW);
            drawCircle(p, 118, 120, 3, Color.RED); // Pupil
            drawCircle(p, 138, 120, 3, Color.RED);

        } else if (type.equals("BOSS")) {
            // DRAGON (Magnificent)
            
            // Wings (Big, back)
            p.setColor(Color.valueOf("#b71c1c")); // Dark Red
            p.fillTriangle(128, 100, 10, 10, 80, 150);
            p.fillTriangle(128, 100, 246, 10, 176, 150);
            
            // Body (Serpentine/Scaly)
            Color bodyColor = Color.valueOf("#d32f2f");
            Color scaleColor = Color.valueOf("#f44336");
            
            // Main torso
            drawRect(p, 80, 80, 96, 140, bodyColor);
            
            // Scales
            p.setColor(scaleColor);
            for(int i=0; i<5; i++) {
                for(int j=0; j<3; j++) {
                    p.fillCircle(96 + j*30, 90 + i*25, 10);
                }
            }
            
            // Belly (Yellowish)
            drawRect(p, 100, 80, 56, 140, Color.valueOf("#ffb300"));
            // Belly lines
            p.setColor(Color.valueOf("#e65100"));
            for(int i=0; i<6; i++) {
                drawRect(p, 100, 90 + i*20, 56, 4, Color.valueOf("#e65100"));
            }

            // Head
            int headY = 40;
            p.setColor(bodyColor);
            p.fillRectangle(70, headY, 116, 80);
            // Snout
            p.fillRectangle(90, headY + 60, 76, 40);
            
            // Horns (Large)
            p.setColor(Color.GOLD);
            p.fillTriangle(80, headY, 60, headY - 40, 100, headY);
            p.fillTriangle(176, headY, 196, headY - 40, 156, headY);
            
            // Eyes (Glowing)
            drawRect(p, 80, headY + 20, 20, 15, Color.GREEN);
            drawRect(p, 156, headY + 20, 20, 15, Color.GREEN);
            // Vertical Pupil
            drawRect(p, 88, headY + 20, 4, 15, Color.BLACK);
            drawRect(p, 164, headY + 20, 4, 15, Color.BLACK);
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
            
            // Broadsword (Wide)
            // Handle
            drawLine(p, 60, 200, 85, 175, 16, Color.valueOf("#3e2723")); 
            drawCircle(p, 55, 205, 12, Color.GOLD); 
            
            // Guard
            drawLine(p, 65, 155, 105, 195, 20, Color.valueOf("#5d4037"));
            // Guard Ornaments
            drawCircle(p, 65, 155, 10, Color.GOLD);
            drawCircle(p, 105, 195, 10, Color.GOLD);
            
            // Blade (Wider: 40px)
            int bladeWidth = 40; 
            drawLine(p, 90, 170, 210, 50, bladeWidth, bladeColor);
            
            // Blade Edge (Lighter)
            drawLine(p, 90, 170, 210, 50, bladeWidth - 8, bladeColor.cpy().mul(1.2f));
            
            // Blood Groove (Darker Center)
            drawLine(p, 95, 165, 205, 55, 6, bladeColor.cpy().mul(0.7f));
            
            // Tip
            p.setColor(bladeColor);
            // Draw a triangle at tip roughly
            // Since we can't draw rotated triangle easily, just use lines to taper
            drawLine(p, 210, 50, 225, 35, 20, bladeColor);
            drawLine(p, 210, 50, 230, 30, 10, bladeColor);

        } else if (name.contains("Shield") || name.contains("盾")) {
            boolean isIron = name.contains("Iron") || name.contains("铁");
            Color c = isIron ? Color.GRAY : Color.valueOf("#5d4037");
            Color border = isIron ? Color.LIGHT_GRAY : Color.valueOf("#8d6e63"); // Wood trim or Iron trim
            
            // Main body
            p.setColor(c);
            p.fillRectangle(50, 50, 156, 120); 
            p.fillCircle(128, 170, 78); 
            
            // Texture
            if (!isIron) {
                // Wood Grain
                p.setColor(Color.valueOf("#4e342e"));
                for(int i=1; i<5; i++) {
                    drawRect(p, 50 + i*30, 50, 4, 180, Color.valueOf("#3e2723"));
                }
            } else {
                // Metal Shine
                p.setColor(Color.WHITE);
                p.setBlending(Pixmap.Blending.SourceOver);
                drawLine(p, 60, 60, 100, 100, 10, new Color(1,1,1,0.2f));
            }
            
            // Border (Trim)
            // Use lines to simulate border
            int bThick = 10;
            // Top
            drawRect(p, 45, 45, 166, bThick, border);
            // Sides
            drawRect(p, 45, 45, bThick, 140, border);
            drawRect(p, 201, 45, bThick, 140, border);
            // Bottom Curve Border (Approximate with circles)
            // Hard to do precise curve stroke. Just draw dots?
            // Or draw a slightly larger circle behind? No, we are drawing on top.
            // Just some rivets at bottom
            drawCircle(p, 128, 230, 10, border);
            drawCircle(p, 80, 210, 10, border);
            drawCircle(p, 176, 210, 10, border);
            
            // Center Boss / Emblem
            drawGradientCircle(p, 128, 120, 30, border, c);
            // Rivets
            p.setColor(Color.GOLD);
            drawCircle(p, 60, 60, 6, Color.GOLD);
            drawCircle(p, 196, 60, 6, Color.GOLD);
            drawCircle(p, 60, 160, 6, Color.GOLD);
            drawCircle(p, 196, 160, 6, Color.GOLD);

        } else if (name.contains("Axe") || name.contains("斧")) {
             // Handle
             drawLine(p, 180, 40, 60, 200, 16, Color.valueOf("#3e2723"));
             
             // Axe Head (Iron Standard)
             // Use 2 Rectangles/Trapezoids to form double bit or single bit
             // Let's do double bit standard shape
             
             int cx = 165;
             int cy = 60;
             
             Color metal = Color.LIGHT_GRAY;
             Color edge = Color.WHITE;
             
             // Left Blade
             // Trapezoid: Narrow near handle, Wide at edge
             // Draw via lines of increasing width? Or multiple rects
             // Rect 1 (Near handle)
             drawRect(p, cx - 30, cy - 20, 30, 40, metal);
             // Rect 2 (Mid)
             drawRect(p, cx - 50, cy - 30, 20, 60, metal);
             // Rect 3 (Edge)
             drawRect(p, cx - 60, cy - 40, 10, 80, edge);
             
             // Right Blade
             drawRect(p, cx, cy - 20, 30, 40, metal);
             drawRect(p, cx + 30, cy - 30, 20, 60, metal);
             drawRect(p, cx + 50, cy - 40, 10, 80, edge);
             
             // Redraw handle top
             drawLine(p, 180, 40, 150, 80, 16, Color.valueOf("#3e2723"));
             
        } else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
             // Fancy Staff
             // Shaft
             drawLine(p, 80, 200, 180, 60, 12, Color.valueOf("#5d4037"));
             
             // Tassels (Wrapping around)
             p.setColor(Color.GOLD);
             for(int i=0; i<5; i++) {
                 int x = 100 + i*15;
                 int y = 170 - i*15;
                 drawCircle(p, x, y, 6, Color.RED); // Beads
             }
             
             // Magic Core (Pyramid/Diamond)
             // Center at 180, 60
             // Diamond shape
             p.setColor(Color.CYAN);
             p.fillTriangle(180, 40, 160, 60, 200, 60); // Top half
             p.fillTriangle(180, 80, 160, 60, 200, 60); // Bottom half
             
             // Glow
             p.setBlending(Pixmap.Blending.SourceOver);
             p.setColor(new Color(0f, 1f, 1f, 0.4f));
             p.fillCircle(180, 60, 40);
             
             // Prongs holding the gem
             drawLine(p, 170, 70, 160, 50, 4, Color.GOLD);
             drawLine(p, 190, 70, 200, 50, 4, Color.GOLD);
             
        } else if (name.contains("Scroll") || name.contains("卷轴") || name.contains("Book") || name.contains("书")) {
             // Open Book / Flipping Book
             // Left Page
             p.setColor(Color.valueOf("#fff9c4")); // Paper
             // Skewed rect for perspective?
             // Simple: Rect
             drawRect(p, 40, 80, 88, 100, Color.valueOf("#fff9c4"));
             // Right Page
             drawRect(p, 128, 80, 88, 100, Color.valueOf("#fff9c4"));
             
             // Spine
             drawRect(p, 126, 80, 4, 100, Color.valueOf("#5d4037"));
             
             // Text/Runes
             p.setColor(Color.BLACK);
             for(int i=0; i<6; i++) {
                 drawLine(p, 50, 90 + i*12, 110, 90 + i*12, 2, Color.BLACK);
                 drawLine(p, 140, 90 + i*12, 200, 90 + i*12, 2, Color.BLACK);
             }
             
             // Flipping Page (Top Right)
             p.setColor(Color.valueOf("#fff59d"));
             p.fillTriangle(216, 80, 180, 80, 216, 110);
             
             // Cover
             drawRect(p, 30, 70, 196, 10, Color.valueOf("#3e2723")); // Top cover edge (back)
             drawRect(p, 30, 180, 196, 10, Color.valueOf("#3e2723")); // Bottom cover edge
             
        } else if (name.contains("Ring") || name.contains("戒指")) {
             boolean isPower = name.contains("Power") || name.contains("力量");
             // Ring Band
             Color bandColor = isPower ? Color.GOLD : Color.SILVER;
             
             // Draw Donut
             p.setColor(bandColor);
             p.fillCircle(128, 128, 50);
             // Inner hole (Clear)
             p.setBlending(Pixmap.Blending.None);
             p.setColor(0,0,0,0);
             p.fillCircle(128, 128, 35);
             p.setBlending(Pixmap.Blending.SourceOver);
             
             // Gem / Setting
             if (isPower) {
                 // Red Gem, Square Cut
                 drawRect(p, 110, 60, 36, 36, Color.RED);
                 drawRect(p, 115, 65, 26, 26, Color.FIREBRICK);
                 // Claws
                 p.setColor(Color.GOLD);
                 drawCircle(p, 108, 58, 4, Color.GOLD);
                 drawCircle(p, 148, 58, 4, Color.GOLD);
                 drawCircle(p, 108, 98, 4, Color.GOLD);
                 drawCircle(p, 148, 98, 4, Color.GOLD);
             } else {
                 // Blue Gem, Oval Cut (Defense)
                 drawGradientCircle(p, 128, 78, 18, Color.CYAN, Color.BLUE);
                 // Silver Setting
                 drawRect(p, 120, 120, 16, 20, Color.SILVER); // Connection
             }
             
        } else if (name.contains("Coin") || name.contains("金币")) {
            // Multi-textured Gold Coin
            // Outer Rim
            drawGradientCircle(p, 128, 128, 80, Color.ORANGE, Color.GOLD);
            // Inner Recess
            drawGradientCircle(p, 128, 128, 60, Color.GOLD, Color.YELLOW);
            
            // Relief Profile (Head?)
            p.setColor(Color.ORANGE);
            p.fillCircle(128, 128, 30);
            
            // Shine
            p.setColor(new Color(1f, 1f, 1f, 0.6f));
            p.fillCircle(110, 110, 10);
            drawLine(p, 100, 100, 160, 160, 4, new Color(1,1,1,0.3f)); // Sparkle line
            
        } else if (name.contains("Armor") || name.contains("Mail") || name.contains("甲")) {
            boolean isLeather = name.contains("Leather") || name.contains("皮");
            Color base = isLeather ? Color.valueOf("#8d6e63") : Color.LIGHT_GRAY;
            Color trim = isLeather ? Color.valueOf("#5d4037") : Color.GRAY;
            
            // Chest piece
            drawRect(p, 64, 64, 128, 128, base);
            
            // Texture
            if (isLeather) {
                // Stitching / Panels
                drawRect(p, 64, 128, 128, 4, trim); // Horizontal seam
                drawRect(p, 126, 64, 4, 128, trim); // Vertical seam
                // Studs
                p.setColor(Color.GOLD);
                drawCircle(p, 80, 80, 4, Color.GOLD);
                drawCircle(p, 176, 80, 4, Color.GOLD);
                drawCircle(p, 80, 176, 4, Color.GOLD);
                drawCircle(p, 176, 176, 4, Color.GOLD);
            } else {
                // Metal Plates
                drawRect(p, 74, 74, 50, 50, Color.WHITE); // Highlight plate
                drawRect(p, 132, 74, 50, 50, Color.WHITE);
                drawRect(p, 74, 132, 50, 50, Color.WHITE);
                drawRect(p, 132, 132, 50, 50, Color.WHITE);
                // Darken base to show plates
                drawRect(p, 64, 64, 128, 128, trim);
                // Draw Plates
                drawRect(p, 70, 70, 50, 50, base);
                drawRect(p, 136, 70, 50, 50, base);
                drawRect(p, 70, 136, 50, 50, base);
                drawRect(p, 136, 136, 50, 50, base);
            }
            
            // Shoulders (Common)
            drawRect(p, 44, 64, 40, 40, trim);
            drawRect(p, 172, 64, 40, 40, trim);
            
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
