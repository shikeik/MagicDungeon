package com.goldsprite.magicdungeon.screens.tests.neonskel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.neonskel.data.*;
import com.goldsprite.neonskel.logic.AnimationState;
import com.goldsprite.neonskel.logic.NeonSkeleton;
import com.goldsprite.neonskel.logic.TrackEntry;

public class NeonLayeredMixTestScreen extends GScreen {

    private NeonSkeleton skeleton;
    private AnimationState animState;
    private ShapeRenderer debugRenderer;
    private SpriteBatch batch;
    private Stage stage;
    
    private TrackEntry track0, track1;

    @Override
    public void create() {
        super.create();
        
        batch = new SpriteBatch();
        stage = new Stage(getUIViewport(), batch);
        
        if (imp != null) {
            imp.addProcessor(stage);
        }
        
        // 1. 创建骨骼
        skeleton = new NeonSkeleton();
        skeleton.setPosition(400, 300);
        
        // Root (created by default)
        
        // Body (attached to root)
        skeleton.createBone("body", "root", 100, null);
        NeonBone body = skeleton.getBone("body");
        body.rotation = 90;
        
        // Arm (attached to body)
        skeleton.createBone("arm", "body", 80, null);
        NeonBone arm = skeleton.getBone("arm");
        arm.x = 100; // at end of body
        arm.rotation = -45;
        
        skeleton.updateWorldTransform();
        
        // 2. 创建动画
        animState = new AnimationState();
        
        // Anim: Idle (Body bobbing)
        NeonAnimation idleAnim = new NeonAnimation("idle", 2.0f, true);
        
        NeonTimeline breathTimeline = new NeonTimeline("body", NeonProperty.SCALE_Y);
        breathTimeline.addKeyframe(0f, 1.0f, NeonCurve.LINEAR);
        breathTimeline.addKeyframe(1.0f, 1.1f, NeonCurve.LINEAR);
        breathTimeline.addKeyframe(2.0f, 1.0f, NeonCurve.LINEAR);
        idleAnim.addTimeline(breathTimeline);
        
        animState.addAnimation(idleAnim);
        
        // Anim: Wave (Arm waving)
        NeonAnimation waveAnim = new NeonAnimation("wave", 1.0f, true);
        
        NeonTimeline waveTimeline = new NeonTimeline("arm", NeonProperty.ROTATION);
        waveTimeline.addKeyframe(0f, -45f, NeonCurve.LINEAR);
        waveTimeline.addKeyframe(0.25f, 0f, NeonCurve.LINEAR);
        waveTimeline.addKeyframe(0.5f, -45f, NeonCurve.LINEAR);
        waveTimeline.addKeyframe(0.75f, -90f, NeonCurve.LINEAR);
        waveTimeline.addKeyframe(1.0f, -45f, NeonCurve.LINEAR);
        waveAnim.addTimeline(waveTimeline);
        
        animState.addAnimation(waveAnim);
        
        // 3. 播放
        track0 = animState.setAnimation(0, "idle", true);
        track1 = animState.setAnimation(1, "wave", true);
        track1.alpha = 0.5f; // Start with 50% mix
        
        // 4. Renderer
        debugRenderer = new ShapeRenderer();
        
        // 5. UI
        setupUI();
    }
    
    private void setupUI() {
        VisTable table = new VisTable(true);
        table.setFillParent(true);
        table.top().right();
        
        table.add(new VisLabel("Neon Layered Mix Test")).row();
        
        // Track 1 Alpha Slider
        table.add(new VisLabel("Track 1 (Wave) Alpha:")).row();
        final VisSlider alphaSlider = new VisSlider(0, 1, 0.01f, false);
        alphaSlider.setValue(track1.alpha);
        alphaSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (track1 != null) {
                    track1.alpha = alphaSlider.getValue();
                }
            }
        });
        table.add(alphaSlider).width(200).row();
        
        // Crossfade Test
        VisTextButton fadeBtn = new VisTextButton("Crossfade Idle (1s)");
        fadeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Crossfade to itself to test mixing
                animState.setAnimation(0, "idle", true, 1.0f);
            }
        });
        table.add(fadeBtn).row();
        
        // Speed
        table.add(new VisLabel("Speed:")).row();
        final VisSlider speedSlider = new VisSlider(0, 2, 0.1f, false);
        speedSlider.setValue(1.0f);
        speedSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                animState.setSpeed(speedSlider.getValue());
            }
        });
        table.add(speedSlider).width(200).row();
        
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        
        // Update
        animState.update(delta);
        animState.apply(skeleton, null);
        skeleton.updateWorldTransform();
        
        // Render Debug
        debugRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        drawBoneRecursive(skeleton.rootBone);
        
        debugRenderer.end();
        
        stage.act(delta);
        stage.draw();
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
    }
    
    private void drawBoneRecursive(NeonBone bone) {
        float x = bone.worldTransform.m02;
        float y = bone.worldTransform.m12;
        
        // Calculate end point based on length and rotation from matrix
        float cos = bone.worldTransform.m00;
        float sin = bone.worldTransform.m10;
        
        float x2 = x + cos * bone.length;
        float y2 = y + sin * bone.length;
        
        debugRenderer.setColor(Color.WHITE);
        debugRenderer.line(x, y, x2, y2);
        
        debugRenderer.setColor(Color.RED);
        debugRenderer.circle(x, y, 3); // Joint
        
        for (NeonBone child : bone.children) {
            drawBoneRecursive(child);
        }
    }
}
