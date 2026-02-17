package com.goldsprite.magicdungeon.utils.texturegenerator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import java.util.Random;

/**
 * Neon 高级纹理工厂 (V2.1 API Fixed)
 * 视觉效果保持 V2.0 的高质量（梯形身材、插值龙脖子、液面修复）。
 * API 接口回滚到支持细粒度配置，以兼容 NeonGenTest。
 */
public class NeonTextureFactory {

	// =========================================================================
	// 1. 终极药水 (Omni-Potion) - V2.0 视觉
	// =========================================================================
	public static class PotionPalette {
		public Color liquidCenter, liquidEdge, glow;
		public static PotionPalette HEALING = new PotionPalette(
			new Color(1.0f, 0.2f, 0.2f, 1f), new Color(0.5f, 0.0f, 0.0f, 1f), new Color(1f, 0.0f, 0.0f, 0.5f));
		public static PotionPalette MANA = new PotionPalette(
			new Color(0.2f, 0.6f, 1.0f, 1f), new Color(0.0f, 0.1f, 0.5f, 1f), new Color(0.0f, 0.5f, 1f, 0.5f));
		public PotionPalette(Color c, Color e, Color g) { liquidCenter=c; liquidEdge=e; glow=g; }
	}

	public static void drawComplexPotion(NeonBatch batch, PotionPalette palette) {
		float cx = 0.5f, cy = 0.45f, radius = 0.32f;
		// 背景
		batch.drawCircle(cx, cy, radius, 0, new Color(0.1f, 0.1f, 0.1f, 0.5f), 32, true);
		// 液体体积
		float liquidR = radius * 0.92f;
		batch.drawRadialGradientCircle(cx, cy, liquidR, palette.liquidCenter, palette.liquidEdge, 48);
		// 内层高光
		batch.drawOval(cx, cy - liquidR * 0.5f, liquidR * 1.2f, liquidR * 0.8f, 0, 0, palette.liquidCenter.cpy().mul(1.2f, 1.2f, 1.2f, 0.4f), 32, true);
		// 液面 (修复透视)
		float surfW = 0.16f, surfH = 0.06f, surfY = cy + radius * 0.6f;
		batch.drawOval(cx - surfW/2, surfY, surfW, surfH, 0, 0, palette.liquidCenter.cpy().add(0.3f, 0.3f, 0.3f, 1f), 16, true);
		// 瓶颈
		float neckW = 0.14f, neckH = 0.25f, neckY = cy + radius * 0.7f;
		batch.drawVerticalGradientRect(cx - neckW/2, neckY, neckW, neckH, new Color(1,1,1,0.1f), new Color(1,1,1,0.3f));
		batch.drawRect(cx - neckW/2, neckY, neckW, neckH, 0, 0.015f, new Color(0.8f, 0.8f, 0.9f, 0.3f), false);
		// 瓶口 & 软木塞
		float rimY = neckY + neckH, rimW = neckW * 1.3f, rimH = 0.05f;
		batch.drawRect(cx - rimW/2, rimY, rimW, rimH, 0, 0, new Color(0.7f, 0.7f, 0.8f, 0.9f), true);
		float corkW = neckW * 0.9f, corkH = 0.12f, corkY = rimY + rimH;
		batch.drawPolygon(new float[]{cx-corkW/2, corkY, cx+corkW/2, corkY, cx+corkW*0.6f, corkY+corkH, cx-corkW*0.6f, corkY+corkH}, 4, 0, Color.valueOf("#795548"), true);
		// 气泡 & 反光
		Random rnd = new Random(111);
		for(int i=0; i<6; i++) batch.drawCircle(cx+(rnd.nextFloat()-0.5f)*0.3f, cy+(rnd.nextFloat()-0.5f)*0.3f, 0.01f+rnd.nextFloat()*0.02f, 0, new Color(1,1,1,0.4f), 8, true);
		batch.drawOval(cx - radius*0.4f, cy + radius*0.3f, 0.08f, 0.12f, 20, 0, new Color(1,1,1,0.6f), 16, true);
	}

	// =========================================================================
	// 2. 远古巨龙 (Elder Dragon) - V2.0 视觉
	// =========================================================================
	public static class DragonPalette {
		public Color skin, belly, horn, glow;
		public static DragonPalette FIRE = new DragonPalette(Color.valueOf("#b71c1c"), Color.valueOf("#ffb74d"), Color.DARK_GRAY, Color.ORANGE);
		public static DragonPalette ICE = new DragonPalette(Color.valueOf("#0277bd"), Color.valueOf("#e1f5fe"), Color.LIGHT_GRAY, Color.CYAN);
		public DragonPalette(Color s, Color b, Color h, Color g) { skin=s; belly=b; horn=h; glow=g; }
	}

	public static void drawComplexDragon(NeonBatch batch, DragonPalette p) {
		// 翅膀
		float wx = 0.5f, wy = 0.4f; Color wCol = p.skin.cpy(); wCol.a = 0.6f;
		batch.drawTriangle(wx, wy, wx-0.4f, wy+0.4f, wx-0.1f, wy+0.1f, 0, wCol, true);
		batch.drawTriangle(wx-0.4f, wy+0.4f, wx-0.5f, wy+0.1f, wx-0.1f, wy+0.1f, 0, wCol, true);
		batch.drawTriangle(wx, wy, wx+0.4f, wy+0.4f, wx+0.1f, wy+0.1f, 0, wCol, true);
		batch.drawTriangle(wx+0.4f, wy+0.4f, wx+0.5f, wy+0.1f, wx+0.1f, wy+0.1f, 0, wCol, true);

		// 身体 & 脖子 (插值平滑)
		float startX = 0.5f, startY = 0.1f, endX = 0.6f, endY = 0.65f;
		int segments = 20;
		for (int i = 0; i <= segments; i++) {
			float t = i / (float)segments;
			float curX = MathUtils.lerp(startX, endX, t) + MathUtils.sin(t * MathUtils.PI) * -0.1f;
			float curY = MathUtils.lerp(startY, endY, t);
			float size = MathUtils.lerp(0.18f, 0.10f, t);
			batch.drawCircle(curX - 0.01f, curY, size/2, 0, p.belly, 12, true);
			batch.drawCircle(curX + 0.01f, curY, size/2, 0, p.skin, 12, true);
		}

		// 头部
		float hx = endX, hy = endY + 0.02f;
		batch.drawCircle(hx, hy, 0.07f, 0, p.skin, 16, true);
		batch.drawRect(hx + 0.02f, hy - 0.04f, 0.12f, 0.06f, 10, 0, p.skin, true); // 吻部
		batch.drawCircleGradientStroke(hx + 0.05f, hy + 0.02f, 0.02f, 0.02f, p.glow, new Color(0,0,0,0), 12); // 发光眼
		float hornX = hx - 0.02f, hornY = hy + 0.05f;
		batch.drawTriangle(hornX, hornY, hornX + 0.03f, hornY, hornX - 0.15f, hornY + 0.15f, 0, p.horn, true); // 角
	}

	// =========================================================================
	// 3. 传奇勇者 (Legendary Hero) - V2.0 视觉 + 修复 API 兼容性
	// =========================================================================

	public static class HeroConfig {
		// [修复] 恢复原有字段，支持细粒度控制
		public String armorType; // "Plate", "Leather", "Robe"
		public String helmetType; // "Full", "Open", "None"
		public String mainHand;   // "Sword", "Axe", "Staff"
		public String offHand;    // "Shield", "Dagger", "None"

		public Color primaryColor = Color.GRAY;   // 装备主色
		public Color skinColor = Color.valueOf("#ffccaa");
		public Color hairColor = Color.valueOf("#5d4037");

		public HeroConfig(String armor, String helmet, String main, String off) {
			this.armorType = armor; this.helmetType = helmet; this.mainHand = main; this.offHand = off;
		}
	}

	public static void drawComplexHero(NeonBatch batch, HeroConfig cfg) {
		float cx = 0.5f;
		float footY = 0.1f;

		// 逻辑判定：是否是重甲/骑士风格
		boolean isPlate = "Plate".equals(cfg.armorType);

		// --- 1. 腿部 (Legs) ---
		// V2.0 梯形腿
		Color pantsColor = isPlate ? Color.DARK_GRAY : Color.valueOf("#4e342e");
		drawTrapezoid(batch, cx - 0.08f, footY, 0.06f, 0.25f, 0.05f, pantsColor); // L
		drawTrapezoid(batch, cx + 0.08f, footY, 0.06f, 0.25f, 0.05f, pantsColor); // R

		// --- 2. 躯干 (Torso) ---
		// V2.0 倒三角身材
		float bodyY = footY + 0.22f;
		float shoulderW = 0.24f, waistW = 0.16f, bodyH = 0.28f;
		Color bodyColor = cfg.primaryColor;
		if ("Leather".equals(cfg.armorType)) bodyColor = Color.valueOf("#795548");

		drawTrapezoidBody(batch, cx, bodyY, waistW, shoulderW, bodyH, bodyColor);

		// 盔甲细节
		if (isPlate) {
			// 胸甲反光
			batch.drawRect(cx - 0.05f, bodyY + 0.1f, 0.1f, 0.12f, 0, 0, Color.LIGHT_GRAY, true);
			// 腰带
			batch.drawRect(cx - waistW/2 - 0.02f, bodyY, waistW + 0.04f, 0.04f, 0, 0, Color.valueOf("#3e2723"), true);
		} else {
			// 皮甲交叉带
			batch.drawLine(cx - 0.08f, bodyY + bodyH, cx + 0.08f, bodyY, 0.02f, Color.valueOf("#3e2723"));
			batch.drawLine(cx + 0.08f, bodyY + bodyH, cx - 0.08f, bodyY, 0.02f, Color.valueOf("#3e2723"));
		}

		// --- 3. 肩甲 (Shoulders) ---
		float shY = bodyY + bodyH - 0.05f;
		Color shColor = isPlate ? cfg.primaryColor : Color.valueOf("#5d4037");
		batch.drawCircle(cx - 0.14f, shY, 0.07f, 0, shColor, 12, true);
		batch.drawCircle(cx + 0.14f, shY, 0.07f, 0, shColor, 12, true);

		// --- 4. 头部 (Head) ---
		float headY = bodyY + bodyH + 0.02f;
		float headSize = 0.18f;

		if ("Full".equals(cfg.helmetType)) {
			// V2.0 全盔样式
			Color helmColor = Color.LIGHT_GRAY;
			batch.drawSector(cx, headY + 0.08f, headSize/2, 0, 180, helmColor, 16);
			// 面甲
			float[] facePoly = { cx - 0.09f, headY + 0.08f, cx + 0.09f, headY + 0.08f, cx + 0.06f, headY - 0.02f, cx - 0.06f, headY - 0.02f };
			batch.drawPolygon(facePoly, 4, 0, helmColor, true);
			// 视窗 & 缨
			batch.drawRect(cx - 0.01f, headY, 0.02f, 0.08f, 0, 0, Color.BLACK, true);
			batch.drawRect(cx - 0.05f, headY + 0.05f, 0.1f, 0.015f, 0, 0, Color.BLACK, true);
			batch.drawTriangle(cx, headY+0.18f, cx-0.05f, headY+0.15f, cx+0.1f, headY+0.25f, 0, cfg.primaryColor, true);
		} else {
			// 露脸样式
			batch.drawRect(cx - headSize/2, headY, headSize, headSize*0.9f, 0, 0, cfg.skinColor, true);
			// 头发/兜帽
			batch.drawSector(cx, headY + 0.05f, headSize*0.6f, 0, 180, cfg.hairColor, 16);
			// 眼睛
			batch.drawRect(cx - 0.04f, headY + 0.05f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);
			batch.drawRect(cx + 0.04f, headY + 0.05f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);
		}

		// --- 5. 武器 (Weapons) ---
		// 主手 (右) - 屏幕左侧
		if ("Sword".equals(cfg.mainHand)) {
			drawSword(batch, cx - 0.28f, footY + 0.3f);
		}

		// 副手 (左) - 屏幕右侧
		if ("Shield".equals(cfg.offHand)) {
			drawShield(batch, cx + 0.28f, footY + 0.25f, isPlate ? cfg.primaryColor : Color.valueOf("#8d6e63"));
		} else if ("Dagger".equals(cfg.offHand)) {
			drawDagger(batch, cx + 0.25f, footY + 0.25f);
		}
	}

	// --- Helpers (保持 V2.0 的形状逻辑) ---

	private static void drawTrapezoid(NeonBatch b, float cx, float by, float wTop, float h, float wBot, Color c) {
		b.drawPolygon(new float[]{cx-wBot/2, by, cx+wBot/2, by, cx+wTop/2, by+h, cx-wTop/2, by+h}, 4, 0, c, true);
	}
	private static void drawTrapezoidBody(NeonBatch b, float cx, float by, float wBot, float wTop, float h, Color c) {
		b.drawPolygon(new float[]{cx-wBot/2, by, cx+wBot/2, by, cx+wTop/2, by+h, cx-wTop/2, by+h}, 4, 0, c, true);
	}
	private static void drawSword(NeonBatch b, float x, float y) {
		b.drawGradientTriangle(x-0.03f, y, Color.GRAY, x+0.03f, y, Color.GRAY, x, y+0.35f, Color.WHITE);
		b.drawRect(x - 0.08f, y, 0.16f, 0.03f, 0, 0, Color.DARK_GRAY, true);
		b.drawRect(x - 0.02f, y - 0.08f, 0.04f, 0.08f, 0, 0, Color.valueOf("#4e342e"), true);
	}
	private static void drawShield(NeonBatch b, float x, float y, Color c) {
		float w = 0.14f;
		b.drawRect(x - w, y, w*2, 0.15f, 0, 0, c, true);
		b.drawTriangle(x - w, y, x + w, y, x, y - 0.2f, 0, c, true);
		b.drawRect(x - 0.02f, y - 0.15f, 0.04f, 0.25f, 0, 0, Color.valueOf("#f5f5f5"), true); // 十字
		b.drawRect(x - 0.1f, y + 0.02f, 0.2f, 0.04f, 0, 0, Color.valueOf("#f5f5f5"), true);
	}
	private static void drawDagger(NeonBatch b, float x, float y) {
		b.drawGradientTriangle(x-0.02f, y+0.15f, Color.GRAY, x+0.02f, y+0.15f, Color.GRAY, x, y, Color.WHITE);
		b.drawRect(x - 0.05f, y + 0.15f, 0.1f, 0.02f, 0, 0, Color.DARK_GRAY, true);
	}
}
