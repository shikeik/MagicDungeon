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
        // Right side of screen (More centered but offset right)
        float centerX = w * 0.7f;
        float centerY = h * 0.5f;

        // Hover animation
        float hoverY = MathUtils.sin(stateTime * 1.5f) * 15f;

        // --- 1. Wings (More detailed & animated) ---
        float wingSpan = 220f;
        float wingFlap = MathUtils.sin(stateTime * 3f) * 0.3f;

        // Wing Structure (Bone)
        float wx = centerX + 20;
        float wy = centerY + 80 + hoverY;
        float wTipX = centerX - wingSpan;
        float wTipY = wy + 120 - wingFlap * 100;

        // Draw Wing Membrane (Translucent)
        // Use a fan shape for membrane
        float[] wingMembrane = new float[] {
            wx, wy, // Shoulder
            wTipX, wTipY, // Tip
            wTipX + 50, wTipY - 80, // Edge 1
            wTipX + 100, wTipY - 60, // Edge 2
            centerX - 20, centerY + hoverY // Back to body
        };
        batch.drawPolygon(wingMembrane, 5, 0, COL_DRAGON_WING, true);

        // Wing Bones (Thick Lines)
        batch.drawLine(wx, wy, wTipX, wTipY, 8f, COL_DRAGON_BODY);
        batch.drawLine(wTipX, wTipY, wTipX + 50, wTipY - 80, 4f, COL_DRAGON_BODY);

        // --- 2. Body (Segmented for volume) ---
        // Instead of a single line, we draw a series of circles along the path

        // Tail -> Head Path Control
        float tX = centerX + 180;
        float tY = centerY - 250 + hoverY * 0.5f;
        float hX = centerX - 80;
        float hY = centerY + 120 + hoverY;

        // Control Points
        float c1x = centerX + 250; float c1y = centerY + hoverY;
        float c2x = centerX - 150; float c2y = centerY - 50 + hoverY;

        int segments = 40;
        for(int i=0; i<=segments; i++) {
            float t = i / (float)segments;
            float u = 1 - t;
            float tt = t*t; float uu = u*u;
            float uuu = uu*u; float ttt = tt*t;

            // Cubic Bezier Point
            float px = uuu * tX + 3 * uu * t * c1x + 3 * u * tt * c2x + ttt * hX;
            float py = uuu * tY + 3 * uu * t * c1y + 3 * u * tt * c2y + ttt * hY;

            // Radius tapers from chest to tail
            // t=0 (tail), t=1 (head)
            // Chest is around t=0.7
            float radius;
            if (t < 0.7f) {
                radius = 10f + t * 50f; // 10 -> 45
            } else {
                radius = 45f - (t-0.7f) * 60f; // 45 -> 27
            }
            radius = Math.max(5f, radius);

            // Body Segment
            batch.drawCircle(px, py, radius, 0, COL_DRAGON_BODY, 12, true);

            // Spikes (Dorsal fin) - Every few segments
            if (i % 3 == 0 && i < segments - 2) {
                // Approximate normal (simplified, just pointing up/back)
                float spikeH = 20f;
                batch.drawPolygon(new float[]{
                    px - 5, py + radius*0.5f,
                    px + 5, py + radius*0.5f,
                    px, py + radius + spikeH
                }, 3, 0, COL_DRAGON_OUTLINE, true);
            }
        }

        // --- 3. Head (More menacing) ---
        // Jaw
        batch.drawPolygon(new float[]{
            hX + 10, hY - 10,
            hX - 50, hY - 20, // Lower jaw tip
            hX - 20, hY + 10
        }, 3, 0, COL_DRAGON_BODY, true);

        // Upper Head
        batch.drawPolygon(new float[]{
            hX + 20, hY + 10,
            hX - 70, hY + 20, // Snout tip
            hX - 30, hY + 50, // Forehead
            hX + 30, hY + 30
        }, 4, 0, COL_DRAGON_BODY, true);

        // Horns
        batch.drawPolygon(new float[]{
            hX + 10, hY + 40,
            hX + 40, hY + 90, // Horn tip
            hX + 30, hY + 40
        }, 3, 0, COL_DRAGON_OUTLINE, true);

        // Glowing Eye (Pulsing)
        float eyePulse = 1f + MathUtils.sin(stateTime * 5f) * 0.3f;
        batch.drawCircle(hX - 30, hY + 25, 6 * eyePulse, 0, COL_DRAGON_EYE, 8, true);

        // Breath Particles (Fire)
        if (MathUtils.randomBoolean(0.3f)) {
            float pX = hX - 70;
            float pY = hY + 10;
            batch.drawRect(pX - MathUtils.random(20), pY - MathUtils.random(10),
                           MathUtils.random(5,10), MathUtils.random(5,10),
                           MathUtils.random(360), 0, new Color(1, 0.2f, 0, 0.8f), true);
        }
    }

    private void drawHero(NeonBatch batch, float w, float h) {
        float cx = w * 0.35f;
        float cy = h * 0.3f; // Slightly higher

        // Breathing animation
        float breath = MathUtils.sin(stateTime * 2f) * 2f;

        // 1. Cape (More dynamic flow)
        // Simulate wave passing through cape
        int capeSegs = 10;
        float[] capeVerts = new float[(capeSegs + 1) * 2 * 2]; // Triangle strip
        float capeLen = 80f;

        // Attach point (Shoulders)
        float sx = cx - 5;
        float sy = cy + 45 + breath;

        // Draw Cape using Bezier curve + thickness simulation
        float endX = sx - capeLen - 20 + MathUtils.sin(stateTime)*10;
        float endY = sy - 40 + MathUtils.cos(stateTime * 1.5f)*10;

        // Cape Body
        batch.drawQuadraticBezier(sx, sy, sx - 30, sy - 50, endX, endY, 20f, COL_HERO_CAPE, 10);

        // 2. Legs (Stance)
        batch.drawRect(cx - 15, cy, 12, 45, 10, 0, COL_HERO_BODY, true); // Left leg
        batch.drawRect(cx + 5, cy, 12, 45, -10, 0, COL_HERO_BODY, true); // Right leg

        // 3. Torso (Armor Plate)
        // Use a hexagon for armor look
        float bodyY = cy + 35 + breath;
        batch.drawPolygon(new float[] {
            cx, bodyY - 25, // Waist
            cx - 15, bodyY, // Left shoulder
            cx, bodyY + 10, // Neck base
            cx + 15, bodyY  // Right shoulder
        }, 4, 0, COL_HERO_BODY, true);

        // 4. Head (Helmet)
        float headY = cy + 65 + breath;
        // Helmet base
        batch.drawRect(cx - 10, headY - 10, 20, 25, 0, 0, COL_HERO_BODY, true);
        // Visor (Dark slit)
        batch.drawRect(cx - 8, headY, 16, 4, 0, 0, Color.BLACK, true);
        // Plume/Crest
        batch.drawPolygon(new float[] {
            cx - 5, headY + 15,
            cx + 5, headY + 15,
            cx - 10, headY + 35
        }, 3, 0, COL_HERO_CAPE, true);

        // 5. Sword (Big Glowing Energy Blade)
        float swAngle = 30f + MathUtils.sin(stateTime * 2f) * 2f;
        float hx = cx + 20; // Hand pos
        float hy = bodyY - 5;

        float bladeLen = 90f;
        float bladeW = 8f;
        float rad = swAngle * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        float tipX = hx + cos * bladeLen;
        float tipY = hy + sin * bladeLen;

        // Hilt
        batch.drawLine(hx, hy, hx - cos*20, hy - sin*20, 4f, Color.DARK_GRAY);
        // Guard
        batch.drawLine(hx - sin*10, hy + cos*10, hx + sin*10, hy - cos*10, 4f, Color.GOLD);

        // Blade (Core)
        batch.drawLine(hx, hy, tipX, tipY, bladeW, Color.WHITE);
        // Blade (Glow)
        Color glowCol = new Color(COL_HERO_SWORD);
        glowCol.a = 0.5f + MathUtils.sin(stateTime * 10f) * 0.2f;
        batch.drawLine(hx, hy, tipX, tipY, bladeW * 2.5f, glowCol);
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
