package com.goldsprite.magicdungeon.screens.tests.neondrawer;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTextureFactory;

public class NeonDrawerTestScreen extends GScreen {

	// 普通的 SpriteBatch，用于绘制生成好的纹理
	private SpriteBatch batch;

	// 缓存生成的纹理区域
	private TextureRegion texPotionHp, texPotionMp;
	private TextureRegion texDragonFire, texDragonIce;
	private TextureRegion texHeroPaladin, texHeroRogue;

	@Override
	public void create() {
		batch = new SpriteBatch();

		// =============================================================
		// 1. 程序化生成阶段 (Baking Phase)
		// 这一步在游戏加载时执行一次，生成纹理常驻内存
		// =============================================================

		int texSize = 256; // 定义生成纹理的分辨率 (越高越清晰)

		// --- 生成药水 ---
		texPotionHp = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.drawComplexPotion(b, NeonTextureFactory.PotionPalette.HEALING);
		});

		texPotionMp = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.drawComplexPotion(b, NeonTextureFactory.PotionPalette.MANA);
		});

		// --- 生成巨龙 ---
		texDragonFire = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.drawComplexDragon(b, NeonTextureFactory.DragonPalette.FIRE);
		});

		texDragonIce = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.drawComplexDragon(b, NeonTextureFactory.DragonPalette.ICE);
		});

		// --- 生成勇者 ---
		// 变种 A: 全副武装的金甲圣骑士
		texHeroPaladin = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.HeroConfig config = new NeonTextureFactory.HeroConfig("Plate", "Full", "Sword", "Shield");
			config.primaryColor = Color.GOLD;
			config.skinColor = Color.valueOf("#ffccaa");
			NeonTextureFactory.drawComplexHero(b, config);
		});

		// 变种 B: 轻装皮甲游侠/刺客
		texHeroRogue = NeonGenerator.getInstance().generate(texSize, texSize, b -> {
			NeonTextureFactory.HeroConfig config = new NeonTextureFactory.HeroConfig("Leather", "None", "Sword", "Shield");
			// 注意：HeroConfig 目前的实现里 Sword/Shield 是硬编码绘制的类型
			// 但我们可以通过参数控制有无。这里演示露脸 + 皮甲。
			config.hairColor = Color.valueOf("#8d6e63");
			config.primaryColor = Color.GREEN; // 某种魔法光泽
			NeonTextureFactory.drawComplexHero(b, config);
		});
	}

	@Override
	public void render(float delta) {
		// 深色背景，突出发光效果
		Gdx.gl.glClearColor(0.12f, 0.12f, 0.18f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();

		float startX = 100;
		float startY = 500;
		float gapX = 300;
		float size = 200; // 屏幕显示大小

		// --- 第一排：药水 ---
		batch.setColor(Color.WHITE);
		batch.draw(texPotionHp, startX, startY, size, size);
		batch.draw(texPotionMp, startX + gapX, startY, size, size);

		// --- 第二排：巨龙 ---
		float row2Y = startY - 220;
		batch.draw(texDragonFire, startX, row2Y, size, size);
		batch.draw(texDragonIce, startX + gapX, row2Y, size, size);

		// --- 第三排：勇者 ---
		float row3Y = row2Y - 220;
		batch.draw(texHeroPaladin, startX, row3Y, size, size);
		batch.draw(texHeroRogue, startX + gapX, row3Y, size, size);

		batch.end();
	}

	@Override
	public void dispose() {
		batch.dispose();
		NeonGenerator.getInstance().dispose();
		// 释放生成的纹理
		if (texPotionHp != null) texPotionHp.getTexture().dispose();
		if (texPotionMp != null) texPotionMp.getTexture().dispose();
		if (texDragonFire != null) texDragonFire.getTexture().dispose();
		if (texDragonIce != null) texDragonIce.getTexture().dispose();
		if (texHeroPaladin != null) texHeroPaladin.getTexture().dispose();
		if (texHeroRogue != null) texHeroRogue.getTexture().dispose();
	}
}
