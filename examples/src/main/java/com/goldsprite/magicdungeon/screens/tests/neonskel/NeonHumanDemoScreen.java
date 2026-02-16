package com.goldsprite.magicdungeon.screens.tests.neonskel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.neonskel.data.*;
import com.goldsprite.neonskel.logic.AnimationState;
import com.goldsprite.neonskel.logic.NeonSkeleton;
import com.goldsprite.neonskel.logic.TrackEntry;

public class NeonHumanDemoScreen extends GScreen {

    private NeonSkeleton skeleton;
    private AnimationState animState;
    private NeonBatch neonBatch;
    private SpriteBatch batch;
    private Stage stage;
    
    // UI Components
    private VisTable gameArea;
    private VisTable controlPanel;
    
    // IK Targets
    private NeonBone targetLeftHand, targetRightHand;
    private NeonBone targetLeftFoot, targetRightFoot;
    
    // Dragging state
    private NeonBone draggingBone = null;
    private Vector3 tempVec3 = new Vector3();
    
    private TrackEntry walkTrack, waveTrack;
    
    // Render State
    private float renderBaseX, renderBaseY;
    private boolean showGizmos = true;

    @Override
    public void create() {
        super.create();
        
        batch = new SpriteBatch();
        neonBatch = new NeonBatch();
        stage = new Stage(getUIViewport(), batch);
        
        if (imp != null) {
            imp.addProcessor(stage);
            imp.addProcessor(new DemoInputHandler());
        }
        
        createSkeleton();
        createAnimations();
        setupUI();
    }
    
    private void createSkeleton() {
        skeleton = new NeonSkeleton();
        skeleton.setPosition(0, 0); 
        
        // --- 1. Structure ---
        // Root -> Body (Hip)
        skeleton.createBone("body", "root", 0, null);
        skeleton.getBone("body").y = 0; 
        
        // Torso -> Head
        skeleton.createBone("torso", "body", 100, null);
        skeleton.getBone("torso").rotation = 90;
        
        skeleton.createBone("head", "torso", 40, null);
        skeleton.getBone("head").x = 100;
        
        // --- Head Details ---
        // Jaw
        skeleton.createBone("jaw", "head", 20, null);
        skeleton.getBone("jaw").x = 10;
        skeleton.getBone("jaw").y = -5;
        skeleton.getBone("jaw").rotation = -30;
        
        // Hair (Dense Clusters)
        // Back hair
        createHairCluster("hair_back", "head", -10, 10, 180, 8, 5f, 40f);
        // Bangs
        createHairCluster("hair_bangs", "head", 30, 15, -45, 5, 8f, 25f);
        // Side hair
        createHairCluster("hair_side", "head", 10, 10, -90, 4, 10f, 35f);
        
        // Arms (Right)
        skeleton.createBone("r_arm_up", "torso", 70, null);
        skeleton.getBone("r_arm_up").x = 90;
        skeleton.getBone("r_arm_up").rotation = -90;
        
        skeleton.createBone("r_arm_low", "r_arm_up", 60, null);
        skeleton.getBone("r_arm_low").x = 70;
        
        createHand("r_hand", "r_arm_low");
        
        // Arms (Left)
        skeleton.createBone("l_arm_up", "torso", 70, null);
        skeleton.getBone("l_arm_up").x = 90;
        skeleton.getBone("l_arm_up").rotation = -90;
        
        skeleton.createBone("l_arm_low", "l_arm_up", 60, null);
        skeleton.getBone("l_arm_low").x = 70;
        
        createHand("l_hand", "l_arm_low");
        
        // Legs (Right)
        skeleton.createBone("r_leg_up", "body", 80, null);
        skeleton.getBone("r_leg_up").rotation = -80;
        
        skeleton.createBone("r_leg_low", "r_leg_up", 80, null);
        skeleton.getBone("r_leg_low").x = 80;
        
        createFoot("r_foot", "r_leg_low");
        
        // Legs (Left)
        skeleton.createBone("l_leg_up", "body", 80, null);
        skeleton.getBone("l_leg_up").rotation = -100;
        
        skeleton.createBone("l_leg_low", "l_leg_up", 80, null);
        skeleton.getBone("l_leg_low").x = 80;
        
        createFoot("l_foot", "l_leg_low");
        
        // --- 2. IK Targets ---
        targetRightHand = skeleton.createBone("target_r_hand", "root", 10, null);
        targetRightHand.x = 80; targetRightHand.y = 80;
        
        targetLeftHand = skeleton.createBone("target_l_hand", "root", 10, null);
        targetLeftHand.x = -80; targetLeftHand.y = 80;
        
        targetRightFoot = skeleton.createBone("target_r_foot", "root", 10, null);
        targetRightFoot.x = 40; targetRightFoot.y = -150;
        
        targetLeftFoot = skeleton.createBone("target_l_foot", "root", 10, null);
        targetLeftFoot.x = -40; targetLeftFoot.y = -150;
        
        // --- 3. IK Constraints ---
        NeonIKConstraint rArmIK = new NeonIKConstraint("r_arm_ik");
        rArmIK.setTarget(targetRightHand);
        rArmIK.addBone(skeleton.getBone("r_arm_low"));
        rArmIK.addBone(skeleton.getBone("r_arm_up"));
        rArmIK.bendPositive = true;
        rArmIK.mix = 0.0f;
        skeleton.addIKConstraint(rArmIK);
        
        NeonIKConstraint lArmIK = new NeonIKConstraint("l_arm_ik");
        lArmIK.setTarget(targetLeftHand);
        lArmIK.addBone(skeleton.getBone("l_arm_low"));
        lArmIK.addBone(skeleton.getBone("l_arm_up"));
        lArmIK.bendPositive = false;
        lArmIK.mix = 0.0f;
        skeleton.addIKConstraint(lArmIK);
        
        NeonIKConstraint rLegIK = new NeonIKConstraint("r_leg_ik");
        rLegIK.setTarget(targetRightFoot);
        rLegIK.addBone(skeleton.getBone("r_leg_low"));
        rLegIK.addBone(skeleton.getBone("r_leg_up"));
        rLegIK.bendPositive = true;
        rLegIK.mix = 0.0f;
        skeleton.addIKConstraint(rLegIK);
        
        NeonIKConstraint lLegIK = new NeonIKConstraint("l_leg_ik");
        lLegIK.setTarget(targetLeftFoot);
        lLegIK.addBone(skeleton.getBone("l_leg_low"));
        lLegIK.addBone(skeleton.getBone("l_leg_up"));
        lLegIK.bendPositive = true;
        lLegIK.mix = 0.0f;
        skeleton.addIKConstraint(lLegIK);
        
        skeleton.updateWorldTransform();
    }
    
    private void createHand(String name, String parent) {
        skeleton.createBone(name, parent, 20, null);
        skeleton.getBone(name).x = 60;
        
        createFinger(name + "_thumb", name, 10, -45);
        createFinger(name + "_index", name, 15, 10);
        createFinger(name + "_mid", name, 16, 0);
        createFinger(name + "_ring", name, 15, -10);
        createFinger(name + "_pinky", name, 12, -20);
    }
    
    private void createFinger(String name, String parent, float length, float rot) {
        skeleton.createBone(name + "_1", parent, length * 0.5f, null);
        NeonBone b1 = skeleton.getBone(name + "_1");
        b1.rotation = rot;
        b1.x = 20;
        
        skeleton.createBone(name + "_2", name + "_1", length * 0.5f, null);
        skeleton.getBone(name + "_2").x = length * 0.5f;
    }
    
    private void createFoot(String name, String parent) {
        skeleton.createBone(name, parent, 20, null);
        skeleton.getBone(name).x = 80;
        skeleton.getBone(name).rotation = 90;
        
        // Heel
        skeleton.createBone(name + "_heel", name, 10, null);
        skeleton.getBone(name + "_heel").x = 0;
        skeleton.getBone(name + "_heel").rotation = 180;

        // Toes
        skeleton.createBone(name + "_toes", name, 15, null);
        skeleton.getBone(name + "_toes").x = 20;
    }
    
    private void createHairCluster(String name, String parent, float x, float y, float rot, int strands, float density, float length) {
        // Cluster Root
        skeleton.createBone(name + "_root", parent, 0, null);
        NeonBone root = skeleton.getBone(name + "_root");
        root.x = x; root.y = y; root.rotation = rot;

        // Strands
        for (int i = 0; i < strands; i++) {
            String id = name + "_s" + i;
            float angleOffset = (i - strands/2f) * density + MathUtils.random(-5f, 5f);
            
            skeleton.createBone(id + "_1", name + "_root", length * 0.5f, null);
            NeonBone b1 = skeleton.getBone(id + "_1");
            b1.rotation = angleOffset;
            
            skeleton.createBone(id + "_2", id + "_1", length * 0.5f, null);
            skeleton.getBone(id + "_2").x = length * 0.5f;
            skeleton.getBone(id + "_2").rotation = MathUtils.random(-10f, 10f);
        }
    }
    
    private void createAnimations() {
        animState = new AnimationState();
        
        // 1. Walk Animation (Legs only)
        NeonAnimation walk = new NeonAnimation("walk", 1.0f, true);
        
        // Right Leg
        NeonTimeline rLeg = new NeonTimeline("r_leg_up", NeonProperty.ROTATION);
        rLeg.addKeyframe(0f, -60f, NeonCurve.SMOOTH);
        rLeg.addKeyframe(0.5f, -100f, NeonCurve.SMOOTH);
        rLeg.addKeyframe(1.0f, -60f, NeonCurve.SMOOTH);
        walk.addTimeline(rLeg);
        
        NeonTimeline rKnee = new NeonTimeline("r_leg_low", NeonProperty.ROTATION);
        rKnee.addKeyframe(0f, 0f, NeonCurve.SMOOTH);
        rKnee.addKeyframe(0.25f, 45f, NeonCurve.SMOOTH); // Lift
        rKnee.addKeyframe(0.5f, 0f, NeonCurve.SMOOTH);
        walk.addTimeline(rKnee);
        
        // Left Leg (Phase shifted)
        NeonTimeline lLeg = new NeonTimeline("l_leg_up", NeonProperty.ROTATION);
        lLeg.addKeyframe(0f, -100f, NeonCurve.SMOOTH);
        lLeg.addKeyframe(0.5f, -60f, NeonCurve.SMOOTH);
        lLeg.addKeyframe(1.0f, -100f, NeonCurve.SMOOTH);
        walk.addTimeline(lLeg);
        
        NeonTimeline lKnee = new NeonTimeline("l_leg_low", NeonProperty.ROTATION);
        lKnee.addKeyframe(0.5f, 0f, NeonCurve.SMOOTH);
        lKnee.addKeyframe(0.75f, 45f, NeonCurve.SMOOTH);
        lKnee.addKeyframe(1.0f, 0f, NeonCurve.SMOOTH);
        walk.addTimeline(lKnee);
        
        // Body Bob
        NeonTimeline bob = new NeonTimeline("body", NeonProperty.Y);
        bob.addKeyframe(0f, 0f, NeonCurve.SMOOTH);
        bob.addKeyframe(0.25f, 10f, NeonCurve.SMOOTH);
        bob.addKeyframe(0.5f, 0f, NeonCurve.SMOOTH);
        bob.addKeyframe(0.75f, 10f, NeonCurve.SMOOTH);
        bob.addKeyframe(1.0f, 0f, NeonCurve.SMOOTH);
        walk.addTimeline(bob);
        
        animState.addAnimation(walk);
        
        // 2. Wave Animation (Right Arm only)
        NeonAnimation wave = new NeonAnimation("wave", 1.0f, true);
        
        NeonTimeline rArm = new NeonTimeline("r_arm_up", NeonProperty.ROTATION);
        rArm.addKeyframe(0f, 45f, NeonCurve.SMOOTH); // Up
        rArm.addKeyframe(0.5f, 135f, NeonCurve.SMOOTH); // High
        rArm.addKeyframe(1.0f, 45f, NeonCurve.SMOOTH);
        wave.addTimeline(rArm);
        
        NeonTimeline rElbow = new NeonTimeline("r_arm_low", NeonProperty.ROTATION);
        rElbow.addKeyframe(0f, 0f, NeonCurve.SMOOTH);
        rElbow.addKeyframe(0.25f, 45f, NeonCurve.SMOOTH); // Wave
        rElbow.addKeyframe(0.5f, 0f, NeonCurve.SMOOTH);
        rElbow.addKeyframe(0.75f, 45f, NeonCurve.SMOOTH);
        rElbow.addKeyframe(1.0f, 0f, NeonCurve.SMOOTH);
        wave.addTimeline(rElbow);
        
        animState.addAnimation(wave);
        
        // Start walking
        walkTrack = animState.play("walk");
    }
    
    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        stage.addActor(root);
        
        // Game Area (Left)
        gameArea = new VisTable();
        gameArea.setTouchable(Touchable.enabled);
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f)));
        
        // Control Panel (Right)
        controlPanel = new VisTable(true);
        controlPanel.setBackground("window");
        controlPanel.pad(10);
        
        buildControlPanel(controlPanel);
        
        VisSplitPane split = new VisSplitPane(gameArea, controlPanel, false);
        split.setSplitAmount(0.7f);
        split.setMinSplitAmount(0.5f);
        split.setMaxSplitAmount(0.9f);
        
        root.add(split).expand().fill();
    }
    
    private void buildControlPanel(VisTable panel) {
        panel.add(new VisLabel("Neon Human Demo")).row();
        panel.add(new VisLabel("Advanced IK & Mixing")).padBottom(10).row();
        
        // Display Options
        final VisCheckBox gizmoCheck = new VisCheckBox("Show Gizmos", true);
        gizmoCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showGizmos = gizmoCheck.isChecked();
            }
        });
        panel.add(gizmoCheck).left().padBottom(10).row();
        
        // IK Control
        panel.add(new VisLabel("--- IK Control ---")).align(Align.left).expandX().fillX().row();
        
        final VisCheckBox handIKCheck = new VisCheckBox("Enable Hand IK", false);
        handIKCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float mix = handIKCheck.isChecked() ? 1f : 0f;
                updateIKMix(mix, "r_arm_ik", "l_arm_ik");
            }
        });
        panel.add(handIKCheck).left().row();
        
        final VisCheckBox legIKCheck = new VisCheckBox("Enable Leg IK", false);
        legIKCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float mix = legIKCheck.isChecked() ? 1f : 0f;
                updateIKMix(mix, "r_leg_ik", "l_leg_ik");
            }
        });
        panel.add(legIKCheck).left().row();
        
        // Animation Control
        panel.add(new VisLabel("--- Animation ---")).align(Align.left).expandX().fillX().padTop(10).row();
        
        final VisCheckBox walkCheck = new VisCheckBox("Play Walk (Base)", true);
        walkCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (walkCheck.isChecked()) {
                    walkTrack = animState.play("walk", true);
                } else {
                    animState.clearTrack(0);
                    walkTrack = null;
                }
            }
        });
        panel.add(walkCheck).left().row();
        
        final VisCheckBox waveCheck = new VisCheckBox("Overlay Wave (Track 1)", false);
        waveCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (waveCheck.isChecked()) {
                    waveTrack = animState.setAnimation(1, "wave", true, 0.5f); // 0.5s fade in
                } else {
                    if (waveTrack != null) {
                        animState.clearTrack(1);
                        waveTrack = null;
                    }
                }
            }
        });
        panel.add(waveCheck).left().row();
        
        panel.add().expand().fill(); // Spacer
    }
    
    private void updateIKMix(float mix, String... names) {
        for (String name : names) {
            NeonIKConstraint ik = skeleton.findIKConstraint(name);
            if (ik != null) {
                ik.mix = mix;
            }
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // UI Layout Update
        stage.act(delta);
        stage.draw();
        
        drawSceneInGameArea(delta);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (neonBatch != null) neonBatch.dispose();
    }
    
    private void drawSceneInGameArea(float delta) {
        Actor area = gameArea;
        Vector2 pos = area.localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x;
        float y = pos.y;
        float w = area.getWidth();
        float h = area.getHeight();
        
        Rectangle scissor = new Rectangle();
        Rectangle clipBounds = new Rectangle(x, y, w, h);
        ScissorStack.calculateScissors(stage.getCamera(), stage.getBatch().getTransformMatrix(), clipBounds, scissor);
        if (ScissorStack.pushScissors(scissor)) {
            animState.update(delta);
            animState.apply(skeleton, null);
            
            renderBaseX = x + w / 2f;
            renderBaseY = y + h / 2f;
            skeleton.setPosition(renderBaseX, renderBaseY);
            skeleton.updateWorldTransform();
            
            neonBatch.setProjectionMatrix(stage.getCamera().combined);
            neonBatch.begin();
            
            drawBoneRecursive(skeleton.rootBone);
            
            if (showGizmos) {
                drawGizmo(targetRightHand, Color.GREEN);
                drawGizmo(targetLeftHand, Color.GREEN);
                drawGizmo(targetRightFoot, Color.GREEN);
                drawGizmo(targetLeftFoot, Color.GREEN);
            }
            
            neonBatch.end();
            
            ScissorStack.popScissors();
        }
    }
    
    private void drawGizmo(NeonBone t, Color color) {
        if (t == null) return;
        float tx = t.worldTransform.m02;
        float ty = t.worldTransform.m12;
        
        neonBatch.drawCircle(tx, ty, 8, 2, color, 16, false);
        neonBatch.drawLine(tx-12, ty, tx+12, ty, 2, color);
        neonBatch.drawLine(tx, ty-12, tx, ty+12, 2, color);
    }
    
    private void drawBoneRecursive(NeonBone bone) {
        if (bone.name.startsWith("target")) return;
        
        float x = bone.worldTransform.m02;
        float y = bone.worldTransform.m12;
        
        float cos = bone.worldTransform.m00;
        float sin = bone.worldTransform.m10;
        float x2 = x + cos * bone.length;
        float y2 = y + sin * bone.length;
        
        Color color = Color.CYAN;
        float thickness = 4f;
        
        if (bone.name.contains("hair")) {
            color = Color.MAGENTA;
            thickness = 2f;
        } else if (bone.name.contains("finger") || bone.name.contains("toes")) {
            color = Color.YELLOW;
            thickness = 2f;
        }
        
        if (bone.length > 0) {
            neonBatch.drawLine(x, y, x2, y2, thickness, color);
        }
        
        if (showGizmos) {
            neonBatch.drawCircle(x, y, 3, 0, Color.WHITE, 8, true);
        }
        
        for (NeonBone child : bone.children) {
            drawBoneRecursive(child);
        }
    }
    
    // --- Input Handler ---
    class DemoInputHandler extends InputAdapter {
        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            stage.getViewport().unproject(tempVec3.set(screenX, screenY, 0));
            // Check targets
            NeonBone[] targets = {targetLeftHand, targetRightHand, targetLeftFoot, targetRightFoot};
            for (NeonBone t : targets) {
                if (t == null) continue;
                float dist = Vector2.dst(tempVec3.x, tempVec3.y, t.worldTransform.m02, t.worldTransform.m12);
                if (dist < 20) {
                    draggingBone = t;
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (draggingBone != null) {
                stage.getViewport().unproject(tempVec3.set(screenX, screenY, 0));
                // Convert world pos back to local relative to Skeleton Root
                // Skeleton Root is at (renderBaseX, renderBaseY)
                // Target bones are direct children of root (in this setup)
                draggingBone.x = tempVec3.x - renderBaseX;
                draggingBone.y = tempVec3.y - renderBaseY;
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            draggingBone = null;
            return false;
        }
    }
}
