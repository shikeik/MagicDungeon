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
		public static PotionPalette ELIXIR = new PotionPalette(
			new Color(0.8f, 0.2f, 1.0f, 1f), new Color(0.4f, 0.0f, 0.6f, 1f), new Color(0.9f, 0.4f, 1.0f, 0.5f));
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
	// 2. 远古巨龙 (Elder Dragon) - V3.0 正面立坐形象
	// =========================================================================
	public static class DragonPalette {
		public Color skin, belly, horn, glow;
		public static DragonPalette FIRE = new DragonPalette(Color.valueOf("#b71c1c"), Color.valueOf("#ffb74d"), Color.DARK_GRAY, Color.ORANGE);
		public static DragonPalette ICE = new DragonPalette(Color.valueOf("#0277bd"), Color.valueOf("#e1f5fe"), Color.LIGHT_GRAY, Color.CYAN);
		public static DragonPalette BLACK = new DragonPalette(Color.valueOf("#212121"), Color.valueOf("#ffe0b2"), Color.WHITE, Color.RED);
		public DragonPalette(Color s, Color b, Color h, Color g) { skin=s; belly=b; horn=h; glow=g; }
	}

	public static void drawComplexDragon(NeonBatch batch, DragonPalette p) {
		float cx = 0.5f;
		float groundY = 0.1f;

		// --- 1. 尾巴 (Tail) ---
		// 盘在身后/侧面
		float tailX = cx + 0.15f;
		float tailY = groundY + 0.05f;
		batch.drawCircle(tailX, tailY, 0.12f, 0, p.skin, 16, true);
		batch.drawCircle(tailX + 0.1f, tailY + 0.05f, 0.08f, 0, p.skin, 16, true);

		// --- 2. 翅膀 (Wings) ---
		// 在背后展开
		float wingRootY = groundY + 0.35f;
		// 左翼
		drawDragonWing(batch, cx - 0.1f, wingRootY, -1, p.skin.cpy().lerp(Color.BLACK, 0.3f), p.horn);
		// 右翼
		drawDragonWing(batch, cx + 0.1f, wingRootY, 1, p.skin.cpy().lerp(Color.BLACK, 0.3f), p.horn);

		// --- 3. 躯干 (Body) - 正面立坐 ---
		// 梨形身材
		float bodyW = 0.35f;
		float bodyH = 0.45f;
		float bodyY = groundY + 0.05f;
		
		// 腹部 (浅色)
		batch.drawOval(cx - bodyW/2, bodyY, bodyW, bodyH, 0, 0, p.skin, 32, true);
		batch.drawOval(cx - bodyW*0.35f, bodyY + 0.05f, bodyW*0.7f, bodyH*0.8f, 0, 0, p.belly, 32, true);

		// 腹部横纹
		for(int i=0; i<5; i++) {
			float y = bodyY + 0.1f + i * 0.06f;
			float w = bodyW * (0.6f - i * 0.05f);
			batch.drawArc(cx - w/2, y, w, 20, 140, 0.02f, p.skin.cpy().mul(0.9f, 0.9f, 0.9f, 1f), 16);
		}

		// --- 4. 四肢 (Limbs) ---
		// 后腿 (坐姿，在身体两侧下方)
		float legY = groundY;
		float legSize = 0.14f;
		// 左腿
		batch.drawOval(cx - bodyW/2 - 0.02f, legY, legSize, legSize*1.2f, 20, 0, p.skin, 16, true);
		batch.drawRect(cx - bodyW/2 - 0.05f, legY, 0.1f, 0.06f, 0, 0, p.horn, true); // 爪子
		// 右腿
		batch.drawOval(cx + bodyW/2 - legSize + 0.02f, legY, legSize, legSize*1.2f, -20, 0, p.skin, 16, true);
		batch.drawRect(cx + bodyW/2 - 0.05f, legY, 0.1f, 0.06f, 0, 0, p.horn, true); // 爪子

		// 前爪 (放在肚子前)
		float armY = bodyY + bodyH * 0.5f;
		float armX = 0.12f;
		batch.drawCircle(cx - armX, armY, 0.06f, 0, p.skin, 12, true);
		batch.drawCircle(cx + armX, armY, 0.06f, 0, p.skin, 12, true);
		// 爪尖
		batch.drawTriangle(cx - armX - 0.03f, armY, cx - armX + 0.03f, armY, cx - armX, armY - 0.05f, 0, p.horn, true);
		batch.drawTriangle(cx + armX - 0.03f, armY, cx + armX + 0.03f, armY, cx + armX, armY - 0.05f, 0, p.horn, true);

		// --- 5. 头部 (Head) - 正面 ---
		float headY = bodyY + bodyH - 0.05f;
		float headSize = 0.22f;
		
		// 脖子 (短粗)
		batch.drawRect(cx - 0.08f, headY - 0.1f, 0.16f, 0.15f, 0, 0, p.skin, true);

		// 头型 (倒梯形/六边形)
		float[] headPoly = {
			cx - 0.12f, headY + 0.1f, // 左耳根
			cx + 0.12f, headY + 0.1f, // 右耳根
			cx + 0.15f, headY + 0.25f, // 右顶
			cx, headY + 0.3f,         // 头顶中
			cx - 0.15f, headY + 0.25f // 左顶
		};
		// 下颚
		batch.drawRect(cx - 0.1f, headY, 0.2f, 0.15f, 0, 0, p.skin, true);
		batch.drawPolygon(headPoly, 5, 0, p.skin, true);

		// 眼睛 (发光)
		float eyeY = headY + 0.12f;
		batch.drawRect(cx - 0.08f, eyeY, 0.05f, 0.04f, 10, 0, Color.BLACK, true);
		batch.drawRect(cx + 0.03f, eyeY, 0.05f, 0.04f, -10, 0, Color.BLACK, true);
		batch.drawCircle(cx - 0.055f, eyeY + 0.02f, 0.015f, 0, p.glow, 8, true);
		batch.drawCircle(cx + 0.055f, eyeY + 0.02f, 0.015f, 0, p.glow, 8, true);

		// 鼻孔
		batch.drawCircle(cx - 0.03f, headY + 0.05f, 0.008f, 0, Color.BLACK, 6, true);
		batch.drawCircle(cx + 0.03f, headY + 0.05f, 0.008f, 0, Color.BLACK, 6, true);

		// 角 (对称)
		float hornY = headY + 0.25f;
		batch.drawTriangle(cx - 0.1f, hornY, cx - 0.05f, hornY, cx - 0.15f, hornY + 0.15f, 20, p.horn, true);
		batch.drawTriangle(cx + 0.1f, hornY, cx + 0.05f, hornY, cx + 0.15f, hornY + 0.15f, -20, p.horn, true);
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
		public String bootsType;  // "Plate", "Leather", "None"

		public Color primaryColor = Color.GRAY;   // 装备主色
		public Color skinColor = Color.valueOf("#ffccaa");
		public Color hairColor = Color.valueOf("#5d4037");

		public HeroConfig(String armor, String helmet, String main, String off, String boots) {
			this.armorType = armor; this.helmetType = helmet; this.mainHand = main; this.offHand = off; this.bootsType = boots;
		}
		
		// 兼容旧构造函数
		public HeroConfig(String armor, String helmet, String main, String off) {
			this(armor, helmet, main, off, "Leather");
		}
	}

	public static void drawComplexHero(NeonBatch batch, HeroConfig cfg) {
		float cx = 0.5f;
		float footY = 0.1f;

		// 逻辑判定
		boolean isPlate = "Plate".equals(cfg.armorType);
		boolean isFullHelm = "Full".equals(cfg.helmetType);
		boolean hasBoots = cfg.bootsType != null && !"None".equals(cfg.bootsType);

		// --- 1. 腿部 (Legs) ---
		// 板甲腿部更粗壮
		Color legColor = isPlate ? Color.DARK_GRAY : Color.valueOf("#4e342e");
		float legW = isPlate ? 0.09f : 0.07f;
		float legH = 0.25f;

		// 左腿
		batch.drawRect(cx - 0.11f, footY, legW, legH, 0, 0, legColor, true);
		// 右腿
		batch.drawRect(cx + 0.11f - legW, footY, legW, legH, 0, 0, legColor, true);

		// 鞋子 (Boots)
		if (hasBoots) {
			Color bootColor = "Plate".equals(cfg.bootsType) ? cfg.primaryColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f) : Color.valueOf("#3e2723");
			float bootH = 0.12f;
			float bootW = legW + 0.02f;
			// 左鞋
			batch.drawRect(cx - 0.11f - 0.01f, footY, bootW, bootH, 0, 0, bootColor, true);
			batch.drawRect(cx - 0.11f - 0.01f, footY + bootH - 0.02f, bootW, 0.02f, 0, 0, bootColor.cpy().mul(1.2f, 1.2f, 1.2f, 1f), true); // 边缘高光
			// 右鞋
			batch.drawRect(cx + 0.11f - legW - 0.01f, footY, bootW, bootH, 0, 0, bootColor, true);
			batch.drawRect(cx + 0.11f - legW - 0.01f, footY + bootH - 0.02f, bootW, 0.02f, 0, 0, bootColor.cpy().mul(1.2f, 1.2f, 1.2f, 1f), true);
		}

		// 膝盖护甲 (如果是板甲且没有被长靴完全覆盖)
		if (isPlate) {
			float kneeY = footY + 0.14f;
			batch.drawCircle(cx - 0.11f + legW/2, kneeY, legW*0.5f, 0, cfg.primaryColor, 8, true);
			batch.drawCircle(cx + 0.11f - legW/2, kneeY, legW*0.5f, 0, cfg.primaryColor, 8, true);
		}

		// --- 2. 躯干 (Torso) ---
		float bodyY = footY + 0.22f;
		float shoulderW = 0.28f; // 更宽的肩膀
		float waistW = 0.20f;
		float bodyH = 0.28f;

		Color bodyColor = cfg.primaryColor;
		if (!isPlate) bodyColor = Color.valueOf("#795548"); // 皮甲颜色

		// 绘制躯干 (梯形) - 增加渐变效果
		// 使用分层矩形模拟垂直渐变
		int layers = 10;
		for(int i=0; i<layers; i++) {
			float t = i / (float)layers;
			float hSlice = bodyH / layers;
			float ySlice = bodyY + i * hSlice;
			// 简单的梯形插值宽度
			float wSliceBot = MathUtils.lerp(waistW, shoulderW, t);
			float wSliceTop = MathUtils.lerp(waistW, shoulderW, (i+1)/(float)layers);
			// 颜色渐变: 下部暗，上部亮
			Color cSlice = bodyColor.cpy().mul(0.8f + t*0.4f, 0.8f + t*0.4f, 0.8f + t*0.4f, 1f);
			
			batch.drawPolygon(new float[]{
				cx - wSliceBot/2, ySlice,
				cx + wSliceBot/2, ySlice,
				cx + wSliceTop/2, ySlice + hSlice,
				cx - wSliceTop/2, ySlice + hSlice
			}, 4, 0, cSlice, true);
		}

		// 盔甲细节
		if (isPlate) {
			// 胸甲高光
			batch.drawRect(cx - 0.08f, bodyY + 0.12f, 0.16f, 0.1f, 0, 0, Color.WHITE.cpy().mul(1,1,1,0.3f), true);
			// 腹部甲片分层
			batch.drawLine(cx - waistW/2, bodyY + 0.08f, cx + waistW/2, bodyY + 0.08f, 0.01f, Color.BLACK);
		} else {
			// 皮甲细节
			batch.drawLine(cx, bodyY, cx, bodyY + bodyH, 0.01f, Color.valueOf("#3e2723")); // 拉链/扣子
		}

		// 腰带
		batch.drawRect(cx - waistW/2 - 0.02f, bodyY, waistW + 0.04f, 0.05f, 0, 0, Color.valueOf("#3e2723"), true);
		batch.drawRect(cx - 0.03f, bodyY, 0.06f, 0.05f, 0, 0, Color.GOLD, true); // 腰带扣

		// --- 3. 肩甲 (Shoulders) ---
		// 更加厚重的肩甲
		float shY = bodyY + bodyH - 0.04f; // 稍微下移一点，避免悬空
		Color shColor = isPlate ? cfg.primaryColor : Color.valueOf("#5d4037");
		float shSize = isPlate ? 0.13f : 0.09f;

		// 绘制圆形肩甲，带渐变
		batch.drawCircle(cx - 0.18f, shY, shSize, 0, shColor.cpy().mul(0.9f, 0.9f, 0.9f, 1f), 12, true);
		batch.drawCircle(cx + 0.18f, shY, shSize, 0, shColor.cpy().mul(0.9f, 0.9f, 0.9f, 1f), 12, true);
		// 高光
		batch.drawCircle(cx - 0.18f + 0.02f, shY + 0.02f, shSize * 0.5f, 0, Color.WHITE.cpy().mul(1,1,1,0.3f), 8, true);
		batch.drawCircle(cx + 0.18f - 0.02f, shY + 0.02f, shSize * 0.5f, 0, Color.WHITE.cpy().mul(1,1,1,0.3f), 8, true);

		// --- 4. 头部 (Head) ---
		float headY = bodyY + bodyH + 0.01f;
		float headSize = 0.22f; // 头稍微大一点，Q版风格

		if (isFullHelm) {
			// 全盔 (桶盔/大头盔)
			Color helmColor = Color.LIGHT_GRAY;
			Color darkHelm = helmColor.cpy().mul(0.8f, 0.8f, 0.8f, 1f);
			
			// 基础头型 (圆角矩形)
			float hr = headSize / 2;
			// 侧面阴影
			batch.drawRect(cx - hr, headY, headSize, headSize, 0, 0, darkHelm, true);
			batch.drawRect(cx - hr + 0.02f, headY, headSize - 0.04f, headSize, 0, 0, helmColor, true);
			
			batch.drawCircle(cx, headY + headSize, hr, 0, helmColor, 16, true); // 顶部圆弧

			// 视窗 (十字形)
			Color visorColor = Color.BLACK;
			float visorY = headY + 0.08f;
			batch.drawRect(cx - hr * 0.8f, visorY, hr * 1.6f, 0.03f, 0, 0, visorColor, true); // 横向
			batch.drawRect(cx - 0.015f, headY + 0.02f, 0.03f, 0.15f, 0, 0, visorColor, true); // 纵向

			// 装饰羽毛/缨 (可选) - 颜色取反或红色
			batch.drawTriangle(cx, headY + headSize + 0.05f, cx - 0.05f, headY + headSize, cx + 0.1f, headY + headSize + 0.15f, 0, Color.RED, true);
		} else {
			// 露脸样式
			float hr = headSize / 2;
			batch.drawRect(cx - hr, headY, headSize, headSize*0.9f, 0, 0, cfg.skinColor, true);
			// 头发
			batch.drawSector(cx, headY + 0.08f, hr*1.1f, 0, 180, cfg.hairColor, 16);
			// 眼睛
			batch.drawRect(cx - 0.04f, headY + 0.05f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);
			batch.drawRect(cx + 0.04f, headY + 0.05f, 0.02f, 0.02f, 0, 0, Color.BLACK, true);
		}

		// --- 5. 武器 (Weapons) ---
		// 主手 (右) - 屏幕左侧
		if ("Sword".equals(cfg.mainHand)) {
			drawBroadSword(batch, cx - 0.32f, footY + 0.3f);
		}

		// 副手 (左) - 屏幕右侧
		if ("Shield".equals(cfg.offHand)) {
			drawHeraldicShield(batch, cx + 0.30f, footY + 0.25f, isPlate ? cfg.primaryColor : Color.valueOf("#8d6e63"), Color.CYAN);
		} else if ("Dagger".equals(cfg.offHand)) {
			drawDagger(batch, cx + 0.28f, footY + 0.25f);
		}
	}

	// --- Helpers (保持 V2.0 的形状逻辑) ---

	private static void drawDragonWing(NeonBatch batch, float rootX, float rootY, float dir, Color membrane, Color bone) {
		// 简单的蝙蝠翼结构
		// 骨架
		float tipX = rootX + 0.4f * dir;
		float tipY = rootY + 0.3f;
		float midX = rootX + 0.2f * dir;
		float midY = rootY + 0.15f;

		// 绘制皮膜 (多边形)
		// 简化为三角形扇
		batch.drawTriangle(rootX, rootY, midX, midY + 0.2f, tipX, tipY, 0, membrane, true);
		batch.drawTriangle(rootX, rootY, tipX, tipY, midX + 0.05f * dir, midY - 0.1f, 0, membrane, true);

		// 骨骼线
		batch.drawLine(rootX, rootY, midX, midY + 0.2f, 0.02f, bone);
		batch.drawLine(midX, midY + 0.2f, tipX, tipY, 0.015f, bone);
		// 翼指
		batch.drawLine(midX, midY + 0.2f, midX + 0.1f*dir, midY - 0.1f, 0.01f, bone);
	}

	private static void drawBroadSword(NeonBatch b, float x, float y) {
		// 宽刃剑
		float w = 0.06f; // 剑身宽
		float h = 0.35f; // 剑身长

		// 剑身
		b.drawRect(x - w/2, y, w, h, 0, 0, Color.LIGHT_GRAY, true);
		b.drawTriangle(x - w/2, y + h, x + w/2, y + h, x, y + h + 0.08f, 0, Color.LIGHT_GRAY, true); // 剑尖
		b.drawLine(x, y, x, y + h + 0.08f, 0.005f, Color.WHITE); // 血槽/中线

		// 护手
		b.drawRect(x - 0.12f, y, 0.24f, 0.04f, 0, 0, Color.DARK_GRAY, true);

		// 剑柄
		b.drawRect(x - 0.02f, y - 0.08f, 0.04f, 0.08f, 0, 0, Color.valueOf("#4e342e"), true);
		b.drawCircle(x, y - 0.10f, 0.04f, 0, Color.GOLD, 8, true); // 配重球
	}

	private static void drawHeraldicShield(NeonBatch b, float x, float y, Color baseColor, Color emblemColor) {
		float w = 0.16f;
		float h = 0.20f;

		// 盾牌形状 (倒三角 + 上部矩形)
		b.drawRect(x - w, y, w*2, h, 0, 0, baseColor, true);
		b.drawTriangle(x - w, y, x + w, y, x, y - 0.25f, 0, baseColor, true);

		// 边框
		Color border = Color.LIGHT_GRAY;
		float bw = 0.02f;
		b.drawRect(x - w, y, bw, h, 0, 0, border, true); // 左
		b.drawRect(x + w - bw, y, bw, h, 0, 0, border, true); // 右
		b.drawRect(x - w, y + h - bw, w*2, bw, 0, 0, border, true); // 上

		// 纹章 (狮子 -> 抽象)
		// 使用金色绘制一个简单的动物剪影
		Color lionColor = Color.GOLD;
		float lx = x, ly = y - 0.05f;
		// 身体
		b.drawRect(lx - 0.05f, ly, 0.08f, 0.06f, 0, 0, lionColor, true);
		// 头
		b.drawCircle(lx - 0.06f, ly + 0.06f, 0.04f, 0, lionColor, 8, true);
		// 腿
		b.drawRect(lx - 0.06f, ly - 0.04f, 0.02f, 0.04f, 0, 0, lionColor, true);
		b.drawRect(lx + 0.02f, ly - 0.04f, 0.02f, 0.04f, 0, 0, lionColor, true);
		// 尾巴
		b.drawLine(lx + 0.03f, ly + 0.03f, lx + 0.08f, ly + 0.08f, 0.01f, lionColor);
	}

	private static void drawDagger(NeonBatch b, float x, float y) {
		b.drawGradientTriangle(x-0.02f, y+0.15f, Color.GRAY, x+0.02f, y+0.15f, Color.GRAY, x, y, Color.WHITE);
		b.drawRect(x - 0.05f, y + 0.15f, 0.1f, 0.02f, 0, 0, Color.DARK_GRAY, true);
	}
}
