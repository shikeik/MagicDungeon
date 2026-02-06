package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

public class Dungeon {
	public int width;
	public int height;
	public int level;
	public Tile[][] map;
	public GridPoint2 startPos;
	public long globalSeed;

	public Dungeon(int width, int height, long globalSeed) {
		this.width = width;
		this.height = height;
		this.level = 1;
		this.globalSeed = globalSeed;
		this.map = new Tile[height][width];
		this.startPos = new GridPoint2(0, 0);
		generate();
	}
	
	private long getLevelSeed() {
		// 每一层的种子基于全局种子和层数，使用简单的混合算法防止线性相关
		return globalSeed + (level * 0x9E3779B97F4A7C15L);
	}
	
	public RandomXS128 getMapRNG() {
		return new RandomXS128(getLevelSeed() ^ 0x10000000L);
	}
	
	public RandomXS128 getMonsterRNG() {
		return new RandomXS128(getLevelSeed() ^ 0x20000000L);
	}
	
	public RandomXS128 getItemRNG() {
		return new RandomXS128(getLevelSeed() ^ 0x30000000L);
	}

	public void generate() {
		MapGenerator generator = new MapGenerator(this.width, this.height);
		// Pass hasUpStairs = true if level > 1
		MapGenerator.GenResult result = generator.generate(this.level > 1, getMapRNG());
		this.map = result.grid;
		this.startPos = result.start;
	}

	public Tile getTile(int x, int y) {
		if (x < 0 || x >= this.width || y < 0 || y >= this.height) {
			return null;
		}
		return this.map[y][x];
	}

	public boolean isWalkable(int x, int y) {
		Tile tile = getTile(x, y);
		return tile != null && tile.walkable;
	}

	public GridPoint2 getRandomWalkableTile(RandomXS128 rng) {
		int attempts = 0;
		while (attempts < 1000) {
			int x = rng.nextInt(this.width - 1);
			int y = rng.nextInt(this.height - 1);
			if (isWalkable(x, y)) {
				return new GridPoint2(x, y);
			}
			attempts++;
		}
		return null;
	}
	
	// Keep for backward compatibility or non-seeded random usage if needed
	public GridPoint2 getRandomWalkableTile() {
		return getRandomWalkableTile(new RandomXS128());
	}
}
