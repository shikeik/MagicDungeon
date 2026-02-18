package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonItemGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonSpriteGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTileGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.SpriteGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.TextureExporter;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Gdx;

import java.util.LinkedList;
import java.util.Queue;

public class TextureManager implements Disposable {
	private static TextureManager instance;

	private Map<String, TextureRegion> regionCache;
	// Track underlying textures for disposal
	private Set<Texture> managedTextures;
    
    // Async Loading Queue
    private Queue<Runnable> loadQueue = new LinkedList<>();
    private int totalTasks = 0;

	private TextureManager() {
		regionCache = new HashMap<>();
		managedTextures = new HashSet<>();
        // loadAll(); // [Change] Removed sync load
	}

	public static TextureManager getInstance() {
		if (instance == null) {
			instance = new TextureManager();
		}
		return instance;
	}
    
    /**
     * Start queuing async tasks
     */
    public void loadAsync() {
        if (totalTasks > 0) return; // Already loaded or loading
        
        loadQueue.clear();
        
        // 1. Load Assets (Sprite Sheets)
        // SpriteSheetLoader is disk I/O, better keep it here or move to separate thread?
        // For now keep in queue to spread frame time.
        loadQueue.add(() -> loadSheet("sprites/all_blocks_sheet.json"));
        loadQueue.add(() -> loadSheet("sprites/all_entities_sheet.json"));
        loadQueue.add(() -> loadSheet("sprites/all_items_sheet.json"));

        // 2. Generate Missing Textures (Fallback)

        // Tiles
        loadQueue.add(() -> checkAndGenerate(TileType.Wall.name(), () -> NeonTileGenerator.createDungeonWallTileset(ThemeConfig.WALL_TOP, ThemeConfig.WALL_FACE)));
        loadQueue.add(() -> checkAndGenerate(TileType.Floor.name(), () -> NeonTileGenerator.createFloor(ThemeConfig.FLOOR_BASE, ThemeConfig.FLOOR_DARK, ThemeConfig.FLOOR_HIGHLIGHT)));

        loadQueue.add(() -> checkAndGenerate(TileType.Door.name(), () -> SpriteGenerator.createDoor()));
        loadQueue.add(() -> checkAndGenerate(TileType.Stairs_Down.name(), () -> SpriteGenerator.createStairs(false)));
        loadQueue.add(() -> checkAndGenerate(TileType.Stairs_Up.name(), () -> SpriteGenerator.createStairs(true)));

        // Camp Tiles
        loadQueue.add(() -> checkAndGenerate(TileType.Tree.name(), () -> SpriteGenerator.createTree()));
        loadQueue.add(() -> checkAndGenerate(TileType.Grass.name(), () -> SpriteGenerator.createGrass()));
        loadQueue.add(() -> checkAndGenerate(TileType.Sand.name(), () -> SpriteGenerator.createSand()));
        loadQueue.add(() -> checkAndGenerate(TileType.Dirt.name(), () -> SpriteGenerator.createDirt()));
        loadQueue.add(() -> checkAndGenerate(TileType.StonePath.name(), () -> SpriteGenerator.createStonePath()));
        loadQueue.add(() -> checkAndGenerate(TileType.Dungeon_Entrance.name(), () -> SpriteGenerator.createDungeonEntrance()));

        // Decor
        loadQueue.add(() -> checkAndGenerate(TileType.Pillar.name(), () -> SpriteGenerator.createPillar()));
        loadQueue.add(() -> checkAndGenerate(TileType.Torch.name(), () -> SpriteGenerator.createTorch()));
        loadQueue.add(() -> checkAndGenerate(TileType.Window.name(), () -> SpriteGenerator.createWindow()));

        // Player
        loadQueue.add(() -> checkAndGenerate("PLAYER", () -> NeonSpriteGenerator.createPlayer()));

        // Monsters (Split into chunks to avoid long frames)
        for (MonsterType type : MonsterType.values()) {
            loadQueue.add(() -> {
                final String enumName = type.name();
                
                try {
                    FileHandle customFile = Gdx.files.internal("textures/monsters/" + enumName + ".png");
                    if (customFile.exists()) {
                        checkAndGenerate(enumName, () -> new TextureRegion(new Texture(customFile)));
                        return;
                    }
                } catch (Exception e) {
                    Gdx.app.error("TextureManager", "Failed to check custom texture for " + enumName, e);
                }

                TextureRegion region = NeonSpriteGenerator.createMonster(type.name);
                if (region != null) {
                    checkAndGenerate(type.name(), () -> region);
                } else {
                    checkAndGenerate(type.name(), () -> SpriteGenerator.createMonster(type.name()));
                }
            });
        }

        // Items
        for (ItemData item : ItemData.values()) {
            loadQueue.add(() -> checkAndGenerate(item.name(), () -> NeonItemGenerator.createItem(item.name)));
        }
        
        totalTasks = loadQueue.size();
    }
    
    /**
     * Process tasks for limited time per frame
     * @return true if all finished
     */
    public boolean update() {
        if (loadQueue.isEmpty()) return true;
        
        long startTime = System.nanoTime();
        // 8ms budget
        while (!loadQueue.isEmpty() && System.nanoTime() - startTime < 8000000) {
            Runnable task = loadQueue.poll();
            if (task != null) task.run();
        }
        
        return loadQueue.isEmpty();
    }
    
    public float getProgress() {
        if (totalTasks == 0) return 1.0f;
        return 1.0f - (float)loadQueue.size() / totalTasks;
    }
    
    private void loadAll() {
        // Legacy support: if called synchronously, run all tasks immediately
        loadAsync();
        while(!update()) {}
	}

	private void loadSheet(String path) {
		Map<String, TextureRegion> sheet = SpriteSheetLoader.load(path);
		if (!sheet.isEmpty()) {
			// Add all regions to cache
			regionCache.putAll(sheet);
			// Track the texture (all regions in a sheet share the same texture)
			Texture texture = sheet.values().iterator().next().getTexture();
			managedTextures.add(texture);
		}
	}

	private interface TextureProducer {
		TextureRegion produce();
	}

	// [Refactor] 修改方法签名以支持 Lambda 中包含逻辑判断
	// 原来的 TextureProducer 接口方法是 TextureRegion produce();
	// 但我们需要能在 checkAndGenerate 外部先判断，或者让 producer 可以返回 null (虽然 checkAndGenerate 已经做了 null 检查)
	// 实际上上面的修改 loop 中已经处理了 null 逻辑，这里只需稍微调整 checkAndGenerate 即可。
	// 但为了更清晰，我们可以在这里直接接受 TextureRegion 参数的重载？
	// 不，保持原样即可，我们在 loop 里做的逻辑是：
	/*
		TextureRegion region = NeonSpriteGenerator.createMonster(type.name);
		if (region != null) {
			checkAndGenerate(type.name(), () -> region);
		} else {
			checkAndGenerate(type.name(), () -> SpriteGenerator.createMonster(type.name()));
		}
	*/
	// 这样是没问题的。
	
	private void checkAndGenerate(String key, TextureProducer producer) {
		String lowerKey = key.toLowerCase();
		if (!regionCache.containsKey(lowerKey)) {
			TextureRegion region = producer.produce();
			
			// 如果 producer 返回 null，则不缓存
			if (region == null) return;

			// DEBUG: Export generated textures to disk for inspection
			// This helps user to see what procedural textures look like
			// [Fix] 只有在非 null 时才导出
			TextureExporter.exportToDisk(region.getTexture(), lowerKey);

			managedTextures.add(region.getTexture());
			regionCache.put(lowerKey, new TextureRegion(region));
		}
	}

	public void updateTexture(String key, Texture newTexture) {
		String lowerKey = key.toLowerCase();

		// If exists, we might need to dispose old texture if it was managed and generated
		// But be careful if it's shared from Atlas.
		// Our "generated" textures are individual Texture objects in managedTextures.

		TextureRegion oldRegion = regionCache.get(lowerKey);
		if (oldRegion != null) {
			Texture oldTex = oldRegion.getTexture();
			if (managedTextures.contains(oldTex)) {
				// Only dispose if it's one of our managed individual textures
				// And check if no other region uses it?
				// For generated textures, usually 1 region = 1 texture.
				managedTextures.remove(oldTex);
				oldTex.dispose();
			}
		}

		managedTextures.add(newTexture);
		regionCache.put(lowerKey, new TextureRegion(newTexture));
	}

	public Map<String, TextureRegion> getAllTextures() {
		return regionCache;
	}

	public TextureRegion get(String key) {
		if (key == null) return null;
		return regionCache.get(key.toLowerCase());
	}

	public TextureRegion getTile(TileType type) {
		return get(type.name());
	}

	public TextureRegion getMonster(String name) {
		return get(name);
	}

	public TextureRegion getItem(String name) {
		return get(name);
	}

	public TextureRegion getQualityStar() {
		String key = "quality_star";
		if (!regionCache.containsKey(key)) {
			TextureRegion region = SpriteGenerator.createQualityStar();
			managedTextures.add(region.getTexture());
			regionCache.put(key, region);
		}
		return regionCache.get(key);
	}

	public TextureRegion getPlayer() {
		return get("PLAYER");
	}

	@Override
	public void dispose() {
		for (Texture t : managedTextures) {
			t.dispose();
		}
		managedTextures.clear();
		regionCache.clear();
		instance = null;
	}
}
