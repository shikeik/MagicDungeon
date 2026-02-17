package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格图块生成器
 * 替代 SpriteGenerator 中的 createDungeonWallTileset 和 createFloor
 */
public class NeonTileGenerator {

    private static final float REF_SIZE_WALL = 64f;
    private static final float REF_SIZE_FLOOR = 32f;

    public static Texture createDungeonWallTileset(Color primary, Color secondary) {
        int size = 64;
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawWallTileset(batch, size, primary, secondary);
        });
        return region == null ? null : region.getTexture();
    }

    public static Texture createFloor(Color base, Color dark, Color highlight) {
        int size = 32;
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawFloor(batch, size, base, dark, highlight);
        });
        return region == null ? null : region.getTexture();
    }

    public static void drawWallTileset(NeonBatch batch, float size, Color primary, Color secondary) {
        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        float scale = size / REF_SIZE_WALL;
        if (scale != 1f) {
            batch.getTransformMatrix().scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());
        }

        try {
            drawWallTilesetImpl(batch, primary, secondary);
        } finally {
            batch.setTransformMatrix(oldTransform);
        }
    }

    private static void drawWallTilesetImpl(NeonBatch batch, Color primary, Color secondary) {
        float size = REF_SIZE_WALL;
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

            float tx = atlasX * 16;
            float ty = atlasY * 16; 

            boolean tl = (mask & 8) != 0;
            boolean tr = (mask & 4) != 0;
            boolean bl = (mask & 2) != 0;
            boolean br = (mask & 1) != 0;

            if (tl) drawWallTop(batch, size, tx, ty, 8, 8, topColor, topHighlight);
            if (tr) drawWallTop(batch, size, tx + 8, ty, 8, 8, topColor, topHighlight);
            if (bl) {
                drawWallTop(batch, size, tx, ty + 8, 8, 8, topColor, topHighlight);
            } else if (tl) {
                drawWallFace(batch, size, tx, ty + 8, 8, 8, faceColor, faceShadow);
            }
            if (br) {
                drawWallTop(batch, size, tx + 8, ty + 8, 8, 8, topColor, topHighlight);
            } else if (tr) {
                drawWallFace(batch, size, tx + 8, ty + 8, 8, 8, faceColor, faceShadow);
            }
        }
    }

    public static void drawFloor(NeonBatch batch, float size, Color base, Color dark, Color highlight) {
        float scale = size / REF_SIZE_FLOOR;
        if (scale == 1f) {
            drawFloorImpl(batch, base, dark, highlight);
            return;
        }

        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        batch.getTransformMatrix().scale(scale, scale, 1f);
        batch.setTransformMatrix(batch.getTransformMatrix());

        try {
            drawFloorImpl(batch, base, dark, highlight);
        } finally {
            batch.setTransformMatrix(oldTransform);
        }
    }

    private static void drawFloorImpl(NeonBatch batch, Color base, Color dark, Color highlight) {
        float size = REF_SIZE_FLOOR;
        drawRectPix(batch, size, 0, 0, size, size, dark);

        int rows = 2;
        int cols = 2;
        float slabW = size / cols;
        float slabH = size / rows;
        float gap = 1;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float x = c * slabW + gap;
                float y = r * slabH + gap;
                float w = slabW - gap * 2;
                float h = slabH - gap * 2;

                float shade = 0.9f + MathUtils.random(0.2f);
                Color slabColor = new Color(base).mul(shade, shade, shade, 1f);

                drawRectPix(batch, size, x, y, w, h, slabColor);
                drawRectPix(batch, size, x, y, w, 1, highlight);
                drawRectPix(batch, size, x, y, 1, h, highlight);
                
                if (MathUtils.randomBoolean(0.3f)) {
                    float cx = x + MathUtils.random(w);
                    float cy = y + MathUtils.random(h);
                    float len = MathUtils.random(2, 8);
                    drawRectPix(batch, size, cx, cy, len, 1, dark);
                }
            }
        }
        
        for(int i=0; i<20; i++) {
             drawRectPix(batch, size, MathUtils.random(size), MathUtils.random(size), 1, 1, new Color(1,1,1,0.1f));
        }
    }

    // --- Helpers (Top-Left Origin) ---

    private static void drawWallTop(NeonBatch batch, float size, float x, float y, float w, float h, Color color, Color highlight) {
        drawRectPix(batch, size, x, y, w, h, color);
        float border = 1;
        drawRectPix(batch, size, x + border, y + border, w - 2*border, h - 2*border, highlight);
        if (MathUtils.randomBoolean(0.1f)) {
            drawRectPix(batch, size, x + MathUtils.random(w-1), y + MathUtils.random(h-1), 1, 1, Color.valueOf("#444444"));
        }
    }

    private static void drawWallFace(NeonBatch batch, float size, float x, float y, float w, float h, Color color, Color shadow) {
        drawRectPix(batch, size, x, y, w, h, color);
        for(int i=0; i<h; i+=4) {
            drawRectPix(batch, size, x, y+i, w, 1, shadow);
        }
        for(int i=0; i<h; i+=4) {
            float offset = (i % 8 == 0) ? 0 : 4;
            if (offset < w) drawRectPix(batch, size, x + offset, y + i + 2, 1, 2, shadow);
            if (offset + 4 < w) drawRectPix(batch, size, x + offset + 4, y + i + 2, 1, 2, shadow);
        }
        drawRectPix(batch, size, x, y, w, 1, Color.BLACK);
    }

    private static void drawRectPix(NeonBatch batch, float totalSize, float x, float y, float w, float h, Color color) {
        // Convert Top-Left (x, y) to Bottom-Left (GL_X, GL_Y)
        // GL_Y = totalSize - y - h
        // Signature: drawRect(x, y, width, height, rotationDeg, lineWidth, color, filled)
        batch.drawRect(x, totalSize - y - h, w, h, 0, 0, color, true);
    }
}
