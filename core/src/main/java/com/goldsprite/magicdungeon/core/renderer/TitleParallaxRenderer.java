package com.goldsprite.magicdungeon.core.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

public class TitleParallaxRenderer {

    private float stateTime = 0;

    // --- Colors ---
    // Sky
    private static final Color COL_SKY_TOP = Color.valueOf("050714"); // Deep Space Blue
    private static final Color COL_SKY_BOT = Color.valueOf("1B264F"); // Horizon Blue
    private static final Color COL_MOON = Color.valueOf("FDFDC0");
    private static final Color COL_MOON_SHADOW = new Color(0.8f, 0.8f, 0.7f, 1f); // Craters
    private static final Color COL_CLOUD = new Color(0.3f, 0.3f, 0.4f, 0.3f);

    // Landscape
    private static final Color COL_MTN_FAR = Color.valueOf("050505");
    private static final Color COL_MTN_MID = Color.valueOf("0F0F12");
    private static final Color COL_GROUND_TOP = Color.valueOf("1A1A20");
    private static final Color COL_GROUND_BOT = Color.valueOf("0D0D10");

    // Tower
    private static final Color COL_TOWER_BASE = Color.valueOf("181520");
    private static final Color COL_TOWER_TRIM = Color.valueOf("2A2535"); // Edges/Cornices
    private static final Color COL_TOWER_WIN = Color.valueOf("FF3333"); // Evil Red Light
    private static final Color COL_TOWER_CRYSTAL = Color.valueOf("A020F0"); // Purple Magic

    // Dragon
    private static final Color COL_DRAGON_SCALE_DARK = Color.valueOf("1A0505"); // Dark Red/Black
    private static final Color COL_DRAGON_SCALE_LIGHT = Color.valueOf("4A0A0A"); // Highlight
    private static final Color COL_DRAGON_BELLY = Color.valueOf("2D1C1C"); // Tan/Grey belly
    private static final Color COL_DRAGON_WING_BONE = Color.valueOf("200505");
    private static final Color COL_DRAGON_WING_MEMBRANE = new Color(0.6f, 0.1f, 0.0f, 0.6f); // Translucent Red/Orange
    private static final Color COL_DRAGON_HORN = Color.valueOf("101010");
    private static final Color COL_DRAGON_EYE = Color.valueOf("FFD700"); // Gold Eye

    // Hero
    private static final Color COL_ARMOR_DARK = Color.valueOf("404040"); // Dark Steel
    private static final Color COL_ARMOR_LIGHT = Color.valueOf("909090"); // Polished Steel
    private static final Color COL_ARMOR_GOLD = Color.valueOf("D4AF37"); // Gold Trim
    private static final Color COL_CLOTH_RED = Color.valueOf("800000"); // Deep Red Cape
    private static final Color COL_SKIN = Color.valueOf("E0AC69"); // Skin (if visible)
    private static final Color COL_SWORD_CORE = Color.valueOf("E0FFFF");
    private static final Color COL_SWORD_GLOW = new Color(0.0f, 1.0f, 1.0f, 0.6f);

    // Star data
    private float[] stars;
    private static final int STAR_COUNT = 80;

    public TitleParallaxRenderer() {
        initStars();
    }

    private void initStars() {
        stars = new float[STAR_COUNT * 3]; // x, y, size
        for(int i=0; i<STAR_COUNT; i++) {
            stars[i*3] = MathUtils.random(0f, 1f); // Relative X
            stars[i*3+1] = MathUtils.random(0.2f, 1f); // Relative Y
            stars[i*3+2] = MathUtils.random(0.5f, 2.5f); // Size
        }
    }

    public void render(NeonBatch batch, float delta, float width, float height) {
        stateTime += delta;
        float scroll = stateTime * 30f; // Global scroll speed

        batch.begin();

        // 1. Sky & Celestial
        drawSky(batch, width, height);
        drawCelestial(batch, width, height, scroll * 0.02f);

        // 2. Far Background (Mountains/Ruins)
        drawMountains(batch, width, height, scroll * 0.1f);

        // 3. The Magic Tower (Mid-Far layer)
        drawTower(batch, width, height, scroll * 0.15f);

        // 4. Ground (Mid-Near)
        drawGround(batch, width, height, scroll * 1.0f);

        // 5. Characters (Focus)
        // Position them relative to screen size
        drawDragon(batch, width, height);
        drawHero(batch, width, height);

        // 6. Foreground Particles (Embers/Magic)
        drawParticles(batch, width, height, delta);

        batch.end();
    }

    private void drawSky(NeonBatch batch, float w, float h) {
        // Vertical Gradient
        float[] verts = new float[] { 0, 0, w, 0, 0, h, w, h };
        float[] colors = new float[] {
            COL_SKY_BOT.toFloatBits(), COL_SKY_BOT.toFloatBits(),
            COL_SKY_TOP.toFloatBits(), COL_SKY_TOP.toFloatBits()
        };
        batch.drawTriangleStrip(verts, colors, 4);
    }

    private void drawCelestial(NeonBatch batch, float w, float h, float scrollX) {
        // --- Stars ---
        for(int i=0; i<STAR_COUNT; i++) {
            float x = (stars[i*3] * w + scrollX * 0.5f) % w;
            float y = stars[i*3+1] * h;
            float size = stars[i*3+2];
            float twinkle = 0.7f + 0.3f * MathUtils.sin(stateTime * 2f + i * 10);
            Color c = new Color(1, 1, 1, twinkle * 0.8f);
            batch.drawCircle(x, y, size, 0, c, 6, true);
        }

        // --- Moon ---
        float moonX = w * 0.8f;
        float moonY = h * 0.75f;
        float moonR = h * 0.12f;

        // Glow
        batch.drawCircle(moonX, moonY, moonR * 1.2f, 0, new Color(1,1,0.9f, 0.1f), 16, true);
        // Main Body
        batch.drawCircle(moonX, moonY, moonR, 0, COL_MOON, 32, true);
        // Craters (Simple circles)
        batch.drawCircle(moonX - moonR*0.3f, moonY + moonR*0.2f, moonR*0.2f, 0, COL_MOON_SHADOW, 12, true);
        batch.drawCircle(moonX + moonR*0.4f, moonY - moonR*0.1f, moonR*0.15f, 0, COL_MOON_SHADOW, 12, true);
        batch.drawCircle(moonX - moonR*0.1f, moonY - moonR*0.4f, moonR*0.1f, 0, COL_MOON_SHADOW, 12, true);

        // --- Clouds (Passing by) ---
        for(int i=0; i<5; i++) {
            float speed = 10f + i * 5f;
            float cx = (stateTime * speed + i * 200) % (w + 400) - 200;
            float cy = h * 0.6f + MathUtils.sin(i)*100;
            float cw = 150 + i * 30;
            float ch = 40;
            batch.drawOval(cx, cy, cw, ch, 0, 0, COL_CLOUD, 12, true);
        }
    }

    private void drawMountains(NeonBatch batch, float w, float h, float scrollX) {
        // Two layers of mountains
        drawMountainLayer(batch, w, h, scrollX * 0.5f, h * 0.3f, COL_MTN_FAR, 123);
        drawMountainLayer(batch, w, h, scrollX, h * 0.15f, COL_MTN_MID, 456);
    }

    private void drawMountainLayer(NeonBatch batch, float w, float h, float scrollX, float baseHeight, Color color, int seedOffset) {
        float segmentW = 80f;
        int segments = (int)(w / segmentW) + 4;
        float startX = -(scrollX % segmentW) - segmentW;

        for(int i=0; i<segments; i++) {
            float x1 = startX + i * segmentW;
            float s = (int)(scrollX / segmentW) + i + seedOffset;
            // Noise function
            float h1 = baseHeight + MathUtils.sin(s)*50 + MathUtils.cos(s * 0.4f)*30 + MathUtils.sin(s*2.1f)*15;
            h1 = Math.max(0, h1);

            float[] poly = new float[] {
                x1, 0,
                x1 + segmentW*0.6f, h1 + 50, // Peak
                x1 + segmentW, 0
            };
            batch.drawPolygon(poly, 3, 0, color, true);
        }
    }

    private void drawTower(NeonBatch batch, float w, float h, float scrollX) {
        // Multi-segmented Gothic Tower
        float worldX = 1000f; // Fixed position in world
        float loopW = 3000f;  // Repeat distance
        float drawX = (worldX - scrollX) % loopW;
        if(drawX < -300) drawX += loopW;

        float groundY = 50f;
        float widthBase = 120f;
        float heightBase = 150f;

        // --- Segment 1: Base ---
        batch.drawRect(drawX, groundY, widthBase, heightBase, 0, 0, COL_TOWER_BASE, true);
        // Trim
        batch.drawRect(drawX - 5, groundY + heightBase - 10, widthBase + 10, 20, 0, 0, COL_TOWER_TRIM, true);

        // --- Segment 2: Mid Section (Narrower) ---
        float widthMid = 100f;
        float heightMid = 120f;
        float xMid = drawX + (widthBase - widthMid)/2;
        float yMid = groundY + heightBase;
        batch.drawRect(xMid, yMid, widthMid, heightMid, 0, 0, COL_TOWER_BASE, true);
        // Windows (Arched)
        batch.drawRect(xMid + 20, yMid + 30, 15, 40, 0, 0, COL_TOWER_WIN, true);
        batch.drawTriangle(xMid + 20, yMid + 70, xMid + 27.5f, yMid + 85, xMid + 35, yMid + 70, 0, COL_TOWER_WIN, true); // Arch top
        batch.drawRect(xMid + 65, yMid + 30, 15, 40, 0, 0, COL_TOWER_WIN, true);
        batch.drawTriangle(xMid + 65, yMid + 70, xMid + 72.5f, yMid + 85, xMid + 80, yMid + 70, 0, COL_TOWER_WIN, true);

        // Trim
        batch.drawRect(xMid - 5, yMid + heightMid - 10, widthMid + 10, 20, 0, 0, COL_TOWER_TRIM, true);

        // --- Segment 3: Top Section ---
        float widthTop = 80f;
        float heightTop = 80f;
        float xTop = drawX + (widthBase - widthTop)/2;
        float yTop = yMid + heightMid;
        batch.drawRect(xTop, yTop, widthTop, heightTop, 0, 0, COL_TOWER_BASE, true);
        // Big Circular Window
        batch.drawCircle(xTop + widthTop/2, yTop + heightTop/2, 20, 0, COL_TOWER_WIN, 8, true);

        // --- Spire (Roof) ---
        float[] spire = new float[] {
            xTop - 10, yTop + heightTop,
            xTop + widthTop + 10, yTop + heightTop,
            xTop + widthTop/2, yTop + heightTop + 120 // Tall sharp point
        };
        batch.drawPolygon(spire, 3, 0, COL_TOWER_BASE, true);

        // --- Floating Crystal ---
        float crystalY = yTop + heightTop + 120 + 20 + MathUtils.sin(stateTime*2f)*10;
        batch.drawRect(xTop + widthTop/2 - 10, crystalY, 20, 40, 45, 0, COL_TOWER_CRYSTAL, true);
    }

    private void drawDragon(NeonBatch batch, float w, float h) {
        float cx = w * 0.75f;
        float cy = h * 0.5f;
        float hover = MathUtils.sin(stateTime * 1.5f) * 20f;
        cy += hover;

        // --- 1. Rear Wing (Behind body) ---
        drawDragonWing(batch, cx + 30, cy + 50, -1, 0.8f);

        // --- 2. Tail ---
        // Bezier tail curling down
        drawDragonTail(batch, cx - 40, cy - 20);

        // --- 3. Rear Leg (Thigh + Shin + Claw) ---
        drawDragonLeg(batch, cx + 40, cy - 40, true);

        // --- 4. Body (Torso) ---
        // Oval shape, rotated
        batch.drawOval(cx, cy, 140, 90, 20, 0, COL_DRAGON_SCALE_DARK, 16, true);
        // Belly (Lighter area)
        batch.drawOval(cx - 10, cy - 10, 100, 60, 20, 0, COL_DRAGON_BELLY, 16, true);

        // --- 5. Front Leg (Arm + Claw) ---
        drawDragonLeg(batch, cx - 20, cy - 30, false);

        // --- 6. Neck ---
        // Curved segments leading to head
        float nx = cx - 60;
        float ny = cy + 30;
        // Draw thick line or polys for neck
        for(int i=0; i<5; i++) {
            float t = i / 4f;
            float px = cx - 50 - i * 15;
            float py = cy + 20 + i * 20;
            float size = 40 - i * 3;
            batch.drawCircle(px, py, size/2, 0, COL_DRAGON_SCALE_DARK, 8, true);
            // Belly stripe
            batch.drawCircle(px + 5, py - 5, size/3, 0, COL_DRAGON_BELLY, 8, true);
        }

        // --- 7. Head ---
        float headX = cx - 130;
        float headY = cy + 110;
        drawDragonHead(batch, headX, headY);

        // --- 8. Front Wing (In front of body) ---
        drawDragonWing(batch, cx - 10, cy + 60, 1, 1f);
    }

    private void drawDragonWing(NeonBatch batch, float rootX, float rootY, float dir, float scale) {
        float flap = MathUtils.sin(stateTime * 3f) * 0.4f * dir; // Flapping
        float tipX = rootX - 200 * scale;
        float tipY = rootY + 150 * scale + flap * 100;
        float jointX = rootX - 50 * scale;
        float jointY = rootY + 100 * scale + flap * 50;

        // Membrane
        float[] poly = new float[] {
            rootX, rootY,
            jointX, jointY,
            tipX, tipY,
            tipX + 60*scale, tipY - 80*scale,
            tipX + 120*scale, tipY - 100*scale,
            rootX + 30*scale, rootY - 30*scale
        };
        batch.drawPolygon(poly, 6, 0, COL_DRAGON_WING_MEMBRANE, true);

        // Bones
        batch.drawLine(rootX, rootY, jointX, jointY, 6f*scale, COL_DRAGON_WING_BONE);
        batch.drawLine(jointX, jointY, tipX, tipY, 4f*scale, COL_DRAGON_WING_BONE);
        // Fingers
        batch.drawLine(tipX, tipY, tipX + 60*scale, tipY - 80*scale, 2f*scale, COL_DRAGON_WING_BONE);
        batch.drawLine(jointX, jointY, tipX + 120*scale, tipY - 100*scale, 2f*scale, COL_DRAGON_WING_BONE);
    }

    private void drawDragonLeg(NeonBatch batch, float rootX, float rootY, boolean isRear) {
        float legScale = isRear ? 1.2f : 1.0f;
        // Thigh/UpperArm
        float kneeX = rootX + 10;
        float kneeY = rootY - 40 * legScale;
        batch.drawPolygon(new float[]{rootX-10, rootY, rootX+20, rootY, kneeX+10, kneeY, kneeX-15, kneeY}, 4, 0, COL_DRAGON_SCALE_DARK, true);

        // Shin/Forearm
        float footX = kneeX - 10;
        float footY = kneeY - 30 * legScale;
        batch.drawPolygon(new float[]{kneeX-10, kneeY, kneeX+10, kneeY, footX+5, footY, footX-5, footY}, 4, 0, COL_DRAGON_SCALE_DARK, true);

        // Claws
        batch.drawTriangle(footX, footY, footX-10, footY-15, footX+5, footY, 0, COL_DRAGON_HORN, true);
        batch.drawTriangle(footX+5, footY, footX+10, footY-10, footX+15, footY, 0, COL_DRAGON_HORN, true);
    }

    private void drawDragonTail(NeonBatch batch, float rootX, float rootY) {
        // Curve down and right
        float cx = rootX;
        float cy = rootY;
        for(int i=0; i<15; i++) {
            float t = i/15f;
            float px = cx + i * 15 + i*i*0.5f;
            float py = cy - i * 10 + MathUtils.sin(stateTime + i*0.2f)*10;
            float r = 25 - i * 1.2f;
            batch.drawCircle(px, py, r, 0, COL_DRAGON_SCALE_DARK, 8, true);
            if(i%2==0) batch.drawCircle(px, py-r*0.5f, r*0.6f, 0, COL_DRAGON_BELLY, 8, true); // Underbelly
        }
    }

    private void drawDragonHead(NeonBatch batch, float x, float y) {
        // Main skull
        batch.drawPolygon(new float[]{
            x, y,
            x+50, y+10,
            x+40, y-30,
            x-20, y-10
        }, 4, 0, COL_DRAGON_SCALE_DARK, true);

        // Snout
        batch.drawPolygon(new float[]{
            x-20, y-10,
            x-60, y-15, // Tip
            x-50, y+5,
            x, y
        }, 4, 0, COL_DRAGON_SCALE_DARK, true);

        // Jaw (Lower)
        float jawOpen = 5 + MathUtils.sin(stateTime*5f)*5;
        batch.drawPolygon(new float[]{
            x-15, y-15,
            x-55, y-20 - jawOpen,
            x-10, y-25
        }, 3, 0, COL_DRAGON_SCALE_DARK, true);

        // Horns
        batch.drawPolygon(new float[]{x+20, y+5, x+60, y+40, x+30, y+5}, 3, 0, COL_DRAGON_HORN, true);
        batch.drawPolygon(new float[]{x+10, y+5, x+40, y+50, x+20, y+5}, 3, 0, COL_DRAGON_HORN, true);

        // Eye
        batch.drawCircle(x, y-5, 5, 0, COL_DRAGON_EYE, 6, true);

        // Breath
        if(MathUtils.randomBoolean(0.1f)) {
             batch.drawRect(x-70, y-20, 10, 10, 0, 0, Color.ORANGE, true);
        }
    }

    private void drawHero(NeonBatch batch, float w, float h) {
        float cx = w * 0.38f;
        float cy = h * 0.35f; // Ground level roughly

        // Breathing
        float breath = MathUtils.sin(stateTime * 2f) * 1f;

        // --- 1. Cape (Behind) ---
        drawHeroCape(batch, cx, cy + 60, breath);

        // --- 2. Legs (Armored) ---
        // Right Leg (Back)
        drawArmoredLimb(batch, cx + 15, cy + 30, 15, 50, -10);
        // Left Leg (Front)
        drawArmoredLimb(batch, cx - 20, cy + 30, 15, 50, 10);

        // --- 3. Torso ---
        float bodyY = cy + 60 + breath;
        // Waist/Hips
        batch.drawRect(cx - 15, bodyY - 35, 30, 20, 0, 0, COL_ARMOR_DARK, true);
        // Chestplate (Breastplate)
        float[] chest = new float[] {
            cx - 20, bodyY + 10,
            cx + 20, bodyY + 10,
            cx + 15, bodyY - 15,
            cx - 15, bodyY - 15
        };
        batch.drawPolygon(chest, 4, 0, COL_ARMOR_LIGHT, true);
        // Gold trim on chest
        batch.drawCircle(cx, bodyY, 5, 0, COL_ARMOR_GOLD, 6, true);

        // --- 4. Shoulders (Pauldrons) ---
        batch.drawPolygon(new float[]{cx-35, bodyY+5, cx-15, bodyY+15, cx-15, bodyY-5}, 3, 0, COL_ARMOR_LIGHT, true);
        batch.drawPolygon(new float[]{cx+35, bodyY+5, cx+15, bodyY+15, cx+15, bodyY-5}, 3, 0, COL_ARMOR_LIGHT, true);

        // --- 5. Arms ---
        // Back arm (holding sword?)
        drawArmoredLimb(batch, cx + 25, bodyY, 10, 40, -20);
        // Front arm
        drawArmoredLimb(batch, cx - 25, bodyY, 10, 40, 20);

        // --- 6. Head (Helmet) ---
        float headY = bodyY + 25;
        // Base
        batch.drawRect(cx - 12, headY - 10, 24, 30, 0, 0, COL_ARMOR_LIGHT, true);
        // Visor slit (T-shape)
        batch.drawRect(cx - 12, headY + 2, 24, 4, 0, 0, Color.BLACK, true); // Horizontal
        batch.drawRect(cx - 2, headY - 5, 4, 10, 0, 0, Color.BLACK, true); // Vertical
        // Plume
        batch.drawTriangle(cx-5, headY+20, cx+5, headY+20, cx-15, headY+40, 0, COL_CLOTH_RED, true);

        // --- 7. Giant Sword ---
        drawHeroSword(batch, cx + 35, bodyY - 10);
    }

    private void drawArmoredLimb(NeonBatch batch, float x, float y, float w, float h, float angle) {
        batch.drawRect(x - w/2, y - h, w, h, angle, 0, COL_ARMOR_DARK, true);
        // Knee/Elbow pad
        batch.drawCircle(x, y - h/2, w*0.7f, 0, COL_ARMOR_GOLD, 6, true);
    }

    private void drawHeroCape(NeonBatch batch, float sx, float sy, float breath) {
        float[] cape = new float[12]; // 6 points
        float endX = sx - 60;
        float endY = sy - 80;
        float wave = MathUtils.sin(stateTime * 4f) * 10;

        // Top (Shoulders)
        cape[0] = sx - 20; cape[1] = sy;
        cape[2] = sx + 20; cape[3] = sy;
        // Mid
        cape[4] = sx - 40; cape[5] = sy - 40;
        cape[6] = sx + 10; cape[7] = sy - 40;
        // Bottom
        cape[8] = endX + wave; cape[9] = endY + wave;
        cape[10] = endX + 40 + wave; cape[11] = endY - 10 + wave;

        batch.drawPolygon(cape, 6, 0, COL_CLOTH_RED, true);
    }

    private void drawHeroSword(NeonBatch batch, float hx, float hy) {
        float angle = 60f + MathUtils.sin(stateTime)*2f;
        float len = 110f;
        float w = 12f;

        // Blade direction
        float rad = angle * MathUtils.degreesToRadians;
        float c = MathUtils.cos(rad);
        float s = MathUtils.sin(rad);

        float tipX = hx + c * len;
        float tipY = hy + s * len;

        // Hilt
        batch.drawLine(hx, hy, hx - c*25, hy - s*25, 4f, COL_ARMOR_DARK);
        // Guard (Winged)
        float gx = hx; float gy = hy;
        batch.drawLine(gx - s*15, gy + c*15, gx + s*15, gy - c*15, 6f, COL_ARMOR_GOLD);

        // Blade Core
        batch.drawLine(hx, hy, tipX, tipY, w, COL_SWORD_CORE);
        // Blade Glow
        Color glow = new Color(COL_SWORD_GLOW);
        glow.a = 0.5f + MathUtils.sin(stateTime * 10f)*0.3f;
        batch.drawLine(hx, hy, tipX, tipY, w+8, glow);
    }

    private void drawGround(NeonBatch batch, float w, float h, float scrollX) {
        // Use a dense polygon for the ground to make it solid
        int segments = 20;
        float step = w / (segments - 1);
        float[] poly = new float[(segments + 2) * 2];
        poly[0] = 0; poly[1] = 0; // Bottom-left corner

        for(int i=0; i<segments; i++) {
            float x = i * step;
            float worldX = x + scrollX;
            // More complex noise
            float y = 40 + MathUtils.sin(worldX * 0.005f) * 30
                         + MathUtils.cos(worldX * 0.02f) * 15
                         + MathUtils.sin(worldX * 0.1f) * 5; // Jagged details
            poly[(i+1)*2] = x;
            poly[(i+1)*2+1] = y;

            // Draw some grass/rocks on top?
            if(i % 3 == 0) {
                 batch.drawTriangle(x, y, x+5, y+10, x+10, y, 0, COL_GROUND_TOP, true);
            }
        }

        poly[(segments+1)*2] = w; poly[(segments+1)*2+1] = 0; // Bottom-right corner
        batch.drawPolygon(poly, segments+2, 0, COL_GROUND_BOT, true);
    }

    private void drawParticles(NeonBatch batch, float w, float h, float delta) {
        // Embers
        for(int i=0; i<30; i++) {
            float t = (stateTime + i * 0.7f) % 5f;
            float p = t / 5f;
            float x = (i * 123 + MathUtils.sin(t)*50) % w;
            float y = p * h * 0.8f;
            float size = 3f * (1-p);
            batch.drawRect(x, y, size, size, t*90, 0, new Color(1, 0.4f, 0.1f, 1-p), true);
        }
    }
}
