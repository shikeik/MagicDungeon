package com.goldsprite.magicdungeon.utils.texturegenerator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTextureFactory;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTextureFactory.HeroConfig;

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
    
    /**
     * 生成 Monster 纹理
     * [Integration] 目前主要支持 Dragon (Boss)，其他类型暂未实现Neon版本
     */
    public static TextureRegion createMonster(String name) {
        // 大小仅影响 FBO 分辨率，不影响绘制逻辑 (都是 0~1)
        int size = 256;
        
        // 只有 Dragon/Boss 实现了 Neon 逻辑
        if (name != null && (name.contains("Dragon") || name.contains("Boss") || name.contains("魔龙"))) {
            return NeonGenerator.getInstance().generate(size, size, batch -> {
                 // [Modification] 切换为黑色魔龙 (Black Dragon)
                 NeonTextureFactory.drawComplexDragon(batch, NeonTextureFactory.DragonPalette.BLACK);
            });
        }
        
        // 其他怪物，目前 Neon 还没有对应实现，
        // 为了避免 TextureManager 逻辑复杂，我们在这里返回 null，
        // 让 TextureManager 决定是否回退到旧生成器。
        // 或者，我们可以暂时借用 SpriteGenerator 的逻辑？
        // 但 SpriteGenerator 是像素操作，Neon 是 FBO 绘制。
        // 最好是在 TextureManager 层面做分发。
        return null;
    }

    private static void drawCharacterImpl(NeonBatch batch, String mainHand, String offHand, String helmet, String armor, String boots) {
        // [Integration] 调用 NeonTextureFactory 的新实现
        String armorType = (armor != null && armor.contains("Plate")) ? "Plate" : "Leather";
        String helmetType = (helmet != null && helmet.contains("Full")) ? "Full" : "Open";
        if (helmet == null) helmetType = "None";
        String mainHandType = (mainHand != null && mainHand.contains("Sword")) ? "Sword" : "None";
        String offHandType = (offHand != null && offHand.contains("Shield")) ? "Shield" : "None";
        String bootsType = (boots != null && boots.contains("Plate")) ? "Plate" : "Leather";
        if (boots == null) bootsType = "None";

        HeroConfig config = new HeroConfig(armorType, helmetType, mainHandType, offHandType, bootsType);
        
        // 自定义一些颜色
        if (armor != null && armor.contains("Gold")) {
            config.primaryColor = Color.GOLD;
        } else if ("Plate".equals(armorType)) {
            config.primaryColor = Color.LIGHT_GRAY;
        } else {
            config.primaryColor = Color.valueOf("#795548");
        }

        NeonTextureFactory.drawComplexHero(batch, config);
    }

    private static void drawRectNorm(NeonBatch batch, float x, float y, float w, float h, Color color) {
        // 直接绘制 0~1 坐标
        batch.drawRect(x, y, w, h, 0, 0, color, true);
    }
}
