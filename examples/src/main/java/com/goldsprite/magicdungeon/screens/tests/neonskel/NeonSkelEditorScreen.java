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
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.badlogic.gdx.graphics.OrthographicCamera;

import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTree;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.Validators;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class NeonSkelEditorScreen extends GScreen {

    private NeonSkeleton skeleton;
    private AnimationState animState;
    private NeonBatch neonBatch;
    private SpriteBatch batch;
    private Stage stage;
    private SimpleCameraController cameraController;
    
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
        
        // Initialize Camera Controller
        // GScreen provides 'worldCamera'
        cameraController = new SimpleCameraController(getWorldCamera());
        
        if (imp != null) {
            imp.addProcessor(stage);
            imp.addProcessor(new DemoInputHandler());
            imp.addProcessor(cameraController);
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
        rArmIK.bendPositive = true; // Fixed: Elbow should bend naturally
        rArmIK.mix = 0.0f;
        skeleton.addIKConstraint(rArmIK);
        
        NeonIKConstraint lArmIK = new NeonIKConstraint("l_arm_ik");
        lArmIK.setTarget(targetLeftHand);
        lArmIK.addBone(skeleton.getBone("l_arm_low"));
        lArmIK.addBone(skeleton.getBone("l_arm_up"));
        lArmIK.bendPositive = true; // Fixed: Elbow should bend naturally
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
    
    // Expose for testing
    public EditorUI getEditorUI() {
        return editorUI;
    }
    
    private EditorUI editorUI;
    
    private void setupUI() {
        gameArea = new VisTable();
        // gameArea.setBackground("border"); // "border" 不存在, 使用 "white" 并设置颜色
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.15f, 0.15f, 0.15f, 1f)));
        
        // 创建 UI 控制器
        editorUI = new EditorUI(stage);
    }
    
    // --- UI Controller ---
    @SuppressWarnings({"unchecked"})
    public class EditorUI {
        public VisTable root;
        public VisTree tree;
        public VisTable propertiesTable;
        public VisSelectBox<Mode> modeSelect;
        
        public EditorUI(Stage stage) {
            root = new VisTable();
            root.setFillParent(true);
            
            // Layout: Left (Tree), Center (GameArea), Right (Properties)
            VisTable leftPanel = new VisTable();
            leftPanel.add(new VisLabel("Skeleton Tree")).pad(5).row();
            
            tree = new VisTree();
            VisScrollPane treePane = new VisScrollPane(tree);
            treePane.setFadeScrollBars(false);
            leftPanel.add(treePane).expand().fill().row();
            
            VisTable rightPanel = new VisTable();
            rightPanel.add(new VisLabel("Mode & Properties")).pad(5).row();
            
            modeSelect = new VisSelectBox<>();
            modeSelect.setItems(Mode.values());
            modeSelect.setSelected(currentMode);
            modeSelect.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (currentMode != modeSelect.getSelected()) {
                        currentMode = modeSelect.getSelected();
                        if (currentMode == Mode.SETUP) {
                            createSkeleton(); 
                            createAnimations();
                            rebuildTree();
                        }
                    }
                }
            });
            
            rightPanel.add(modeSelect).expandX().fillX().pad(5).row();
            rightPanel.addSeparator().pad(5).row();
            
            addGlobalSettings(rightPanel);
            rightPanel.addSeparator().pad(5).row();
            
            propertiesTable = new VisTable();
            rightPanel.add(propertiesTable).expand().fill().top();
            
            // SplitPane Layout
            // Left | Center(Game) | Right
            // Use nested split panes
            
            VisSplitPane splitRight = new VisSplitPane(gameArea, rightPanel, false);
            splitRight.setSplitAmount(0.8f);
            
            VisSplitPane splitMain = new VisSplitPane(leftPanel, splitRight, false);
            splitMain.setSplitAmount(0.2f);
            
            root.add(splitMain).expand().fill();
            
            stage.addActor(root);
            
            rebuildTree();
            
            // Tree Selection Listener
            tree.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    NeonBoneNode node = (NeonBoneNode) tree.getSelection().first();
                    if(node != null) {
                        NeonBone bone = node.bone;
                        // Also update draggingBone to highlight it?
                        draggingBone = bone;
                        updateProperties(bone);
                    }
                }
            });
        }
        
        public void rebuildTree() {
            tree.clearChildren();
            if(skeleton != null && skeleton.rootBone != null) {
                NeonBoneNode rootNode = buildNodeRecursive(skeleton.rootBone);
                tree.add(rootNode);
                rootNode.expandAll();
            }
        }
        
        private NeonBoneNode buildNodeRecursive(final NeonBone bone) {
            VisLabel label = new VisLabel(bone.name);
            NeonBoneNode node = new NeonBoneNode(label, bone);
            
            for(NeonBone child : bone.children) {
                node.add(buildNodeRecursive(child));
            }
            return node;
        }
        
        // 自定义 Node 类以避免 raw type 警告并正确继承
        class NeonBoneNode extends VisTree.Node {
            public final NeonBone bone;
            
            public NeonBoneNode(Actor actor, NeonBone bone) {
                super(actor);
                this.bone = bone;
            }
        }
        
        private void addGlobalSettings(VisTable panel) {
            VisLabel title = new VisLabel("Global Settings");
            panel.add(title).colspan(2).row();
            
            final VisCheckBox gizmoCheck = new VisCheckBox("Show Gizmos");
            gizmoCheck.setChecked(showGizmos);
            gizmoCheck.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showGizmos = gizmoCheck.isChecked();
                }
            });
            panel.add(gizmoCheck).left().colspan(2).row();
            
            // Animation Toggles
            final VisCheckBox walkToggle = new VisCheckBox("Walk Anim");
            walkToggle.setChecked(true);
            walkToggle.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                     if (walkToggle.isChecked()) {
                         walkTrack = animState.play("walk", true);
                     } else {
                         animState.clearTrack(0);
                         walkTrack = null;
                     }
                }
            });
            panel.add(walkToggle).left().colspan(2).row();
             
             final VisCheckBox waveToggle = new VisCheckBox("Wave Anim");
             waveToggle.setChecked(false);
             waveToggle.addListener(new ChangeListener() {
                 @Override
                 public void changed(ChangeEvent event, Actor actor) {
                     if (waveToggle.isChecked()) {
                         waveTrack = animState.setAnimation(1, "wave", true, 0.5f);
                     } else {
                         animState.clearTrack(1);
                         waveTrack = null;
                     }
                 }
             });
             panel.add(waveToggle).left().colspan(2).row();
             
             // IK Toggles
            final VisCheckBox ikToggle = new VisCheckBox("Enable IK");
            ikToggle.setChecked(true);
            ikToggle.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float mix = ikToggle.isChecked() ? 1f : 0f;
                    updateIKMix(mix, "r_leg_ik", "l_leg_ik", "r_arm_ik", "l_arm_ik");
                }
            });
            panel.add(ikToggle).left().colspan(2).row();
            
            final VisTextButton exportBtn = new VisTextButton("Export JSON (Log)");
            exportBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Json json = new Json();
                    json.setOutputType(JsonWriter.OutputType.json);
                    json.setUsePrototypes(false);
                    // 仅打印 rootBone 的结构
                    System.out.println(json.prettyPrint(skeleton.rootBone));
                }
            });
            panel.add(exportBtn).colspan(2).fillX().padTop(5).row();
        }
        
        public void updateProperties(final NeonBone bone) {
            propertiesTable.clear();
            if(bone == null) {
                propertiesTable.add(new VisLabel("No bone selected"));
                return;
            }
            
            propertiesTable.add(new VisLabel("Bone: " + bone.name)).colspan(2).row();
            propertiesTable.addSeparator().colspan(2).pad(5).row();
            
            // Properties
            addFloatInput("X", bone.x, v -> { bone.x = v; skeleton.updateWorldTransform(); });
            addFloatInput("Y", bone.y, v -> { bone.y = v; skeleton.updateWorldTransform(); });
            addFloatInput("Rotation", bone.rotation, v -> { bone.rotation = v; skeleton.updateWorldTransform(); });
            addFloatInput("Length", bone.length, v -> { bone.length = v; });
            addFloatInput("ScaleX", bone.scaleX, v -> { bone.scaleX = v; skeleton.updateWorldTransform(); });
            addFloatInput("ScaleY", bone.scaleY, v -> { bone.scaleY = v; skeleton.updateWorldTransform(); });
        }
        
        private void addFloatInput(String label, float value, final FloatConsumer consumer) {
            propertiesTable.add(new VisLabel(label)).right().padRight(5);
            final VisTextField field = new VisTextField(String.valueOf(value));
            
            // Use ChangeListener instead of TextFieldListener for better compatibility
            field.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    try {
                        float val = Float.parseFloat(field.getText());
                        consumer.accept(val);
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
            });
            
            propertiesTable.add(field).width(60).row();
        }
        
        // Simple functional interface for Java 7/8 compat (if needed, or use java.util.function)
        interface FloatConsumer {
            void accept(float value);
        }
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
        
        // 更新相机视口大小以匹配游戏区域
        OrthographicCamera cam = getWorldCamera();
        //cam.viewportWidth = w;
        //cam.viewportHeight = h;
        cam.update();

        Rectangle scissor = new Rectangle();
        Rectangle clipBounds = new Rectangle(x, y, w, h);
        ScissorStack.calculateScissors(stage.getCamera(), stage.getBatch().getTransformMatrix(), clipBounds, scissor);
        if (ScissorStack.pushScissors(scissor)) {
            if (currentMode == Mode.ANIMATE) {
                animState.update(delta);
                animState.apply(skeleton, null);
            }
            
            // 骨骼位置固定在世界原点
            skeleton.setPosition(0, 0);
            skeleton.updateWorldTransform();
            
            // 使用世界相机的投影矩阵
            neonBatch.setProjectionMatrix(cam.combined);
            neonBatch.begin();
            
            // 绘制网格线作为参考
            drawGrid(neonBatch);

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
    
    private void drawGrid(NeonBatch batch) {
        // Simple grid
        Color axisColor = new Color(0.5f, 0.5f, 0.5f, 0.5f);
        batch.drawRect(-1000, -1, 2000, 2, 0, 0, axisColor, false); // X Axis
        batch.drawRect(-1, -1000, 2, 2000, 0, 0, axisColor, false); // Y Axis
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
        
        private boolean unprojectToWorld(int screenX, int screenY, Vector3 out) {
            // 1. 获取鼠标在 Stage 上的位置
            Vector2 stagePos = stage.screenToStageCoordinates(new Vector2(screenX, screenY));
            
            // 2. 检查是否在 gameArea 内
            Vector2 areaPos = gameArea.localToStageCoordinates(new Vector2(0, 0));
            Rectangle areaRect = new Rectangle(areaPos.x, areaPos.y, gameArea.getWidth(), gameArea.getHeight());
            if (!areaRect.contains(stagePos)) return false;

            // 3. 转换为世界坐标
            // 鼠标相对于 gameArea 中心的偏移
            float centerX = areaPos.x + gameArea.getWidth() / 2f;
            float centerY = areaPos.y + gameArea.getHeight() / 2f;
            
            float dx = stagePos.x - centerX;
            float dy = stagePos.y - centerY;
            
            OrthographicCamera cam = getWorldCamera();
            float worldX = cam.position.x + dx * cam.zoom;
            float worldY = cam.position.y + dy * cam.zoom;
            
            out.set(worldX, worldY, 0);
            return true;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            if (!unprojectToWorld(screenX, screenY, tempVec3)) return false;

            // Check targets
            NeonBone[] targets = {targetLeftHand, targetRightHand, targetLeftFoot, targetRightFoot};
            for (NeonBone t : targets) {
                if (t == null) continue;
                float dist = Vector2.dst(tempVec3.x, tempVec3.y, t.worldTransform.m02, t.worldTransform.m12);
                OrthographicCamera cam = getWorldCamera();
                // 拾取半径随缩放调整，保证视觉上一致
                if (dist < 20 * cam.zoom) {
                    draggingBone = t;
                    return true;
                }
            }
            
            // 任务2: 配置模式下允许移动任意骨骼
            if (currentMode == Mode.SETUP) {
                // 遍历所有骨骼寻找最近的
                NeonBone bestMatch = null;
                float minDst = 20 * getWorldCamera().zoom;
                
                // 简单的全遍历
                // 这里我们没有方便的 getAllBones 列表，只能递归或者访问 map values
                // NeonSkeleton 并没有公开 boneMap.values()，但有 getBone(name)
                // 我们需要一个遍历所有骨骼的方法。
                // 暂时只支持 root 下的一级骨骼或者硬编码的几个
                // 或者我们可以给 NeonSkeleton 加个 getAllBones()
                
                // 暂时只处理 targets 以外的骨骼?
                // 实际上我们可以通过递归 rootBone 来查找
                bestMatch = findClosestBone(skeleton.rootBone, tempVec3.x, tempVec3.y, minDst);
                if(bestMatch != null) {
                    draggingBone = bestMatch;
                    return true;
                }
            }
            
            return false;
        }
        
        private NeonBone findClosestBone(NeonBone bone, float x, float y, float maxDist) {
            if(bone == null) return null;
            NeonBone best = null;
            float minDist = maxDist;
            
            // Check self
            float d = Vector2.dst(x, y, bone.worldTransform.m02, bone.worldTransform.m12);
            if(d < minDist) {
                minDist = d;
                best = bone;
            }
            
            // Check children
            for(NeonBone child : bone.children) {
                NeonBone res = findClosestBone(child, x, y, minDist);
                if(res != null) {
                    float dc = Vector2.dst(x, y, res.worldTransform.m02, res.worldTransform.m12);
                    if(dc < minDist) {
                        minDist = dc;
                        best = res;
                    }
                }
            }
            return best;
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (draggingBone != null) {
                if (!unprojectToWorld(screenX, screenY, tempVec3)) return true; // Keep dragging even if out of bounds? Maybe.

                // Task 2 & IK Target Dragging logic
                // Update bone position
                
                if (currentMode == Mode.ANIMATE) {
                     // In Animate mode, we only drag IK targets (which are usually independent or children of root)
                     // Assuming they are children of root for now as per setup
                     draggingBone.x = tempVec3.x;
                     draggingBone.y = tempVec3.y;
                } else {
                    // In Setup mode, we drag bones to change their bind pose (local transform)
                    // If bone has parent, we need to convert world pos to parent's local space
                    NeonBone parent = draggingBone.parent;
                    if (parent != null) {
                        // Parent world transform
                        // World = ParentWorld * Local
                        // Local = ParentWorldInv * World
                        // We need to invert parent world transform
                        
                        // Affine2 inv = new Affine2(parent.worldTransform).inv(); // Affine2 doesn't have easy inv() in LibGDX?
                        // Matrix3 has inv(). Affine2 is efficient.
                        // Let's manually calculate or use Matrix4 if needed.
                        // Affine2: [m00 m01 m02]
                        //          [m10 m11 m12]
                        
                        // Simple approach: unrotate and untranslate
                        // dx = worldX - parentWorldX
                        // dy = worldY - parentWorldY
                        // localX = dx * cos(-ang) - dy * sin(-ang)
                        // localY = dx * sin(-ang) + dy * cos(-ang)
                        // This assumes uniform scale. If parent has scale, it's more complex.
                        
                        float parentWorldX = parent.worldTransform.m02;
                        float parentWorldY = parent.worldTransform.m12;
                        float dx = tempVec3.x - parentWorldX;
                        float dy = tempVec3.y - parentWorldY;
                        
                        // Extract parent world rotation
                        float parentRot = (float) Math.atan2(parent.worldTransform.m10, parent.worldTransform.m00);
                        float cos = (float) Math.cos(-parentRot);
                        float sin = (float) Math.sin(-parentRot);
                        
                        // Apply inverse rotation
                        float localX = dx * cos - dy * sin;
                        float localY = dx * sin + dy * cos;
                        
                        // Apply inverse scale (if any)
                        // Assuming uniform scale for now or extracting from matrix
                        float parentScaleX = (float) Math.sqrt(parent.worldTransform.m00 * parent.worldTransform.m00 + parent.worldTransform.m10 * parent.worldTransform.m10);
                        float parentScaleY = (float) Math.sqrt(parent.worldTransform.m01 * parent.worldTransform.m01 + parent.worldTransform.m11 * parent.worldTransform.m11);
                        
                        draggingBone.x = localX / parentScaleX;
                        draggingBone.y = localY / parentScaleY;
                        
                    } else {
                        // No parent (Root)
                        draggingBone.x = tempVec3.x;
                        draggingBone.y = tempVec3.y;
                    }
                    
                    // Update whole skeleton to see changes immediately
                    skeleton.updateWorldTransform();
                }
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
