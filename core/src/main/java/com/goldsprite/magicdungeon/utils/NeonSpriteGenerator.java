package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格精灵生成器
 * 替代原有的 SpriteGenerator，使用 NeonBatch 进行矢量绘制
 */
public class NeonSpriteGenerator {

    private static final float REF_SIZE = 256f;

    /**
     * 生成 Player 纹理 (Neon 风格)
     */
    public static Texture createPlayer() {
        return generateCharacterTexture("Sword", null, "Helmet", "Armor", "Boots");
    }

    /**
     * 生成带装备的角色纹理
     */
    public static TextureRegion generateCharacterRegion(String mainHand, String offHand, String helmet, String armor, String boots) {
        int size = 256; // High res
        return NeonGenerator.getInstance().generate(size, size, batch -> {
            drawCharacter(batch, size, mainHand, offHand, helmet, armor, boots);
        });
    }

    /**
     * 生成带装备的角色纹理 (Texture 包装)
     */
    public static Texture generateCharacterTexture(String mainHand, String offHand, String helmet, String armor, String boots) {
        TextureRegion region = generateCharacterRegion(mainHand, offHand, helmet, armor, boots);
        return region == null ? null : region.getTexture();
    }

    public static void drawCharacter(NeonBatch batch, float size, String mainHand, String offHand, String helmet, String armor, String boots) {
        // Auto-scale to fit size if different from REF_SIZE
        Matrix4 oldTransform = batch.getTransformMatrix().cpy();
        float scale = size / REF_SIZE;
        if (scale != 1f) {
            batch.getTransformMatrix().scale(scale, scale, 1f);
            batch.setTransformMatrix(batch.getTransformMatrix());
        }

        try {
            drawCharacterImpl(batch, mainHand, offHand, helmet, armor, boots);
        } finally {
            batch.setTransformMatrix(oldTransform);
        }
    }

    private static void drawCharacterImpl(NeonBatch batch, String mainHand, String offHand, String helmet, String armor, String boots) {
        // Use REF_SIZE for all coordinate calculations
        float size = REF_SIZE;

        // Colors
        Color skin = Color.valueOf("#ffccaa");
        Color legsColor = Color.valueOf("#8d6e63");

        // 1. Legs
        drawRectPix(batch, size, 90, 180, 25, 60, legsColor);
        drawRectPix(batch, size, 141, 180, 25, 60, legsColor);

        // 2. Boots
        if (boots != null) {
            Color bootsColor = Color.valueOf("#3E2723");
            int bootY = 215;
            int bootW = 65;
            int bootH = 45;
            drawRectPix(batch, size, 53, bootY, bootW, bootH, bootsColor);
            drawRectPix(batch, size, 138, bootY, bootW, bootH, bootsColor);

            // Boot Detail
            Color bootLight = bootsColor.cpy().mul(1.2f);
            drawRectPix(batch, size, 53, bootY + bootH - 10, bootW, 10, Color.BLACK); // Soles
            drawRectPix(batch, size, 138, bootY + bootH - 10, bootW, 10, Color.BLACK);
            drawRectPix(batch, size, 53 + 5, bootY + 5, 10, 20, bootLight); // Highlights
            drawRectPix(batch, size, 138 + 5, bootY + 5, 10, 20, bootLight);
        } else {
            // Default Feet (Small shoes)
            Color shoeColor = Color.valueOf("#5d4037");
            drawRectPix(batch, size, 90, 230, 25, 10, shoeColor);
            drawRectPix(batch, size, 141, 230, 25, 10, shoeColor);
        }

        // 3. Body & Armor
        if (armor != null) {
            Color armorColor = Color.valueOf("#2196F3");
            Color darkArmor = Color.valueOf("#1565C0");
            Color gold = Color.GOLD;

            drawRectPix(batch, size, 70, 100, 116, 90, armorColor); // Main Chest
            drawRectPix(batch, size, 50, 90, 30, 40, darkArmor); // Shoulder Pads
            drawRectPix(batch, size, 176, 90, 30, 40, darkArmor);

            drawRectPix(batch, size, 80, 110, 96, 70, darkArmor); // Chest Plate Detail
            drawRectPix(batch, size, 118, 110, 20, 70, gold); // Center strip

            // Belt
            drawRectPix(batch, size, 70, 180, 116, 15, Color.valueOf("#3e2723"));
            drawRectPix(batch, size, 118, 180, 20, 15, Color.GOLD);
        } else {
            // Default Tunic
            Color tunicColor = Color.valueOf("#4caf50"); // Green tunic
            drawRectPix(batch, size, 75, 100, 106, 90, tunicColor);
            // Belt
            drawRectPix(batch, size, 75, 180, 106, 10, Color.valueOf("#3e2723"));
        }

        // Arms
        Color armColor = armor != null ? Color.valueOf("#1565C0") : skin; // Armor or Skin
        drawRectPix(batch, size, 40, 100, 25, 70, armColor); // Left
        drawRectPix(batch, size, 191, 100, 25, 70, armColor); // Right
        // Hands
        drawRectPix(batch, size, 40, 170, 25, 25, skin);
        drawRectPix(batch, size, 191, 170, 25, 25, skin);

        // 4. Head
        int headW = 76;
        int headH = 64;
        int headX = 128 - headW/2;
        int headY = 36;
        drawRectPix(batch, size, headX, headY, headW, headH, skin);

        // Face (Simple)
        drawRectPix(batch, size, headX + 15, headY + 25, 10, 10, Color.BLACK); // Eyes
        drawRectPix(batch, size, headX + headW - 25, headY + 25, 10, 10, Color.BLACK);

        // Helmet
        if (helmet != null) {
            Color helmColor = Color.valueOf("#607d8b");
            Color helmDark = Color.valueOf("#455a64");

            drawRectPix(batch, size, headX - 5, headY - 10, headW + 10, 30, helmColor); // Top
            drawRectPix(batch, size, headX - 5, headY + 20, 10, 50, helmColor); // Sides
            drawRectPix(batch, size, headX + headW - 5, headY + 20, 10, 50, helmColor);

            // Horns?
            drawRectPix(batch, size, headX - 15, headY - 20, 10, 40, Color.WHITE);
            drawRectPix(batch, size, headX + headW + 5, headY - 20, 10, 40, Color.WHITE);
        } else {
            // Hair
            drawRectPix(batch, size, headX, headY, headW, 15, Color.BROWN);
        }

        // Weapons
        if (mainHand != null) {
            // Sword
            drawRectPix(batch, size, 10, 130, 20, 100, Color.GRAY); // Blade
            drawRectPix(batch, size, 5, 230, 30, 10, Color.DARK_GRAY); // Guard
            drawRectPix(batch, size, 12, 240, 16, 30, Color.valueOf("#5d4037")); // Hilt
        }
    }

    private static void drawRectPix(NeonBatch batch, float totalSize, float x, float y, float w, float h, Color color) {
        // NeonBatch.drawRect rotates around center.
        // Signature: drawRect(x, y, width, height, rotationDeg, lineWidth, color, filled)
        batch.drawRect(x, y, w, h, 0, 0, color, true);
    }
}
