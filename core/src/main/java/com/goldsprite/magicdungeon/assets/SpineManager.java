package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Disposable;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;

import java.util.HashMap;
import java.util.Map;

public class SpineManager implements Disposable {
    private static SpineManager instance;
    
    private Map<String, SkeletonData> skeletonCache;
    private Map<String, TextureAtlas> atlasCache;

    private SpineManager() {
        skeletonCache = new HashMap<>();
        atlasCache = new HashMap<>();
        loadAll();
    }

    public static SpineManager getInstance() {
        if (instance == null) {
            instance = new SpineManager();
        }
        return instance;
    }

    private void loadAll() {
        loadSpine("Wolf", "spines/wolf/wolf");
    }

    private void loadSpine(String key, String basePath) {
        try {
            FileHandle atlasFile = Gdx.files.internal(basePath + ".atlas");
            if (!atlasFile.exists()) {
                Gdx.app.error("SpineManager", "Atlas not found: " + atlasFile.path());
                return;
            }

            TextureAtlas atlas = new TextureAtlas(atlasFile);
            atlasCache.put(key, atlas);

            SkeletonJson json = new SkeletonJson(atlas);
            json.setScale(1.0f); // Default scale
            
            FileHandle jsonFile = Gdx.files.internal(basePath + ".json");
            if (!jsonFile.exists()) {
                 Gdx.app.error("SpineManager", "JSON not found: " + jsonFile.path());
                 return;
            }
            
            SkeletonData data = json.readSkeletonData(jsonFile);
            skeletonCache.put(key, data);
            
            Gdx.app.log("SpineManager", "Loaded spine: " + key);
        } catch (Exception e) {
            Gdx.app.error("SpineManager", "Failed to load spine: " + key, e);
        }
    }

    public SkeletonData get(String key) {
        return skeletonCache.get(key);
    }

    @Override
    public void dispose() {
        for (TextureAtlas atlas : atlasCache.values()) {
            atlas.dispose();
        }
        atlasCache.clear();
        skeletonCache.clear();
        // Do not nullify instance here to avoid NPE if accessed after dispose, 
        // but typically singleton dispose is end of app.
    }
}
