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
        loadingRenderer.setText("初始化游戏资源中...");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1. Update Asset Loading
        if (!assetsLoaded) {
            if (assetProxy.update()) {
                assetsLoaded = true;
                loadingRenderer.setText("生成纹理中...");
            }
        } else if (!generationStarted) {
            // 2. Start Texture Generation (Synchronous for now, can be optimized)
            generationStarted = true;
            // Use postRunnable to ensure the "Generating..." text is rendered at least once
            Gdx.app.postRunnable(() -> {
                try {
                    TextureManager.getInstance(); // This triggers loadAll()
                    generationFinished = true;
                } catch (Exception e) {
                    Gdx.app.error("Preloader", "Failed to generate textures", e);
                    generationFinished = true; // Proceed anyway?
                }
            });
        }

        // 3. Render Loading Animation
        loadingRenderer.render(delta, 1f);

        // 4. Check completion
        if (assetsLoaded && generationFinished && !transitionTriggered) {
            transitionTriggered = true;
            // Transition to Main Menu
            ScreenManager.getInstance().playTransition(() -> {
                ScreenManager.getInstance().goScreen(new MainMenuScreen());
            });
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (loadingRenderer != null) loadingRenderer.dispose();
    }
}
