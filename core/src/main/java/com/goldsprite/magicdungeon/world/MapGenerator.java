package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;

import java.util.ArrayList;
import java.util.List;

public class MapGenerator {
	private int width;
	private int height;

	public MapGenerator(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public static class GenResult {
		public Tile[][] grid;
		public GridPoint2 start;

		public GenResult(Tile[][] grid, GridPoint2 start) {
			this.grid = grid;
			this.start = start;
		}
	}

	private static class Room {
		int x, y, w, h;
		Room(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}

	public GenResult generate(boolean hasUpStairs, RandomXS128 rng) {
		Tile[][] map = new Tile[height][width];
		List<Room> rooms = new ArrayList<>();

		// Initialize with walls
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				map[y][x] = new Tile(TileType.Wall);
			}
		}

		// Generate Rooms
		int maxRooms = 15;
		int minSize = 6;
		int maxSize = 12;

		for (int i = 0; i < maxRooms; i++) {
			int w = rng.nextInt(maxSize - minSize + 1) + minSize;
			int h = rng.nextInt(maxSize - minSize + 1) + minSize;
			int x = rng.nextInt(width - w - 2) + 1;
			int y = rng.nextInt(height - h - 2) + 1;

			Room newRoom = new Room(x, y, w, h);

			// Check collision with other rooms
			boolean failed = false;
			for (Room other : rooms) {
				if (newRoom.x <= other.x + other.w && newRoom.x + newRoom.w >= other.x &&
					newRoom.y <= other.y + other.h && newRoom.y + newRoom.h >= other.y) {
					failed = true;
					break;
				}
			}

			if (!failed) {
				createRoom(map, newRoom);

				if (!rooms.isEmpty()) {
					Room prevRoom = rooms.get(rooms.size() - 1);
					GridPoint2 prevCenter = new GridPoint2(
						prevRoom.x + prevRoom.w / 2,
						prevRoom.y + prevRoom.h / 2
					);
					GridPoint2 newCenter = new GridPoint2(
						newRoom.x + newRoom.w / 2,
						newRoom.y + newRoom.h / 2
					);

					// Coin flip for corridor direction
					if (rng.nextBoolean()) {
						createHCorridor(map, prevCenter.x, newCenter.x, prevCenter.y);
						createVCorridor(map, prevCenter.y, newCenter.y, newCenter.x);
					} else {
						createVCorridor(map, prevCenter.y, newCenter.y, prevCenter.x);
						createHCorridor(map, prevCenter.x, newCenter.x, newCenter.y);
					}
				}

				rooms.add(newRoom);
			}
		}

		// Place Stairs in the last room
		if (!rooms.isEmpty()) {
			Room lastRoom = rooms.get(rooms.size() - 1);
			int stairsX = lastRoom.x + lastRoom.w / 2;
			int stairsY = lastRoom.y + lastRoom.h / 2;
			map[stairsY][stairsX].type = TileType.Stairs_Down;
		}

		// Start position
		GridPoint2 start = new GridPoint2(0, 0);
		if (!rooms.isEmpty()) {
			Room startRoom = rooms.get(0);
			start.set(startRoom.x + startRoom.w / 2, startRoom.y + startRoom.h / 2);
			
			// Place Stairs Up in the start room if needed
            if (hasUpStairs) {
                // Place it slightly away from center if possible, or just at center (player spawns on it usually)
                // Let's place it exactly at start position.
                // Player spawns at start, so they will be standing on stairs up.
                map[start.y][start.x].type = TileType.Stairs_Up;
            }
		}

		// Decorate
		decorate(map, rooms, rng);

		return new GenResult(map, start);
	}
	
	private void decorate(Tile[][] map, List<Room> rooms, RandomXS128 rng) {
		// 1. Place Torches and Windows on North Walls
		for (int y = 1; y < height; y++) {
			for (int x = 1; x < width - 1; x++) {
				if (map[y][x].type == TileType.Wall) {
					// Check if below is floor (North Wall of a room/corridor)
					if (map[y-1][x].type == TileType.Floor) {
						// Random chance
						float roll = rng.nextFloat();
						if (roll < 0.15f) { // 15% Torch
							map[y][x].type = TileType.Torch;
						} else if (roll > 0.90f) { // 10% Window
							map[y][x].type = TileType.Window;
						}
					}
				}
			}
		}
		
		// 2. Place Pillars in Rooms
		for (Room room : rooms) {
			if (room.w >= 6 && room.h >= 6) {
				// Place pillars near corners (inset by 2)
				int px1 = room.x + 2;
				int py1 = room.y + 2;
				int px2 = room.x + room.w - 3;
				int py2 = room.y + room.h - 3;
				
				// Top-Left
				if (isValidPillarSpot(map, px1, py2)) {
					map[py2][px1].type = TileType.Pillar;
					map[py2][px1].walkable = false;
				}
				// Top-Right
				if (isValidPillarSpot(map, px2, py2)) {
					map[py2][px2].type = TileType.Pillar;
					map[py2][px2].walkable = false;
				}
				// Bottom-Left
				if (isValidPillarSpot(map, px1, py1)) {
					map[py1][px1].type = TileType.Pillar;
					map[py1][px1].walkable = false;
				}
				// Bottom-Right
				if (isValidPillarSpot(map, px2, py1)) {
					map[py1][px2].type = TileType.Pillar;
					map[py1][px2].walkable = false;
				}
			}
		}
	}
	
	private boolean isValidPillarSpot(Tile[][] map, int x, int y) {
		if (x < 0 || x >= width || y < 0 || y >= height) return false;
		return map[y][x].type == TileType.Floor;
	}

	private void createRoom(Tile[][] map, Room room) {
		for (int y = room.y; y < room.y + room.h; y++) {
			for (int x = room.x; x < room.x + room.w; x++) {
				map[y][x].type = TileType.Floor;
				map[y][x].walkable = true;
			}
		}
	}

	private void createHCorridor(Tile[][] map, int x1, int x2, int y) {
		for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
			map[y][x].type = TileType.Floor;
			map[y][x].walkable = true;
		}
	}

	private void createVCorridor(Tile[][] map, int y1, int y2, int x) {
		for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
			map[y][x].type = TileType.Floor;
			map[y][x].walkable = true;
		}
	}
}
