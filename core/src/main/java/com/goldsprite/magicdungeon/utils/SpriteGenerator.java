package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

public class SpriteGenerator {
	// High resolution texture size
	private static final int TEX_SIZE = 256;

	private static Pixmap createPixmap() {
		Pixmap p = new Pixmap(TEX_SIZE, TEX_SIZE, Pixmap.Format.RGBA8888);
		p.setColor(0, 0, 0, 0);
		p.fill();
		return p;
	}

	private static Texture toTexture(Pixmap pixmap) {
		Texture texture = new Texture(pixmap);
		texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		pixmap.dispose();
		return texture;
	}

	// --- Helper Drawing Methods ---

	private static void drawRect(Pixmap p, int x, int y, int w, int h, Color c) {
		p.setColor(c);
		p.fillRectangle(x, y, w, h);
	}

	private static void drawCircle(Pixmap p, int x, int y, int r, Color c) {
		p.setColor(c);
		p.fillCircle(x, y, r);
	}

	// Draw a gradient circle (simulated by concentric circles)
	private static void drawGradientCircle(Pixmap p, int x, int y, int r, Color centerColor, Color edgeColor) {
		for (int i = r; i > 0; i--) {
			float t = (float) i / r;
			p.setColor(edgeColor.cpy().lerp(centerColor, 1 - t));
			p.fillCircle(x, y, i);
		}
	}

	// Draw a diagonal line with thickness
	private static void drawLine(Pixmap p, int x1, int y1, int x2, int y2, int thickness, Color c) {
		p.setColor(c);
		// Simple implementation: draw multiple lines
		for(int i = -thickness/2; i <= thickness/2; i++) {
			p.drawLine(x1 + i, y1, x2 + i, y2);
			p.drawLine(x1, y1 + i, x2, y2 + i);
		}
	}

	// Add noise to the whole pixmap or a region
	private static void applyNoise(Pixmap p, float intensity) {
		// This is slow for 256x256 in Java loop, but acceptable for loading screen
		for (int y = 0; y < TEX_SIZE; y++) {
			for (int x = 0; x < TEX_SIZE; x++) {
				if (Math.random() < 0.3) {
					int pixel = p.getPixel(x, y);
					Color c = new Color(pixel);
					float factor = 1.0f + (float)((Math.random() - 0.5) * intensity);
					c.mul(factor);
					p.setColor(c);
					p.drawPixel(x, y);
				}
			}
		}
	}

	public static Texture createQualityStar() {
		Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		p.setColor(0, 0, 0, 0);
		p.fill();

		p.setColor(Color.WHITE);
		
		// Draw a 4-pointed star
		int c = 32;
		int r = 32;
		int w = 8; // Half width of the star arm at center

		// Vertical arm
		p.fillTriangle(c, c, c - w, c, c, c - r); // Top-Left part
		p.fillTriangle(c, c, c + w, c, c, c - r); // Top-Right part
		p.fillTriangle(c, c, c - w, c, c, c + r); // Bottom-Left part
		p.fillTriangle(c, c, c + w, c, c, c + r); // Bottom-Right part

		// Horizontal arm
		p.fillTriangle(c, c, c, c - w, c - r, c); // Left-Top part
		p.fillTriangle(c, c, c, c + w, c - r, c); // Left-Bottom part
		p.fillTriangle(c, c, c, c - w, c + r, c); // Right-Top part
		p.fillTriangle(c, c, c, c + w, c + r, c); // Right-Bottom part

		Texture t = new Texture(p);
		t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		p.dispose();
		return t;
	}

	// --- Tile Generators ---

	public static Texture createDungeonWallTileset() {
		// 4x4 tiles, 16px each -> 64x64 texture
		Pixmap p = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
		p.setColor(0, 0, 0, 0);
		p.fill();
		
		// Dual Grid Mask to Atlas Mapping (From DualGridDungeonRenderer)
		int[] MASK_TO_ATLAS_X = { -1, 1, 0, 3, 0, 1, 2, 1, 3, 0, 3, 2, 1, 2, 3, 2 };
		int[] MASK_TO_ATLAS_Y = { -1, 3, 0, 0, 2, 0, 3, 1, 3, 1, 2, 0, 2, 2, 1, 1 };

		// Wall Colors
		Color topColor = Color.valueOf("#555555");
		Color topHighlight = Color.valueOf("#666666");
		Color faceColor = Color.valueOf("#3E3E3E"); // Darker for vertical face
		Color faceShadow = Color.valueOf("#2E2E2E");

		// Iterate through all 16 masks
		for (int mask = 0; mask < 16; mask++) {
			int atlasX = MASK_TO_ATLAS_X[mask];
			int atlasY = MASK_TO_ATLAS_Y[mask];
			
			if (atlasX == -1 || atlasY == -1) continue;

			// Target Tile Position in Pixmap
			int tx = atlasX * 16;
			int ty = atlasY * 16;
			
			// Decode Mask: TL, TR, BL, BR
			// Mask bits: TL=8, TR=4, BL=2, BR=1
			boolean tl = (mask & 8) != 0;
			boolean tr = (mask & 4) != 0;
			boolean bl = (mask & 2) != 0;
			boolean br = (mask & 1) != 0;

			// Draw Quadrants (8x8 each)
			
			// --- Top-Left Quadrant ---
			if (tl) {
				drawWallTop(p, tx, ty, 8, 8, topColor, topHighlight);
			}
			
			// --- Top-Right Quadrant ---
			if (tr) {
				drawWallTop(p, tx + 8, ty, 8, 8, topColor, topHighlight);
			}
			
			// --- Bottom-Left Quadrant ---
			if (bl) {
				// Wall Top (Front) obscures everything
				drawWallTop(p, tx, ty + 8, 8, 8, topColor, topHighlight);
			} else {
				// No Wall here. Check if Wall above (TL) projects a face
				if (tl) {
					drawWallFace(p, tx, ty + 8, 8, 8, faceColor, faceShadow);
				}
			}
			
			// --- Bottom-Right Quadrant ---
			if (br) {
				drawWallTop(p, tx + 8, ty + 8, 8, 8, topColor, topHighlight);
			} else {
				// No Wall here. Check if Wall above (TR) projects a face
				if (tr) {
					drawWallFace(p, tx + 8, ty + 8, 8, 8, faceColor, faceShadow);
				}
			}
		}
		
		return toTexture(p);
	}

	private static void drawWallTop(Pixmap p, int x, int y, int w, int h, Color color, Color highlight) {
		p.setColor(color);
		p.fillRectangle(x, y, w, h);
		// Bevel / Detail
		p.setColor(highlight);
		p.drawRectangle(x, y, w, h);
		// Random noise/cracks
		if (MathUtils.randomBoolean(0.1f)) {
			p.setColor(Color.valueOf("#444444"));
			p.drawPixel(x + MathUtils.random(w-1), y + MathUtils.random(h-1));
		}
	}

	private static void drawWallFace(Pixmap p, int x, int y, int w, int h, Color color, Color shadow) {
		p.setColor(color);
		p.fillRectangle(x, y, w, h);
		// Horizontal Brick Lines
		p.setColor(shadow);
		for(int i=0; i<h; i+=4) {
			p.drawLine(x, y+i, x+w, y+i);
		}
		// Vertical Brick Lines (Staggered)
		for(int i=0; i<h; i+=4) {
			int offset = (i % 8 == 0) ? 0 : 4;
			if (offset < w) p.drawPixel(x + offset, y + i + 2); // approximate vertical line pixel
			if (offset + 4 < w) p.drawPixel(x + offset + 4, y + i + 2);
		}
		// Shadow at top (under the overhang of the wall top)
		p.setColor(Color.BLACK);
		p.drawLine(x, y, x+w, y);
	}

	public static Texture createFloor() {
		Pixmap p = createPixmap();

		// Base Stone Color (Warm Grey)
		Color baseColor = Color.valueOf("#3E3E3E");
		Color darkColor = Color.valueOf("#2E2E2E");
		Color highlightColor = Color.valueOf("#4E4E4E");
		
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, darkColor);

		// Large Stone Slabs (irregular grid)
		int rows = 2;
		int cols = 2;
		int slabW = TEX_SIZE / cols;
		int slabH = TEX_SIZE / rows;
		int gap = 4;

		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int x = c * slabW + gap;
				int y = r * slabH + gap;
				int w = slabW - gap * 2;
				int h = slabH - gap * 2;

				// Variation
				float shade = 0.9f + MathUtils.random(0.2f);
				Color slabColor = new Color(baseColor).mul(shade, shade, shade, 1f);

				drawRect(p, x, y, w, h, slabColor);
				
				// Bevel Highlight (Top/Left)
				drawRect(p, x, y, w, 4, highlightColor);
				drawRect(p, x, y, 4, h, highlightColor);
				
				// Cracks
				if (MathUtils.randomBoolean(0.3f)) {
					int cx = x + MathUtils.random(w);
					int cy = y + MathUtils.random(h);
					int len = MathUtils.random(10, 30);
					drawLine(p, cx, cy, cx + len, cy + len, 2, darkColor);
				}
			}
		}

		applyNoise(p, 0.05f);
		return toTexture(p);
	}

	public static Texture createPillar() {
		Pixmap p = createPixmap();
		
		// Transparent Background
		
		// Pillar Dimensions
		int w = 64; // relatively thin
		int h = 180;
		int x = (TEX_SIZE - w) / 2;
		int y = (TEX_SIZE - h) / 2;

		Color stoneColor = Color.valueOf("#555555");
		Color highlight = Color.valueOf("#777777");
		Color shadow = Color.valueOf("#333333");

		// Base
		drawRect(p, x - 10, y + h - 20, w + 20, 20, shadow);
		drawRect(p, x - 5, y + h - 25, w + 10, 5, highlight);

		// Shaft (Cylinder shading)
		// Left side dark, center light, right side mid
		drawRect(p, x, y + 20, w/3, h - 40, shadow);
		drawRect(p, x + w/3, y + 20, w/3, h - 40, stoneColor);
		drawRect(p, x + 2*w/3, y + 20, w/3, h - 40, shadow);
		
		// Capital (Top)
		drawRect(p, x - 10, y, w + 20, 20, highlight);
		drawRect(p, x - 5, y + 20, w + 10, 5, shadow);

		// Cracks / Moss
		applyNoise(p, 0.1f);
		
		return toTexture(p);
	}

	public static Texture createTorch() {
		Pixmap p = createPixmap();
		
		// Torch Holder (Wood/Metal)
		int w = 20;
		int h = 60;
		int x = (TEX_SIZE - w) / 2;
		int y = (TEX_SIZE - h) / 2 + 20;

		drawRect(p, x, y, w, h, Color.valueOf("#4A3B2A")); // Wood
		drawRect(p, x-5, y, w+10, 10, Color.GRAY); // Metal ring top
		
		// Fire
		int fx = TEX_SIZE / 2;
		int fy = y - 20;
		drawGradientCircle(p, fx, fy, 25, Color.YELLOW, Color.ORANGE);
		drawGradientCircle(p, fx, fy - 10, 15, Color.WHITE, Color.YELLOW);

		return toTexture(p);
	}

	public static Texture createWindow() {
		Pixmap p = createPixmap();
		
		// Frame
		int w = 80;
		int h = 100;
		int x = (TEX_SIZE - w) / 2;
		int y = (TEX_SIZE - h) / 2;
		
		Color frameColor = Color.valueOf("#333333");
		drawRect(p, x, y, w, h, frameColor);
		
		// Bars
		drawRect(p, x + w/3, y, 4, h, Color.BLACK);
		drawRect(p, x + 2*w/3, y, 4, h, Color.BLACK);
		
		// Background (Dark Blue/Black)
		drawRect(p, x+4, y+4, w-8, h-8, Color.valueOf("#111122"));

		return toTexture(p);
	}

	public static Texture createStairs(boolean up) {
		Pixmap p = createPixmap();

		// Unify Color: Stone Grey Style for both
		Color stairColor = Color.valueOf("#555555");
		Color stairHighlight = Color.valueOf("#777777");
		Color stairShadow = Color.valueOf("#333333");

		// Max Width: 2/3 of TEX_SIZE (approx 170px)
		int maxWidth = (int)(TEX_SIZE * 0.75f);
		int maxBackWidth = (int)(TEX_SIZE * 0.9f);

		if (!up) {
			// STAIRS DOWN: Trapezoid Perspective
			// Background: Dark Floor
			drawRect(p, (TEX_SIZE - maxBackWidth)/2, 0, maxBackWidth, TEX_SIZE, Color.valueOf("#000000"));

			// Draw stairs from Top (Light) to Bottom (Dark)
			int steps = 8;
			for(int i=0; i<steps; i++) {
				// Interpolate color for depth
				float t = (float)i / (steps-1);
				Color c = stairColor.cpy().lerp(Color.BLACK, t * 0.8f);

				// Width decreases from maxWidth to ~80
				int topW = maxWidth;
				int botW = 80;
				int currentW = (int)(topW - (topW - botW) * t);
				int h = 30 - i * 2;

				int x = (TEX_SIZE - currentW) / 2;
				int y = i * 35 + 10; // Start a bit lower

				drawRect(p, x, y, currentW, h, c);
				// Highlight edge
				drawRect(p, x, y, currentW, 2, c.cpy().add(0.1f, 0.1f, 0.1f, 0));
			}

		} else {
			// STAIRS UP: Pyramid shape
			// Background: Transparent (or simple floor)

			maxWidth = (int)(TEX_SIZE * 0.8f);
			int steps = 7;
			for(int i=0; i<steps; i++) {
				// Width increases from small to maxWidth
				int topW = 120;
				int botW = maxWidth;
				// i=0 is top, i=steps-1 is bottom
				float t = (float)i / (steps-1);
				int width = (int)(topW + (botW - topW) * t);

				int height = 30;
				int x = (TEX_SIZE - width) / 2;
				int y = 40 + i * 30;

				// Step top
				drawRect(p, x, y, width, height, stairColor);
				// Step front highlight
				drawRect(p, x, y, width, 5, stairHighlight);
				// Shadow sides
				drawRect(p, x, y, 5, height, stairShadow);
				drawRect(p, x + width - 5, y, 5, height, stairShadow);
			}
		}

		return toTexture(p);
	}

	// For compatibility if needed, though we should update calls
	public static Texture createStairs() {
		return createStairs(false); // Default to down
	}

	public static Texture createDoor() {
		Pixmap p = createPixmap();

		// Frame
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#3e2723"));
		drawRect(p, 20, 20, TEX_SIZE - 40, TEX_SIZE - 20, Color.valueOf("#5d4037")); // Inner Door

		// Wood Planks
		p.setColor(Color.valueOf("#4e342e"));
		for (int i=1; i<4; i++) {
			int x = 20 + i * ((TEX_SIZE - 40) / 4);
			p.fillRectangle(x, 20, 4, TEX_SIZE - 20);
		}

		// Knob
		drawCircle(p, 40, TEX_SIZE / 2, 12, Color.GOLD);
		drawCircle(p, 40, TEX_SIZE / 2, 8, Color.YELLOW);

		return toTexture(p);
	}

	public static Texture createTree() {
		Pixmap p = createPixmap();
		
		// Grass Background
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#4caf50"));
		
		// Tree Trunk
		drawRect(p, 110, 150, 36, 106, Color.valueOf("#5d4037"));
		
		// Leaves (Cluster of circles)
		Color leaves = Color.valueOf("#2e7d32");
		Color lightLeaves = Color.valueOf("#43a047");
		
		// Bottom Layer
		drawCircle(p, 80, 160, 40, leaves);
		drawCircle(p, 176, 160, 40, leaves);
		drawCircle(p, 128, 160, 45, leaves);
		
		// Middle Layer
		drawCircle(p, 60, 100, 40, leaves);
		drawCircle(p, 196, 100, 40, leaves);
		drawCircle(p, 128, 90, 50, leaves);
		
		// Top Layer
		drawCircle(p, 128, 50, 40, leaves);
		
		// Highlights
		drawCircle(p, 110, 40, 20, lightLeaves);
		drawCircle(p, 150, 80, 20, lightLeaves);
		drawCircle(p, 70, 90, 15, lightLeaves);

		return toTexture(p);
	}

	public static Texture createGrass() {
		Pixmap p = createPixmap();
		
		// Base Green
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#4caf50"));
		
		// Grass blades / Texture
		p.setColor(Color.valueOf("#388e3c"));
		for(int i=0; i<300; i++) {
			int x = MathUtils.random(TEX_SIZE);
			int y = MathUtils.random(TEX_SIZE);
			int h = MathUtils.random(4, 10);
			p.drawLine(x, y, x, y - h);
		}
		
		// Flowers
		for(int i=0; i<10; i++) {
			int x = MathUtils.random(20, TEX_SIZE-20);
			int y = MathUtils.random(20, TEX_SIZE-20);
			Color flowerColor = MathUtils.randomBoolean() ? Color.YELLOW : Color.WHITE;
			drawCircle(p, x, y, 3, flowerColor);
		}

		return toTexture(p);
	}

	public static Texture createSand() {
		Pixmap p = createPixmap();
		
		// Base Sand
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#fff59d"));
		
		// Grain
		p.setColor(Color.valueOf("#fbc02d"));
		for(int i=0; i<500; i++) {
			int x = MathUtils.random(TEX_SIZE);
			int y = MathUtils.random(TEX_SIZE);
			p.drawPixel(x, y);
		}
		
		return toTexture(p);
	}

	public static Texture createStonePath() {
		Pixmap p = createPixmap();
		
		// Base Dirt/Grass
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#795548")); // Brown dirt
		
		// Stones
		Color stoneColor = Color.GRAY;
		Color highlight = Color.LIGHT_GRAY;
		
		for(int i=0; i<10; i++) {
			int w = MathUtils.random(40, 70);
			int h = MathUtils.random(30, 60);
			int x = MathUtils.random(0, TEX_SIZE - w);
			int y = MathUtils.random(0, TEX_SIZE - h);
			
			drawRect(p, x, y, w, h, stoneColor);
			drawRect(p, x+2, y+2, w-4, h-4, highlight);
		}
		
		return toTexture(p);
	}

	public static Texture createDungeonEntrance() {
		Pixmap p = createPixmap();
		
		// Base Grass
		drawRect(p, 0, 0, TEX_SIZE, TEX_SIZE, Color.valueOf("#4caf50"));
		
		// Stone Structure
		drawRect(p, 20, 20, TEX_SIZE-40, TEX_SIZE-40, Color.DARK_GRAY);
		
		// Dark Entrance (Stairs Down)
		drawRect(p, 60, 60, TEX_SIZE-120, TEX_SIZE-100, Color.BLACK);
		
		// Archway / Portal effect
		p.setColor(Color.PURPLE);
		for(int i=0; i<20; i++) {
			int x = MathUtils.random(60, TEX_SIZE-60);
			int y = MathUtils.random(60, TEX_SIZE-60);
			p.fillCircle(x, y, MathUtils.random(2, 5));
		}
		
		return toTexture(p);
	}

	// --- Character Generators ---

	public static Texture createPlayer() {
		return generateCharacterTexture(null, null, null, null, null);
	}

	public static Texture generateCharacterTexture(String mainHand, String offHand, String helmet, String armor, String boots) {
		Pixmap p = createPixmap();

		// Colors
		Color skin = Color.valueOf("#ffccaa");
		Color pantsColor = Color.valueOf("#8d6e63"); // Brown Pants
		
		// 1. Body Base (Legs, Torso, Head, Arms)
		drawBodyBase(p, skin, pantsColor);
		
		// 2. Boots
		if (boots != null) {
			drawBoots(p, boots);
		} else {
			// Default Shoes/Feet
			drawDefaultFeet(p);
		}
		
		// 3. Armor (Body)
		if (armor != null) {
			drawArmor(p, armor);
		} else {
			// Default Tunic
			drawDefaultTunic(p);
		}
		
		// 4. Head / Helmet
		// Draw Face first
		drawFace(p, 128, 68); // Center X, Face Y
		
		if (helmet != null) {
			drawHelmet(p, helmet);
		} else {
			// Default Hair
			drawHair(p);
		}
		
		// 5. Weapons
		if (mainHand != null) {
			// Draw Main Hand (Right Hand -> Screen Left)
			drawWeapon(p, mainHand, true);
		}
		
		if (offHand != null) {
			// Draw Off Hand (Left Hand -> Screen Right)
			drawWeapon(p, offHand, false);
		}

		return toTexture(p);
	}

	private static void drawBodyBase(Pixmap p, Color skin, Color pants) {
		// Legs
		drawRect(p, 90, 180, 25, 60, pants);
		drawRect(p, 141, 180, 25, 60, pants);
		
		// Torso (Skin/Undershirt)
		drawRect(p, 70, 100, 116, 90, skin);
		
		// Arms
		drawRect(p, 40, 100, 25, 70, skin); // Left Arm
		drawRect(p, 191, 100, 25, 70, skin); // Right Arm
		
		// Hands
		drawRect(p, 40, 170, 25, 25, skin); // Left Hand
		drawRect(p, 191, 170, 25, 25, skin); // Right Hand
		
		// Head Base
		int headW = 76;
		int headH = 64;
		int headX = 128 - headW/2;
		int headY = 36;
		drawRect(p, headX, headY, headW, headH, skin);
	}
	
	private static void drawDefaultFeet(Pixmap p) {
		Color shoes = Color.valueOf("#3E2723");
		drawRect(p, 90, 230, 25, 10, shoes);
		drawRect(p, 141, 230, 25, 10, shoes);
	}
	
	private static void drawDefaultTunic(Pixmap p) {
		// Simple Shirt
		Color shirt = Color.valueOf("#a1887f");
		drawRect(p, 70, 100, 116, 90, shirt);
		// Belt
		drawRect(p, 70, 180, 116, 15, Color.valueOf("#3e2723"));
		drawRect(p, 118, 180, 20, 15, Color.GOLD);
	}
	
	private static void drawHair(Pixmap p) {
		Color hair = Color.valueOf("#5d4037");
		int headX = 128 - 38;
		int headY = 36;
		drawRect(p, headX, headY, 76, 20, hair); // Top
		drawRect(p, headX - 5, headY, 10, 50, hair); // Sideburns
		drawRect(p, headX + 71, headY, 10, 50, hair);
	}

	private static void drawFace(Pixmap p, int cx, int cy) {
		// Eyes
		drawRect(p, cx - 20, cy, 12, 12, Color.BLACK);
		drawRect(p, cx + 8, cy, 12, 12, Color.BLACK);
		drawRect(p, cx - 18, cy + 2, 4, 4, Color.WHITE);
		drawRect(p, cx + 10, cy + 2, 4, 4, Color.WHITE);
	}

	private static void drawBoots(Pixmap p, String name) {
		Pixmap itemP = createPixmap();
		drawBootsIcon(itemP, name);
		
		// Icon Boots: ~50x100 each. Y range 80-180.
		// Character Legs: ~25x60 each. Y range 180-240.
		
		// Left Boot
		// Source: x=60, y=80, w=50, h=100
		// Dest: x=88, y=180, w=30, h=60 (Covering leg + foot)
		p.drawPixmap(itemP, 60, 80, 50, 100, 88, 180, 30, 60);
		
		// Right Boot
		// Source: x=140, y=80, w=50, h=100
		// Dest: x=138, y=180, w=30, h=60
		p.drawPixmap(itemP, 140, 80, 50, 100, 138, 180, 30, 60);
		
		itemP.dispose();
	}
	
	private static void drawArmor(Pixmap p, String name) {
		Pixmap itemP = createPixmap();
		drawArmorIcon(itemP, name);
		
		// Character Torso: x=70, y=100, w=116, h=90
		// We want to fit the armor onto this torso.
		// Previous implementation drew full 256x256 shifted by 40y.
		// New implementation: Scale and position to fit torso with offset/stretch control.
		
		// Source Region (Approximate the armor part of the icon)
		// Armor Icon usually fills 60-196 X, 60-190 Y.
		int srcX = 50;
		int srcY = 50;
		int srcW = 156;
		int srcH = 150;
		
		// Target Region (Covering Torso)
		// Center X = 128. Torso W = 116.
		// We make armor slightly wider to cover edges.
		int dstW = 130; 
		int dstH = 110;
		int dstX = 128 - dstW/2;
		int dstY = 90; // Slightly above torso start (100)
		
		// Apply Offset & Stretch (Configuration placeholders)
		int offsetX = 0;
		int offsetY = 0;
		float scaleX = 1.0f;
		float scaleY = 1.0f;
		
		dstX += offsetX;
		dstY += offsetY;
		dstW = (int)(dstW * scaleX);
		dstH = (int)(dstH * scaleY);
		
		p.drawPixmap(itemP, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
		
		itemP.dispose();
	}
	
	private static void drawHelmet(Pixmap p, String name) {
		Pixmap itemP = createPixmap();
		drawHelmetIcon(itemP, name);
		
		// Icon Helmet: Circle centered at 128,128, R=70. Bounds approx 58,58,140,140.
		// Character Head: 128,36. W=76, H=64.
		// We need to scale down the helmet significantly and move it up.
		
		// Source Region (The Helmet Dome + Guard)
		int srcX = 50;
		int srcY = 50;
		int srcW = 156;
		int srcH = 130;
		
		// Dest Region (Covering Head)
		// Head center X=128. Head Top Y=36.
		// Let's target a box slightly larger than the head.
		int dstW = 90;
		int dstH = 80;
		int dstX = 128 - dstW/2;
		int dstY = 10; // Moved up from 20 to 10 (higher on texture/screen?) - Pixmap 0,0 is Top-Left. So 10 is higher.
		
		p.drawPixmap(itemP, srcX, srcY, srcW, srcH, dstX, dstY, dstW, dstH);
		
		itemP.dispose();
	}
	
	private static void drawWeapon(Pixmap p, String name, boolean isMainHand) {
		Pixmap itemP = createPixmap();
		drawItemToPixmap(itemP, name);
		
		// Hand Positions (Adjusted to be closer to body center)
		// Body Hands are at ~52 (Left) and ~203 (Right)
		int handX = isMainHand ? 52 : 203; // Screen X
		int handY = 160; // Moved down from 140
		
		if (name.contains("Shield") || name.contains("盾")) {
			// Shield: Center on hand, scale down
			int size = 96; // Larger shield (was 64)
			// Adjust X to be closer to body to avoid clipping
			int drawX = isMainHand ? handX + 10 : handX - 10;
			p.drawPixmap(itemP, 0, 0, 256, 256, drawX - size/2, handY - size/2, size, size);
		} else {
			// Weapon (Sword, Axe, etc.)
			float scale = 0.6f; // Larger scale (was 0.4f)
			int targetSize = (int)(256 * scale);
			
			// Adjust overlap to pull weapon inward
			int overlap = 30; 
			
			if (isMainHand) {
				// Mirror for Main Hand (Left Side of Screen)
				Pixmap flipped = flipPixmap(itemP);
				
				// Handle in Flipped Image:
				// Original Handle X ~ 60. Flipped X = 196.
				// We want Flipped Handle (196*scale, 200*scale) to align with Hand (handX, handY).
				
				int destX = (int)(handX - 196 * scale) + overlap; // Pull right
				int destY = (int)(handY - 200 * scale);
				
				p.drawPixmap(flipped, 0, 0, 256, 256, destX, destY, targetSize, targetSize);
				flipped.dispose();
			} else {
				// Off Hand (Right Side of Screen)
				// Handle X ~ 60.
				
				int destX = (int)(handX - 60 * scale) - overlap; // Pull left
				int destY = (int)(handY - 200 * scale);
				
				p.drawPixmap(itemP, 0, 0, 256, 256, destX, destY, targetSize, targetSize);
			}
		}
		
		itemP.dispose();
	}

	private static Pixmap flipPixmap(Pixmap src) {
		int w = src.getWidth();
		int h = src.getHeight();
		Pixmap flipped = new Pixmap(w, h, src.getFormat());
		
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				flipped.drawPixel(w - 1 - x, y, src.getPixel(x, y));
			}
		}
		return flipped;
	}

	public static Texture createAvatar() {
		// Just reuse the new generator with no equipment
		return generateCharacterTexture(null, null, null, null, null);
	}

	public static Texture createMonster(String type) {
		Pixmap p = createPixmap();

		if (type.equals("Slime")) {
			// Slime Body
			p.setColor(Color.valueOf("#44aaff"));
			p.fillCircle(128, 140, 100);
			// Fix: Extend rectangle width by 1 pixel to match circle edge perfectly (28+201=229 vs 128+100=228?)
			// Actually circle boundary calculation might be slightly different. Let's add overlap.
			p.fillRectangle(28, 140, 202, 80);

			// Highlight (Glossy)
			p.setColor(Color.valueOf("#88ccff"));
			p.fillCircle(160, 100, 30);

			// Eyes
			drawCircle(p, 90, 130, 20, Color.WHITE);
			drawCircle(p, 166, 130, 20, Color.WHITE);
			drawCircle(p, 90, 130, 8, Color.BLACK);
			drawCircle(p, 166, 130, 8, Color.BLACK);

		} else if (type.equals("Skeleton")) {
			// Skull (Improved)
			int headW = 70;
			int headH = 60;
			int headX = 128 - headW/2;
			int headY = 40;
			drawRect(p, headX, headY, headW, headH, Color.valueOf("#eeeeee"));

			// Jaw
			drawRect(p, headX + 10, headY + headH, headW - 20, 20, Color.valueOf("#dddddd"));
			// Teeth lines
			p.setColor(Color.valueOf("#cccccc"));
			for(int i=1; i<4; i++) {
				drawRect(p, headX + 10 + i*10, headY + headH, 2, 20, Color.valueOf("#cccccc"));
			}

			// Eyes
			drawRect(p, headX + 10, headY + 20, 18, 18, Color.BLACK);
			drawRect(p, headX + headW - 28, headY + 20, 18, 18, Color.BLACK);

			// Spine & Ribs (Detailed)
			// Spine
			drawRect(p, 120, 100, 16, 100, Color.valueOf("#dddddd"));
			
			// Ribs (Curved look via rects)
			for(int i=0; i<4; i++) {
				int ribY = 110 + i*18;
				int ribW = 80 - i*10; // Tapering down
				int ribX = 128 - ribW/2;
				drawRect(p, ribX, ribY, ribW, 8, Color.valueOf("#eeeeee"));
			}
			
			// Arms (Bone segments)
			// Left Arm
			drawRect(p, 80, 110, 10, 40, Color.valueOf("#eeeeee")); // Upper
			drawCircle(p, 85, 155, 6, Color.valueOf("#dddddd")); // Elbow
			drawRect(p, 80, 160, 10, 35, Color.valueOf("#eeeeee")); // Lower
			// Right Arm
			drawRect(p, 166, 110, 10, 40, Color.valueOf("#eeeeee"));
			drawCircle(p, 171, 155, 6, Color.valueOf("#dddddd"));
			drawRect(p, 166, 160, 10, 35, Color.valueOf("#eeeeee"));

			// Pelvis
			drawRect(p, 108, 190, 40, 20, Color.valueOf("#dddddd"));

			// Legs (Femur & Tibia)
			// Left Leg
			drawRect(p, 108, 210, 12, 40, Color.valueOf("#eeeeee")); // Upper
			drawCircle(p, 114, 250, 6, Color.valueOf("#dddddd")); // Knee
			drawRect(p, 108, 250, 10, 40, Color.valueOf("#eeeeee")); // Lower
			// Right Leg
			drawRect(p, 136, 210, 12, 40, Color.valueOf("#eeeeee"));
			drawCircle(p, 142, 250, 6, Color.valueOf("#dddddd"));
			drawRect(p, 136, 250, 10, 40, Color.valueOf("#eeeeee"));
			
			// Feet (Bigger - match player)
			drawRect(p, 100, 280, 25, 15, Color.valueOf("#aaaaaa")); // Left
			drawRect(p, 130, 280, 25, 15, Color.valueOf("#aaaaaa")); // Right

		} else if (type.equals("Orc")) {
			// Orc Warrior (Full Body)
			Color skin = Color.valueOf("#558b2f"); // Olive Green
			Color darkSkin = Color.valueOf("#33691e");
			Color leather = Color.valueOf("#3e2723");
			Color metal = Color.GRAY;

			// Legs
			drawRect(p, 90, 180, 30, 60, leather);
			drawRect(p, 136, 180, 30, 60, leather);
			
			// Body (Muscular)
			drawRect(p, 70, 90, 116, 100, skin);
			// Pecs/Abs definition
			drawRect(p, 80, 100, 45, 30, darkSkin);
			drawRect(p, 131, 100, 45, 30, darkSkin);
			drawRect(p, 90, 140, 30, 20, darkSkin);
			drawRect(p, 136, 140, 30, 20, darkSkin);
			drawRect(p, 90, 165, 30, 20, darkSkin);
			drawRect(p, 136, 165, 30, 20, darkSkin);

			// Arms (Big)
			drawRect(p, 30, 90, 35, 90, skin); // Left
			drawRect(p, 191, 90, 35, 90, skin); // Right
			
			// Armor: Shoulder Pad (One side)
			drawRect(p, 180, 80, 50, 40, metal);
			drawRect(p, 190, 90, 10, 10, Color.RED); // Spike?

			// Belt/Loincloth
			drawRect(p, 70, 180, 116, 30, leather);
			drawRect(p, 118, 180, 20, 30, metal); // Buckle

			// Head
			int headW = 90;
			int headH = 80;
			int headX = 128 - headW/2;
			int headY = 20;
			
			drawRect(p, headX, headY, headW, headH, skin);
			
			// Jaw (Big)
			drawRect(p, headX - 5, headY + 50, headW + 10, 40, skin);
			
			// Tusks
			drawRect(p, headX + 10, headY + 70, 15, 30, Color.valueOf("#fffde7"));
			drawRect(p, headX + headW - 25, headY + 70, 15, 30, Color.valueOf("#fffde7"));
			
			// Eyes (Red, small)
			drawRect(p, headX + 10, headY + 30, 20, 10, Color.RED);
			drawRect(p, headX + headW - 30, headY + 30, 20, 10, Color.RED);

		} else if (type.equals("Bat")) {
			// Wings (Dark Purple/Grey)
			p.setColor(Color.valueOf("#424242"));
			p.fillTriangle(128, 128, 20, 40, 50, 180);
			p.fillTriangle(128, 128, 236, 40, 206, 180);

			// Wing membranes
			p.setColor(Color.valueOf("#616161"));
			p.fillTriangle(128, 128, 30, 50, 60, 170);
			p.fillTriangle(128, 128, 226, 50, 196, 170);

			// Body (Lighter than wings)
			drawCircle(p, 128, 128, 40, Color.valueOf("#757575"));

			// Eyes
			drawCircle(p, 118, 120, 8, Color.YELLOW);
			drawCircle(p, 138, 120, 8, Color.YELLOW);
			drawCircle(p, 118, 120, 3, Color.RED); // Pupil
			drawCircle(p, 138, 120, 3, Color.RED);

		} else if (type.equals("Boss")) {
			// DRAGON (Full Body Boss)
			
			// Wings (Big, back)
			p.setColor(Color.valueOf("#b71c1c")); // Dark Red
			p.fillTriangle(128, 80, 10, 0, 80, 140);
			p.fillTriangle(128, 80, 246, 0, 176, 140);
			
			// Tail (Curved behind)
			p.setColor(Color.valueOf("#d32f2f"));
			// Simple representation: thick line
			drawLine(p, 128, 200, 200, 240, 20, Color.valueOf("#d32f2f"));
			drawLine(p, 200, 240, 240, 200, 10, Color.valueOf("#d32f2f"));

			// Body (Serpentine/Scaly)
			Color bodyColor = Color.valueOf("#d32f2f");
			Color scaleColor = Color.valueOf("#f44336");

			// Main torso (Upright)
			drawRect(p, 80, 80, 96, 140, bodyColor);
			
			// Legs
			drawRect(p, 70, 200, 40, 50, bodyColor);
			drawRect(p, 146, 200, 40, 50, bodyColor);
			// Claws
			drawRect(p, 60, 240, 50, 10, Color.BLACK);
			drawRect(p, 146, 240, 50, 10, Color.BLACK);
			
			// Arms
			drawRect(p, 40, 100, 30, 80, bodyColor);
			drawRect(p, 186, 100, 30, 80, bodyColor);
			// Hands/Claws
			drawCircle(p, 55, 190, 15, bodyColor);
			drawCircle(p, 201, 190, 15, bodyColor);

			// Scales
			p.setColor(scaleColor);
			for(int i=0; i<5; i++) {
				for(int j=0; j<3; j++) {
					p.fillCircle(96 + j*30, 90 + i*25, 10);
				}
			}

			// Belly (Yellowish)
			drawRect(p, 100, 80, 56, 140, Color.valueOf("#ffb300"));
			// Belly lines
			p.setColor(Color.valueOf("#e65100"));
			for(int i=0; i<6; i++) {
				drawRect(p, 100, 90 + i*20, 56, 4, Color.valueOf("#e65100"));
			}

			// Head
			int headY = 20;
			p.setColor(bodyColor);
			p.fillRectangle(70, headY, 116, 80);
			// Snout
			p.fillRectangle(90, headY + 60, 76, 40);

			// Horns (Large)
			p.setColor(Color.GOLD);
			p.fillTriangle(80, headY, 60, headY - 40, 100, headY);
			p.fillTriangle(176, headY, 196, headY - 40, 156, headY);

			// Eyes (Glowing)
			drawRect(p, 80, headY + 20, 20, 15, Color.GREEN);
			drawRect(p, 156, headY + 20, 20, 15, Color.GREEN);
			// Vertical Pupil
			drawRect(p, 88, headY + 20, 4, 15, Color.BLACK);
			drawRect(p, 164, headY + 20, 4, 15, Color.BLACK);
		}

		return toTexture(p);
	}

	public static Texture createItem(String name) {
		Pixmap p = createPixmap();
		drawItemToPixmap(p, name);
		return toTexture(p);
	}

	private static void drawItemToPixmap(Pixmap p, String name) {
		if (name.contains("Potion") || name.contains("Elixir") || name.contains("药水") || name.contains("万能药")) {
			drawPotionIcon(p, name);
		} else if (name.contains("Sword") || name.contains("Blade") || name.contains("剑") || name.contains("刃")) {
			drawSwordIcon(p, name);
		} else if (name.contains("Shield") || name.contains("盾")) {
			drawShieldIcon(p, name);
		} else if (name.contains("Axe") || name.contains("斧")) {
			drawAxeIcon(p, name);
		} else if (name.contains("Wand") || name.contains("Staff") || name.contains("魔杖")) {
			drawWandIcon(p, name);
		} else if (name.contains("Scroll") || name.contains("卷轴") || name.contains("Book") || name.contains("书")) {
			drawScrollIcon(p, name);
		} else if (name.contains("Helmet") || name.contains("Hat") || name.contains("帽") || name.contains("盔")) {
			drawHelmetIcon(p, name);
		} else if (name.contains("Boots") || name.contains("Shoes") || name.contains("靴") || name.contains("鞋")) {
			drawBootsIcon(p, name);
		} else if (name.contains("Necklace") || name.contains("项链")) {
			drawNecklaceIcon(p, name);
		} else if (name.contains("Bracelet") || name.contains("手环")) {
			drawBraceletIcon(p, name);
		} else if (name.contains("Ring") || name.contains("戒指")) {
			drawRingIcon(p, name);
		} else if (name.contains("Coin") || name.contains("金币")) {
			drawCoinIcon(p, name);
		} else if (name.contains("Armor") || name.contains("Mail") || name.contains("甲")) {
			drawArmorIcon(p, name);
		} else {
			drawDefaultIcon(p, name);
		}
	}

	private static void drawPotionIcon(Pixmap p, String name) {
		Color liquid = Color.RED;
		if (name.contains("Mana") || name.contains("魔法")) liquid = Color.BLUE;
		if (name.contains("Elixir") || name.contains("万能")) liquid = Color.PURPLE;

		// Flask Body (Bottom Circle) - Gradient
		drawGradientCircle(p, 128, 170, 70, liquid, liquid.cpy().mul(0.6f));

		// Neck
		drawRect(p, 108, 60, 40, 110, liquid.cpy().mul(0.8f));
		// Rim
		drawRect(p, 100, 50, 56, 15, Color.valueOf("#cccccc"));
		// Cork
		drawRect(p, 113, 30, 30, 20, Color.valueOf("#8d6e63"));

		// Bubbles
		p.setColor(new Color(1f, 1f, 1f, 0.3f));
		for(int i=0; i<5; i++) {
			int bx = 100 + (int)(Math.random()*56);
			int by = 130 + (int)(Math.random()*80);
			p.fillCircle(bx, by, 3 + (int)(Math.random()*4));
		}

		// Highlight / Reflection
		p.setColor(new Color(1f, 1f, 1f, 0.4f));
		p.fillRectangle(140, 70, 10, 80);
		p.fillCircle(150, 150, 10);
	}

	private static void drawSwordIcon(Pixmap p, String name) {
		// Blade Color
		Color bladeColor = Color.LIGHT_GRAY;
		if (name.contains("Gold") || name.contains("Legendary") || name.contains("传奇")) bladeColor = Color.GOLD;
		if (name.contains("Rusty") || name.contains("生锈")) bladeColor = Color.valueOf("#8d6e63");

		// Broadsword (Wide)
		// Handle
		drawLine(p, 60, 200, 85, 175, 16, Color.valueOf("#3e2723"));
		drawCircle(p, 55, 205, 12, Color.GOLD);

		// Guard
		drawLine(p, 65, 155, 105, 195, 20, Color.valueOf("#5d4037"));
		// Guard Ornaments
		drawCircle(p, 65, 155, 10, Color.GOLD);
		drawCircle(p, 105, 195, 10, Color.GOLD);

		// Blade (Wider: 40px)
		int bladeWidth = 40;
		drawLine(p, 90, 170, 210, 50, bladeWidth, bladeColor);

		// Blade Edge (Lighter)
		drawLine(p, 90, 170, 210, 50, bladeWidth - 8, bladeColor.cpy().mul(1.2f));

		// Blood Groove (Darker Center)
		drawLine(p, 95, 165, 205, 55, 6, bladeColor.cpy().mul(0.7f));

		// Tip
		p.setColor(bladeColor);
		// Draw a triangle at tip roughly
		// Since we can't draw rotated triangle easily, just use lines to taper
		drawLine(p, 210, 50, 225, 35, 20, bladeColor);
		drawLine(p, 210, 50, 230, 30, 10, bladeColor);
	}

	private static void drawShieldIcon(Pixmap p, String name) {
		boolean isIron = name.contains("Iron") || name.contains("铁");
		Color c = isIron ? Color.GRAY : Color.valueOf("#5d4037");
		Color border = isIron ? Color.LIGHT_GRAY : Color.valueOf("#8d6e63"); // Wood trim or Iron trim

		// Main body
		p.setColor(c);
		p.fillRectangle(50, 50, 156, 120);
		p.fillCircle(128, 170, 78);

		// Texture
		if (!isIron) {
			// Wood Grain
			p.setColor(Color.valueOf("#4e342e"));
			for(int i=1; i<5; i++) {
				drawRect(p, 50 + i*30, 50, 4, 180, Color.valueOf("#3e2723"));
			}
		} else {
			// Metal Shine
			p.setColor(Color.WHITE);
			p.setBlending(Pixmap.Blending.SourceOver);
			drawLine(p, 60, 60, 100, 100, 10, new Color(1,1,1,0.2f));
		}

		// Border (Trim)
		// Use lines to simulate border
		int bThick = 10;
		// Top
		drawRect(p, 45, 45, 166, bThick, border);
		// Sides
		drawRect(p, 45, 45, bThick, 140, border);
		drawRect(p, 201, 45, bThick, 140, border);
		// Bottom Curve Border (Approximate with circles)
		drawCircle(p, 128, 230, 10, border);
		drawCircle(p, 80, 210, 10, border);
		drawCircle(p, 176, 210, 10, border);

		// Center Boss / Emblem
		drawGradientCircle(p, 128, 120, 30, border, c);
		// Rivets
		p.setColor(Color.GOLD);
		drawCircle(p, 60, 60, 6, Color.GOLD);
		drawCircle(p, 196, 60, 6, Color.GOLD);
		drawCircle(p, 60, 160, 6, Color.GOLD);
		drawCircle(p, 196, 160, 6, Color.GOLD);
	}

	private static void drawAxeIcon(Pixmap p, String name) {
		 // Battle Axe (Double Bit)
		 // Handle
		 drawLine(p, 180, 40, 60, 200, 16, Color.valueOf("#3e2723"));
		 // Grip
		 for(int i=0; i<3; i++) {
			 drawRect(p, 65+i*20, 175-i*25, 20, 10, Color.valueOf("#5d4037"));
		 }

		 // Axe Head Center
		 int cx = 165;
		 int cy = 60;
		 Color metal = Color.LIGHT_GRAY;
		 Color darkMetal = Color.GRAY;
		 Color edge = Color.WHITE;

		 // Central Block
		 drawRect(p, cx - 15, cy - 25, 30, 50, darkMetal);
		 drawCircle(p, cx, cy, 8, Color.BLACK); // Bolt

		 // Left Blade (Curved)
		 // Draw using triangles/rects to simulate curve
		 p.setColor(metal);
		 p.fillTriangle(cx - 15, cy - 15, cx - 15, cy + 15, cx - 70, cy); 
		 p.fillTriangle(cx - 15, cy - 25, cx - 60, cy - 40, cx - 15, cy);
		 p.fillTriangle(cx - 15, cy + 25, cx - 60, cy + 40, cx - 15, cy);
		 // Edge
		 p.setColor(edge);
		 p.fillTriangle(cx - 70, cy, cx - 60, cy - 40, cx - 65, cy - 45); // Top tip
		 p.fillTriangle(cx - 70, cy, cx - 60, cy + 40, cx - 65, cy + 45); // Bottom tip
		 drawLine(p, cx - 60, cy - 40, cx - 60, cy + 40, 6, edge);

		 // Right Blade (Mirror)
		 p.setColor(metal);
		 p.fillTriangle(cx + 15, cy - 15, cx + 15, cy + 15, cx + 70, cy);
		 p.fillTriangle(cx + 15, cy - 25, cx + 60, cy - 40, cx + 15, cy);
		 p.fillTriangle(cx + 15, cy + 25, cx + 60, cy + 40, cx + 15, cy);
		 // Edge
		 p.setColor(edge);
		 p.fillTriangle(cx + 70, cy, cx + 60, cy - 40, cx + 65, cy - 45);
		 p.fillTriangle(cx + 70, cy, cx + 60, cy + 40, cx + 65, cy + 45);
		 drawLine(p, cx + 60, cy - 40, cx + 60, cy + 40, 6, edge);

		 // Top Spike
		 p.setColor(darkMetal);
		 p.fillTriangle(cx - 10, cy - 25, cx + 10, cy - 25, cx, cy - 50);
	}

	private static void drawWandIcon(Pixmap p, String name) {
		 // Fancy Staff (Improved)
		 // Shaft (Twisted/Gnarled wood)
		 p.setColor(Color.valueOf("#5d4037"));
		 // Draw overlapping circles to simulate twist
		 for(int i=0; i<20; i++) {
			 drawCircle(p, 80 + i*5, 200 - i*7, 8, Color.valueOf("#5d4037"));
		 }
		 
		 // Magic Aura (Outer Glow)
		 p.setBlending(Pixmap.Blending.SourceOver);
		 drawGradientCircle(p, 180, 60, 60, new Color(0, 1, 1, 0), new Color(0, 1, 1, 0.3f));

		 // Tassels / Vines
		 p.setColor(Color.valueOf("#2e7d32")); // Vines
		 for(int i=0; i<10; i++) {
			 drawCircle(p, 100 + i*8 + (int)(Math.sin(i)*5), 180 - i*10, 4, Color.valueOf("#2e7d32"));
		 }

		 // Magic Core (Crystal)
		 // Center at 180, 60
		 // Octagon shape
		 p.setColor(Color.CYAN);
		 p.fillCircle(180, 60, 25);
		 p.setColor(Color.WHITE);
		 p.fillCircle(180, 60, 15);
		 p.setColor(Color.CYAN);
		 p.fillCircle(180, 60, 10);

		 // Inner Glow
		 p.setColor(new Color(0.5f, 1f, 1f, 0.8f));
		 p.fillCircle(175, 55, 5);

		 // Prongs holding the gem (Gold)
		 p.setColor(Color.GOLD);
		 drawLine(p, 160, 80, 170, 70, 6, Color.GOLD);
		 drawLine(p, 200, 80, 190, 70, 6, Color.GOLD);
		 drawLine(p, 160, 40, 170, 50, 6, Color.GOLD);
		 drawLine(p, 200, 40, 190, 50, 6, Color.GOLD);
		 
		 // Particles
		 for(int i=0; i<10; i++) {
			 int px = 180 + (int)(Math.random()*60 - 30);
			 int py = 60 + (int)(Math.random()*60 - 30);
			 p.setColor(new Color(0, 1, 1, 0.5f));
			 p.fillCircle(px, py, 2);
		 }
	}

	private static void drawScrollIcon(Pixmap p, String name) {
		 // Open Book / Flipping Book
		 // Left Page
		 p.setColor(Color.valueOf("#fff9c4")); // Paper
		 // Skewed rect for perspective?
		 // Simple: Rect
		 drawRect(p, 40, 80, 88, 100, Color.valueOf("#fff9c4"));
		 // Right Page
		 drawRect(p, 128, 80, 88, 100, Color.valueOf("#fff9c4"));

		 // Spine
		 drawRect(p, 126, 80, 4, 100, Color.valueOf("#5d4037"));

		 // Text/Runes
		 p.setColor(Color.BLACK);
		 for(int i=0; i<6; i++) {
			 drawLine(p, 50, 90 + i*12, 110, 90 + i*12, 2, Color.BLACK);
			 drawLine(p, 140, 90 + i*12, 200, 90 + i*12, 2, Color.BLACK);
		 }

		 // Flipping Page (Top Right)
		 p.setColor(Color.valueOf("#fff59d"));
		 p.fillTriangle(216, 80, 180, 80, 216, 110);

		 // Cover
		 drawRect(p, 30, 70, 196, 10, Color.valueOf("#3e2723")); // Top cover edge (back)
		 drawRect(p, 30, 180, 196, 10, Color.valueOf("#3e2723")); // Bottom cover edge
	}

	private static void drawHelmetIcon(Pixmap p, String name) {
		// Helmet
		boolean isIron = name.contains("Iron") || name.contains("铁");
		Color c = isIron ? Color.LIGHT_GRAY : Color.valueOf("#5d4037");
		Color trim = isIron ? Color.GRAY : Color.valueOf("#3e2723");

		// Dome
		p.setColor(c);
		p.fillCircle(128, 128, 70);
		// Bottom cut
		p.setColor(new Color(0,0,0,0));
		p.setBlending(Pixmap.Blending.None);
		p.fillRectangle(50, 160, 156, 60);
		p.setBlending(Pixmap.Blending.SourceOver);
		
		// Rim/Guard
		drawRect(p, 50, 150, 156, 20, trim);
		
		// Top Spike or Decoration
		drawRect(p, 123, 40, 10, 30, trim);
		
		if(isIron) {
			// Visor slit
			drawRect(p, 126, 100, 4, 50, Color.BLACK);
			drawRect(p, 90, 120, 76, 4, Color.BLACK);
		}
	}

	private static void drawBootsIcon(Pixmap p, String name) {
		// Boots Pair
		Color c = name.contains("Iron") || name.contains("铁") ? Color.GRAY : Color.valueOf("#5d4037");
		Color trim = name.contains("Iron") ? Color.LIGHT_GRAY : Color.valueOf("#3e2723");

		// Left Boot (Left side of icon) - Points Left (Outward)
		drawRect(p, 60, 80, 50, 100, c); // Leg
		drawRect(p, 40, 160, 70, 40, c); // Foot (Moved left to point outward)
		drawRect(p, 60, 80, 50, 10, trim); // Top Trim
		
		// Right Boot (Right side of icon) - Points Right (Outward)
		drawRect(p, 140, 80, 50, 100, c);
		drawRect(p, 140, 160, 70, 40, c); // Foot (Points right)
		drawRect(p, 140, 80, 50, 10, trim);
	}

	private static void drawNecklaceIcon(Pixmap p, String name) {
		// Chain
		p.setColor(Color.GOLD);
		drawCircle(p, 128, 100, 70, Color.GOLD);
		p.setBlending(Pixmap.Blending.None);
		p.setColor(0,0,0,0);
		p.fillCircle(128, 100, 64);
		p.setBlending(Pixmap.Blending.SourceOver);

		// Pendant
		Color gemColor = Color.RED;
		if(name.contains("Blue") || name.contains("蓝")) gemColor = Color.BLUE;
		if(name.contains("Green") || name.contains("绿")) gemColor = Color.GREEN;
		
		drawRect(p, 118, 170, 20, 30, Color.GOLD); // Setting
		drawCircle(p, 128, 185, 12, gemColor); // Gem
	}

	private static void drawBraceletIcon(Pixmap p, String name) {
		// Bracelet
		p.setColor(Color.GOLD);
		p.fillCircle(128, 128, 60);
		p.setBlending(Pixmap.Blending.None);
		p.setColor(0,0,0,0);
		p.fillCircle(128, 128, 45);
		p.setBlending(Pixmap.Blending.SourceOver);
		
		// Decoration
		drawCircle(p, 128, 68, 8, Color.RED);
		drawCircle(p, 128, 188, 8, Color.RED);
		drawCircle(p, 68, 128, 8, Color.RED);
		drawCircle(p, 188, 128, 8, Color.RED);
	}

	private static void drawRingIcon(Pixmap p, String name) {
		 boolean isPower = name.contains("Power") || name.contains("力量");
		 // Ring Band
		Color bandColor = isPower ? Color.GOLD : Color.LIGHT_GRAY;

		// Draw Donut
		 p.setColor(bandColor);
		 p.fillCircle(128, 128, 50);
		 // Inner hole (Clear)
		 p.setBlending(Pixmap.Blending.None);
		 p.setColor(0,0,0,0);
		 p.fillCircle(128, 128, 35);
		 p.setBlending(Pixmap.Blending.SourceOver);

		 // Gem / Setting
		 if (isPower) {
			 // Red Gem, Square Cut
			 drawRect(p, 110, 60, 36, 36, Color.RED);
			 drawRect(p, 115, 65, 26, 26, Color.FIREBRICK);
			 // Claws
			 p.setColor(Color.GOLD);
			 drawCircle(p, 108, 58, 4, Color.GOLD);
			 drawCircle(p, 148, 58, 4, Color.GOLD);
			 drawCircle(p, 108, 98, 4, Color.GOLD);
			 drawCircle(p, 148, 98, 4, Color.GOLD);
		 } else {
			 // Blue Gem, Oval Cut (Defense)
			 drawGradientCircle(p, 128, 78, 18, Color.CYAN, Color.BLUE);
			 // Silver Setting
			 drawRect(p, 120, 120, 16, 20, Color.LIGHT_GRAY); // Connection
		 }
	}

	private static void drawCoinIcon(Pixmap p, String name) {
		// Multi-textured Gold Coin
		// Outer Rim
		drawGradientCircle(p, 128, 128, 80, Color.ORANGE, Color.GOLD);
		// Inner Recess
		drawGradientCircle(p, 128, 128, 60, Color.GOLD, Color.YELLOW);

		// Relief Profile (Head?)
		p.setColor(Color.ORANGE);
		p.fillCircle(128, 128, 30);

		// Shine
		p.setColor(new Color(1f, 1f, 1f, 0.6f));
		p.fillCircle(110, 110, 10);
		drawLine(p, 100, 100, 160, 160, 4, new Color(1,1,1,0.3f)); // Sparkle line
	}

	private static void drawArmorIcon(Pixmap p, String name) {
		boolean isLeather = name.contains("Leather") || name.contains("皮");
		Color base = isLeather ? Color.valueOf("#8d6e63") : Color.LIGHT_GRAY;
		Color trim = isLeather ? Color.valueOf("#5d4037") : Color.GRAY;
		Color highlight = isLeather ? base.cpy().mul(1.2f) : Color.WHITE;
		Color shadow = isLeather ? base.cpy().mul(0.8f) : Color.GRAY;

		// Base Shape (Torso) - Tapered
		// Instead of a box, draw a breastplate shape
		// Main body background
		// drawRect(p, 64, 64, 128, 128, shadow); // Removed simple rect

		if (isLeather) {
			// Leather Armor (Studded)
			// Chest
			drawRect(p, 70, 60, 116, 80, base);
			// Stomach (Segmented)
			drawRect(p, 75, 145, 106, 25, base);
			drawRect(p, 75, 175, 106, 20, base);
			
			// Straps / Belts
			drawRect(p, 60, 100, 136, 10, trim); // Chest strap
			drawRect(p, 120, 60, 16, 140, trim); // Center vertical strap
			
			// Shoulders (Rounded)
			drawCircle(p, 60, 70, 25, trim);
			drawCircle(p, 196, 70, 25, trim);
			
			// Studs (Gold/Brass)
			p.setColor(Color.valueOf("#ffd54f"));
			for(int i=0; i<3; i++) {
				for(int j=0; j<3; j++) {
					if ((i+j)%2==0)
						drawCircle(p, 80 + j*48, 80 + i*40, 5, Color.valueOf("#ffd54f"));
				}
			}
			
		} else {
			// Plate Mail (Segmented)
			// Gorget (Neck)
			drawRect(p, 100, 50, 56, 20, trim);
			
			// Breastplate
			drawRect(p, 60, 70, 136, 70, base);
			// Highlight curve
			p.setColor(highlight);
			p.fillTriangle(60, 70, 196, 70, 128, 140);
			
			// Plackart (Stomach plates) - Overlapping
			drawRect(p, 70, 145, 116, 20, base);
			drawRect(p, 75, 165, 106, 20, base);
			drawRect(p, 80, 185, 96, 20, base);
			
			// Pauldrons (Shoulders) - Large, layered
			// Left
			drawCircle(p, 50, 70, 30, trim);
			drawCircle(p, 50, 70, 20, base);
			// Right
			drawCircle(p, 206, 70, 30, trim);
			drawCircle(p, 206, 70, 20, base);
			
			// Trim / Edges
			drawRect(p, 126, 70, 4, 70, trim); // Center line
			// Rivets
			p.setColor(Color.LIGHT_GRAY);
			drawCircle(p, 70, 80, 4, Color.LIGHT_GRAY);
			drawCircle(p, 186, 80, 4, Color.LIGHT_GRAY);
		}
	}

	private static void drawDefaultIcon(Pixmap p, String name) {
		// Default Box
		drawRect(p, 64, 64, 128, 128, Color.MAGENTA);
		drawRect(p, 80, 80, 96, 96, Color.valueOf("#aa00aa"));
		// Question Mark
		drawRect(p, 120, 100, 16, 40, Color.WHITE);
		drawRect(p, 120, 150, 16, 16, Color.WHITE);
	}
}
