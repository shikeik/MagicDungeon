package com.goldsprite.magicdungeon.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.magicdungeon.core.screens.GameScreen;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class LoadingScreen extends GScreen {
    private String saveName;
    private String playerName;
    private boolean isNewGame;

    private PolygonSpriteBatch polyBatch;
    private SkeletonRenderer skeletonRenderer;
    private Skeleton skeleton;
    private AnimationState animationState;
    private VisLabel statusLabel;
    private VisTextButton backButton;
    private com.badlogic.gdx.scenes.scene2d.Stage stage;

    private boolean taskStarted = false;
    private boolean taskFinished = false;
    private Exception taskError;

    public LoadingScreen(String saveName, String playerName, boolean isNewGame) {
        this.saveName = saveName;
        this.playerName = playerName;
        this.isNewGame = isNewGame;
    }

    @Override
    public void create() {
        polyBatch = new PolygonSpriteBatch();
        skeletonRenderer = new SkeletonRenderer();
        skeletonRenderer.setPremultipliedAlpha(true);

        loadSpine();
        initUI();
    }

    private void loadSpine() {
        TextureAtlas atlas = new TextureAtlas(Gdx.files.internal("spines/large_sworder/large_sworder.atlas"));
        SkeletonJson json = new SkeletonJson(atlas);
        json.setScale(1.0f);
        SkeletonData skeletonData = json.readSkeletonData(Gdx.files.internal("spines/large_sworder/large_sworder.json"));

        skeleton = new Skeleton(skeletonData);
        skeleton.setPosition(getViewSize().x / 2, getViewSize().y / 2 - 100);

        AnimationStateData stateData = new AnimationStateData(skeletonData);
        animationState = new AnimationState(stateData);
        animationState.setAnimation(0, "move", true); // Assuming "run" animation exists
    }

    private void initUI() {
        stage = new com.badlogic.gdx.scenes.scene2d.Stage(getUIViewport());
        VisTable table = new VisTable();
        table.setFillParent(true);
        statusLabel = new VisLabel("Loading...");
        statusLabel.setColor(Color.WHITE);
        table.add(statusLabel).bottom().padBottom(20).row();
        
        backButton = new VisTextButton("返回主菜单");
        backButton.setVisible(false);
        backButton.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                com.goldsprite.gdengine.screens.ScreenManager.getInstance().setCurScreen(new com.goldsprite.magicdungeon.core.screens.MainMenuScreen());
            }
        });
        table.add(backButton).bottom().padBottom(30);
        
        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        animationState.update(delta);
        animationState.apply(skeleton);
        skeleton.updateWorldTransform();

        polyBatch.begin();
        skeletonRenderer.draw(polyBatch, skeleton);
        polyBatch.end();
        
        if (stage != null) {
            stage.act(delta);
            stage.draw();
        }

        super.render(delta);

        if (!taskStarted) {
            startTask();
        }

        if (taskFinished) {
            if (taskError != null) {
                statusLabel.setText("Error: " + taskError.getMessage());
                if (!backButton.isVisible()) {
                    backButton.setVisible(true);
                    // Ensure stage handles input for the button
                    Gdx.input.setInputProcessor(stage);
                }
            } else {
                // Transition to GameScreen
                ScreenManager.getInstance().setCurScreen(new GameScreen(saveName));
            }
        }
    }

    private void startTask() {
        taskStarted = true;
        new Thread(() -> {
            try {
                if (isNewGame) {
                    Gdx.app.postRunnable(() -> statusLabel.setText("Creating World..."));
                    SaveManager.createSave(saveName, playerName);
                } else {
                    Gdx.app.postRunnable(() -> statusLabel.setText("Loading Save..."));
                    // Simulate loading delay or verify save
                    Thread.sleep(500);
                }
                taskFinished = true;
            } catch (Exception e) {
                taskError = e;
                taskFinished = true;
                e.printStackTrace();
            }
        }).start();
    }

    @Override
    public void dispose() {
        polyBatch.dispose();
        if (stage != null) stage.dispose();
        // Atlas disposal logic?
        super.dispose();
    }
}
