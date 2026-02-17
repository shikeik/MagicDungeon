package com.goldsprite.magicdungeon.utils.texturegenerator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
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
        // High Quality Character (Viking / Warrior Style)
        // Body Proportions:
        // Head: 64x64
        // Body: 80x80
        // Legs: 40x50

        // Center X = 128
        float cx = 128 * S;
        float groundY = 16 * S; // Feet on ground

        // --- 1. Legs (Thicker, separated) ---
        Color pantsColor = Color.valueOf("#5d4037");
        // Left Leg
        float legW = 30 * S;
        float legH = 50 * S;
        float legGap = 10 * S;

        // Left Leg: x = cx - gap/2 - w
        drawRectNorm(batch, cx - legGap/2 - legW, groundY, legW, legH, pantsColor);
        // Right Leg
        drawRectNorm(batch, cx + legGap/2, groundY, legW, legH, pantsColor);

        // Boots (if present)
        if (boots != null) {
            Color bootColor = Color.valueOf("#3e2723");
            float bootH = 20 * S;
            // Left Boot
            drawRectNorm(batch, cx - legGap/2 - legW - 5*S, groundY, legW + 10*S, bootH, bootColor);
            // Right Boot
            drawRectNorm(batch, cx + legGap/2 - 5*S, groundY, legW + 10*S, bootH, bootColor);
        }

        // --- 2. Body / Armor (Trapezoid shape for chest) ---
        float bodyY = groundY + legH;
        float bodyW_bot = 70 * S;
        float bodyW_top = 90 * S;
        float bodyH = 80 * S;

        Color armorColor = (armor != null) ? Color.GRAY : Color.valueOf("#a1887f"); // Metal or Leather
        if (armor != null && armor.contains("Leather")) armorColor = Color.valueOf("#5d4037");

        // Trapezoid Body
        float[] bodyPoly = new float[] {
            cx - bodyW_bot/2, bodyY,          // BL
            cx + bodyW_bot/2, bodyY,          // BR
            cx + bodyW_top/2, bodyY + bodyH,  // TR
            cx - bodyW_top/2, bodyY + bodyH   // TL
        };
        batch.drawPolygon(bodyPoly, 4, 0, armorColor, true);

        // Armor Detail (Cross or Plate)
        if (armor != null) {
            batch.drawRect(cx - 10*S, bodyY + 20*S, 20*S, 40*S, 0, 0, Color.DARK_GRAY, true);
            batch.drawRect(cx - 30*S, bodyY + 35*S, 60*S, 10*S, 0, 0, Color.DARK_GRAY, true);
        } else {
            // Belt
            batch.drawRect(cx - bodyW_bot/2 - 5*S, bodyY, bodyW_bot + 10*S, 15*S, 0, 0, Color.valueOf("#3e2723"), true);
            // Buckle
            batch.drawRect(cx - 8*S, bodyY, 16*S, 15*S, 0, 0, Color.GOLD, true);
        }

        // --- 3. Head (Rounder) ---
        float headY = bodyY + bodyH - 5*S; // Slight overlap
        float headSize = 60 * S;
        Color skinColor = Color.valueOf("#ffccaa");

        // Head Base (Rounded Rect or Circle)
        // Using circle for top part, rect for jaw
        // Rect part
        drawRectNorm(batch, cx - headSize/2, headY, headSize, headSize*0.6f, skinColor);
        // Top circle part
        // batch.drawCircle(cx, headY + headSize*0.6f, headSize/2, 0, skinColor, 16, true);
        // Let's just use a rounded rect approximation: a rect + circle on top
        drawRectNorm(batch, cx - headSize/2, headY, headSize, headSize/2, skinColor);
        batch.drawCircle(cx, headY + headSize/2, headSize/2, 0, skinColor, 16, true);

        // Face Features
        float eyeY = headY + headSize * 0.4f;
        float eyeX_L = cx - 12 * S;
        float eyeX_R = cx + 12 * S;

        // Eyes (Black dots)
        batch.drawCircle(eyeX_L, eyeY, 4*S, 0, Color.BLACK, 8, true);
        batch.drawCircle(eyeX_R, eyeY, 4*S, 0, Color.BLACK, 8, true);

        // Mouth (Line)
        batch.drawLine(cx - 5*S, headY + 15*S, cx + 5*S, headY + 15*S, 2*S, Color.valueOf("#d84315"));

        // --- 4. Helmet (Viking Style) ---
        if (helmet != null) {
            Color helmColor = Color.GRAY;
            float helmY = headY + headSize * 0.4f; // Sits lower on head

            // Helmet Dome (Half Circle)
            batch.drawSector(cx, helmY, headSize*0.6f, 0, 180, helmColor, 16);

            // Rim
            batch.drawRect(cx - headSize*0.6f, helmY, headSize*1.2f, 8*S, 0, 0, Color.LIGHT_GRAY, true);

            // Nose Guard
            batch.drawRect(cx - 4*S, helmY - 15*S, 8*S, 20*S, 0, 0, Color.LIGHT_GRAY, true);

            // Horns (Triangles)
            Color hornColor = Color.WHITE;
            // Left Horn
            float hx = cx - headSize*0.5f;
            float hy = helmY + 10*S;
            // Pointing up-left
            float[] hornL = new float[] {
                hx, hy,
                hx - 10*S, hy + 5*S,
                hx - 25*S, hy + 40*S // Tip
            };
            batch.drawPolygon(hornL, 3, 0, hornColor, true);

            // Right Horn
            hx = cx + headSize*0.5f;
            float[] hornR = new float[] {
                hx, hy,
                hx + 10*S, hy + 5*S,
                hx + 25*S, hy + 40*S // Tip
            };
            batch.drawPolygon(hornR, 3, 0, hornColor, true);
        } else {
            // Hair (Brown, messy)
            Color hairColor = Color.valueOf("#5d4037");
            // Top hair
            batch.drawSector(cx, headY + headSize*0.5f, headSize*0.55f, 0, 180, hairColor, 16);
            // Sideburns
            drawRectNorm(batch, cx - headSize/2 - 2*S, headY, 10*S, headSize*0.6f, hairColor);
            drawRectNorm(batch, cx + headSize/2 - 8*S, headY, 10*S, headSize*0.6f, hairColor);
        }

        // --- 5. Weapons ---
        // Draw weapons *in front* of body? Or hand placement?
        // Let's draw them at the sides for now, maybe slightly angled.

        if (mainHand != null) {
            // Right Hand (Screen Left side relative to body? No, Right hand is usually User's Right)
            // Let's put Main Hand on Right (cx + bodyW)
            float handX = cx + bodyW_top/2 + 10*S;
            float handY = bodyY + bodyH * 0.6f;

            // Sword pointing up
            Color blade = Color.LIGHT_GRAY;
            Color hilt = Color.valueOf("#3e2723");

            // Handle
            batch.drawRect(handX - 2*S, handY - 10*S, 4*S, 20*S, 0, 0, hilt, true);
            // Guard
            batch.drawRect(handX - 10*S, handY + 10*S, 20*S, 4*S, 0, 0, hilt, true);
            // Blade (Triangle-ish)
            float[] swordPoly = new float[] {
                handX - 6*S, handY + 14*S, // BL
                handX + 6*S, handY + 14*S, // BR
                handX, handY + 80*S        // Tip
            };
            batch.drawPolygon(swordPoly, 3, 0, blade, true);
        }

        if (offHand != null) {
            // Left Hand (Shield?)
            float handX = cx - bodyW_top/2 - 10*S;
            float handY = bodyY + bodyH * 0.5f;

            // Round Shield
            batch.drawCircle(handX, handY, 30*S, 0, Color.valueOf("#5d4037"), 16, true); // Wood
            batch.drawCircle(handX, handY, 8*S, 0, Color.GRAY, 8, true); // Boss
            batch.drawRect(handX - 25*S, handY - 2*S, 50*S, 4*S, 0, 0, Color.GRAY, true); // Metal rim?
        }
    }

    private static void drawRectNorm(NeonBatch batch, float x, float y, float w, float h, Color color) {
        // 直接绘制 0~1 坐标
        batch.drawRect(x, y, w, h, 0, 0, color, true);
    }
}
