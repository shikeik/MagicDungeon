package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TextureManager implements Disposable {
	private static TextureManager instance;
	
	private Map<String, TextureRegion> regionCache;
	// Track underlying textures for disposal
	private Set<Texture> managedTextures;
	
	private TextureManager() {
		regionCache = new HashMap<>();
		managedTextures = new HashSet<>();
		loadAll();
	}
	
	public static TextureManager getInstance() {
		if (instance == null) {
			instance = new TextureManager();
		}
		return instance;
	}
	
	private void loadAll() {
		// 1. Load Assets (Sprite Sheets)
		loadSheet("sprites/all_blocks_sheet.json");
		loadSheet("sprites/all_entities_sheet.json");
		loadSheet("sprites/all_items_sheet.json");

		// 2. Generate Missing Textures (Fallback)
		
		// Tiles
		checkAndGenerate(TileType.Wall.name(), () -> SpriteGenerator.createWall());
		checkAndGenerate(TileType.Floor.name(), () -> SpriteGenerator.createFloor());
		checkAndGenerate(TileType.Door.name(), () -> SpriteGenerator.createDoor());
		checkAndGenerate(TileType.Stairs_Down.name(), () -> SpriteGenerator.createStairs(false));
		checkAndGenerate(TileType.Stairs_Up.name(), () -> SpriteGenerator.createStairs(true));
		
		// Camp Tiles
		checkAndGenerate(TileType.Tree.name(), () -> SpriteGenerator.createTree());
		checkAndGenerate(TileType.Grass.name(), () -> SpriteGenerator.createGrass());
		checkAndGenerate(TileType.Sand.name(), () -> SpriteGenerator.createSand());
		checkAndGenerate(TileType.StonePath.name(), () -> SpriteGenerator.createStonePath());
		checkAndGenerate(TileType.Dungeon_Entrance.name(), () -> SpriteGenerator.createDungeonEntrance());
		
		// Player
		checkAndGenerate("PLAYER", () -> SpriteGenerator.createPlayer());
		
		// Monsters
		for (MonsterType type : MonsterType.values()) {
			checkAndGenerate(type.name(), () -> SpriteGenerator.createMonster(type.name()));
		}
		
		// Items
		for (ItemData item : ItemData.values()) {
			checkAndGenerate(item.name(), () -> SpriteGenerator.createItem(item.name));
		}
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
		Texture produce();
	}

	private void checkAndGenerate(String key, TextureProducer producer) {
		String lowerKey = key.toLowerCase();
		if (!regionCache.containsKey(lowerKey)) {
			Texture tex = producer.produce();
			managedTextures.add(tex);
			regionCache.put(lowerKey, new TextureRegion(tex));
		}
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
			Texture tex = SpriteGenerator.createQualityStar();
			managedTextures.add(tex);
			regionCache.put(key, new TextureRegion(tex));
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
