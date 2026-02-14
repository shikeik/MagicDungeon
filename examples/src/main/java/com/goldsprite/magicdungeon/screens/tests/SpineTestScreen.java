package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.esotericsoftware.spine.SkeletonRendererDebug;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

public class SpineTestScreen extends GScreen {
    private PolygonSpriteBatch batch;
    private SkeletonRenderer renderer;
    private SkeletonRendererDebug debugRenderer;

    private TextureAtlas atlas;
    private Skeleton skeleton;
    private AnimationState state;

    @Override
    public void create() {
        batch = new PolygonSpriteBatch();
        renderer = new SkeletonRenderer();
        renderer.setPremultipliedAlpha(false); // 通常不需要 premultiplied alpha，除非导出设置了

        debugRenderer = new SkeletonRendererDebug();
        debugRenderer.setBoundingBoxes(false);
        debugRenderer.setRegionAttachments(false);

        // Load Spine resources
        // Path: assets/spines/wolf/exports/
        String atlasPath = "spines/wolf/exports/spine_108_02.atlas";
        String jsonPath = "spines/wolf/exports/spine_108_02.json";

        if (!Gdx.files.internal(atlasPath).exists() || !Gdx.files.internal(jsonPath).exists()) {
            Gdx.app.error("SpineTest", "Spine files not found: " + atlasPath);
            return;
        }

        atlas = new TextureAtlas(Gdx.files.internal(atlasPath));
        SkeletonJson json = new SkeletonJson(atlas);
        // Scale note: standard pixel art often needs scaling. Assuming 1:1 for now or adjust based on TILE_SIZE.
        json.setScale(1.0f);

        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal(jsonPath));

        skeleton = new Skeleton(skeletonData);
        // Center on screen
        skeleton.setPosition(0, 0);

        AnimationStateData stateData = new AnimationStateData(skeletonData);
        state = new AnimationState(stateData);

        // Print available animations
        System.out.println("Available animations:");
        for (Animation anim : skeletonData.getAnimations()) {
            System.out.println(anim.getName());
        }

        // Set default animation - usually "idle" or "animation"
        String defaultAnim = "idle";
        // 如果找不到 idle，尝试找名字里包含 idle 的
        if (skeletonData.findAnimation(defaultAnim) == null) {
            for(Animation anim : skeletonData.getAnimations()) {
                if (anim.getName().toLowerCase().contains("idle") || anim.getName().toLowerCase().contains("stand")) {
                    defaultAnim = anim.getName();
                    break;
                }
            }
            // 还是找不到，就用第一个
            if (skeletonData.findAnimation(defaultAnim) == null && skeletonData.getAnimations().size > 0) {
                defaultAnim = skeletonData.getAnimations().get(0).getName();
            }
        }

        if (defaultAnim != null && skeletonData.findAnimation(defaultAnim) != null) {
            System.out.println("Playing animation: " + defaultAnim);
            state.setAnimation(0, defaultAnim, true);
        } else {
            System.out.println("No animation found to play.");
        }
    }

    @Override
    protected void initViewport() {
        // Adjust viewport for better visibility
        this.viewSizeShort = 600;
        this.viewSizeLong = 800;
        this.worldScale = 1.0f; // 1:1 pixel
        this.autoCenterWorldCamera = false;
        super.initViewport();
    }

    @Override
    public void render0(float delta) {
        // Clear with gray background to see transparency
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (skeleton == null) return;

        state.update(delta);
        state.apply(skeleton);
        skeleton.updateWorldTransform();

        // Draw
        batch.setProjectionMatrix(getWorldCamera().combined);
        batch.begin();
        renderer.draw(batch, skeleton);
        batch.end();

        // Debug draw
        // debugRenderer.getShapeRenderer().setProjectionMatrix(getWorldCamera().combined);
        // debugRenderer.draw(skeleton);
    }

    @Override
    public void dispose() {
        if (atlas != null) atlas.dispose();
    }
}
