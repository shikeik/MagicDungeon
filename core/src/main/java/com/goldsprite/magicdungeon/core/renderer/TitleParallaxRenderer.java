package com.goldsprite.magicdungeon.core.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class TitleParallaxRenderer {
    
    private float stateTime = 0;
    
    // Colors
    private static final Color COL_SKY_TOP = Color.valueOf("0B0E25");
    private static final Color COL_SKY_BOT = Color.valueOf("1B264F");
    private static final Color COL_MOON = Color.valueOf("FDFDC0");
    private static final Color COL_MOON_GLOW = new Color(1, 1, 1, 0.1f);
    
    private static final Color COL_TOWER = Color.valueOf("15111A");
    private static final Color COL_TOWER_WIN = Color.valueOf("D93B3B"); // Red light
    
    private static final Color COL_MTN_FAR = Color.valueOf("0A0A0A");
    private static final Color COL_MTN_NEAR = Color.valueOf("111111");
    
    private static final Color COL_DRAGON_BODY = Color.valueOf("2D2D2D");
    private static final Color COL_DRAGON_OUTLINE = Color.valueOf("000000");
    private static final Color COL_DRAGON_EYE = Color.valueOf("FF0000");
    private static final Color COL_DRAGON_WING = new Color(0.2f, 0f, 0f, 0.5f);

    private static final Color COL_HERO_BODY = Color.valueOf("AAAAAA");
    private static final Color COL_HERO_CAPE = Color.valueOf("D02020");
    private static final Color COL_HERO_SWORD = Color.valueOf("00FFFF"); // Cyan energy sword

    // Star data
    private float[] stars;
    private static final int STAR_COUNT = 50;

    public TitleParallaxRenderer() {
        initStars();
    }

    private void initStars() {
        stars = new float[STAR_COUNT * 3]; // x, y, size
        for(int i=0; i<STAR_COUNT; i++) {
            stars[i*3] = MathUtils.random(0f, 1f); // Relative X
            stars[i*3+1] = MathUtils.random(0.3f, 1f); // Relative Y (Top part)
            stars[i*3+2] = MathUtils.random(1f, 3f); // Size
        }
    }

    public void render(NeonBatch batch, float delta, float width, float height) {
        stateTime += delta;
        float scroll = stateTime * 20f; // 20 pixels per second

        batch.begin();

        // 1. Sky (Static gradient)
        // Draw using gradient rect simulation (or just solid for now if NeonBatch doesn't support easy gradient rect)
        // NeonBatch supports gradient via drawSkewGradientRect or custom vertices.
        // Let's use drawSkewGradientRect with 0 skew for vertical gradient? 
        // Wait, drawSkewGradientRect is Horizontal gradient (Left-Right).
        // For Vertical, we can use drawTriangleStrip with custom colors.
        drawSky(batch, width, height);

        // 2. Stars & Moon (Parallax Layer 0 - Very Slow)
        drawCelestial(batch, width, height, scroll * 0.05f);

        // 3. Mountains (Parallax Layer 1 - Slow)
        drawMountains(batch, width, height, scroll * 0.2f);

        // 4. Tower (Parallax Layer 2 - Mid Slow)
        // Tower is static relative to the world, but we scroll past it?
        // Let's put the tower in the "Far" layer but anchored specificly.
        drawTower(batch, width, height, scroll * 0.2f);

        // 5. Mid Layer - Dragon vs Hero (Static position relative to camera, or slight movement)
        // In a title screen, usually the characters are the focus.
        // Let's keep them centered or slightly offset.
        drawDragon(batch, width, height);
        drawHero(batch, width, height);

        // 6. Foreground (Parallax Layer 3 - Fast)
        drawGround(batch, width, height, scroll * 1.0f);
        drawParticles(batch, width, height, delta);

        batch.end();
    }

    private void drawSky(NeonBatch batch, float w, float h) {
        // Vertical Gradient manually using triangles
        float[] verts = new float[] {
            0, 0,
            w, 0,
            0, h,
            w, h
        };
        float[] colors = new float[] {
            COL_SKY_BOT.toFloatBits(),
            COL_SKY_BOT.toFloatBits(),
            COL_SKY_TOP.toFloatBits(),
            COL_SKY_TOP.toFloatBits()
        };
        batch.drawTriangleStrip(verts, colors, 4);
    }

    private void drawCelestial(NeonBatch batch, float w, float h, float scrollX) {
        // Moon (Fixed in sky roughly)
        float moonX = w * 0.7f;
        float moonY = h * 0.75f;
        float moonR = h * 0.15f;

        // Glow
        for(int i=0; i<3; i++) {
            float r = moonR + (i+1) * 10 + MathUtils.sin(stateTime * 2f + i) * 5f;
            Color c = new Color(COL_MOON_GLOW);
            c.a /= (i+1);
            batch.drawCircle(moonX, moonY, r, 2f, c, 32, true); // Filled glow? No, filled circle
        }
        batch.drawCircle(moonX, moonY, moonR, 0, COL_MOON, 64, true);

        // Stars
        for(int i=0; i<STAR_COUNT; i++) {
            float x = (stars[i*3] * w + scrollX * 0.1f) % w; // Loop stars
            float y = stars[i*3+1] * h;
            float size = stars[i*3+2];
            
            float alpha = 0.5f + 0.5f * MathUtils.sin(stateTime * 3f + i);
            Color c = new Color(1,1,1, alpha);
            
            batch.drawCircle(x, y, size, 0, c, 8, true);
        }
    }

    private void drawMountains(NeonBatch batch, float w, float h, float scrollX) {
        // Procedural jagged mountains
        // We simulate a repeating pattern
        float baseH = h * 0.2f;
        float peakH = h * 0.4f;
        float segmentW = 100f;
        
        int segments = (int)(w / segmentW) + 4;
        float startX = -(scrollX % segmentW) - segmentW;
        
        // Far Mountains
        for(int i=0; i<segments; i++) {
            float x1 = startX + i * segmentW;
            float seed = (int)(scrollX / segmentW) + i; // Pseudo random based on position
            float h1 = baseH + MathUtils.sin(seed)*50 + MathUtils.cos(seed * 0.5f)*30;
            
            // Draw a big triangle
            float[] poly = new float[] {
                x1, 0,
                x1 + segmentW/2, h1 + 50,
                x1 + segmentW, 0
            };
            batch.drawPolygon(poly, 3, 0, COL_MTN_FAR, true);
        }
    }

    private void drawTower(NeonBatch batch, float w, float h, float scrollX) {
        // Draw a tower at a "fixed" world position, so it scrolls by
        // Let's place it at x=800 initially
        float towerWorldX = 800f;
        float drawX = towerWorldX - scrollX; // Moves left
        
        // Loop it or just let it pass? For title screen, maybe loop it far away
        float loopW = 2000f;
        drawX = (drawX % loopW);
        if(drawX < -200) drawX += loopW;
        
        float tw = 80f;
        float th = h * 0.6f;
        
        // Base
        batch.drawRect(drawX, 0, tw, th, 0, 0, COL_TOWER, true);
        
        // Top Spire
        float[] spire = new float[] {
            drawX - 10, th,
            drawX + tw + 10, th,
            drawX + tw/2, th + 100
        };
        batch.drawPolygon(spire, 3, 0, COL_TOWER, true);
        
        // Windows
        int floors = 5;
        for(int i=0; i<floors; i++) {
            float wy = 100 + i * 60;
            batch.drawRect(drawX + 20, wy, 15, 30, 0, 0, COL_TOWER_WIN, true);
            batch.drawRect(drawX + 45, wy, 15, 30, 0, 0, COL_TOWER_WIN, true);
        }
    }

    private void drawDragon(NeonBatch batch, float w, float h) {
        // Right side of screen
        float centerX = w * 0.8f;
        float centerY = h * 0.5f;
        
        // Hover animation
        float hoverY = MathUtils.sin(stateTime) * 20f;
        
        // 1. Wings (Back)
        float wingSpan = 200f;
        float wingFlap = MathUtils.sin(stateTime * 2f) * 0.2f;
        
        float[] wingL = new float[] {
            centerX, centerY + hoverY,
            centerX - wingSpan, centerY + 150 + hoverY - wingFlap*100,
            centerX - wingSpan/2, centerY - 50 + hoverY
        };
        batch.drawPolygon(wingL, 3, 0, COL_DRAGON_WING, true);
        
        // 2. Body (Bezier S-shape)
        // Tail -> Head
        float tailX = centerX + 150;
        float tailY = centerY - 200 + hoverY;
        float headX = centerX - 50;
        float headY = centerY + 100 + hoverY;
        
        // Control points for S-curve
        float cp1x = centerX + 200; 
        float cp1y = centerY + hoverY;
        float cp2x = centerX - 100;
        float cp2y = centerY - 50 + hoverY;
        
        // Use multiple quadratic or one cubic? NeonBatch has Cubic.
        float[] curve = new float[] {
            tailX, tailY,
            cp1x, cp1y,
            cp2x, cp2y,
            headX, headY
        };
        
        // Draw thick body segments manually or thick line
        // NeonBatch drawCubicBezier draws a line.
        // To make it look like a body, we can draw multiple parallel lines or just a very thick line.
        // Let's try a thick line first.
        batch.drawCubicBezier(curve, 40f, COL_DRAGON_BODY, 20);
        
        // 3. Head (Polygon)
        float[] head = new float[] {
            headX - 10, headY - 10,
            headX - 60, headY + 10, // Snout
            headX - 10, headY + 40, // Horn base
            headX + 20, headY + 20
        };
        batch.drawPolygon(head, 4, 0, COL_DRAGON_BODY, true);
        
        // Eye
        batch.drawCircle(headX - 30, headY + 15, 5, 0, COL_DRAGON_EYE, 8, true);
    }

    private void drawHero(NeonBatch batch, float w, float h) {
        float cx = w * 0.25f;
        float cy = h * 0.25f; // On a hill?
        
        // Bobbing
        float bob = MathUtils.sin(stateTime * 3f) * 2f;
        
        // 1. Cape (Flowing)
        float capeEndY = cy - 20 + MathUtils.sin(stateTime * 5f) * 10f;
        float capeEndX = cx - 60;
        
        batch.drawQuadraticBezier(cx, cy + 30, cx - 30, cy + 10, capeEndX, capeEndY, 5f, COL_HERO_CAPE, 10);
        
        // Fill Cape (Triangle approx)
        float[] capePoly = new float[] {
            cx, cy + 30,
            cx, cy - 10,
            capeEndX, capeEndY
        };
        batch.drawPolygon(capePoly, 3, 0, COL_HERO_CAPE, true);

        // 2. Body
        batch.drawRect(cx - 10, cy, 20, 40, 0, 0, COL_HERO_BODY, true);
        
        // 3. Head
        batch.drawCircle(cx, cy + 50, 12, 0, COL_HERO_BODY, 16, true);
        
        // 4. Sword (Glowing)
        float swAngle = 45f + MathUtils.sin(stateTime) * 5f; // Breathing stance
        // Pivot at hand (cx+10, cy+20)
        float hx = cx + 10;
        float hy = cy + 20;
        
        float len = 60f;
        float rad = swAngle * MathUtils.degreesToRadians;
        float tipX = hx + MathUtils.cos(rad) * len;
        float tipY = hy + MathUtils.sin(rad) * len;
        
        batch.drawLine(hx, hy, tipX, tipY, 4f, COL_HERO_SWORD);
    }

    private void drawGround(NeonBatch batch, float w, float h, float scrollX) {
        // Foreground hills
        float[] verts = new float[22]; // 10 segments -> 20 verts + 2 ends
        int segments = 10;
        float step = w / (segments-1);
        
        // Triangle strip for ground? 
        // Or just polygon. Polygon is easier for filling bottom.
        // Let's make a big polygon
        float[] poly = new float[(segments + 2) * 2];
        
        // Start bottom-left
        poly[0] = 0; poly[1] = 0;
        
        for(int i=0; i<segments; i++) {
            float x = i * step;
            // Ground height noise
            float worldX = x + scrollX;
            float y = 50 + MathUtils.sin(worldX * 0.01f) * 20 + MathUtils.cos(worldX * 0.05f) * 10;
            
            poly[(i+1)*2] = x;
            poly[(i+1)*2+1] = y;
        }
        
        // End bottom-right
        poly[(segments+1)*2] = w;
        poly[(segments+1)*2+1] = 0;
        
        batch.drawPolygon(poly, segments+2, 0, COL_MTN_NEAR, true);
    }

    private void drawParticles(NeonBatch batch, float w, float h, float delta) {
        // Embers rising
        // For simplicity, just draw random rects based on hash of time+index
        // A real particle system would be better but this is "Neon" procedural.
        
        int count = 20;
        for(int i=0; i<count; i++) {
            // Pseudo-random lifecycle
            float loopTime = 5f;
            float timeOffset = i * (loopTime / count);
            float t = (stateTime + timeOffset) % loopTime;
            float progress = t / loopTime;
            
            float x = (i * 7393 + 123) % w;
            float y = progress * h; // Rising
            
            float alpha = 1f - progress;
            float size = 4f * (1f - progress);
            
            Color c = new Color(1, 0.5f, 0, alpha);
            batch.drawRect(x, y, size, size, progress * 360, 0, c, true);
        }
    }
}
