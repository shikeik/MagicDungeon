package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;

public class CampMapGenerator {
	private static final int WIDTH = 30;
	private static final int HEIGHT = 30;

	public static MapGenerator.GenResult generate() {
		Tile[][] map = new Tile[HEIGHT][WIDTH];

		// 填充草地
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				map[y][x] = new Tile(TileType.Grass);
				map[y][x].walkable = true;
			}
		}

		// 在边缘绘制树木
		for (int y = 0; y < HEIGHT; y++) {
			for (int x = 0; x < WIDTH; x++) {
				if (x == 0 || x == WIDTH - 1 || y == 0 || y == HEIGHT - 1) {
					map[y][x].type = TileType.Tree;
					map[y][x].walkable = false;
				}
				
				// 在内部添加一些随机树木以营造“森林”感，但保持中心和路径畅通
				if (MathUtils.randomBoolean(0.05f)) {
					// 避开中心区域
					if (Math.abs(x - WIDTH/2) > 5 || Math.abs(y - HEIGHT/2) > 5) {
						// 同时避开入口周围区域
						if (Math.abs(x - 5) > 3 || Math.abs(y - (HEIGHT - 6)) > 3) {
							map[y][x].type = TileType.Tree;
							map[y][x].walkable = false;
						}
					}
				}
			}
		}

		// 添加沙地斑块
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

		// 放置地牢入口 (左上)
		// 放在大约 (5, HEIGHT - 5)
		int entX = 5;
		int entY = HEIGHT - 6;
		map[entY][entX].type = TileType.Dungeon_Entrance;
		map[entY][entX].walkable = true;

		// 起始位置 (中心)
		int startX = WIDTH / 2;
		int startY = HEIGHT / 2;
		GridPoint2 startPos = new GridPoint2(startX, startY);

		// 从起点到入口的石路
		// 简单的 L 形或对角线
		// 绘制路径
		int currX = startX;
		int currY = startY;
		
		// 先移动 Y 轴，再移动 X 轴
		while(currY < entY) {
			currY++;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		while(currY > entY) { // 给定位置应该不会发生
			currY--;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		
		while(currX > entX) {
			currX--;
			if (map[currY][currX].type != TileType.Dungeon_Entrance)
				map[currY][currX].type = TileType.StonePath;
		}
		
		// 确保起点和入口可行走且正确
		map[startY][startX].type = TileType.StonePath; // 起点在路上
		map[entY][entX].type = TileType.Dungeon_Entrance;
		map[startY][startX].walkable = true;
		map[entY][entX].walkable = true;
		
		// 确保路径可行走（覆盖后来生成的任何树木）
		currX = startX;
		currY = startY;
		
		// 重新追踪路径并清除树木
		while(currY < entY) {
			currY++;
			map[currY][currX].walkable = true;
			if (map[currY][currX].type == TileType.Tree) map[currY][currX].type = TileType.StonePath;
		}
		while(currX > entX) {
			currX--;
			map[currY][currX].walkable = true;
			if (map[currY][currX].type == TileType.Tree) map[currY][currX].type = TileType.StonePath;
		}

		return new MapGenerator.GenResult(map, startPos);
	}
}
