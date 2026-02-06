package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;

public class Dungeon {
	public int width;
	public int height;
	public int level;
	public Tile[][] map;
	public GridPoint2 startPos;

	public Dungeon(int width, int height) {
		this.width = width;
		this.height = height;
		this.level = 1;
		this.map = new Tile[height][width];
		this.startPos = new GridPoint2(0, 0);
		generate();
	}

	public void generate() {
		MapGenerator generator = new MapGenerator(this.width, this.height);
		MapGenerator.GenResult result = generator.generate();
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

	public GridPoint2 getRandomWalkableTile() {
		int attempts = 0;
		while (attempts < 1000) {
			int x = MathUtils.random(this.width - 1);
			int y = MathUtils.random(this.height - 1);
			if (isWalkable(x, y)) {
				return new GridPoint2(x, y);
			}
			attempts++;
		}
		return null;
	}
}
