package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon.utils.NeonItemGenerator;
import com.goldsprite.magicdungeon.utils.NeonSpriteGenerator;
import com.goldsprite.magicdungeon.utils.NeonTileGenerator;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.magicdungeon.utils.NeonGenerator;
import com.goldsprite.magicdungeon.assets.ThemeConfig;

public class NeonGenTestScreen extends GScreen {

    private NeonBatch neonBatch;
    private SpriteBatch batch;
    private Stage stage;

    // Preview Data
    private TextureRegion bakedRegion;
    private OrthographicCamera fbCamera;

    // Settings
    public enum Mode { LIVE_VECTOR, BAKED_TEXTURE }
    private Mode currentMode = Mode.LIVE_VECTOR;

    public enum GeneratorType { CHARACTER, WALL, FLOOR, ITEM }
    private GeneratorType currentType = GeneratorType.CHARACTER;

    private int generateSize = 128;
    private String itemName = "Health Potion";
    private boolean stretchToFill = false;
    private float displayScale = 256f; // Pixel size for long edge in Fixed mode
    private long currentSeed = 12345;

    // Auto Refresh
    private boolean dirty = true;
    private float timeSinceLastRegen = 0;
    private static final float REGEN_INTERVAL = 1f / 30f;

    // UI
    private VisTable gameArea;
    private VisLabel infoLabel;

    @Override
    public void create() {
        if(!VisUI.isLoaded()) VisUI.load();

        batch = new SpriteBatch();
        neonBatch = new NeonBatch();
        stage = new Stage(getUIViewport(), batch);
        fbCamera = new OrthographicCamera();

        if(imp != null) imp.addProcessor(stage);

        setupUI();
        // Initial regen
        regenerate();
    }

    private void markDirty() {
        dirty = true;
    }

    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");

        // 1. Control Panel (Right)
        VisTable controls = new VisTable(true);
        // controls.setBackground("window"); // Removed background as requested
        controls.add(new VisLabel("Neon Generator")).pad(10).expandX().row();
        controls.addSeparator().padBottom(10).expandX().fillX();

        // Item Name Input
        final VisTable itemInputTable = new VisTable(true);
        itemInputTable.add(new VisLabel("Item Name:")).left();
        final VisTextField nameField = new VisTextField(itemName);
        nameField.setMessageText("Enter item name...");
        nameField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                itemName = nameField.getText();
                markDirty();
            }
        });
        itemInputTable.add(nameField).expandX().fillX();
        itemInputTable.setVisible(currentType == GeneratorType.ITEM);

        // Generator Select
        controls.add(new VisLabel("Type:")).left();
        final VisSelectBox<GeneratorType> typeSelect = new VisSelectBox<>();
        typeSelect.setItems(GeneratorType.values());
        typeSelect.setSelected(currentType);
        typeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentType = typeSelect.getSelected();
                // Update default size based on type
                if (currentType == GeneratorType.CHARACTER) generateSize = 256;
                else if (currentType == GeneratorType.WALL) generateSize = 64;
                else if (currentType == GeneratorType.FLOOR) generateSize = 32;
                else if (currentType == GeneratorType.ITEM) generateSize = 256;

                itemInputTable.setVisible(currentType == GeneratorType.ITEM);
                markDirty();
            }
        });
        controls.add(typeSelect).expandX().fillX().row();

        // Seed
        VisTable seedTable = new VisTable(true);
        seedTable.add(new VisLabel("Seed:")).left();
        final VisTextField seedField = new VisTextField(String.valueOf(currentSeed));
        seedField.setTextFieldFilter(new VisTextField.TextFieldFilter.DigitsOnlyFilter());
        seedField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    currentSeed = Long.parseLong(seedField.getText());
                    markDirty();
                } catch (NumberFormatException ignored) {}
            }
        });
        seedTable.add(seedField).expandX().fillX();

        VisTextButton randBtn = new VisTextButton("R");
        randBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentSeed = MathUtils.random.nextLong();
                seedField.setText(String.valueOf(currentSeed));
                markDirty();
            }
        });
        seedTable.add(randBtn);
        controls.add(seedTable).expandX().fillX().row();

        controls.add(itemInputTable).expandX().fillX().row();

        // Mode Select
        controls.add(new VisLabel("Mode:")).left();
        VisSelectBox<Mode> modeSelect = new VisSelectBox<>();
        modeSelect.setItems(Mode.values());
        modeSelect.setSelected(currentMode);
        modeSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentMode = modeSelect.getSelected();
                // No need to regen, just render mode change
            }
        });
        controls.add(modeSelect).expandX().fillX().row();

        controls.addSeparator().pad(10).expandX().fillX();

        // Generation Settings
        controls.add(new VisLabel("Generation Size:")).left();
        final VisLabel sizeLabel = new VisLabel(String.valueOf(generateSize));
        VisSlider sizeSlider = new VisSlider(16, 512, 16, false);
        sizeSlider.setValue(generateSize);
        sizeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                generateSize = (int) sizeSlider.getValue();
                sizeLabel.setText(String.valueOf(generateSize));
                markDirty();
            }
        });
        controls.add(sizeLabel).width(40);
        controls.row();
        controls.add(sizeSlider).colspan(2).fillX().padBottom(10).row();

        // Preview Settings
        controls.add(new VisLabel("Preview Settings")).colspan(2).left().row();

        final VisCheckBox stretchCheck = new VisCheckBox("Stretch (Fit)");
        stretchCheck.setChecked(stretchToFill);
        stretchCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stretchToFill = stretchCheck.isChecked();
            }
        });
        controls.add(stretchCheck).colspan(2).left().row();

        controls.add(new VisLabel("Fixed Size:")).left();
        final VisLabel scaleLabel = new VisLabel(String.valueOf((int)displayScale));
        VisSlider scaleSlider = new VisSlider(0, 1024, 64, false);
        scaleSlider.setValue(displayScale);
        scaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                displayScale = scaleSlider.getValue();
                if (displayScale == 0) {
                    scaleLabel.setText("Original");
                } else {
                    scaleLabel.setText(String.valueOf((int)displayScale));
                }
            }
        });
        controls.add(scaleLabel).width(60).row();
        controls.add(scaleSlider).colspan(2).fillX().row();

        controls.addSeparator().pad(10).expandX().fillX();

        infoLabel = new VisLabel("");
        controls.add(infoLabel).colspan(2).left().growY().top();

        // Wrap controls in HoverFocusScrollPane
        HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(controls);
        scrollPane.setFlickScroll(false); // Disable flick to prevent conflict with sliders
        scrollPane.setFadeScrollBars(false);

        // 2. Game Area (Left)
        gameArea = new VisTable();
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f)));

        VisSplitPane split = new VisSplitPane(gameArea, scrollPane, false);
        split.setSplitAmount(0.7f);
        split.setMaxSplitAmount(0.85f);
        split.setMinSplitAmount(0.15f);

        root.add(split).expand().fill();
        stage.addActor(root);
    }

    private void regenerate() {
        // Use the centralized NeonGenerator logic to ensure consistency
        // Note: NeonGenerator.generate returns a TextureRegion that is ready to draw (upright)
        bakedRegion = NeonGenerator.getInstance().generate(generateSize, generateSize, batch -> {
            drawContent(batch);
        });
    }

    private void drawContent(NeonBatch batch) {
        // Set deterministic random seed
        MathUtils.random.setSeed(currentSeed);

        // 生成器现在使用 0~1 标准化坐标，无需传递尺寸或处理缩放/翻转。
        // 外部通过矩阵控制最终大小。

        try {
            if (currentType == GeneratorType.CHARACTER) {
                NeonSpriteGenerator.drawCharacter(batch, "Sword", null, "Helmet", "Armor", "Boots");
            } else if (currentType == GeneratorType.WALL) {
                NeonTileGenerator.drawWallTileset(batch, Color.valueOf("#555555"), Color.valueOf("#3E3E3E"));
            } else if (currentType == GeneratorType.FLOOR) {
                NeonTileGenerator.drawFloor(batch, 
                    ThemeConfig.FLOOR_BASE,
                    ThemeConfig.FLOOR_DARK,
                    ThemeConfig.FLOOR_HIGHLIGHT);
            } else if (currentType == GeneratorType.ITEM) {
                NeonItemGenerator.drawItem(batch, itemName);
            }
        } finally {
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Auto Refresh Logic
        timeSinceLastRegen += delta;
        if (dirty && timeSinceLastRegen >= REGEN_INTERVAL) {
            regenerate();
            timeSinceLastRegen = 0;
            dirty = false;
        }

        stage.act(delta);
        stage.draw();

        drawSceneInGameArea();

        updateInfo();

        // Restore random state to avoid side effects
        MathUtils.random.setSeed(TimeUtils.nanoTime());
    }

    private void updateInfo() {
        String info = "Info:\n";
        info += "Mode: " + currentMode + "\n";
        info += "Gen Size: " + generateSize + "x" + generateSize + "\n";
        if (currentMode == Mode.LIVE_VECTOR) {
            info += "Rendering: Vector (Lossless)\n";
        } else {
            info += "Rendering: Texture (Pixels)\n";
        }
        info += "Stretch: " + (stretchToFill ? "Fit" : "Fixed") + "\n";
        if (!stretchToFill) {
            if (displayScale == 0) {
                info += "Display: Original (" + generateSize + "px)\n";
            } else {
                info += "Display: " + (int)displayScale + "px\n";
            }
        }

        infoLabel.setText(info);
    }

    private void drawSceneInGameArea() {
        Vector2 pos = gameArea.localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x;
        float y = pos.y;
        float w = gameArea.getWidth();
        float h = gameArea.getHeight();

        Rectangle scissor = new Rectangle();
        Rectangle clipBounds = new Rectangle(x, y, w, h);
        ScissorStack.calculateScissors(stage.getCamera(), stage.getBatch().getTransformMatrix(), clipBounds, scissor);

        if (ScissorStack.pushScissors(scissor)) {
            float cx, cy, cw, ch;

            if (currentMode == Mode.LIVE_VECTOR) {
                if (stretchToFill) {
                    // Fit to gameArea while maintaining aspect ratio (Square assumption for generators)
                    Vector2 scaled = Scaling.fit.apply(generateSize, generateSize, w, h);
                    cw = scaled.x;
                    ch = scaled.y;
                } else {
                    float size = (displayScale == 0) ? generateSize : displayScale;
                    cw = size;
                    ch = size;
                }
                cx = x + (w - cw) / 2;
                cy = y + (h - ch) / 2;

                neonBatch.setProjectionMatrix(stage.getCamera().combined);
                neonBatch.begin();

                // 使用矩阵将 0~1 的绘制指令缩放到目标尺寸
                // 现在的生成器产生的是正立的 0~1 坐标 (y-up)
                // 我们的 Stage 也是 y-up
                // 所以只需要 Translate 到位置，然后 Scale 到目标大小即可，不需要翻转
                neonBatch.getTransformMatrix().idt()
                    .translate(cx, cy, 0)
                    .scale(cw, ch, 1f); // Scale 0~1 to cw,ch
                neonBatch.setTransformMatrix(neonBatch.getTransformMatrix()); // Flush matrix

                // 注意：drawContent 不再需要传入宽高进行内部计算，
                // 因为生成器内部使用标准化坐标。
                // 我们只需要调用生成方法即可。
                drawContent(neonBatch);

                neonBatch.getTransformMatrix().idt();
                neonBatch.setTransformMatrix(neonBatch.getTransformMatrix()); // Reset matrix
                neonBatch.end();

            } else {
                if (bakedRegion != null) {
                    float regionW = bakedRegion.getRegionWidth();
                    float regionH = bakedRegion.getRegionHeight();

                    if (stretchToFill) {
                        Vector2 scaled = Scaling.fit.apply(regionW, regionH, w, h);
                        cw = scaled.x;
                        ch = scaled.y;
                    } else {
                        // Use displayScale but keep aspect ratio
                        float size = (displayScale == 0) ? generateSize : displayScale;
                        float ratio = regionW / regionH;
                        if (ratio >= 1) {
                            cw = size; ch = size / ratio;
                        } else {
                            ch = size; cw = size * ratio;
                        }
                    }
                    cx = x + (w - cw) / 2;
                    cy = y + (h - ch) / 2;

                    batch.setProjectionMatrix(stage.getCamera().combined);
                    batch.begin();
                    batch.draw(bakedRegion, cx, cy, cw, ch);
                    batch.end();
                }
            }

            ScissorStack.popScissors();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (batch != null) batch.dispose();
        if (neonBatch != null) neonBatch.dispose();
        if (stage != null) stage.dispose();
        // bakedRegion uses texture from NeonGenerator (managed there) or needs dispose?
        // NeonGenerator reuses FBO, but generate returns a new Texture from Pixmap.
        // So we own the texture in bakedRegion.
        if (bakedRegion != null && bakedRegion.getTexture() != null) {
            bakedRegion.getTexture().dispose();
        }
    }
}
