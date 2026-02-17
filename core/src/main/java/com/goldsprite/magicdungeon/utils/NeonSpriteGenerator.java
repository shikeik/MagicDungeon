package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格精灵生成器
 * 替代原有的 SpriteGenerator，使用 NeonBatch 进行矢量绘制
 */
public class NeonSpriteGenerator {

    /**
     * 生成 Player 纹理 (Neon 风格)
     */
    public static Texture createPlayer() {
        return generateCharacterTexture("Sword", null, "Helmet", "Armor", "Boots");
    }

    /**
     * 生成带装备的角色纹理
     */
    public static Texture generateCharacterTexture(String mainHand, String offHand, String helmet, String armor, String boots) {
        int size = 128; // 使用 128x128 提高清晰度
        TextureRegion region = NeonGenerator.getInstance().generate(size, size, batch -> {
            drawCharacter(batch, size, mainHand, offHand, helmet, armor, boots);
        });
        return region == null ? null : region.getTexture();
    }

    private static void drawCharacter(NeonBatch batch, float size, String mainHand, String offHand, String helmet, String armor, String boots) {
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

        // Face Details
        int eyeY = headY + 30;
        drawRectPix(batch, size, 128 - 20, eyeY, 12, 12, Color.BLACK);
        drawRectPix(batch, size, 128 + 8, eyeY, 12, 12, Color.BLACK);
        drawRectPix(batch, size, 128 - 18, eyeY + 2, 4, 4, Color.WHITE);
        drawRectPix(batch, size, 128 + 10, eyeY + 2, 4, 4, Color.WHITE);

        // 5. Helmet or Hair
        if (helmet != null) {
            Color helmetColor = Color.valueOf("#CFD8DC");
            Color darkHelmet = Color.valueOf("#90A4AE");
            
            drawRectPix(batch, size, headX - 5, headY - 10, headW + 10, 30, helmetColor); // Top Dome
            drawRectPix(batch, size, headX - 5, headY + 10, 15, headH, darkHelmet); // Sides
            drawRectPix(batch, size, headX + headW - 10, headY + 10, 15, headH, darkHelmet);
            drawRectPix(batch, size, 128 - 5, headY - 20, 10, 20, Color.RED); // Crest
        } else {
            // Hair
            Color hairColor = Color.valueOf("#5d4037");
            drawRectPix(batch, size, headX, headY, headW, 20, hairColor); // Top
            drawRectPix(batch, size, headX - 5, headY, 10, 50, hairColor); // Sideburns
            drawRectPix(batch, size, headX + headW - 5, headY, 10, 50, hairColor);
        }
        
        // 6. Weapons
        if (mainHand != null) {
            // Simple Sword in Right Hand (Screen Left)
            // Hand at 40, 170
            // Blade
            Color blade = Color.LIGHT_GRAY;
            // Angle it? drawRectPix handles upright.
            // Let's draw it upright for now
            drawRectPix(batch, size, 40 + 5, 170 - 60, 15, 60, blade);
            // Hilt
            drawRectPix(batch, size, 30, 170, 35, 10, Color.valueOf("#5d4037"));
        }
        
        // 7. Neon 增强: 增加一点描边和光效，体现矢量优势
        batch.drawCircle(size/2, size * 0.1f, size * 0.35f, 2f, new Color(0, 1, 1, 0.3f), 32, false);
    }

    /**
     * 辅助方法：使用 256x256 的 Pixmap 坐标系进行绘制，内部自动转为 UV 并应用到当前 size
     * Pixmap 坐标: (0,0) 在左上角, Y 向下
     * Neon 坐标: (0,0) 在左下角, Y 向上
     */
    private static void drawRectPix(NeonBatch batch, float size, float px, float py, float w, float h, Color color) {
        float base = 256f;
        float u = px / base;
        // Y 轴翻转核心逻辑
        float v = 1.0f - (py + h) / base;
        float uw = w / base;
        float vh = h / base;

        batch.drawRect(u * size, v * size, uw * size, vh * size, 0, 0, color, true);
    }
}
