package com.goldsprite.magicdungeon.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.kotcrab.vis.ui.widget.VisLabel;

public class MagicDungeonLoadingRenderer implements ScreenManager.LoadingRenderer {
    private PolygonSpriteBatch polyBatch;
    private SpriteBatch batch;
    private SkeletonRenderer skeletonRenderer;
    private Skeleton skeleton;
    private AnimationState animationState;
    private BitmapFont font;
    
    private boolean initialized = false;

    public MagicDungeonLoadingRenderer() {
        // Lazy init in render or explicit init?
        // Since ScreenManager might be created early, let's init on first use or in constructor if context ready.
        // But context is ready when this is created.
    }

    private void init() {
        if (initialized) return;
        
        polyBatch = new PolygonSpriteBatch();
        batch = new SpriteBatch();
        skeletonRenderer = new SkeletonRenderer();
        skeletonRenderer.setPremultipliedAlpha(true);
        
        try {
            // Load Spine (Using large_sworder as in LoadingScreen)
            TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("spines/large_sworder/large_sworder.atlas"));
            SkeletonJson json = new SkeletonJson(atlas);
            json.setScale(1.0f);
            SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("spines/large_sworder/large_sworder.json"));
    
            skeleton = new Skeleton(skeletonData);
            
            AnimationStateData stateData = new AnimationStateData(skeletonData);
            animationState = new AnimationState(stateData);
            animationState.setAnimation(0, "move", true); // "move" or "run"? LoadingScreen used "move"
        } catch (Exception e) {
            Gdx.app.error("LoadingRenderer", "Failed to load spine", e);
        }
        
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        
        initialized = true;
    }

    @Override
    public void render(float delta, float alpha) {
        if (!initialized) init();
        if (skeleton == null) return;

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();
        
        // Update Spine
        skeleton.setPosition(w / 2, h / 2 - 50);
        animationState.update(delta);
        animationState.apply(skeleton);
        skeleton.updateWorldTransform();
        
        // Draw
        polyBatch.begin();
        skeletonRenderer.draw(polyBatch, skeleton);
        polyBatch.end();
        
        // Draw Text
        batch.begin();
        String text = "正在前往目标区域...";
        // Simple centering
        font.draw(batch, text, w / 2 - 60, h / 2 - 80);
        batch.end();
    }
    
    public void dispose() {
        if (polyBatch != null) polyBatch.dispose();
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        // Atlas disposal? Ideally we should manage it.
    }
}
