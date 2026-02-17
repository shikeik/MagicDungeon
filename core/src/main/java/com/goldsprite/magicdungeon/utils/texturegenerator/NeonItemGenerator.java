package com.goldsprite.magicdungeon.utils.texturegenerator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTextureFactory.PotionPalette;

/**
 * Neon 风格物品生成器
 * 替代 SpriteGenerator 中的 createItem 和 drawItemToPixmap
 *
 * [Refactor]
 * 1. 坐标系改为 y-up (左下角为 0,0)
 * 2. 坐标单位改为 0.0~1.0 标准化坐标
 */
public class NeonItemGenerator {

    private static final float S = 1.0f / 256.0f;
    private static final float REF_SIZE = 256f;

    public static TextureRegion createItemRegion(String name) {
        int size = 256;
        return NeonGenerator.getInstance().generate(size, size, batch -> {
            drawItem(batch, name);
        });
    }

    public static TextureRegion createItem(String name) {
        return createItemRegion(name);
    }

    public static void drawItem(NeonBatch batch, float size, String name) {
        drawItem(batch, name);
    }

    public static void drawItem(NeonBatch batch, String name) {
        drawItemImpl(batch, name);
    }

    private static void drawItemImpl(NeonBatch batch, String name) {
        if (name.contains("Potion") || name.contains("Elixir") || name.contains("药水") || name.contains("万能药")) {
            drawPotion(batch, name);
        } else if (name.contains("Sword") || name.contains("Blade") || name.contains("剑") || name.contains("刃")) {
            drawSword(batch, name);
        } else if (name.contains("Shield") || name.contains("盾")) {
            drawShield(batch, name);
        } else if (name.contains("Helmet") || name.contains("Hat") || name.contains("帽") || name.contains("盔")) {
            drawHelmet(batch, name);
        } else if (name.contains("Boots") || name.contains("Shoes") || name.contains("靴") || name.contains("鞋")) {
            drawBoots(batch, name);
        } else if (name.contains("Armor") || name.contains("Mail") || name.contains("甲")) {
            drawArmor(batch, name);
        } else if (name.contains("Axe") || name.contains("斧")) {
            drawAxe(batch, name);
        } else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
            drawWand(batch, name);
        } else if (name.contains("Scroll") || name.contains("卷轴") || name.contains("Book") || name.contains("书")) {
            drawScroll(batch, name);
        } else if (name.contains("Ring") || name.contains("戒指")) {
            drawRing(batch, name);
        } else if (name.contains("Necklace") || name.contains("项链")) {
            drawNecklace(batch, name);
        } else if (name.contains("Bracelet") || name.contains("手环")) {
            drawBracelet(batch, name);
        } else if (name.contains("Coin") || name.contains("金币")) {
            drawCoin(batch, name);
        } else {
            drawGenericItem(batch, name);
        }
    }

    // --- Specific Generators (All use S = 1/256) ---

    private static void drawPotion(NeonBatch batch, String name) {
        // [Integration] 使用 NeonTextureFactory.drawComplexPotion
        PotionPalette palette = PotionPalette.HEALING;
        if (name.contains("Mana") || name.contains("魔法")) {
            palette = PotionPalette.MANA;
        } else if (name.contains("Elixir") || name.contains("万能") || name.contains("Strength") || name.contains("力量")) {
            palette = PotionPalette.ELIXIR;
        }
        
        NeonTextureFactory.drawComplexPotion(batch, palette);
    }

    private static void drawSword(NeonBatch batch, String name) {
        Color bladeColor = Color.LIGHT_GRAY;
        if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
        if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");

        // Handle: 60, 200, 24, 16 -> Y=256-200-16=40
        drawRectNorm(batch, 60*S, 40*S, 24*S, 16*S, Color.valueOf("#3e2723"), 45);

        // Blade: 60, 200 to 200, 60
        // Y1 = 256-200=56, Y2 = 256-60=196
        drawLineNorm(batch, 60*S, 56*S, 200*S, 196*S, 20*S, bladeColor);

        // Guard: 50, 190 to 90, 230
        // Y1 = 256-190=66, Y2 = 256-230=26
        drawLineNorm(batch, 50*S, 66*S, 90*S, 26*S, 8*S, Color.valueOf("#5d4037"));

        // Hilt: 30, 230 to 50, 210
        // Y1 = 256-230=26, Y2 = 256-210=46
        drawLineNorm(batch, 30*S, 26*S, 50*S, 46*S, 8*S, Color.valueOf("#3e2723"));
    }

    private static void drawShield(NeonBatch batch, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.GRAY : Color.valueOf("#5d4037");
        Color border = isIron ? Color.LIGHT_GRAY : Color.valueOf("#8d6e63");

        // 50, 50, 156, 120 -> Y=256-50-120=86
        drawRectNorm(batch, 50*S, 86*S, 156*S, 120*S, c);
        // Circle 128, 170 -> Y=256-170=86
        drawCircleNorm(batch, 128*S, 86*S, 78*S, c);

        int bThick = 10;
        // Top: 45, 45, 166, 10 -> Y=256-45-10=201
        drawRectNorm(batch, 45*S, 201*S, 166*S, bThick*S, border);
        // Left: 45, 45, 10, 140 -> Y=256-45-140=71
        drawRectNorm(batch, 45*S, 71*S, bThick*S, 140*S, border);
        // Right: 201, 45, 10, 140 -> Y=71
        drawRectNorm(batch, 201*S, 71*S, bThick*S, 140*S, border);

        // 128, 230 -> Y=256-230=26
        drawCircleNorm(batch, 128*S, 26*S, 10*S, border);
        // 128, 120 -> Y=256-120=136
        drawCircleNorm(batch, 128*S, 136*S, 30*S, border);
    }

    private static void drawHelmet(NeonBatch batch, String name) {
        boolean isIron = name.contains("Iron") || name.contains("铁");
        Color c = isIron ? Color.LIGHT_GRAY : Color.valueOf("#5d4037");
        Color trim = isIron ? Color.GRAY : Color.valueOf("#3e2723");

        // 128, 128, 70 -> Y=256-128=128
        drawCircleNorm(batch, 128*S, 128*S, 70*S, c);
        // 50, 150, 156, 20 -> Y=256-150-20=86
        drawRectNorm(batch, 50*S, 86*S, 156*S, 20*S, trim);
        // 123, 40, 10, 30 -> Y=256-40-30=186
        drawRectNorm(batch, 123*S, 186*S, 10*S, 30*S, trim);
    }

    private static void drawBoots(NeonBatch batch, String name) {
        Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
        Color trim = name.contains("Iron") ? Color.LIGHT_GRAY : Color.valueOf("#3e2723");

        // Left Boot: 60, 80, 50, 100 -> Y=256-80-100=76
        drawRectNorm(batch, 60*S, 76*S, 50*S, 100*S, c);
        // 40, 160, 70, 40 -> Y=256-160-40=56
        drawRectNorm(batch, 40*S, 56*S, 70*S, 40*S, c);
        // 60, 80, 50, 10 -> Y=256-80-10=166
        drawRectNorm(batch, 60*S, 166*S, 50*S, 10*S, trim);

        // Right Boot: 140, 80, 50, 100 -> Y=76
        drawRectNorm(batch, 140*S, 76*S, 50*S, 100*S, c);
        // 140, 160, 70, 40 -> Y=56
        drawRectNorm(batch, 140*S, 56*S, 70*S, 40*S, c);
        // 140, 80, 50, 10 -> Y=166
        drawRectNorm(batch, 140*S, 166*S, 50*S, 10*S, trim);
    }

    private static void drawArmor(NeonBatch batch, String name) {
        Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
        Color trim = Color.DARK_GRAY;

        // 68, 60, 120, 140 -> Y=256-60-140=56
        drawRectNorm(batch, 68*S, 56*S, 120*S, 140*S, c);
        // 60, 80 -> Y=256-80=176
        drawCircleNorm(batch, 60*S, 176*S, 30*S, trim);
        // 196, 80 -> Y=176
        drawCircleNorm(batch, 196*S, 176*S, 30*S, trim);
        // 88, 80, 80, 80 -> Y=256-80-80=96
        drawRectNorm(batch, 88*S, 96*S, 80*S, 80*S, c.cpy().mul(1.2f));
    }

    private static void drawAxe(NeonBatch batch, String name) {
        // 60, 200 -> 180, 80
        // Y1=256-200=56, Y2=256-80=176
        drawLineNorm(batch, 60*S, 56*S, 180*S, 176*S, 12*S, Color.valueOf("#3e2723"));
        // 180, 80 -> Y=176
        drawCircleNorm(batch, 180*S, 176*S, 40*S, Color.GRAY);
        // 170, 90 -> Y=256-90=166
        drawCircleNorm(batch, 170*S, 166*S, 30*S, Color.DARK_GRAY);
    }

    private static void drawWand(NeonBatch batch, String name) {
        // 60, 200 -> 180, 80 => Y1=56, Y2=176
        drawLineNorm(batch, 60*S, 56*S, 180*S, 176*S, 8*S, Color.valueOf("#5d4037"));
        // 180, 80 -> Y=176
        drawCircleNorm(batch, 180*S, 176*S, 16*S, Color.CYAN);
        drawCircleNorm(batch, 180*S, 176*S, 8*S, Color.WHITE);
    }

    private static void drawScroll(NeonBatch batch, String name) {
        Color paper = Color.valueOf("#fff9c4");
        // 80, 80 -> Y=256-80=176
        drawCircleNorm(batch, 80*S, 176*S, 20*S, paper.cpy().mul(0.9f));
        // 80, 180 -> Y=256-180=76
        drawCircleNorm(batch, 80*S, 76*S, 20*S, paper.cpy().mul(0.9f));
        // 80, 80, 100, 100 -> Y=256-80-100=76
        drawRectNorm(batch, 80*S, 76*S, 100*S, 100*S, paper);
        for(int i=0; i<3; i++) {
            // y = 100 + i*20 -> Y = 256 - (100+i*20) - 4
            float y = 100 + i*20;
            float glY = 256 - y - 4;
            drawRectNorm(batch, 100*S, glY*S, 60*S, 4*S, Color.BLACK);
        }
    }

    private static void drawRing(NeonBatch batch, String name) {
        // 128, 128 -> Y=128
        drawCircleNorm(batch, 128*S, 128*S, 40*S, Color.GOLD);
        drawCircleNorm(batch, 128*S, 128*S, 30*S, Color.BLACK);
        // 128, 88 -> Y=256-88=168
        drawCircleNorm(batch, 128*S, 168*S, 16*S, Color.RED);
    }

    private static void drawNecklace(NeonBatch batch, String name) {
        // 80, 60 -> 128, 160 => Y1=256-60=196, Y2=256-160=96
        drawLineNorm(batch, 80*S, 196*S, 128*S, 96*S, 4*S, Color.GOLD);
        // 176, 60 -> 128, 160 => Y1=196, Y2=96
        drawLineNorm(batch, 176*S, 196*S, 128*S, 96*S, 4*S, Color.GOLD);
        // 128, 160 -> Y=96
        drawCircleNorm(batch, 128*S, 96*S, 20*S, Color.BLUE);
    }

    private static void drawBracelet(NeonBatch batch, String name) {
        // 128, 128 -> Y=128
        drawCircleNorm(batch, 128*S, 128*S, 50*S, Color.GOLD);
        drawCircleNorm(batch, 128*S, 128*S, 40*S, Color.BLACK);
    }

    private static void drawCoin(NeonBatch batch, String name) {
        // 128, 128 -> Y=128
        drawCircleNorm(batch, 128*S, 128*S, 60*S, Color.GOLD);
        // 108, 108, 40, 40 -> Y=256-108-40=108
        drawRectNorm(batch, 108*S, 108*S, 40*S, 40*S, Color.YELLOW);
    }

    private static void drawGenericItem(NeonBatch batch, String name) {
        // 128, 128 -> Y=128
        drawCircleNorm(batch, 128*S, 128*S, 80*S, Color.GRAY);
        // 120, 80, 16, 60 -> Y=256-80-60=116
        drawRectNorm(batch, 120*S, 116*S, 16*S, 60*S, Color.WHITE);
        // 120, 160, 16, 16 -> Y=256-160-16=80
        drawRectNorm(batch, 120*S, 80*S, 16*S, 16*S, Color.WHITE);
    }

    // --- Helpers (0~1 Normalized Coordinates) ---

    private static void drawRectNorm(NeonBatch batch, float x, float y, float w, float h, Color color) {
        drawRectNorm(batch, x, y, w, h, color, 0);
    }

    private static void drawRectNorm(NeonBatch batch, float x, float y, float w, float h, Color color, float rotationDeg) {
        batch.drawRect(x, y, w, h, rotationDeg, 0, color, true);
    }

    private static void drawCircleNorm(NeonBatch batch, float cx, float cy, float r, Color color) {
        batch.drawCircle(cx, cy, r, 0, color, 32, true);
    }

    private static void drawLineNorm(NeonBatch batch, float x1, float y1, float x2, float y2, float width, Color color) {
        batch.drawLine(x1, y1, x2, y2, width, color);
    }
}
