package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.goldsprite.gdengine.log.DLog;

/**
 * 统一资源代理
 * 负责管理 AssetManager 的生命周期和资源加载队列
 */
public class AssetProxy {
    private static AssetProxy instance;
    private AssetManager manager;
    
    private AssetProxy() {
        manager = new AssetManager();
        // Configure loaders if needed
        manager.setLoader(TiledMap.class, new TmxMapLoader(new InternalFileHandleResolver()));
    }
    
    public static AssetProxy getInstance() {
        if (instance == null) {
            instance = new AssetProxy();
        }
        return instance;
    }
    
    public AssetManager getManager() {
        return manager;
    }
    
    /**
     * 排队加载全局通用资源
     */
    public void loadGlobalAssets() {
        DLog.log("AssetProxy", "Queueing global assets...");
        
        // 1. Audio
        AudioAssets.loadAll(manager);
        
        // 2. UI Skin (if not already loaded by VisUI, but maybe we want to manage it here)
        // VisUIHelper loads it synchronously usually. We might want to preload the atlas/json.
        
        // 3. Spines (Example)
        // manager.load("spines/large_sworder/large_sworder.atlas", TextureAtlas.class);
        
        // 4. Texture Sheets (Used by TextureManager)
        // We can preload the textures for TextureManager to use later
        // manager.load("sprites/all_blocks_sheet.png", Texture.class);
        // manager.load("sprites/all_entities_sheet.png", Texture.class);
        // manager.load("sprites/all_items_sheet.png", Texture.class);
    }
    
    /**
     * 更新加载进度
     * @return true if finished
     */
    public boolean update() {
        return manager.update();
    }
    
    public float getProgress() {
        return manager.getProgress();
    }
    
    public <T> T get(String fileName, Class<T> type) {
        return manager.get(fileName, type);
    }
    
    public void dispose() {
        manager.dispose();
    }
}
