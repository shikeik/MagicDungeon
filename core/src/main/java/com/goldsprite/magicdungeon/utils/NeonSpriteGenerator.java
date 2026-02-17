package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格精灵生成器
 * 替代原有的 SpriteGenerator，使用 NeonBatch 进行矢量绘制
 * 
 * [Refactor]
 * 1. 坐标系改为 y-up (左下角为 0,0)
 * 2. 坐标单位改为 0.0~1.0 标准化坐标
 */
public class NeonSpriteGenerator {

    // 缩放因子：将原有的像素坐标 (基于 256) 转换为 0~1
    private static final float S = 1.0f / 256.0f;
    // 原始参考尺寸，仅用于旧逻辑参考或特定计算，新绘制应尽量直接使用 S
    private static final float REF_SIZE = 256f;

    /**
     * 生成 Player 纹理 (Neon 风格)
     */
    public static TextureRegion createPlayer() {
        return generateCharacterRegion("Sword", null, "Helmet", "Armor", "Boots");
    }

    /**
     * 生成带装备的角色纹理
     */
    public static TextureRegion generateCharacterRegion(String mainHand, String offHand, String helmet, String armor, String boots) {
        // 大小仅影响 FBO 分辨率，不影响绘制逻辑 (都是 0~1)
        int size = 256; 
        return NeonGenerator.getInstance().generate(size, size, batch -> {
            drawCharacter(batch, mainHand, offHand, helmet, armor, boots);
        });
    }

    /**
     * 生成带装备的角色纹理 (Texture 包装) - Deprecated, use generateCharacterRegion
     */
    public static TextureRegion generateCharacterTexture(String mainHand, String offHand, String helmet, String armor, String boots) {
        return generateCharacterRegion(mainHand, offHand, helmet, armor, boots);
    }

    /**
     * 绘制角色
     * @param batch NeonBatch
     * @param size (Deprecated) 实际上不再需要传入 size 来控制缩放，因为我们使用 0~1 坐标。
     *             但在 NeonGenTestScreen 中可能需要适配。
     *             为了统一，我们修改签名去掉 size，或者忽略它。
     *             为了兼容旧接口，保留参数但不使用它进行计算。
     */
    public static void drawCharacter(NeonBatch batch, float size, String mainHand, String offHand, String helmet, String armor, String boots) {
        // 不需要缩放矩阵，因为我们直接绘制 0~1
        // 但如果调用者传入的 size 不是 FBO 的大小，而是希望在某个大坐标系中绘制小人？
        // 不，NeonGenerator.generate 设定了 0~1 的视口。
        // 所以我们直接绘制 0~1 即可。
        drawCharacterImpl(batch, mainHand, offHand, helmet, armor, boots);
    }
    
    // 重载一个方便的入口
    public static void drawCharacter(NeonBatch batch, String mainHand, String offHand, String helmet, String armor, String boots) {
        drawCharacterImpl(batch, mainHand, offHand, helmet, armor, boots);
    }

    private static void drawCharacterImpl(NeonBatch batch, String mainHand, String offHand, String helmet, String armor, String boots) {
        // Colors
        Color skin = Color.valueOf("#ffccaa");
        Color legsColor = Color.valueOf("#8d6e63");

        // 转换公式:
        // Old (Top-Left Origin): x, y, w, h
        // New (Bottom-Left Origin, 0~1): 
        // X = x * S
        // Y = (REF_SIZE - y - h) * S  (将 Top Y 转换为 Bottom Y)
        // W = w * S
        // H = h * S

        // 1. Legs
        // L: 90, 180, 25, 60 -> Y=(256-180-60)=16
        drawRectNorm(batch, 90*S, 16*S, 25*S, 60*S, legsColor);
        // R: 141, 180, 25, 60
        drawRectNorm(batch, 141*S, 16*S, 25*S, 60*S, legsColor);

        // 2. Boots
        if (boots != null) {
            Color bootsColor = Color.valueOf("#3E2723");
            float bootY_top = 215;
            float bootW = 65;
            float bootH = 45;
            // Y = 256 - 215 - 45 = -4. (超出底部，截断)
            // 实际上我们允许绘制在负坐标，视口会裁切
            float bootY_bot = (REF_SIZE - bootY_top - bootH); 
            
            drawRectNorm(batch, 53*S, bootY_bot*S, bootW*S, bootH*S, bootsColor);
            drawRectNorm(batch, 138*S, bootY_bot*S, bootW*S, bootH*S, bootsColor);

            // Boot Detail
            Color bootLight = bootsColor.cpy().mul(1.2f);
            // Soles: y + h - 10 (Top system) -> Lower in visual (Higher Y value)
            // Old: y = bootY + bootH - 10 = 215 + 45 - 10 = 250.
            // Old H = 10.
            // New Y = 256 - 250 - 10 = -4.
            float soleY_top = bootY_top + bootH - 10;
            float soleY_bot = REF_SIZE - soleY_top - 10;
            drawRectNorm(batch, 53*S, soleY_bot*S, bootW*S, 10*S, Color.BLACK); 
            drawRectNorm(batch, 138*S, soleY_bot*S, bootW*S, 10*S, Color.BLACK);
            
            // Highlights: y + 5 (Top system) -> Higher in visual (Lower Y value)
            // Old: y = 215 + 5 = 220. H=20.
            // New Y = 256 - 220 - 20 = 16.
            float highY_top = bootY_top + 5;
            float highY_bot = REF_SIZE - highY_top - 20;
            drawRectNorm(batch, (53 + 5)*S, highY_bot*S, 10*S, 20*S, bootLight); 
            drawRectNorm(batch, (138 + 5)*S, highY_bot*S, 10*S, 20*S, bootLight);
        } else {
            // Default Feet (Small shoes)
            Color shoeColor = Color.valueOf("#5d4037");
            // 90, 230, 25, 10 -> Y=256-230-10=16
            drawRectNorm(batch, 90*S, 16*S, 25*S, 10*S, shoeColor);
            drawRectNorm(batch, 141*S, 16*S, 25*S, 10*S, shoeColor);
        }

        // 3. Body & Armor
        if (armor != null) {
            Color armorColor = Color.valueOf("#2196F3");
            Color darkArmor = Color.valueOf("#1565C0");
            Color gold = Color.GOLD;

            // Main Chest: 70, 100, 116, 90 -> Y=256-100-90=66
            drawRectNorm(batch, 70*S, 66*S, 116*S, 90*S, armorColor);
            // Shoulder Pads: 50, 90, 30, 40 -> Y=256-90-40=126
            drawRectNorm(batch, 50*S, 126*S, 30*S, 40*S, darkArmor);
            // 176, 90, 30, 40 -> Y=126
            drawRectNorm(batch, 176*S, 126*S, 30*S, 40*S, darkArmor);

            // Chest Plate Detail: 80, 110, 96, 70 -> Y=256-110-70=76
            drawRectNorm(batch, 80*S, 76*S, 96*S, 70*S, darkArmor);
            // Center strip: 118, 110, 20, 70 -> Y=76
            drawRectNorm(batch, 118*S, 76*S, 20*S, 70*S, gold);

            // Belt: 70, 180, 116, 15 -> Y=256-180-15=61
            drawRectNorm(batch, 70*S, 61*S, 116*S, 15*S, Color.valueOf("#3e2723"));
            // 118, 180, 20, 15 -> Y=61
            drawRectNorm(batch, 118*S, 61*S, 20*S, 15*S, Color.GOLD);
        } else {
            // Default Tunic
            Color tunicColor = Color.valueOf("#4caf50");
            // 75, 100, 106, 90 -> Y=256-100-90=66
            drawRectNorm(batch, 75*S, 66*S, 106*S, 90*S, tunicColor);
            // Belt: 75, 180, 106, 10 -> Y=256-180-10=66
            drawRectNorm(batch, 75*S, 66*S, 106*S, 10*S, Color.valueOf("#3e2723"));
        }

        // Arms
        Color armColor = armor != null ? Color.valueOf("#1565C0") : skin;
        // Left: 40, 100, 25, 70 -> Y=256-100-70=86
        drawRectNorm(batch, 40*S, 86*S, 25*S, 70*S, armColor);
        // Right: 191, 100, 25, 70 -> Y=86
        drawRectNorm(batch, 191*S, 86*S, 25*S, 70*S, armColor);
        
        // Hands
        // 40, 170, 25, 25 -> Y=256-170-25=61
        drawRectNorm(batch, 40*S, 61*S, 25*S, 25*S, skin);
        // 191, 170, 25, 25 -> Y=61
        drawRectNorm(batch, 191*S, 61*S, 25*S, 25*S, skin);

        // 4. Head
        float headW = 76;
        float headH = 64;
        float headX = 128 - headW/2;
        float headY = 36; // Top Y
        // New Y = 256 - 36 - 64 = 156
        float headY_bot = REF_SIZE - headY - headH;
        drawRectNorm(batch, headX*S, headY_bot*S, headW*S, headH*S, skin);

        // Face (Simple)
        // Eyes: x+15, y+25, 10, 10
        // New Y = 256 - (36+25) - 10 = 256 - 61 - 10 = 185
        float eyesY_bot = REF_SIZE - (headY + 25) - 10;
        drawRectNorm(batch, (headX + 15)*S, eyesY_bot*S, 10*S, 10*S, Color.BLACK);
        drawRectNorm(batch, (headX + headW - 25)*S, eyesY_bot*S, 10*S, 10*S, Color.BLACK);

        // Helmet
        if (helmet != null) {
            Color helmColor = Color.valueOf("#607d8b");
            // Top: headX-5, headY-10, headW+10, 30
            // Y = 256 - (36-10) - 30 = 256 - 26 - 30 = 200
            drawRectNorm(batch, (headX - 5)*S, 200*S, (headW + 10)*S, 30*S, helmColor);
            
            // Sides: headX-5, headY+20, 10, 50
            // Y = 256 - (36+20) - 50 = 256 - 56 - 50 = 150
            drawRectNorm(batch, (headX - 5)*S, 150*S, 10*S, 50*S, helmColor);
            drawRectNorm(batch, (headX + headW - 5)*S, 150*S, 10*S, 50*S, helmColor);

            // Horns: headX-15, headY-20, 10, 40
            // Y = 256 - (36-20) - 40 = 256 - 16 - 40 = 200
            drawRectNorm(batch, (headX - 15)*S, 200*S, 10*S, 40*S, Color.WHITE);
            drawRectNorm(batch, (headX + headW + 5)*S, 200*S, 10*S, 40*S, Color.WHITE);
        } else {
            // Hair: headX, headY, headW, 15
            // Y = 256 - 36 - 15 = 205
            drawRectNorm(batch, headX*S, 205*S, headW*S, 15*S, Color.BROWN);
        }

        // Weapons
        if (mainHand != null) {
            // Sword
            // Blade: 10, 130, 20, 100 -> Y=256-130-100=26
            drawRectNorm(batch, 10*S, 26*S, 20*S, 100*S, Color.GRAY);
            // Guard: 5, 230, 30, 10 -> Y=256-230-10=16
            drawRectNorm(batch, 5*S, 16*S, 30*S, 10*S, Color.DARK_GRAY);
            // Hilt: 12, 240, 16, 30 -> Y=256-240-30=-14
            drawRectNorm(batch, 12*S, -14*S, 16*S, 30*S, Color.valueOf("#5d4037"));
        }
    }

    private static void drawRectNorm(NeonBatch batch, float x, float y, float w, float h, Color color) {
        // 直接绘制 0~1 坐标
        batch.drawRect(x, y, w, h, 0, 0, color, true);
    }
}
