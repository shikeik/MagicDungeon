package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.HashMap;
import java.util.Map;

public class TextureManager implements Disposable {
	private static TextureManager instance;
	
	private Map<String, Texture> textureCache;
	
	private TextureManager() {
		textureCache = new HashMap<>();
		loadAll();
	}
	
	public static TextureManager getInstance() {
		if (instance == null) {
			instance = new TextureManager();
		}
		return instance;
	}
	
	private void loadAll() {
		// Tiles
		textureCache.put(TileType.Wall.name(), SpriteGenerator.createWall());
		textureCache.put(TileType.Floor.name(), SpriteGenerator.createFloor());
		textureCache.put(TileType.Door.name(), SpriteGenerator.createDoor());
		textureCache.put(TileType.Stairs_Down.name(), SpriteGenerator.createStairs(false));
		textureCache.put(TileType.Stairs_Up.name(), SpriteGenerator.createStairs(true));
		
		// Player
		textureCache.put("PLAYER", SpriteGenerator.createPlayer());
		
		// Monsters
		for (MonsterType type : MonsterType.values()) {
			Texture tex = SpriteGenerator.createMonster(type.name());
			// Only Enum Name (e.g. "Slime") - Standard Key
			textureCache.put(type.name(), tex);
		}
		
		// Items
		for (ItemData item : ItemData.values()) {
			Texture tex = SpriteGenerator.createItem(item.name);
			// Only Enum Name (e.g. "Rusty_Sword") - Standard Key
			textureCache.put(item.name(), tex);
		}
	}

	public Map<String, Texture> getAllTextures() {
		return textureCache;
	}
	
	public Texture get(String key) {
		return textureCache.get(key);
	}
	
	public Texture getTile(TileType type) {
		return get(type.name());
	}
	
	public Texture getMonster(String name) {
		return get(name);
	}
	
	public Texture getItem(String name) {
		return get(name);
	}
	
	public Texture getQualityStar() {
		if (!textureCache.containsKey("QUALITY_STAR")) {
			textureCache.put("QUALITY_STAR", SpriteGenerator.createQualityStar());
		}
		return textureCache.get("QUALITY_STAR");
	}
	
	public Texture getPlayer() {
		return get("PLAYER");
	}

	@Override
	public void dispose() {
		// Use a Set to avoid disposing the same texture multiple times
		// (since we map multiple keys to the same texture)
		java.util.Set<Texture> uniqueTextures = new java.util.HashSet<>(textureCache.values());
		for (Texture t : uniqueTextures) {
			t.dispose();
		}
		textureCache.clear();
		instance = null;
	}
}
