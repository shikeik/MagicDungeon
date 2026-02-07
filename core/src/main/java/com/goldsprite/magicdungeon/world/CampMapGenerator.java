package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;

public class CampMapGenerator {
	private static final int WIDTH = 30;
	private static final int HEIGHT = 30;

	public static MapGenerator.GenResult generate() {
		Tile[][] map = new Tile[HEIGHT][WIDTH];

		// Fill with Grass
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				map[y][x] = new Tile(TileType.Grass);
				map[y][x].walkable = true;
			}
		}

		// Draw Trees on Edges
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				if (x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1) {
					map[y][x].type = TileType.Tree;
					map[y][x].walkable = false;
				}
				
				// Add some random trees inside for "foresty" feel, but keep center clear
				if (MathUtils.randomBoolean(0.05f)) {
					// Avoid center area
					if (Math.abs(x - WIDTH/2) > 5 || Math.abs(y - HEIGHT/2) > 5) {
						map[y][x].type = TileType.Tree;
						map[y][x].walkable = false;
					}
				}
			}
		}

		// Add Sand Patches
		for (int i = 0; i < 5; i++) {
			int cx = MathUtils.random(5, WIDTH - 5);
			int cy = MathUtils.random(5, HEIGHT - 5);
			int radius = MathUtils.random(2, 4);
			
			for (int y = cy - radius; y <= cy + radius; y++) {
				for (int x = cx - radius; x <= cx + radius; x++) {
					if (x > 0 && x < WIDTH - 1 && y > 0 && y < HEIGHT - 1) {
						if (Math.sqrt((x-cx)*(x-cx) + (y-cy)*(y-cy)) <= radius) {
							if (map[y][x].type == TileType.Grass) {
								map[y][x].type = TileType.Sand;
							}
						}
					}
				}
			}
		}

		// Place Dungeon Entrance (Top Left)
		// Let's place it at roughly (5, HEIGHT - 5)
		int entX = 5;
		int entY = HEIGHT - 6;
		map[entY][entX].type = TileType.Dungeon_Entrance;
		map[entY][entX].walkable = true;

		// Start Position (Center)
		int startX = WIDTH / 2;
		int startY = HEIGHT / 2;
		GridPoint2 startPos = new GridPoint2(startX, startY);

		// Stone Path from Start to Entrance
		// Simple L-shape or diagonal
		// Let's draw a path
		int currX = startX;
		int currY = startY;
		
		// Move Y first then X
		while(currY < entY) {
			currY++;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		while(currY > entY) { // Should not happen given positions
			currY--;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		
		while(currX > entX) {
			currX--;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		
		// Ensure Start and Entrance are walkable/correct
		map[startY][startX].type = TileType.StonePath; // Start on path
		map[entY][entX].type = TileType.Dungeon_Entrance;

		return new MapGenerator.GenResult(map, startPos);
	}
}
