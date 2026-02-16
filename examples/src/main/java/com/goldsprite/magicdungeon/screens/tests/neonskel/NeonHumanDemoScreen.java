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
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.kotcrab.vis.ui.widget.VisTextButton;
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
    private VisCheckBox walkCheck, waveCheck, handIKCheck, legIKCheck;
    
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
    
    public enum Mode { SETUP, ANIMATE }
    private Mode currentMode = Mode.ANIMATE;

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
        skeleton.getBone("torso").rotation = 90; // Up
        
        skeleton.createBone("head", "torso", 40, null);
        skeleton.getBone("head").x = 100;
        
        // --- Head Details ---
        // Jaw
        skeleton.createBone("jaw", "head", 20, null);
        skeleton.getBone("jaw").x = 10;
        skeleton.getBone("jaw").y = -5;
        skeleton.getBone("jaw").rotation = -30;
        
        // Hair (Fan Distribution from Top of Head)
        // Head tip is at (40, 0) in local space
        float headTipX = 40;
        
        // Back hair (Fan: 160-220 deg relative to head)
        createHairCluster("hair_back", "head", headTipX, 0, 180, 8, 10f, 50f);
        // Top/Front hair (Fan: 60-120 deg)
        createHairCluster("hair_top", "head", headTipX, 5, 90, 6, 15f, 30f);
        // Bangs (Front: 30-60 deg)
        createHairCluster("hair_bangs", "head", headTipX-5, 10, 45, 5, 10f, 25f);
        
        // Arms (Right - Front)
        skeleton.createBone("r_arm_up", "torso", 70, null);
        skeleton.getBone("r_arm_up").x = 90;
        skeleton.getBone("r_arm_up").rotation = -110; // Down-Right
        
        skeleton.createBone("r_arm_low", "r_arm_up", 60, null);
        skeleton.getBone("r_arm_low").x = 70;
        
        createHand("r_hand", "r_arm_low");
        
        // Arms (Left - Back)
        skeleton.createBone("l_arm_up", "torso", 70, null);
        skeleton.getBone("l_arm_up").x = 90;
        skeleton.getBone("l_arm_up").rotation = -70; // Down-Left (Back)
        
        skeleton.createBone("l_arm_low", "l_arm_up", 60, null);
        skeleton.getBone("l_arm_low").x = 70;
        
        createHand("l_hand", "l_arm_low");
        
        // Legs (Right - Front)
        skeleton.createBone("r_leg_up", "body", 80, null);
        skeleton.getBone("r_leg_up").rotation = -80;
        
        skeleton.createBone("r_leg_low", "r_leg_up", 80, null);
        skeleton.getBone("r_leg_low").x = 80;
        
        createFoot("r_foot", "r_leg_low");
        
        // Legs (Left - Back)
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
        rArmIK.bendPositive = false; // Elbow Left (Back)
        rArmIK.mix = 0.0f;
        skeleton.addIKConstraint(rArmIK);
        
        NeonIKConstraint lArmIK = new NeonIKConstraint("l_arm_ik");
        lArmIK.setTarget(targetLeftHand);
        lArmIK.addBone(skeleton.getBone("l_arm_low"));
        lArmIK.addBone(skeleton.getBone("l_arm_up"));
        lArmIK.bendPositive = false; // Elbow Left (Back)
        lArmIK.mix = 0.0f;
        skeleton.addIKConstraint(lArmIK);
        
        NeonIKConstraint rLegIK = new NeonIKConstraint("r_leg_ik");
        rLegIK.setTarget(targetRightFoot);
        rLegIK.addBone(skeleton.getBone("r_leg_low"));
        rLegIK.addBone(skeleton.getBone("r_leg_up"));
        rLegIK.bendPositive = true; // Knee Forward (Right)
        rLegIK.mix = 0.0f;
        skeleton.addIKConstraint(rLegIK);
        
        NeonIKConstraint lLegIK = new NeonIKConstraint("l_leg_ik");
        lLegIK.setTarget(targetLeftFoot);
        lLegIK.addBone(skeleton.getBone("l_leg_low"));
        lLegIK.addBone(skeleton.getBone("l_leg_up"));
        lLegIK.bendPositive = true; // Knee Forward (Right)
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
        
        // Mode Switch
        VisTable modeTable = new VisTable();
        final VisTextButton setupBtn = new VisTextButton("Setup", "toggle");
        final VisTextButton animateBtn = new VisTextButton("Animate", "toggle");
        ButtonGroup<VisTextButton> modeGroup = new ButtonGroup<>(setupBtn, animateBtn);
        modeGroup.setMaxCheckCount(1);
        modeGroup.setMinCheckCount(1);
        
        setupBtn.setChecked(currentMode == Mode.SETUP);
        animateBtn.setChecked(currentMode == Mode.ANIMATE);
        
        ChangeListener modeListener = new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (setupBtn.isChecked()) {
                    if (currentMode != Mode.SETUP) {
                        currentMode = Mode.SETUP;
                        // Reset to bind pose
                        createSkeleton(); 
                        createAnimations(); // Re-bind animations
                    }
                } else {
                    currentMode = Mode.ANIMATE;
                }
            }
        };
        setupBtn.addListener(modeListener);
        animateBtn.addListener(modeListener);
        
        modeTable.add(setupBtn).width(80).padRight(5);
        modeTable.add(animateBtn).width(80);
        panel.add(modeTable).padBottom(10).row();
        
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
        
        handIKCheck = new VisCheckBox("Enable Hand IK", false);
        handIKCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float mix = handIKCheck.isChecked() ? 1f : 0f;
                updateIKMix(mix, "r_arm_ik", "l_arm_ik");
            }
        });
        panel.add(handIKCheck).left().row();
        
        legIKCheck = new VisCheckBox("Enable Leg IK", false);
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
        
        walkCheck = new VisCheckBox("Play Walk (Base)", true);
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
        
        waveCheck = new VisCheckBox("Overlay Wave (Track 1)", false);
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
            if (currentMode == Mode.ANIMATE) {
                animState.update(delta);
                animState.apply(skeleton, null);
            }
            
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
        
        Color color = Color.CYAN;
        float thickness = 10f; // Body parts width
        
        if (bone.name.contains("hair")) {
            color = Color.MAGENTA;
            thickness = 2f;
        } else if (bone.name.contains("finger") || bone.name.contains("toes")) {
            color = Color.YELLOW;
            thickness = 4f;
        } else if (bone.name.contains("hand") || bone.name.contains("foot")) {
            thickness = 6f;
        }
        
        if (bone.length > 0) {
            if (thickness <= 2f) {
                // Draw line for hair
                float x = bone.worldTransform.m02;
                float y = bone.worldTransform.m12;
                float cos = bone.worldTransform.m00;
                float sin = bone.worldTransform.m10;
                float x2 = x + cos * bone.length;
                float y2 = y + sin * bone.length;
                neonBatch.drawLine(x, y, x2, y2, thickness, color);
            } else {
                // Draw hollow rect
                float w = thickness;
                float l = bone.length;
                float halfW = w / 2f;
                
                // 4 Local corners: (0, -halfW), (l, -halfW), (l, halfW), (0, halfW)
                float[] lx = {0, l, l, 0};
                float[] ly = {-halfW, -halfW, halfW, halfW};
                
                float[] verts = new float[8];
                float m00 = bone.worldTransform.m00;
                float m01 = bone.worldTransform.m01;
                float m02 = bone.worldTransform.m02;
                float m10 = bone.worldTransform.m10;
                float m11 = bone.worldTransform.m11;
                float m12 = bone.worldTransform.m12;
                
                for(int i=0; i<4; i++) {
                    verts[i*2] = m00 * lx[i] + m01 * ly[i] + m02;
                    verts[i*2+1] = m10 * lx[i] + m11 * ly[i] + m12;
                }
                
                neonBatch.drawPolygon(verts, 2f, color, false);
                
                if (bone.name.equals("head")) {
                     // Draw head oval
                     float cx = l * 0.5f;
                     float cy = 0;
                     
                     // Transform (cx, cy) to world
                     float wx = m00 * cx + m01 * cy + m02;
                     float wy = m10 * cx + m11 * cy + m12;
                     
                     float rotRad = (float)Math.atan2(m10, m00);
                     float rotDeg = rotRad * MathUtils.radiansToDegrees;
                     
                     // Oval size: width=30, height=40 (along bone)
                     // Since bone is X-axis, "width" in drawOval corresponds to X-axis if rot=0
                     neonBatch.drawOval(wx, wy, l, l*0.8f, rotDeg, 2f, color, 20, false);
                }
            }
        }
        
        if (showGizmos) {
            float x = bone.worldTransform.m02;
            float y = bone.worldTransform.m12;
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
