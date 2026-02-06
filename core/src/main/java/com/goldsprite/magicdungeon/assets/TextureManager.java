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
        textureCache.put(TileType.WALL.name(), SpriteGenerator.createWall());
        textureCache.put(TileType.FLOOR.name(), SpriteGenerator.createFloor());
        textureCache.put(TileType.DOOR.name(), SpriteGenerator.createDoor());
        textureCache.put(TileType.STAIRS_DOWN.name(), SpriteGenerator.createStairs(false));
        textureCache.put(TileType.STAIRS_UP.name(), SpriteGenerator.createStairs(true));
        
        // Player
        textureCache.put("PLAYER", SpriteGenerator.createPlayer());
        
        // Monsters
        for (MonsterType type : MonsterType.values()) {
            // Key is the display name (e.g. "史莱姆") used by Entity.name
            // Generator expects Enum name (e.g. "SLIME")
            textureCache.put(type.name, SpriteGenerator.createMonster(type.name()));
        }
        
        // Items
        for (ItemData item : ItemData.values()) {
            textureCache.put(item.name, SpriteGenerator.createItem(item.name));
        }
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
    
    public Texture getPlayer() {
        return get("PLAYER");
    }

    @Override
    public void dispose() {
        for (Texture t : textureCache.values()) {
            t.dispose();
        }
        textureCache.clear();
        instance = null;
    }
}
