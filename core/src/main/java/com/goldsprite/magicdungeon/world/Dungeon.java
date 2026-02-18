package com.goldsprite.magicdungeon.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.magicdungeon.world.data.GameMapData;

public class Dungeon {
	public int width;
	public int height;
	public int level;
	public Tile[][] map;
	public GridPoint2 startPos;
	public long globalSeed;
    public DungeonTheme theme = DungeonTheme.DEFAULT;
    
    // 暂存加载的地图数据，供 GameScreen 读取实体
    public GameMapData loadedMapData;

	public Dungeon(int width, int height, long globalSeed, DungeonTheme theme) {
		this.width = width;
		this.height = height;
		this.level = 1;
		this.globalSeed = globalSeed;
        this.theme = theme;
		this.map = new Tile[height][width];
		this.startPos = new GridPoint2(0, 0);
		generate();
	}

    public Dungeon(int width, int height, long globalSeed) {
        this(width, height, globalSeed, DungeonTheme.DEFAULT);
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
        this.loadedMapData = null; // Reset
        
		if (this.level == 0) {
            // 优先尝试加载编辑器生成的地图
            if (Gdx.files.internal("maps/camp.json").exists()) {
                try {
                    Json json = new Json();
                    GameMapData data = json.fromJson(GameMapData.class, Gdx.files.internal("maps/camp.json"));
                    loadFromData(data);
                    this.loadedMapData = data;
                    return;
                } catch (Exception e) {
                    Gdx.app.error("Dungeon", "Failed to load camp.json", e);
                }
            }
            
			MapGenerator.GenResult result = CampMapGenerator.generate();
			this.map = result.grid;
			this.height = this.map.length;
			this.width = this.map[0].length;
			this.startPos = result.start;
		} else {
			// Reset to standard dungeon size
			this.width = 50;
			this.height = 50;
			
			MapGenerator generator = new MapGenerator(this.width, this.height);
			// Always enable up stairs for levels >= 1 (to allow returning to camp from level 1)
			MapGenerator.GenResult result = generator.generate(true, getMapRNG(), this.theme);
			this.map = result.grid;
			this.startPos = result.start;
		}
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
    
    public void loadFromData(GameMapData data) {
        this.width = data.width;
        this.height = data.height;
        this.map = new Tile[height][width];
        
        // Load Grid
        for(int y=0; y<height; y++) {
            for(int x=0; x<width; x++) {
                String typeName = data.grid[y][x];
                if(typeName != null) {
                    try {
                        TileType type = TileType.valueOf(typeName);
                        this.map[y][x] = new Tile(type);
                    } catch (IllegalArgumentException e) {
                        // Ignore invalid types
                    }
                }
            }
        }
        
        // Find start pos (first available floor or specific type)
        // Default center
        this.startPos = new GridPoint2(width/2, height/2);
    }
}
