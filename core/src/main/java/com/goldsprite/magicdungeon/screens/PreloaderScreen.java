package com.goldsprite.magicdungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon.assets.AssetProxy;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.core.screens.MainMenuScreen;
import com.goldsprite.magicdungeon.ui.MagicDungeonLoadingRenderer;

/**
 * 预加载屏幕
 * 负责加载所有核心资源，并在完成后切换至主菜单。
 */
public class PreloaderScreen extends GScreen {
    private MagicDungeonLoadingRenderer loadingRenderer;
    private AssetProxy assetProxy;
    private boolean assetsLoaded = false;
    private boolean generationStarted = false;
    private boolean generationFinished = false;
    private boolean transitionTriggered = false;
    private float stateTime = 0f;

    public PreloaderScreen() {
        this.loadingRenderer = new MagicDungeonLoadingRenderer();
        this.assetProxy = AssetProxy.getInstance();
    }

    @Override
    public void show() {
        super.show();
        // Start asset loading
        if (assetProxy.getManager().getQueuedAssets() == 0) {
             assetProxy.loadGlobalAssets();
        }
        
        // Start texture generation async
        TextureManager.getInstance().loadAsync();
        
        loadingRenderer.setText("初始化游戏资源中...");
    }

    @Override
    public void render(float delta) {
        stateTime += delta;
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Update Asset Loading
        if (!assetsLoaded) {
            if (assetProxy.update()) {
                assetsLoaded = true;
                loadingRenderer.setText("生成纹理中...");
            }
        } 
        
        // 2. Update Texture Generation
        if (!generationFinished) {
            // Process texture generation tasks (time-sliced)
            // [Async Proof] Ensure we process tasks but also simulate "weight" or just show progress
            boolean done = TextureManager.getInstance().update();
            
            float progress = TextureManager.getInstance().getProgress();
            loadingRenderer.setText("生成纹理中... " + (int)(progress * 100) + "%");
            
            // [Async Proof] Add a small artificial delay to make the loading screen visible
            // This proves the screen is rendering while "loading" happens in background (time-sliced)
            if (done) {
                 // Ensure we show 100% for a moment
                 if (stateTime > 1.0f) { // Min 1 second duration
                     
                     // [Optimization] Pre-initialize MainMenuScreen to avoid lag on transition
                     // This compiles shaders (NeonBatch, Bloom) while loading screen is still visible
                     loadingRenderer.setText("准备主菜单...");
                     MainMenuScreen mainMenu = new MainMenuScreen();
                     // Manually add and initialize to trigger create()
                     ScreenManager.getInstance().addScreen(mainMenu);
                     // Force initialization (compile shaders)
                     if (!mainMenu.isInitialized()) {
                         mainMenu.initialize();
                     }
                     
                     generationFinished = true;
                 }
            }
        }

        // 3. Render Loading Animation
        loadingRenderer.render(delta, 1f);

        // 4. Check completion
        if (assetsLoaded && generationFinished && !transitionTriggered) {
            transitionTriggered = true;
            // Transition to Main Menu
            ScreenManager.getInstance().playTransition(() -> {
                // Go to Main Menu (Already initialized above)
                ScreenManager.getInstance().replaceScreen(MainMenuScreen.class);
            });
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (loadingRenderer != null) loadingRenderer.dispose();
    }
}
