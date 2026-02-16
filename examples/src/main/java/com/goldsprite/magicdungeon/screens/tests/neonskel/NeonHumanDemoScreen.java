package com.goldsprite.magicdungeon.screens.tests.neonskel;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.neonskel.data.*;
import com.goldsprite.neonskel.logic.AnimationState;
import com.goldsprite.neonskel.logic.NeonSkeleton;
import com.goldsprite.neonskel.logic.TrackEntry;

public class NeonHumanDemoScreen extends GScreen {

    private NeonSkeleton skeleton;
    private AnimationState animState;
    private ShapeRenderer debugRenderer;
    private SpriteBatch batch;
    private Stage stage;
    
    // IK Targets
    private NeonBone targetLeftHand, targetRightHand;
    private NeonBone targetLeftFoot, targetRightFoot;
    
    // Dragging state
    private NeonBone draggingBone = null;
    private Vector3 tempVec3 = new Vector3();
    
    private TrackEntry walkTrack, waveTrack;

    @Override
    public void create() {
        super.create();
        
        batch = new SpriteBatch();
        stage = new Stage(getUIViewport(), batch);
        debugRenderer = new ShapeRenderer();
        
        if (imp != null) {
            // Priority: UI > Dragging > Others
            imp.addProcessor(stage);
            imp.addProcessor(new InputAdapter() {
                @Override
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    unproject(screenX, screenY);
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
                        unproject(screenX, screenY);
                        // Convert to local space of root (since targets are children of root)
                        draggingBone.x = tempVec3.x - skeleton.x;
                        draggingBone.y = tempVec3.y - skeleton.y;
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                    draggingBone = null;
                    return false;
                }
            });
        }
        
        createSkeleton();
        createAnimations();
        setupUI();
    }
    
    private void createSkeleton() {
        skeleton = new NeonSkeleton();
        skeleton.setPosition(600, 300); // Center
        
        // --- 1. Structure ---
        // Root -> Body (Hip)
        skeleton.createBone("body", "root", 0, null); // Hip center
        skeleton.getBone("body").y = 0; 
        
        // Torso -> Head
        skeleton.createBone("torso", "body", 100, null);
        skeleton.getBone("torso").rotation = 90;
        
        skeleton.createBone("head", "torso", 40, null);
        skeleton.getBone("head").x = 100; // End of torso
        
        // Arms (Right)
        skeleton.createBone("r_arm_up", "torso", 70, null);
        skeleton.getBone("r_arm_up").x = 90; // Shoulder
        skeleton.getBone("r_arm_up").rotation = -90; // Down
        
        skeleton.createBone("r_arm_low", "r_arm_up", 60, null);
        skeleton.getBone("r_arm_low").x = 70;
        
        // Arms (Left)
        skeleton.createBone("l_arm_up", "torso", 70, null);
        skeleton.getBone("l_arm_up").x = 90;
        skeleton.getBone("l_arm_up").rotation = -90;
        
        skeleton.createBone("l_arm_low", "l_arm_up", 60, null);
        skeleton.getBone("l_arm_low").x = 70;
        
        // Legs (Right)
        skeleton.createBone("r_leg_up", "body", 80, null);
        skeleton.getBone("r_leg_up").rotation = -80; // Slightly apart
        
        skeleton.createBone("r_leg_low", "r_leg_up", 80, null);
        skeleton.getBone("r_leg_low").x = 80;
        
        skeleton.createBone("r_foot", "r_leg_low", 20, null);
        skeleton.getBone("r_foot").x = 80;
        skeleton.getBone("r_foot").rotation = 90;
        
        // Legs (Left)
        skeleton.createBone("l_leg_up", "body", 80, null);
        skeleton.getBone("l_leg_up").rotation = -100;
        
        skeleton.createBone("l_leg_low", "l_leg_up", 80, null);
        skeleton.getBone("l_leg_low").x = 80;
        
        skeleton.createBone("l_foot", "l_leg_low", 20, null);
        skeleton.getBone("l_foot").x = 80;
        skeleton.getBone("l_foot").rotation = 90;
        
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
        // Right Arm
        NeonIKConstraint rArmIK = new NeonIKConstraint("r_arm_ik");
        rArmIK.setTarget(targetRightHand);
        rArmIK.addBone(skeleton.getBone("r_arm_low"));
        rArmIK.addBone(skeleton.getBone("r_arm_up"));
        rArmIK.bendPositive = true; // Elbow out
        rArmIK.mix = 0.0f; // Start with FK
        skeleton.addIKConstraint(rArmIK);
        
        // Left Arm
        NeonIKConstraint lArmIK = new NeonIKConstraint("l_arm_ik");
        lArmIK.setTarget(targetLeftHand);
        lArmIK.addBone(skeleton.getBone("l_arm_low"));
        lArmIK.addBone(skeleton.getBone("l_arm_up"));
        lArmIK.bendPositive = false; // Elbow out (other side)
        lArmIK.mix = 0.0f;
        skeleton.addIKConstraint(lArmIK);
        
        // Right Leg
        NeonIKConstraint rLegIK = new NeonIKConstraint("r_leg_ik");
        rLegIK.setTarget(targetRightFoot);
        rLegIK.addBone(skeleton.getBone("r_leg_low"));
        rLegIK.addBone(skeleton.getBone("r_leg_up"));
        rLegIK.bendPositive = true; // Knee forward
        rLegIK.mix = 0.0f;
        skeleton.addIKConstraint(rLegIK);
        
        // Left Leg
        NeonIKConstraint lLegIK = new NeonIKConstraint("l_leg_ik");
        lLegIK.setTarget(targetLeftFoot);
        lLegIK.addBone(skeleton.getBone("l_leg_low"));
        lLegIK.addBone(skeleton.getBone("l_leg_up"));
        lLegIK.bendPositive = true;
        lLegIK.mix = 0.0f;
        skeleton.addIKConstraint(lLegIK);
        
        skeleton.updateWorldTransform();
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
    
    private void unproject(int screenX, int screenY) {
        stage.getViewport().unproject(tempVec3.set(screenX, screenY, 0));
    }
    
    private void setupUI() {
        VisTable table = new VisTable(true);
        table.setFillParent(true);
        table.top().right();
        
        table.add(new VisLabel("Human Skeleton Demo")).row();
        table.add(new VisLabel("Drag Green Circles for IK")).row();
        
        // IK Control
        table.add(new VisLabel("--- IK Control ---")).row();
        
        final VisCheckBox handIKCheck = new VisCheckBox("Enable Hand IK", false);
        handIKCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                // Not ideal way to access constraints, should have map
                // But for demo assuming order or references
                // We didn't save references to constraints in class, let's just toggle all for now or specific
                // Better to just update mix based on UI
                float mix = handIKCheck.isChecked() ? 1f : 0f;
                updateIKMix(mix, "r_arm_ik", "l_arm_ik");
            }
        });
        table.add(handIKCheck).left().row();
        
        final VisCheckBox legIKCheck = new VisCheckBox("Enable Leg IK", false);
        legIKCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float mix = legIKCheck.isChecked() ? 1f : 0f;
                updateIKMix(mix, "r_leg_ik", "l_leg_ik");
            }
        });
        table.add(legIKCheck).left().row();
        
        // Animation Control
        table.add(new VisLabel("--- Animation ---")).row();
        
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
        table.add(walkCheck).left().row();
        
        final VisCheckBox waveCheck = new VisCheckBox("Overlay Wave (Track 1)", false);
        waveCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (waveCheck.isChecked()) {
                    waveTrack = animState.setAnimation(1, "wave", true, 0.5f); // 0.5s fade in
                } else {
                    if (waveTrack != null) {
                        // Fade out? Current API doesn't support easy fade out clearing
                        // But we can set alpha to 0 or clear track
                        animState.clearTrack(1);
                        waveTrack = null;
                    }
                }
            }
        });
        table.add(waveCheck).left().row();
        
        stage.addActor(table);
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
        super.render(delta);
        
        // Update
        animState.update(delta);
        animState.apply(skeleton, null);
        skeleton.updateWorldTransform();
        
        // Render Debug
        debugRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        drawBoneRecursive(skeleton.rootBone);
        
        // Draw Targets
        debugRenderer.setColor(Color.GREEN);
        drawTarget(targetRightHand);
        drawTarget(targetLeftHand);
        drawTarget(targetRightFoot);
        drawTarget(targetLeftFoot);
        
        debugRenderer.end();
        
        stage.act(delta);
        stage.draw();
    }
    
    private void drawTarget(NeonBone t) {
        if (t == null) return;
        float tx = t.worldTransform.m02;
        float ty = t.worldTransform.m12;
        debugRenderer.circle(tx, ty, 10);
    }
    
    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
        if (stage != null) stage.dispose();
        if (debugRenderer != null) debugRenderer.dispose();
    }
    
    private void drawBoneRecursive(NeonBone bone) {
        if (bone.name.startsWith("target")) return;
        
        float x = bone.worldTransform.m02;
        float y = bone.worldTransform.m12;
        float cos = bone.worldTransform.m00;
        float sin = bone.worldTransform.m10;
        float x2 = x + cos * bone.length;
        float y2 = y + sin * bone.length;
        
        debugRenderer.setColor(Color.WHITE);
        debugRenderer.line(x, y, x2, y2);
        debugRenderer.setColor(Color.RED);
        debugRenderer.circle(x, y, 3);
        
        for (NeonBone child : bone.children) {
            drawBoneRecursive(child);
        }
    }
}
