package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
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

	public GenResult generate() {
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
			int w = MathUtils.random(minSize, maxSize);
			int h = MathUtils.random(minSize, maxSize);
			int x = MathUtils.random(1, width - w - 2);
			int y = MathUtils.random(1, height - h - 2);

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
					if (MathUtils.randomBoolean()) {
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
		}

		return new GenResult(map, start);
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
