package com.goldsprite.magicdungeon.utils.texturegenerator;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

import java.util.Random;

/**
 * Neon 高级纹理工厂
 * 专用于生成复杂的、美术级的程序化纹理。
 * 所有绘制均在 0.0 ~ 1.0 的归一化坐标系中进行。
 */
public class NeonTextureFactory {

	// =========================================================================
	// 1. 终极药水 (Omni-Potion)
	// =========================================================================

	public static class PotionPalette {
		public Color liquidCenter, liquidEdge;
		public Color glassDark, glassLight;
		public Color corkDark, corkLight;
		public Color glow;

		// 预设：生命药水
		public static PotionPalette HEALING = new PotionPalette(
			new Color(1.0f, 0.3f, 0.3f, 1f),    // Bright Red
			new Color(0.6f, 0.0f, 0.0f, 1f),    // Dark Red
			new Color(1f, 0.2f, 0.2f, 0.4f)     // Red Glow
		);
		// 预设：法力药水
		public static PotionPalette MANA = new PotionPalette(
			new Color(0.3f, 0.6f, 1.0f, 1f),    // Bright Blue
			new Color(0.0f, 0.1f, 0.6f, 1f),    // Dark Blue
			new Color(0.2f, 0.4f, 1f, 0.4f)     // Blue Glow
		);

		public PotionPalette(Color center, Color edge, Color glowColor) {
			this.liquidCenter = center;
			this.liquidEdge = edge;
			this.glassDark = new Color(1f, 1f, 1f, 0.1f);
			this.glassLight = new Color(1f, 1f, 1f, 0.4f);
			this.corkDark = new Color(0.4f, 0.25f, 0.15f, 1f);
			this.corkLight = new Color(0.6f, 0.4f, 0.25f, 1f);
			this.glow = glowColor;
		}
	}

	public static void drawComplexPotion(NeonBatch batch, PotionPalette palette) {
		float cx = 0.5f;
		float cy = 0.4f; // 瓶身重心偏下
		float radius = 0.3f; // 瓶身半径

		// 1. 背景光晕 (Bloom) - 使用羽化描边
		// Inner: Glow Color, Outer: Transparent
		Color glowOuter = palette.glow.cpy(); glowOuter.a = 0f;
		batch.drawCircleGradientStroke(cx, cy, radius * 0.9f, 0.15f, palette.glow, glowOuter, 64);

		// 2. 瓶颈 (Neck) - 后层
		// 这是一个梯形，连接瓶口和瓶身
		float neckW = 0.12f;
		float neckH = 0.25f;
		float neckY = cy + radius * 0.8f;
		batch.drawVerticalGradientRect(cx - neckW/2, neckY, neckW, neckH, palette.liquidEdge, palette.liquidEdge);

		// 3. 瓶身液体 (Liquid Body) - 球体体积感
		// 使用径向渐变：中心亮 -> 边缘暗
		batch.drawRadialGradientCircle(cx, cy, radius, palette.liquidCenter, palette.liquidEdge, 64);

		// 4. 液体表面 (Meniscus) - 瓶身内部上方
		// 画一个稍微扁一点的椭圆，表示液面
		Color liquidSurface = palette.liquidCenter.cpy().add(0.2f, 0.2f, 0.2f, 0); // 提亮
		batch.drawOval(cx - radius*0.7f, cy + radius*0.4f, radius*1.4f, radius*0.4f, 0, 0, liquidSurface, 32, true);

		// 5. 气泡 (Bubbles) - 随机分布
		Random rnd = new Random(123); // 固定种子保证一致性
		for(int i=0; i<5; i++) {
			float bx = cx + (rnd.nextFloat() - 0.5f) * radius * 1.2f;
			float by = cy + (rnd.nextFloat() - 0.5f) * radius * 1.2f;
			float bSize = 0.015f + rnd.nextFloat() * 0.02f;
			batch.drawCircle(bx, by, bSize, 0, new Color(1,1,1,0.3f), 12, true);
		}

		// 6. 玻璃高光 (Glass Highlights)
		// 左上角主反光 (月牙形)
		batch.drawOval(cx - radius*0.5f, cy + radius*0.4f, 0.1f, 0.15f, 20, 0, new Color(1,1,1,0.5f), 16, true);
		// 右下角反光 (Rim Light) - 细线
		batch.drawArc(cx, cy, radius - 0.01f, 290, 40, 0.01f, new Color(1,1,1,0.4f), 16);

		// 7. 瓶颈玻璃质感 (Neck Glass)
		// 覆盖一层淡淡的白色渐变
		batch.drawHorizontalGradientRect(cx - neckW/2, neckY, neckW, neckH,
			palette.glassLight, palette.glassDark, palette.glassLight);

		// 8. 瓶口环 (Bottle Rim)
		float rimY = neckY + neckH;
		float rimW = neckW * 1.4f;
		float rimH = 0.04f;
		// 绘制一个圆角矩形或椭圆作为瓶口玻璃环
		batch.drawRect(cx - rimW/2, rimY, rimW, rimH, 0, 0, new Color(0.8f, 0.8f, 0.9f, 0.8f), true);

		// 9. 软木塞 (Cork)
		float corkW_bot = neckW * 0.9f;
		float corkW_top = neckW * 1.1f;
		float corkH = 0.08f;
		float corkY = rimY + rimH;

		// 使用多边形画梯形软木塞
		float[] corkPoly = new float[] {
			cx - corkW_bot/2, corkY,           // BL
			cx + corkW_bot/2, corkY,           // BR
			cx + corkW_top/2, corkY + corkH,   // TR
			cx - corkW_top/2, corkY + corkH    // TL
		};
		// 软木塞纹理：水平渐变模拟圆柱
		// 由于 drawPolygon 不支持复杂渐变，我们先画底色，再画纹理
		batch.drawPolygon(corkPoly, 4, 0, palette.corkDark, true);
		// 简单的纹理线条
		batch.drawLine(cx - corkW_bot*0.3f, corkY+corkH*0.3f, cx + corkW_bot*0.2f, corkY+corkH*0.3f, 0.005f, palette.corkLight);
		batch.drawLine(cx - corkW_bot*0.2f, corkY+corkH*0.6f, cx + corkW_bot*0.4f, corkY+corkH*0.6f, 0.005f, palette.corkLight);
	}

	// =========================================================================
	// 2. 远古巨龙 (Elder Dragon) - 复杂半身像
	// =========================================================================

	public static class DragonPalette {
		public Color skinBase, skinDark, skinLight;
		public Color belly, horn, eye, eyeGlow;

		public static DragonPalette FIRE = new DragonPalette(
			Color.valueOf("#d32f2f"), // Skin Red
			Color.valueOf("#8e0000"), // Skin Dark
			Color.valueOf("#ff6659"), // Skin Light
			Color.valueOf("#ffcc80"), // Belly Orange
			Color.valueOf("#424242"), // Horn Black
			Color.YELLOW,             // Eye
			new Color(1f, 0.8f, 0f, 0.6f) // Glow
		);

		public static DragonPalette ICE = new DragonPalette(
			Color.valueOf("#0288d1"), // Skin Blue
			Color.valueOf("#005b9f"),
			Color.valueOf("#5eb8ff"),
			Color.valueOf("#e1f5fe"), // Belly White
			Color.valueOf("#b0bec5"), // Horn Grey
			Color.CYAN,
			new Color(0f, 1f, 1f, 0.6f)
		);

		public DragonPalette(Color base, Color dark, Color light, Color belly, Color horn, Color eye, Color glow) {
			this.skinBase = base; this.skinDark = dark; this.skinLight = light;
			this.belly = belly; this.horn = horn; this.eye = eye; this.eyeGlow = glow;
		}
	}

	public static void drawComplexDragon(NeonBatch batch, DragonPalette p) {
		// 0. 翅膀 (Wings) - 背景层
		// 使用半透明渐变，画出巨大的翼膜
		float wingCy = 0.5f;
		Color wingInner = p.skinBase.cpy().mul(0.8f, 0.8f, 0.8f, 0.9f);
		Color wingOuter = p.skinDark.cpy(); wingOuter.a = 0.2f; // 边缘透明

		// 左翼
		batch.drawGradientTriangle(0.5f, 0.4f, wingInner, 0.0f, 0.9f, wingOuter, 0.3f, 0.2f, wingInner);
		// 右翼
		batch.drawGradientTriangle(0.5f, 0.4f, wingInner, 1.0f, 0.9f, wingOuter, 0.7f, 0.2f, wingInner);
		// 翼骨
		batch.drawLine(0.5f, 0.4f, 0.0f, 0.9f, 0.02f, p.skinDark);
		batch.drawLine(0.5f, 0.4f, 1.0f, 0.9f, 0.02f, p.skinDark);

		// 1. 脖子 (Neck) - S型
		// 使用一系列重叠的圆或矩形来模拟鳞片覆盖的脖子
		float[] neckX = {0.5f, 0.52f, 0.48f, 0.5f};
		float[] neckY = {0.0f, 0.15f, 0.30f, 0.45f};
		float[] neckW = {0.25f, 0.22f, 0.18f, 0.15f};

		for(int i=0; i<neckX.length; i++) {
			// 腹部鳞片 (前侧)
			batch.drawCircle(neckX[i], neckY[i], neckW[i]/2, 0, p.belly, 16, true);
			// 背部鳞片 (后侧覆盖) - 稍微偏移画深色
			batch.drawSector(neckX[i], neckY[i], neckW[i]/2, 0, 180, p.skinBase, 16);
		}

		// 2. 头部 (Head) - 复杂多边形
		float hx = 0.5f, hy = 0.6f;
		// 这种形状很难用基本形拼，我们用多边形定义侧脸轮廓
		float[] headPoly = new float[] {
			hx - 0.05f, hy,         // 脖子连接处
			hx - 0.12f, hy + 0.15f, // 后脑勺
			hx - 0.05f, hy + 0.22f, // 头顶
			hx + 0.15f, hy + 0.18f, // 鼻梁
			hx + 0.20f, hy + 0.10f, // 鼻尖
			hx + 0.12f, hy + 0.05f, // 上颚
			hx + 0.10f, hy - 0.02f, // 下巴尖
			hx - 0.02f, hy - 0.05f  // 下颚骨
		};
		// 绘制头部底色 (带羽化边缘，稍微柔和一点)
		batch.drawSmoothPolygon(headPoly, 8, p.skinBase, 0.005f);

		// 3. 细节：眼睛 (Glowing Eye)
		float eyeX = hx + 0.02f;
		float eyeY = hy + 0.12f;
		// 眼睛发光晕
		Color eyeGlowTransparent = p.eyeGlow.cpy(); eyeGlowTransparent.a = 0f;
		batch.drawCircleGradientStroke(eyeX, eyeY, 0.03f, 0.02f, p.eyeGlow, eyeGlowTransparent, 12);
		// 眼球
		batch.drawPolygon(new float[]{
			eyeX - 0.02f, eyeY,
			eyeX, eyeY + 0.015f,
			eyeX + 0.02f, eyeY
		}, 3, 0, p.eye, true); // 怒目圆睁的三角眼

		// 4. 细节：角 (Horns)
		// 大角 (向后弯曲)
		float hornBaseX = hx - 0.08f;
		float hornBaseY = hy + 0.20f;
		float hornTipX = hx - 0.25f;
		float hornTipY = hy + 0.35f;
		// 使用贝塞尔曲线模拟弯角? 或者简单的渐变三角形
		// 根部深色，尖端浅色
		batch.drawGradientTriangle(
			hornBaseX, hornBaseY, p.horn,
			hornBaseX + 0.04f, hornBaseY - 0.02f, p.horn,
			hornTipX, hornTipY, Color.WHITE // 尖端发白
		);

		// 5. 细节：鼻孔烟雾 (Smoke)
		float noseX = hx + 0.18f;
		float noseY = hy + 0.12f;
		batch.drawCircle(noseX, noseY, 0.008f, 0, Color.BLACK, 6, true);
		// 烟雾粒子
		batch.drawCircle(noseX + 0.02f, noseY + 0.02f, 0.015f, 0, new Color(0.5f, 0.5f, 0.5f, 0.4f), 8, true);
		batch.drawCircle(noseX + 0.04f, noseY + 0.03f, 0.010f, 0, new Color(0.5f, 0.5f, 0.5f, 0.2f), 8, true);

		// 6. 鳞片纹理 (Scales Overlay)
		// 在脸上画几个深色的小三角形增加细节
		batch.drawTriangle(hx+0.05f, hy+0.05f, hx+0.07f, hy+0.06f, hx+0.06f, hy+0.03f, 0, p.skinDark, true);
		batch.drawTriangle(hx-0.02f, hy+0.08f, hx, hy+0.09f, hx-0.01f, hy+0.06f, 0, p.skinDark, true);
	}

	// =========================================================================
	// 3. 传奇勇者 (Legendary Hero) - 模块化
	// =========================================================================

	/**
	 * 勇者装备配置单
	 */
	public static class HeroConfig {
		public Color skinColor = Color.valueOf("#ffccaa");
		public Color hairColor = Color.valueOf("#5d4037");
		public String armorType; // "Plate", "Leather", "Robe"
		public String helmetType; // "Full", "Open", "None"
		public String mainHand;   // "Sword", "Axe", "Staff" (Draw on LEFT side of screen / Right hand)
		public String offHand;    // "Shield", "Dagger", "None" (Draw on RIGHT side of screen / Left hand)
		public Color primaryColor = Color.CYAN; // 装备主色

		public HeroConfig(String armor, String helmet, String main, String off) {
			this.armorType = armor; this.helmetType = helmet; this.mainHand = main; this.offHand = off;
		}
	}

	public static void drawComplexHero(NeonBatch batch, HeroConfig config) {
		float cx = 0.5f;
		float groundY = 0.1f;
		float bodyH = 0.35f;
		float headSize = 0.22f;
		float headY = groundY + bodyH + 0.02f;

		// --- Layer 1: Back Items (比如披风、背后的武器) ---
		// 简单画个披风
		batch.drawTriangle(cx, headY, cx-0.2f, groundY, cx+0.2f, groundY, 0,
			config.primaryColor.cpy().mul(0.6f, 0.6f, 0.6f, 1f), true);

		// --- Layer 2: Main Body (Legs & Torso) ---
		// 腿 (Legs)
		Color legColor = Color.DARK_GRAY; // 裤子
		float legW = 0.08f;
		float legGap = 0.04f;
		// 左腿
		batch.drawRect(cx - legGap/2 - legW, groundY, legW, 0.2f, 0, 0, legColor, true);
		// 右腿
		batch.drawRect(cx + legGap/2, groundY, legW, 0.2f, 0, 0, legColor, true);

		// 鞋子 (Boots)
		Color bootColor = Color.valueOf("#3e2723");
		batch.drawRect(cx - legGap/2 - legW - 0.01f, groundY, legW+0.02f, 0.08f, 0, 0, bootColor, true); // L
		batch.drawRect(cx + legGap/2 - 0.01f, groundY, legW+0.02f, 0.08f, 0, 0, bootColor, true);       // R

		// 躯干 (Torso)
		float bodyW = 0.22f;
		// 基础身体颜色
		batch.drawRect(cx - bodyW/2, groundY + 0.15f, bodyW, 0.25f, 0, 0, config.skinColor, true);

		// --- Layer 3: Armor ---
		if ("Plate".equals(config.armorType)) {
			// 胸甲 (Chestplate) - 渐变金属
			Color metalLight = Color.LIGHT_GRAY;
			Color metalDark = Color.GRAY;
			batch.drawHorizontalGradientRect(cx - bodyW/2 - 0.02f, groundY + 0.18f, bodyW + 0.04f, 0.25f,
				metalDark, metalLight, metalDark);

			// 宝石核心
			batch.drawCircle(cx, groundY + 0.3f, 0.03f, 0, config.primaryColor, 8, true);

			// 肩甲 (Shoulder Pads)
			float shoulderY = groundY + 0.38f;
			batch.drawSector(cx - 0.18f, shoulderY, 0.08f, 0, 180, metalLight, 12); // L
			batch.drawSector(cx + 0.18f, shoulderY, 0.08f, 0, 180, metalLight, 12); // R
		} else if ("Leather".equals(config.armorType)) {
			// 皮甲
			Color leather = Color.valueOf("#795548");
			batch.drawRect(cx - bodyW/2, groundY + 0.18f, bodyW, 0.25f, 0, 0, leather, true);
			// 皮带 (X型)
			batch.drawLine(cx - bodyW/2, groundY + 0.4f, cx + bodyW/2, groundY + 0.2f, 0.02f, Color.BLACK);
			batch.drawLine(cx + bodyW/2, groundY + 0.4f, cx - bodyW/2, groundY + 0.2f, 0.02f, Color.BLACK);
		}

		// --- Layer 4: Head & Helmet ---
		// 脸部 (Face)
		batch.drawRect(cx - headSize/2, headY, headSize, headSize*0.9f, 0, 0, config.skinColor, true);
		// 眼睛
		batch.drawRect(cx - 0.05f, headY + 0.1f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);
		batch.drawRect(cx + 0.03f, headY + 0.1f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);

		if ("Full".equals(config.helmetType)) {
			// 全盔 (Full Helm) - 骑士风格
			Color helmColor = Color.LIGHT_GRAY;
			// 头盔主体
			batch.drawSector(cx, headY + 0.1f, headSize*0.6f, 0, 180, helmColor, 16); // Top dome
			batch.drawRect(cx - headSize*0.6f, headY, headSize*1.2f, 0.1f, 0, 0, helmColor, true); // Jaw guard

			// 视窗 (Visor) - 十字形黑洞
			batch.drawRect(cx - 0.06f, headY + 0.08f, 0.12f, 0.02f, 0, 0, Color.BLACK, true); // Horz
			batch.drawRect(cx - 0.01f, headY + 0.05f, 0.02f, 0.10f, 0, 0, Color.BLACK, true); // Vert

			// 盔缨 (Plume)
			batch.drawTriangle(cx, headY+0.2f, cx-0.1f, headY+0.15f, cx+0.1f, headY+0.35f, 0, config.primaryColor, true);

		} else {
			// 头发 (Hair)
			batch.drawSector(cx, headY + 0.15f, headSize*0.6f, 0, 180, config.hairColor, 16);
			// 刘海
			batch.drawTriangle(cx, headY+0.2f, cx-0.08f, headY+0.12f, cx+0.08f, headY+0.12f, 0, config.hairColor, true);
		}

		// --- Layer 5: Weapons (Front) ---
		// 按照要求：MainHand 在屏幕左边 (角色的右手), OffHand 在屏幕右边 (角色的左手)

		// 主手 (Main Hand) - Sword/Axe
		if (config.mainHand != null) {
			float handX = cx - 0.25f;
			float handY = groundY + 0.25f;
			drawWeapon(batch, config.mainHand, handX, handY, true, config.primaryColor);
		}

		// 副手 (Off Hand) - Shield
		if (config.offHand != null) {
			float handX = cx + 0.25f;
			float handY = groundY + 0.2f;
			drawWeapon(batch, config.offHand, handX, handY, false, config.primaryColor);
		}
	}

	private static void drawWeapon(NeonBatch batch, String type, float x, float y, boolean isRightHand, Color color) {
		if ("Sword".equals(type)) {
			// 剑刃 (Blade) - 渐变
			float[] blade = {
				x - 0.03f, y + 0.1f,
				x + 0.03f, y + 0.1f,
				x, y + 0.45f
			};
			batch.drawGradientTriangle(blade[0], blade[1], Color.GRAY, blade[2], blade[3], Color.GRAY, blade[4], blade[5], Color.WHITE);
			// 护手 (Guard)
			batch.drawRect(x - 0.08f, y + 0.08f, 0.16f, 0.02f, 0, 0, Color.DARK_GRAY, true);
			// 剑柄 (Hilt)
			batch.drawRect(x - 0.02f, y, 0.04f, 0.08f, 0, 0, Color.valueOf("#3e2723"), true);
		}
		else if ("Shield".equals(type)) {
			// 盾牌 (Shield)
			// 鸢盾形状 (Kite Shield)
			float w = 0.15f;
			float h = 0.25f;
			// 盾牌底色
			batch.drawRect(x - w, y - 0.05f, w*2, h*0.7f, 0, 0, color, true);
			// 盾牌尖底
			batch.drawTriangle(x - w, y - 0.05f, x + w, y - 0.05f, x, y - 0.2f, 0, color, true);
			// 边框 (Rim)
			batch.drawRect(x - w, y - 0.05f, w*2, 0.02f, 0, 0, Color.GOLD, true);
			batch.drawTriangle(x - w, y - 0.05f, x + w, y - 0.05f, x, y - 0.2f, 0.02f, Color.GOLD, false); // Stroke
		}
	}
}
