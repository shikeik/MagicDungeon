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
import com.goldsprite.magicdungeon.assets.ThemeConfig;
import com.goldsprite.magicdungeon.utils.NeonItemGenerator;
import com.goldsprite.magicdungeon.utils.NeonSpriteGenerator;
import com.goldsprite.magicdungeon.utils.NeonTileGenerator;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class NeonGenTestScreen extends GScreen {

    private NeonBatch neonBatch;
    private SpriteBatch batch;
    private Stage stage;

    // Preview Data
    private FrameBuffer frameBuffer;
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
        regenerate();
    }

    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");

        // 1. Control Panel (Right)
        VisTable controls = new VisTable(true);
        controls.setBackground("window");
        controls.add(new VisLabel("Neon Generator")).pad(10).row();
        controls.addSeparator().padBottom(10).fillX();

        // Item Name Input (Created first for reference, added later)
        final VisTable itemInputTable = new VisTable(true);
        itemInputTable.add(new VisLabel("Item Name:")).left();
        final VisTextField nameField = new VisTextField(itemName);
        nameField.setMessageText("Enter item name...");
        nameField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                itemName = nameField.getText();
                regenerate();
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
                if (currentType == GeneratorType.CHARACTER) generateSize = 128;
                else if (currentType == GeneratorType.WALL) generateSize = 64;
                else if (currentType == GeneratorType.FLOOR) generateSize = 32;
                else if (currentType == GeneratorType.ITEM) generateSize = 128;
                
                itemInputTable.setVisible(currentType == GeneratorType.ITEM);
                
                // Refresh UI if needed (slider value)
                regenerate();
            }
        });
        controls.add(typeSelect).expandX().fillX().row();
        
        // Add Item Input Table
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
            }
        });
        controls.add(modeSelect).expandX().fillX().row();

        controls.addSeparator().pad(10).fillX();

        // Generation Settings
        controls.add(new VisLabel("Generation Size:")).left();
        VisSlider sizeSlider = new VisSlider(32, 512, 32, false);
        sizeSlider.setValue(generateSize);
        final VisLabel sizeLabel = new VisLabel(String.valueOf(generateSize));
        sizeSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                generateSize = (int) sizeSlider.getValue();
                sizeLabel.setText(String.valueOf(generateSize));
            }
        });
        controls.add(sizeLabel).width(40);
        controls.row();
        controls.add(sizeSlider).colspan(2).fillX().padBottom(10).row();

        VisTextButton regenBtn = new VisTextButton("Regenerate / Bake");
        regenBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                regenerate();
            }
        });
        controls.add(regenBtn).colspan(2).fillX().padBottom(20).row();

        // Preview Settings
        controls.add(new VisLabel("Preview Settings")).colspan(2).left().row();
        
        final VisCheckBox stretchCheck = new VisCheckBox("Stretch to Fill");
        stretchCheck.setChecked(stretchToFill);
        stretchCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                stretchToFill = stretchCheck.isChecked();
            }
        });
        controls.add(stretchCheck).colspan(2).left().row();

        controls.add(new VisLabel("Fixed Size:")).left();
        VisSlider scaleSlider = new VisSlider(64, 1024, 64, false);
        scaleSlider.setValue(displayScale);
        final VisLabel scaleLabel = new VisLabel(String.valueOf((int)displayScale));
        scaleSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                displayScale = scaleSlider.getValue();
                scaleLabel.setText(String.valueOf((int)displayScale));
            }
        });
        controls.add(scaleLabel).width(40).row();
        controls.add(scaleSlider).colspan(2).fillX().row();

        controls.addSeparator().pad(10).fillX();
        
        infoLabel = new VisLabel("");
        controls.add(infoLabel).colspan(2).left().growY().top();

        // 2. Game Area (Left)
        gameArea = new VisTable();
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f)));

        // Split Pane
        VisSplitPane split = new VisSplitPane(gameArea, controls, false);
        split.setSplitAmount(0.7f);
        split.setMaxSplitAmount(0.85f);
        split.setMinSplitAmount(0.15f);

        root.add(split).expand().fill();
        stage.addActor(root);
    }

    private void regenerate() {
        // Only needed for Baked mode or to update generation parameters
        if (frameBuffer != null) {
            frameBuffer.dispose();
        }
        try {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, generateSize, generateSize, false);
            
            fbCamera.setToOrtho(false, generateSize, generateSize);
            fbCamera.update();
            
            frameBuffer.begin();
            ScreenUtils.clear(0, 0, 0, 0);
            
            neonBatch.setProjectionMatrix(fbCamera.combined);
            neonBatch.begin();
            // Call the shared drawing logic
            drawContent(neonBatch, generateSize);
            neonBatch.end();
            
            frameBuffer.end();
            
            Texture tex = frameBuffer.getColorBufferTexture();
            tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            
            bakedRegion = new TextureRegion(tex);
            bakedRegion.flip(false, true); // FB flip
            
        } catch (Exception e) {
            Gdx.app.error("NeonGen", "Generation failed", e);
        }
    }

    private void drawContent(NeonBatch batch, float size) {
        if (currentType == GeneratorType.CHARACTER) {
            NeonSpriteGenerator.drawCharacter(batch, size, "Sword", null, "Helmet", "Armor", "Boots");
        } else if (currentType == GeneratorType.WALL) {
            // Use default colors from SpriteGenerator logic
            NeonTileGenerator.drawWallTileset(batch, size, Color.valueOf("#555555"), Color.valueOf("#3E3E3E"));
        } else if (currentType == GeneratorType.FLOOR) {
            // Use default colors
            NeonTileGenerator.drawFloor(batch, size, 
                com.goldsprite.magicdungeon.assets.ThemeConfig.FLOOR_BASE, 
                com.goldsprite.magicdungeon.assets.ThemeConfig.FLOOR_DARK, 
                com.goldsprite.magicdungeon.assets.ThemeConfig.FLOOR_HIGHLIGHT);
        } else if (currentType == GeneratorType.ITEM) {
            NeonItemGenerator.drawItem(batch, size, itemName);
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        drawSceneInGameArea();
        
        updateInfo();
    }
    
    private void updateInfo() {
        String info = "Info:\n";
        info += "Mode: " + currentMode + "\n";
        info += "Gen Size: " + generateSize + "x" + generateSize + "\n";
        if (currentMode == Mode.LIVE_VECTOR) {
            info += "Rendering: Vector (Lossless)\n";
        } else {
            info += "Rendering: Texture (Pixels)\n";
            info += "Stretch: " + stretchToFill + "\n";
            if (!stretchToFill) info += "Display: " + (int)displayScale + "px\n";
        }
        infoLabel.setText(info);
    }

    private void drawSceneInGameArea() {
        Vector2 pos = gameArea.localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x;
        float y = pos.y;
        float w = gameArea.getWidth();
        float h = gameArea.getHeight();

        // Update Camera for Live view (use screen coordinates)
        OrthographicCamera cam = getWorldCamera(); // From GScreen
        // GScreen.worldCamera usually follows some logic, but here we want UI-like behavior in the game area
        // Let's just use stage's camera combined matrix but mapped to our drawing
        
        Rectangle scissor = new Rectangle();
        Rectangle clipBounds = new Rectangle(x, y, w, h);
        ScissorStack.calculateScissors(stage.getCamera(), stage.getBatch().getTransformMatrix(), clipBounds, scissor);
        
        if (ScissorStack.pushScissors(scissor)) {
            // Draw Grid Background
            batch.setProjectionMatrix(stage.getCamera().combined);
            batch.begin();
            // We could draw a checkerboard here if needed
            batch.end();

            if (currentMode == Mode.LIVE_VECTOR) {
                // Live Vector: Draw to fit the area
                float drawSize = Math.min(w, h);
                if (!stretchToFill) {
                    // Use displayScale if not stretch
                    drawSize = displayScale;
                } else {
                    // If stretch, fill the smallest dimension to keep aspect ratio 1:1 (since character is 1:1)
                    // Or stretch to fill W/H?
                    // User said "stretch to fill drawing area". If area is rectangular, square character will distort.
                    // Usually "Stretch" means filling bounds.
                    // Let's assume square logic for character but fit to bounds.
                }
                
                // Calculate position to center
                float cx, cy, cw, ch;
                if (stretchToFill) {
                    cx = x; cy = y; cw = w; ch = h; // Distort
                } else {
                    cw = drawSize; ch = drawSize;
                    cx = x + (w - cw) / 2;
                    cy = y + (h - ch) / 2;
                }
                
                neonBatch.setProjectionMatrix(stage.getCamera().combined);
                neonBatch.begin();
                
                // NeonBatch coordinates: 0,0 at bottom-left of drawing area
                // We need to translate or set matrix
                // drawCharacter expects (0,0) to be bottom-left of the "size" box
                
                // Apply transform
                neonBatch.getTransformMatrix().idt().translate(cx, cy, 0);
                
                // If stretchToFill is true and w!=h, we need non-uniform scale?
                // drawCharacter takes a single 'size' float. It assumes square.
                // To support non-square stretch, we need to scale the matrix.
                if (stretchToFill && w != h) {
                    float sX = w / h; // Normalize to H
                    // This is getting complicated. drawCharacter uses `size` for both dims.
                    // Let's pass `size=1` and scale via matrix.
                    neonBatch.getTransformMatrix().idt().translate(cx, cy, 0).scale(w, h, 1);
                    drawContent(neonBatch, 1f);
                } else {
                    // Uniform
                    drawContent(neonBatch, cw);
                }
                
                neonBatch.getTransformMatrix().idt(); // Reset
                neonBatch.end();
                
            } else {
                // Baked Texture
                if (bakedRegion != null) {
                    float cx, cy, cw, ch;
                    if (stretchToFill) {
                        cx = x; cy = y; cw = w; ch = h;
                    } else {
                        // Maintain Aspect Ratio of the TEXTURE (which is square)
                        // Scale based on displayScale (long edge)
                        float ratio = (float)bakedRegion.getRegionWidth() / bakedRegion.getRegionHeight();
                        if (ratio >= 1) {
                            cw = displayScale; ch = displayScale / ratio;
                        } else {
                            ch = displayScale; cw = displayScale * ratio;
                        }
                        cx = x + (w - cw) / 2;
                        cy = y + (h - ch) / 2;
                    }
                    
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
        if (frameBuffer != null) frameBuffer.dispose();
    }
}
