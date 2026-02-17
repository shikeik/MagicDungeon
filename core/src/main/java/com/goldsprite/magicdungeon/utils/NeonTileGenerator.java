package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格图块生成器
 * 替代 SpriteGenerator 中的 createDungeonWallTileset 和 createFloor
 */
public class NeonTileGenerator {

    /**
     * 生成地牢墙壁图块集 (64x64)
     */
    public static Texture createDungeonWallTileset(Color primary, Color secondary) {
        int size = 64;
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawWallTileset(batch, size, primary, secondary);
        });
        return region == null ? null : region.getTexture();
    }

    /**
     * 生成地板纹理 (32x32)
     */
    public static Texture createFloor(Color base, Color dark, Color highlight) {
        int size = 32;
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawFloor(batch, size, base, dark, highlight);
        });
        return region == null ? null : region.getTexture();
    }

    // --- Implementation ---

    public static void drawWallTileset(NeonBatch batch, float size, Color primary, Color secondary) {
        // Dual Grid Mask to Atlas Mapping
        int[] MASK_TO_ATLAS_X = { -1, 1, 0, 3, 0, 1, 2, 1, 3, 0, 3, 2, 1, 2, 3, 2 };
        int[] MASK_TO_ATLAS_Y = { -1, 3, 0, 0, 2, 0, 3, 1, 3, 1, 2, 0, 2, 2, 1, 1 };

        Color topColor = primary;
        Color topHighlight = primary.cpy().mul(1.2f, 1.2f, 1.2f, 1f);
        Color faceColor = secondary;
        Color faceShadow = secondary.cpy().mul(0.7f, 0.7f, 0.7f, 1f);

        for (int mask = 0; mask < 16; mask++) {
            int atlasX = MASK_TO_ATLAS_X[mask];
            int atlasY = MASK_TO_ATLAS_Y[mask];
            
            if (atlasX == -1 || atlasY == -1) continue;

            // SpriteGenerator uses Top-Left origin.
            // atlasY=0 is Top.
            // NeonBatch uses Bottom-Left origin.
            // To match the texture layout:
            // GL Y = size - (atlasY * 16) - 16
            
            float tx = atlasX * 16;
            float ty = size - (atlasY * 16) - 16; 

            boolean tl = (mask & 8) != 0;
            boolean tr = (mask & 4) != 0;
            boolean bl = (mask & 2) != 0;
            boolean br = (mask & 1) != 0;

            // Quadrants (8x8)
            // In SpriteGenerator:
            // TL is at (tx, ty)
            // TR is at (tx+8, ty)
            // BL is at (tx, ty+8)
            // BR is at (tx+8, ty+8)
            
            // In GL (Bottom-Left origin), "Top" in texture is Higher Y.
            // So "Top-Left" quadrant is at (tx, ty + 8)
            // "Bottom-Left" quadrant is at (tx, ty)
            
            // Wait, let's verify standard Texture coordinates.
            // (0,0) in Texture is usually Bottom-Left for GL, but LibGDX Texture(Pixmap) loads 0,0 as Top-Left?
            // Pixmap (0,0) is Top-Left.
            // When uploaded to Texture, Pixmap(0,0) maps to UV(0,0) usually.
            // In LibGDX, UV (0,0) is Top-Left for TextureRegion if flipped, but standard GL texture 0,0 is Bottom-Left.
            // NeonGenerator.generate returns a TextureRegion.
            // The FrameBuffer logic:
            // batch.setProjectionMatrix(0, 0, w, h); -> (0,0) is Bottom-Left.
            // When we draw at (0,0), it appears at Bottom-Left of the FBO.
            // When FBO is used as Texture, (0,0) is Bottom-Left.
            // If we want the result to match Pixmap layout where (0,0) is Top-Left:
            // We need to draw "Top" things at High Y.
            
            // So:
            // Top-Left Quadrant: x=tx, y=ty+8
            // Top-Right Quadrant: x=tx+8, y=ty+8
            // Bottom-Left Quadrant: x=tx, y=ty
            // Bottom-Right Quadrant: x=tx+8, y=ty

            // --- Top-Left Quadrant ---
            if (tl) {
                drawWallTop(batch, tx, ty + 8, 8, 8, topColor, topHighlight);
            }

            // --- Top-Right Quadrant ---
            if (tr) {
                drawWallTop(batch, tx + 8, ty + 8, 8, 8, topColor, topHighlight);
            }

            // --- Bottom-Left Quadrant ---
            if (bl) {
                drawWallTop(batch, tx, ty, 8, 8, topColor, topHighlight);
            } else {
                if (tl) {
                    drawWallFace(batch, tx, ty, 8, 8, faceColor, faceShadow);
                }
            }

            // --- Bottom-Right Quadrant ---
            if (br) {
                drawWallTop(batch, tx + 8, ty, 8, 8, topColor, topHighlight);
            } else {
                if (tr) {
                    drawWallFace(batch, tx + 8, ty, 8, 8, faceColor, faceShadow);
                }
            }
        }
    }

    private static void drawWallTop(NeonBatch batch, float x, float y, float w, float h, Color color, Color highlight) {
        batch.drawRect(x, y, w, h, 0, 0, color, true);
        // Bevel / Highlight (Inset)
        float border = 1;
        batch.drawRect(x + border, y + border, w - 2*border, h - 2*border, 0, 0, highlight, true);
        
        // Noise (Simulated with small rects)
        if (MathUtils.randomBoolean(0.1f)) {
            batch.drawRect(x + MathUtils.random(w-2), y + MathUtils.random(h-2), 1, 1, 0, 0, Color.valueOf("#444444"), true);
        }
    }

    private static void drawWallFace(NeonBatch batch, float x, float y, float w, float h, Color color, Color shadow) {
        batch.drawRect(x, y, w, h, 0, 0, color, true);
        
        // Horizontal Brick Lines (every 4px)
        // In Pixmap (Top-Down): y+0, y+4...
        // In GL (Bottom-Up): we want lines at same visual positions.
        // If height is 8.
        // Pixmap y=0 is GL y=8. Pixmap y=4 is GL y=4.
        // So lines at y=0, y=4 relative to bottom?
        // Let's draw lines at relative Y=0, Y=4.
        
        float lineH = 0.5f; // Thin line
        for(int i=0; i<h; i+=4) {
            batch.drawRect(x, y + i, w, lineH, 0, 0, shadow, true);
        }

        // Vertical Brick Lines (Staggered)
        for(int i=0; i<h; i+=4) {
            float offset = (i % 8 == 0) ? 0 : 4;
            if (offset < w) {
                 batch.drawRect(x + offset, y + i, 0.5f, 4, 0, 0, shadow, true);
            }
            if (offset + 4 < w) {
                 batch.drawRect(x + offset + 4, y + i, 0.5f, 4, 0, 0, shadow, true);
            }
        }
        
        // Shadow at Top (under overhang)
        // In GL, Top is y+h. So draw just below y+h.
        batch.drawRect(x, y + h - 1, w, 1, 0, 0, Color.BLACK, true);
    }

    public static void drawFloor(NeonBatch batch, float size, Color base, Color dark, Color highlight) {
        // Fill background
        batch.drawRect(0, 0, size, size, 0, 0, dark, true);

        // Grid 2x2
        int rows = 2;
        int cols = 2;
        float slabW = size / cols;
        float slabH = size / rows;
        float gap = 1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // In Pixmap, r=0 is Top.
                // In GL, r=0 should be Top to match visual? Or Bottom?
                // Floor pattern is symmetric usually, so order doesn't matter much.
                
                float x = c * slabW + gap;
                float y = r * slabH + gap;
                float w = slabW - gap * 2;
                float h = slabH - gap * 2;

                // Variation
                float shade = 0.9f + MathUtils.random(0.2f);
                Color slabColor = new Color(base).mul(shade, shade, shade, 1f);

                batch.drawRect(x, y, w, h, 0, 0, slabColor, true);

                // Bevel Highlight (Top/Left)
                // In GL, Top is y+h, Left is x.
                batch.drawRect(x, y + h - 1, w, 1, 0, 0, highlight, true); // Top
                batch.drawRect(x, y, 1, h, 0, 0, highlight, true); // Left

                // Cracks
                if (MathUtils.randomBoolean(0.3f)) {
                    float cx = x + MathUtils.random(w);
                    float cy = y + MathUtils.random(h);
                    float len = MathUtils.random(2, 8);
                    // Draw diagonal line
                    // NeonBatch doesn't have drawLine, use rotated rect or thin rect
                    // For simple noise, just small rects
                     batch.drawRect(cx, cy, len, 0.5f, 45, 0, dark, true);
                }
            }
        }
    }
}
