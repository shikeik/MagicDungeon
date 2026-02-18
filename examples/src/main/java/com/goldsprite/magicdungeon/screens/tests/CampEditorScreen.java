package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.DualGridConfig;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.DualGridRenderer;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.GridData;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.TerrainType;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class CampEditorScreen extends GScreen {

    // --- 数据模型 ---
    public enum EditorMode { TERRAIN, ENTITY }
    public enum EntityType {
        PLAYER(Color.GREEN),
        ENEMY_SLIME(Color.RED),
        ITEM_POTION(Color.YELLOW),
        NPC_VILLAGER(Color.BLUE);

        public final Color color;
        EntityType(Color color) { this.color = color; }
    }

    public static class EditorEntity {
        public float x, y;
        public EntityType type;
        public EditorEntity(float x, float y, EntityType type) {
            this.x = x; this.y = y; this.type = type;
        }
    }

    // --- 核心组件 ---
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private GridData gridData;
    private DualGridRenderer dualRenderer;
    private Stage uiStage;
    private OrthographicCamera gameCamera;

    // --- 状态 ---
    private EditorMode currentMode = EditorMode.TERRAIN;
    private TerrainType selectedTerrain = TerrainType.GRASS;
    private EntityType selectedEntity = EntityType.PLAYER;
    private int selectedLayer = 0;
    private boolean showGrid = true;
    private Array<EditorEntity> entities = new Array<>();

    // --- UI 组件 ---
    private VisTable gameArea;
    private Rectangle scissorRect = new Rectangle();

    // --- 输入控制 ---
    private int lastGx = -1, lastGy = -1;
    private Vector2 lastMousePos = new Vector2();
    private int dragButton = -1;

    public CampEditorScreen() {
        // GScreen 的 worldCamera 将被用于 GameArea，但我们需要手动管理视口
        this.worldScale = 1.0f;
    }

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        gridData = new GridData();
        dualRenderer = new DualGridRenderer();
        dualRenderer.load();

        // 初始化一些地形
        for (int x = 5; x <= 15; x++) {
            for (int y = 5; y <= 12; y++) {
                gridData.setTile(0, x, y, TerrainType.GRASS);
            }
        }

        // 初始化独立相机
        gameCamera = new OrthographicCamera();
        gameCamera.zoom = 1.0f;

        // 初始化 UI
        uiStage = new Stage(new ScreenViewport()); // 使用 ScreenViewport 保证 UI 清晰
        setupUI();

        // 设置输入
        imp = new InputMultiplexer(uiStage, new EditorInputHandler());
    }

    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg"); // 假设有这个背景，或者默认

        // 左侧 GameArea
        gameArea = new VisTable();
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        gameArea.setTouchable(Touchable.enabled); // 允许接收事件

        // 右侧工具栏
        VisTable toolsPanel = new VisTable(true);
        toolsPanel.setBackground("window");
        toolsPanel.add(new VisLabel("Camp Editor")).pad(10).row();

        // 模式切换
        VisTable modeTable = new VisTable();
        VisTextButton btnModeTerrain = new VisTextButton("Terrain", "toggle");
        VisTextButton btnModeEntity = new VisTextButton("Entity", "toggle");
        ButtonGroup<VisTextButton> modeGroup = new ButtonGroup<>(btnModeTerrain, btnModeEntity);
        
        btnModeTerrain.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btnModeTerrain.isChecked()) currentMode = EditorMode.TERRAIN;
            }
        });
        btnModeEntity.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btnModeEntity.isChecked()) currentMode = EditorMode.ENTITY;
            }
        });

        modeTable.add(btnModeTerrain).width(80).pad(2);
        modeTable.add(btnModeEntity).width(80).pad(2);
        toolsPanel.add(modeTable).row();

        // 工具内容容器
        // 这里简化处理：直接上下堆叠
        VisTable terrainTools = createTerrainTools();
        VisTable entityTools = createEntityTools();

        toolsPanel.add(terrainTools).expandX().fillX().padTop(10).row();
        toolsPanel.add(entityTools).expandX().fillX().padTop(10).row();

        // 通用设置
        VisCheckBox cbGrid = new VisCheckBox("Show Grid", true);
        cbGrid.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { showGrid = cbGrid.isChecked(); }
        });
        toolsPanel.add(cbGrid).padTop(20).left().row();

        VisTextButton btnClear = new VisTextButton("Clear All Entities");
        btnClear.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { entities.clear(); }
        });
        toolsPanel.add(btnClear).fillX().padTop(5).row();


        // SplitPane
        VisSplitPane splitPane = new VisSplitPane(gameArea, toolsPanel, false);
        splitPane.setSplitAmount(0.7f);
        splitPane.setMinSplitAmount(0.5f);
        splitPane.setMaxSplitAmount(0.85f);

        root.add(splitPane).expand().fill();
        uiStage.addActor(root);
    }

    private VisTable createTerrainTools() {
        VisTable t = new VisTable();
        t.add(new VisLabel("Terrain Layer: 0")).row(); // 简化层级，默认层0
        
        VisTable grid = new VisTable();
        ButtonGroup<Button> group = new ButtonGroup<>();
        int col = 0;
        for (TerrainType type : TerrainType.values()) {
            if (type == TerrainType.AIR) continue;
            VisImageButton btn = new VisImageButton("default");
            VisImageButton.VisImageButtonStyle style = new VisImageButton.VisImageButtonStyle(btn.getStyle());
            TextureRegion icon = dualRenderer.getIcon(type);
            if (icon != null) style.imageUp = new TextureRegionDrawable(icon);
            btn.setStyle(style);
            
            if (type == selectedTerrain) btn.setChecked(true);
            btn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (btn.isChecked()) {
                        selectedTerrain = type;
                        currentMode = EditorMode.TERRAIN; // 自动切模式
                    }
                }
            });
            group.add(btn);
            grid.add(btn).size(40, 40).pad(2);
            col++;
            if (col % 4 == 0) grid.row();
        }
        
        // Eraser
        VisTextButton btnEraser = new VisTextButton("Eraser", "toggle");
        group.add(btnEraser);
        btnEraser.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btnEraser.isChecked()) {
                    selectedTerrain = TerrainType.AIR;
                    currentMode = EditorMode.TERRAIN;
                }
            }
        });

        t.add(grid).row();
        t.add(btnEraser).fillX().padTop(5).row();
        return t;
    }

    private VisTable createEntityTools() {
        VisTable t = new VisTable();
        t.add(new VisLabel("Entities")).row();
        
        VisList<String> list = new VisList<>();
        Array<String> items = new Array<>();
        for (EntityType type : EntityType.values()) items.add(type.name());
        list.setItems(items);
        
        list.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                String name = list.getSelected();
                if (name != null) {
                    selectedEntity = EntityType.valueOf(name);
                    currentMode = EditorMode.ENTITY;
                }
            }
        });
        
        t.add(list).expandX().fillX().height(100).row();
        return t;
    }

    @Override
    public void render0(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        uiStage.act(delta);
        uiStage.draw();

        drawGameScene();
    }

    private void drawGameScene() {
        // 计算 GameArea 在屏幕上的位置
        Vector2 pos = gameArea.localToStageCoordinates(new Vector2(0, 0));
        float areaW = gameArea.getWidth();
        float areaH = gameArea.getHeight();

        // 更新相机视口 (如果 GameArea 大小改变)
        if (gameCamera.viewportWidth != areaW || gameCamera.viewportHeight != areaH) {
            gameCamera.viewportWidth = areaW;
            gameCamera.viewportHeight = areaH;
            gameCamera.update();
        }

        // 设置裁剪
        ScissorStack.calculateScissors(uiStage.getCamera(), uiStage.getBatch().getTransformMatrix(), 
            new Rectangle(pos.x, pos.y, areaW, areaH), scissorRect);
        
        if (ScissorStack.pushScissors(scissorRect)) {
            
            batch.setProjectionMatrix(gameCamera.combined);
            shapeRenderer.setProjectionMatrix(gameCamera.combined);
            
            // 设置 Viewport 到局部区域
            HdpiUtils.glViewport((int)pos.x, (int)pos.y, (int)areaW, (int)areaH);
            
            batch.begin();
            // 1. 绘制地图
            for (int l = 0; l < gridData.getLayerCount(); l++) {
                dualRenderer.renderLayer(batch, gridData, l);
            }
            batch.end();
            
            // 2. 绘制实体
            Gdx.gl.glEnable(GL20.GL_BLEND);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (EditorEntity e : entities) {
                shapeRenderer.setColor(e.type.color);
                shapeRenderer.circle(e.x, e.y, 10);
            }
            shapeRenderer.end();
            
            // 3. 绘制 Grid
            if (showGrid) {
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(1, 1, 1, 0.3f);
                int w = DualGridConfig.GRID_W * DualGridConfig.TILE_SIZE;
                int h = DualGridConfig.GRID_H * DualGridConfig.TILE_SIZE;
                for (int x=0; x<=w; x+=DualGridConfig.TILE_SIZE) shapeRenderer.line(x, 0, x, h);
                for (int y=0; y<=h; y+=DualGridConfig.TILE_SIZE) shapeRenderer.line(0, y, w, y);
                
                // 绘制原点
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.line(-20, 0, 20, 0);
                shapeRenderer.line(0, -20, 0, 20);
                shapeRenderer.end();
            }

            // 恢复 Viewport
            HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            ScissorStack.popScissors();
        }
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        dualRenderer.dispose();
        uiStage.dispose();
    }

    // --- 输入处理 ---
    class EditorInputHandler extends InputAdapter {
        @Override
        public boolean scrolled(float amountX, float amountY) {
            // 检查鼠标是否在 GameArea 内
            Vector2 stagePos = uiStage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            if (gameArea.hit(stagePos.x, stagePos.y, true) != null) {
                gameCamera.zoom += amountY * 0.1f;
                gameCamera.zoom = Math.max(0.1f, Math.min(gameCamera.zoom, 5f));
                gameCamera.update();
                return true;
            }
            return false;
        }

        @Override
        public boolean touchDown(int screenX, int screenY, int pointer, int button) {
            Vector2 stagePos = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
            // 只有点击在 GameArea 内才处理
            Actor hit = uiStage.hit(stagePos.x, stagePos.y, true);
            if (hit != null && (hit == gameArea || hit.isDescendantOf(gameArea))) {
                
                if (button == Input.Buttons.RIGHT) {
                    dragButton = button;
                    lastMousePos.set(screenX, screenY);
                    return true;
                } else if (button == Input.Buttons.LEFT) {
                    handleEditorClick(screenX, screenY, true);
                    dragButton = button;
                    return true;
                }
            }
            return false; // 让事件传递给 UI
        }

        @Override
        public boolean touchDragged(int screenX, int screenY, int pointer) {
            if (dragButton == Input.Buttons.RIGHT) {
                float dx = screenX - lastMousePos.x;
                float dy = screenY - lastMousePos.y; 
                gameCamera.translate(-dx * gameCamera.zoom, dy * gameCamera.zoom);
                gameCamera.update();
                lastMousePos.set(screenX, screenY);
                return true;
            } else if (dragButton == Input.Buttons.LEFT) {
                handleEditorClick(screenX, screenY, false);
                return true;
            }
            return false;
        }

        @Override
        public boolean touchUp(int screenX, int screenY, int pointer, int button) {
            if (button == dragButton) {
                dragButton = -1;
                lastGx = -1; lastGy = -1;
            }
            return false;
        }
    }

    private void handleEditorClick(int screenX, int screenY, boolean isNewClick) {
        Vector2 areaPos = gameArea.localToStageCoordinates(new Vector2(0, 0));
        
        // 修正：使用 Vector3 接收 unproject 结果
        Vector3 vec3 = new Vector3(screenX, screenY, 0);
        gameCamera.unproject(vec3, areaPos.x, areaPos.y, gameArea.getWidth(), gameArea.getHeight());
        
        if (currentMode == EditorMode.TERRAIN) {
            int gx = (int) Math.floor(vec3.x / DualGridConfig.TILE_SIZE);
            int gy = (int) Math.floor(vec3.y / DualGridConfig.TILE_SIZE);
            
            if (isNewClick || (gx != lastGx || gy != lastGy)) {
                gridData.setTile(selectedLayer, gx, gy, selectedTerrain);
                lastGx = gx; lastGy = gy;
            }
        } else if (currentMode == EditorMode.ENTITY && isNewClick) {
            entities.add(new EditorEntity(vec3.x, vec3.y, selectedEntity));
        }
    }
}
