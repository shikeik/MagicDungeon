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
import com.goldsprite.neonskel.logic.NeonSkeleton;

public class NeonIKTestScreen extends GScreen {

    private NeonSkeleton skeleton;
    private ShapeRenderer debugRenderer;
    private SpriteBatch batch;
    private Stage stage;
    
    private NeonBone targetBone;
    private NeonIKConstraint ikConstraint;
    
    private boolean isDraggingTarget = false;
    private Vector3 tempVec3 = new Vector3();

    @Override
    public void create() {
        super.create();
        
        batch = new SpriteBatch();
        stage = new Stage(getUIViewport(), batch);
        
        if (imp != null) {
            imp.addProcessor(stage);
            imp.addProcessor(new InputAdapter() {
                @Override
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    // Check if clicked near target
                    unproject(screenX, screenY);
                    float dist = Vector2.dst(tempVec3.x, tempVec3.y, targetBone.x, targetBone.y);
                    if (dist < 20) {
                        isDraggingTarget = true;
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean touchDragged(int screenX, int screenY, int pointer) {
                    if (isDraggingTarget) {
                        unproject(screenX, screenY);
                        // Update target bone local position (relative to skeleton root, which is 0,0 in local space of root)
                        // But targetBone is child of root? No, let's make it child of root for simplicity, or separate.
                        // If it's a child of root, setting x/y sets local pos.
                        // Since root is at 400,300, and targetBone is child of root,
                        // we need to convert world mouse pos to root local space.
                        
                        // Root transform: translate(400, 300)
                        // Inverse: translate(-400, -300)
                        
                        targetBone.x = tempVec3.x - skeleton.x;
                        targetBone.y = tempVec3.y - skeleton.y;
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                    isDraggingTarget = false;
                    return false;
                }
            });
        }
        
        // 1. 创建骨骼
        skeleton = new NeonSkeleton();
        skeleton.setPosition(400, 300);
        
        // Body Structure: Root -> Body -> UpperArm -> LowerArm -> Hand
        skeleton.createBone("body", "root", 0, null);
        
        skeleton.createBone("upper_arm", "body", 100, null);
        skeleton.getBone("upper_arm").rotation = 0; // Point right
        
        skeleton.createBone("lower_arm", "upper_arm", 80, null);
        skeleton.getBone("lower_arm").rotation = 0; // Point right
        
        skeleton.createBone("hand", "lower_arm", 30, null);
        
        // Target Bone (Independent, attached to root for coordinate convenience)
        skeleton.createBone("target", "root", 10, null);
        targetBone = skeleton.getBone("target");
        targetBone.x = 150;
        targetBone.y = 50;
        
        // 2. Setup IK
        ikConstraint = new NeonIKConstraint("arm_ik");
        ikConstraint.setTarget(targetBone);
        ikConstraint.addBone(skeleton.getBone("lower_arm")); // Child
        ikConstraint.addBone(skeleton.getBone("upper_arm")); // Parent
        ikConstraint.bendPositive = true;
        
        skeleton.addIKConstraint(ikConstraint);
        
        // 3. Renderer
        debugRenderer = new ShapeRenderer();
        
        // 4. UI
        setupUI();
    }
    
    private void unproject(int screenX, int screenY) {
        // We use UI Viewport for everything here for simplicity
        stage.getViewport().unproject(tempVec3.set(screenX, screenY, 0));
    }
    
    private void setupUI() {
        VisTable table = new VisTable(true);
        table.setFillParent(true);
        table.top().right();
        
        table.add(new VisLabel("Neon IK Test (2-Bone)")).row();
        table.add(new VisLabel("Drag the Green Target")).row();
        
        // Bend Direction
        final VisCheckBox bendCheck = new VisCheckBox("Bend Positive", true);
        bendCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ikConstraint.bendPositive = bendCheck.isChecked();
            }
        });
        table.add(bendCheck).left().row();
        
        // Mix
        table.add(new VisLabel("IK Mix:")).row();
        final VisSlider mixSlider = new VisSlider(0, 1, 0.01f, false);
        mixSlider.setValue(1.0f);
        mixSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ikConstraint.mix = mixSlider.getValue();
            }
        });
        table.add(mixSlider).width(200).row();
        
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        super.render(delta);
        
        // Update
        skeleton.updateWorldTransform();
        
        // Render Debug
        debugRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        debugRenderer.begin(ShapeRenderer.ShapeType.Line);
        
        drawBoneRecursive(skeleton.rootBone);
        
        // Draw Target specially
        debugRenderer.setColor(Color.GREEN);
        float tx = targetBone.worldTransform.m02;
        float ty = targetBone.worldTransform.m12;
        debugRenderer.circle(tx, ty, 10);
        debugRenderer.line(tx-15, ty, tx+15, ty);
        debugRenderer.line(tx, ty-15, tx, ty+15);
        
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
        // Skip target bone in regular draw
        if (bone.name.equals("target")) return;
        
        float x = bone.worldTransform.m02;
        float y = bone.worldTransform.m12;
        
        float cos = bone.worldTransform.m00;
        float sin = bone.worldTransform.m10;
        
        float x2 = x + cos * bone.length;
        float y2 = y + sin * bone.length;
        
        // Color coding
        if (ikConstraint.bones.contains(bone, true)) {
            debugRenderer.setColor(Color.CYAN);
        } else {
            debugRenderer.setColor(Color.WHITE);
        }
        
        debugRenderer.line(x, y, x2, y2);
        
        debugRenderer.setColor(Color.RED);
        debugRenderer.circle(x, y, 3); // Joint
        
        for (NeonBone child : bone.children) {
            drawBoneRecursive(child);
        }
    }
}
