package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon.assets.SpineManager;
import com.goldsprite.magicdungeon.assets.TextureManager;
import com.goldsprite.magicdungeon.core.renderer.DualGridDungeonRenderer;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.Item;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.Monster;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.ui.ItemRenderer;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.world.Tile;
import com.goldsprite.magicdungeon.world.TileType;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.magicdungeon.world.data.GameMapData;
import com.goldsprite.magicdungeon.world.data.MapEntityData;
import com.goldsprite.magicdungeon.world.data.MapItemData;
import com.goldsprite.magicdungeon.world.DungeonTheme;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.goldsprite.magicdungeon.model.LayerData;
import com.goldsprite.magicdungeon.model.SaveData;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.utils.MapIO;
import com.goldsprite.magicdungeon.core.MonsterState;
import com.goldsprite.magicdungeon.core.ItemState;
import com.badlogic.gdx.files.FileHandle;

/**
 * 简易营地编辑器 (CampEditorScreen)
 * 支持多层地图编辑、实体/物品投放、AI逻辑验证
 */
public class CampEditorScreen extends GScreen {

    // --- 数据模型 ---
    public enum EditorMode { MAP, ITEM, ENTITY }
    public enum EditScope { INTERNAL, LOCAL }
    
    // 实体选择包装
    public static class EntitySelection {
        public Object type; // MonsterType, ItemData, TileType
        public EntitySelection(Object type) { this.type = type; }
    }

    // Spine 状态缓存 (仅视觉用)
    private static class SpineState {
        Skeleton skeleton;
        AnimationState animationState;
    }

    // --- 核心组件 ---
    private NeonBatch batch;
    private PolygonSpriteBatch polyBatch;
    private SkeletonRenderer skeletonRenderer;
    private ShapeRenderer shapeRenderer;
    
    // 数据层
    private List<Tile[][]> mapLayers = new ArrayList<>();
    private int selectedMapLayerIndex = 0;
    
    // 渲染代理 Dungeon (用于欺骗 DualGridDungeonRenderer)
    private Dungeon renderProxy;
    private DualGridDungeonRenderer dungeonRenderer;
    
    // 实体存储
    private List<Monster> monsters = new ArrayList<>();
    private List<Item> items = new ArrayList<>();

    private Stage uiStage;
    private OrthographicCamera gameCamera;

    // --- 状态 ---
    private EditorMode currentMode = EditorMode.MAP;
    private EditScope currentScope = EditScope.INTERNAL;
    private String currentSaveName = null;
    private String currentAreaId = "camp";
    private int currentFloor = 1; // Default floor

    private TileType selectedTileType = TileType.Floor;
    private EntitySelection selectedEntity = null; // Item or Monster
    private boolean showGrid = true;
    private boolean worldLogicEnabled = false;

    // Spine 缓存 map
    private Map<Monster, SpineState> spineCache = new HashMap<>();

    // --- UI 组件 ---
    private VisTable gameArea;
    private VisTable contentPanel; // 右侧动态内容区
    private Rectangle scissorRect = new Rectangle();
    
    // --- 输入控制 ---
    private int lastGx = -1, lastGy = -1;
    private Vector2 lastMousePos = new Vector2();
    private int dragButton = -1;

    public CampEditorScreen() {
        this.worldScale = 1.0f;
    }

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();

        batch = new NeonBatch();
        polyBatch = new PolygonSpriteBatch();
        skeletonRenderer = new SkeletonRenderer();
        shapeRenderer = new ShapeRenderer();
        
        // 初始化代理 Dungeon
        renderProxy = new Dungeon(50, 50, 0); 
        renderProxy.level = 0; // 营地模式
        
        // 初始化图层 (固定两层: 0=Floor, 1=Block)
        mapLayers.clear();
        for (int i = 0; i < 2; i++) {
            Tile[][] layer = new Tile[50][50];
            mapLayers.add(layer);
            if (i == 0) {
                // 第一层填充草地
                for (int x = 0; x < 50; x++) {
                    for (int y = 0; y < 50; y++) {
                        layer[y][x] = new Tile(TileType.Grass);
                    }
                }
            }
        }
        selectedMapLayerIndex = 0;
        
        // 初始化碰撞层
        rebuildCollisionMap();

        dungeonRenderer = new DualGridDungeonRenderer();

        // 确保资源加载
        TextureManager.getInstance();
        SpineManager.getInstance();

        // 初始化独立相机
        gameCamera = new OrthographicCamera();
        gameCamera.zoom = 1.0f;

        // 初始化 UI
        uiStage = new Stage(new ScreenViewport());
        setupUI();

        // 设置输入
        imp = new InputMultiplexer(uiStage, new EditorInputHandler());
    }

    private void addMapLayer() {
        Tile[][] newLayer = new Tile[50][50];
        // 默认全空 (null)
        mapLayers.add(newLayer);
        // 层级增加，理论上需要重建碰撞图，但新增的是空层，其实不需要，除非...
        // 还是全量重建最稳妥
        rebuildCollisionMap();
    }
    
    private void removeMapLayer(int index) {
        if (index >= 0 && index < mapLayers.size() && mapLayers.size() > 1) {
            mapLayers.remove(index);
            if (selectedMapLayerIndex >= mapLayers.size()) {
                selectedMapLayerIndex = mapLayers.size() - 1;
            }
            rebuildCollisionMap();
        }
    }
    
    private void rebuildCollisionMap() {
        if (renderProxy == null || mapLayers.isEmpty()) return;
        
        for (int x = 0; x < 50; x++) {
            for (int y = 0; y < 50; y++) {
                updateCollisionAt(x, y);
            }
        }
    }
    
    private void updateCollisionAt(int x, int y) {
        Tile topTile = null;
        // 从最上层往下找
        for (int i = mapLayers.size() - 1; i >= 0; i--) {
            Tile[][] layer = mapLayers.get(i);
            if (y < layer.length && x < layer[0].length) {
                Tile t = layer[y][x];
                if (t != null) {
                    topTile = t;
                    break;
                }
            }
        }
        renderProxy.map[y][x] = topTile;
    }

    private void setupUI() {
        // Initialize contentPanel early to avoid NPE in listeners
        contentPanel = new VisTable();

        VisTable root = new VisTable();
        root.setFillParent(true);
        root.setBackground("window-bg");

        // 左侧 GameArea
        gameArea = new VisTable();
        gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.1f, 0.1f, 0.1f, 1f)));
        gameArea.setTouchable(Touchable.enabled);

        // 右侧工具栏容器
        VisTable toolsContainer = new VisTable();
        toolsContainer.setBackground("window");
        
        // 工具栏内容
        VisTable toolsContent = new VisTable();
        toolsContent.top();
        
        toolsContent.add(new VisLabel("Camp Editor")).pad(10).row();

        // Scope Selection
        VisTable scopeTable = new VisTable();
        VisTextButton btnInternal = new VisTextButton("Internal", "toggle");
        VisTextButton btnLocal = new VisTextButton("Local", "toggle");
        ButtonGroup<VisTextButton> scopeGroup = new ButtonGroup<>(btnInternal, btnLocal);
        
        ChangeListener scopeListener = new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btnInternal.isChecked()) currentScope = EditScope.INTERNAL;
                else if (btnLocal.isChecked()) currentScope = EditScope.LOCAL;
                refreshContentPanel();
            }
        };
        btnInternal.addListener(scopeListener);
        btnLocal.addListener(scopeListener);
        btnInternal.setChecked(true);
        
        scopeTable.add(btnInternal).width(80).pad(2);
        scopeTable.add(btnLocal).width(80).pad(2);
        toolsContent.add(scopeTable).row();

        // Save Selection (Visible only in Local mode ideally, but keep simple)
        VisSelectBox<String> saveSelect = new VisSelectBox<>();
        List<SaveData> saves = SaveManager.listSaves();
        Array<String> saveNames = new Array<>();
        for(SaveData s : saves) saveNames.add(s.saveName);
        if (saveNames.size == 0) saveNames.add("No Saves");
        saveSelect.setItems(saveNames);
        
        saveSelect.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                currentSaveName = saveSelect.getSelected();
                if ("No Saves".equals(currentSaveName)) currentSaveName = null;
            }
        });
        // Initial selection
        if (saveNames.size > 0 && !"No Saves".equals(saveNames.first())) {
            currentSaveName = saveNames.first();
        }
        
        toolsContent.add(new VisLabel("Save:")).padTop(5).row();
        toolsContent.add(saveSelect).expandX().fillX().padBottom(5).row();

        // Save/Area/Floor Inputs
        VisTable metaTable = new VisTable();
        metaTable.defaults().pad(2);
        
        VisTextField areaField = new VisTextField("camp");
        areaField.setMessageText("Area ID");
        areaField.setTextFieldListener((field, c) -> currentAreaId = field.getText());
        
        VisTextField floorField = new VisTextField("1");
        floorField.setMessageText("Floor");
        floorField.setTextFieldListener((field, c) -> {
            try { currentFloor = Integer.parseInt(field.getText()); } catch(Exception e){}
        });

        metaTable.add(new VisLabel("Area:")).right();
        metaTable.add(areaField).width(60).row();
        metaTable.add(new VisLabel("Floor:")).right();
        metaTable.add(floorField).width(60).row();
        
        toolsContent.add(metaTable).row();

        // 模式切换 (Map, Item, Entity)
        VisTable modeTable = new VisTable();
        VisTextButton btnModeMap = new VisTextButton("Map", "toggle");
        VisTextButton btnModeItem = new VisTextButton("Item", "toggle");
        VisTextButton btnModeEntity = new VisTextButton("Entity", "toggle");
        ButtonGroup<VisTextButton> modeGroup = new ButtonGroup<>(btnModeMap, btnModeItem, btnModeEntity);
        
        ChangeListener modeListener = new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                if (btnModeMap.isChecked()) currentMode = EditorMode.MAP;
                else if (btnModeItem.isChecked()) currentMode = EditorMode.ITEM;
                else if (btnModeEntity.isChecked()) currentMode = EditorMode.ENTITY;
                refreshContentPanel();
            }
        };
        btnModeMap.addListener(modeListener);
        btnModeItem.addListener(modeListener);
        btnModeEntity.addListener(modeListener);
        
        btnModeMap.setChecked(true);

        modeTable.add(btnModeMap).width(60).pad(2);
        modeTable.add(btnModeItem).width(60).pad(2);
        modeTable.add(btnModeEntity).width(60).pad(2);
        toolsContent.add(modeTable).row();

        // 动态内容区
        // contentPanel already initialized
        toolsContent.add(contentPanel).expandX().fillX().padTop(10).row();
        refreshContentPanel(); // 初始化内容

        // Settings (Bottom)
        toolsContent.add(new VisLabel("--- Settings ---")).padTop(20).row();
        
        // Save Button
        VisTable saveLoadTable = new VisTable();
        VisTextButton btnSave = new VisTextButton("Save");
        btnSave.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { 
                saveMap();
            }
        });
        
        VisTextButton btnLoad = new VisTextButton("Load");
        btnLoad.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { 
                loadMap();
            }
        });
        
        saveLoadTable.add(btnSave).expandX().fillX().pad(5);
        saveLoadTable.add(btnLoad).expandX().fillX().pad(5);
        toolsContent.add(saveLoadTable).fillX().row();
        
        // Grid Toggle
        VisCheckBox cbGrid = new VisCheckBox("Show Grid", true);
        cbGrid.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { showGrid = cbGrid.isChecked(); }
        });
        toolsContent.add(cbGrid).left().padLeft(10).row();
        
        // Logic Toggle
        VisTextButton btnLogic = new VisTextButton("World Logic: OFF", "toggle");
        btnLogic.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { 
                worldLogicEnabled = btnLogic.isChecked();
                btnLogic.setText(worldLogicEnabled ? "World Logic: ON" : "World Logic: OFF");
            }
        });
        toolsContent.add(btnLogic).fillX().pad(10).row();

        VisTextButton btnClear = new VisTextButton("Clear Entities");
        btnClear.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) { 
                monsters.clear();
                items.clear();
                spineCache.clear();
            }
        });
        toolsContent.add(btnClear).fillX().pad(10).row();

        // ScrollPane
        HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(toolsContent);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        
        toolsContainer.add(scrollPane).expand().fill();

        // SplitPane
        VisSplitPane splitPane = new VisSplitPane(gameArea, toolsContainer, false);
        splitPane.setSplitAmount(0.75f);
        splitPane.setMinSplitAmount(0.5f);
        splitPane.setMaxSplitAmount(0.9f);

        root.add(splitPane).expand().fill();
        uiStage.addActor(root);
    }
    
    private void refreshContentPanel() {
        contentPanel.clear();
        
        if (currentMode == EditorMode.MAP) {
            // --- Map Layers Control ---
            VisTable layerControl = new VisTable();
            layerControl.add(new VisLabel("Layers:")).padRight(5);
            
            VisSelectBox<String> layerSelect = new VisSelectBox<>();
            updateLayerSelectItems(layerSelect);
            layerControl.add(layerSelect).width(100).padRight(5);
            
            VisTextButton btnAdd = new VisTextButton("+");
            btnAdd.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    addMapLayer();
                    selectedMapLayerIndex = mapLayers.size() - 1;
                    updateLayerSelectItems(layerSelect);
                }
            });
            layerControl.add(btnAdd).width(30).padRight(2);
            
            VisTextButton btnDel = new VisTextButton("-");
            btnDel.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    removeMapLayer(layerSelect.getSelectedIndex());
                    updateLayerSelectItems(layerSelect);
                }
            });
            layerControl.add(btnDel).width(30);
            
            // Listener for selection change
            layerSelect.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    selectedMapLayerIndex = layerSelect.getSelectedIndex();
                }
            });

            contentPanel.add(layerControl).row();
            contentPanel.add(new VisLabel("--- Tile Palette ---")).padTop(10).row();
            contentPanel.add(createTilePalette()).expandX().fillX().row();
            
        } else if (currentMode == EditorMode.ITEM) {
            contentPanel.add(new VisLabel("--- Item Palette ---")).padTop(10).row();
            contentPanel.add(createItemPalette()).expandX().fillX().row();
            
        } else if (currentMode == EditorMode.ENTITY) {
            contentPanel.add(new VisLabel("--- Entity Palette ---")).padTop(10).row();
            contentPanel.add(createEntityPalette()).expandX().fillX().row();
        }
    }
    
    private void updateLayerSelectItems(VisSelectBox<String> box) {
        String[] items = new String[mapLayers.size()];
        for(int i=0; i<mapLayers.size(); i++) items[i] = "Layer " + i;
        box.setItems(items);
        box.setSelectedIndex(Math.min(selectedMapLayerIndex, mapLayers.size()-1));
    }

    private VisTable createTilePalette() {
        VisTable t = new VisTable();
        VisTable grid = new VisTable();
        ButtonGroup<Button> group = new ButtonGroup<>();
        int col = 0;
        
        for (TileType type : TileType.values()) {
            VisImageButton btn = new VisImageButton("default");
            VisImageButton.VisImageButtonStyle style = new VisImageButton.VisImageButtonStyle(btn.getStyle());
            
            TextureRegion icon = TextureManager.getInstance().getTile(type);
            if (icon != null) style.imageUp = new TextureRegionDrawable(icon);
            else btn.add(new VisLabel("?"));
            
            btn.setStyle(style);
            new Tooltip.Builder(type.name()).target(btn).build();
            
            if (type == selectedTileType) btn.setChecked(true);
            
            btn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (btn.isChecked()) selectedTileType = type;
                }
            });
            group.add(btn);
            grid.add(btn).size(40, 40).pad(2);
            col++;
            if (col % 5 == 0) grid.row();
        }
        
        t.add(grid).row();
        return t;
    }
    
    private VisTable createItemPalette() {
        Array<EntitySelection> list = new Array<>();
        for (ItemData t : ItemData.values()) list.add(new EntitySelection(t));
        return createGridSelector(list);
    }
    
    private VisTable createEntityPalette() {
        Array<EntitySelection> list = new Array<>();
        for (MonsterType t : MonsterType.values()) list.add(new EntitySelection(t));
        return createGridSelector(list);
    }

    private VisTable createGridSelector(Array<EntitySelection> items) {
        VisTable grid = new VisTable();
        ButtonGroup<Button> group = new ButtonGroup<>();
        int col = 0;
        
        for (EntitySelection item : items) {
            VisImageButton btn = new VisImageButton("default");
            VisImageButton.VisImageButtonStyle style = new VisImageButton.VisImageButtonStyle(btn.getStyle());
            
            TextureRegion icon = null;
            String tooltipText = "";
            
            if (item.type instanceof MonsterType) {
                MonsterType mt = (MonsterType) item.type;
                icon = TextureManager.getInstance().getMonster(mt.name());
                tooltipText = mt.name;
            } else if (item.type instanceof ItemData) {
                ItemData id = (ItemData) item.type;
                icon = TextureManager.getInstance().get(id.name());
                if (icon == null) icon = TextureManager.getInstance().getItem(id.name());
                tooltipText = id.name;
            }
            
            if (icon != null) style.imageUp = new TextureRegionDrawable(icon);
            else btn.add(new VisLabel("?"));
            
            btn.setStyle(style);
            new Tooltip.Builder(tooltipText).target(btn).build();
            
            btn.addListener(new ChangeListener() {
                @Override public void changed(ChangeEvent event, Actor actor) {
                    if (btn.isChecked()) selectedEntity = item;
                }
            });
            
            grid.add(btn).size(40, 40).pad(2);
            col++;
            if (col % 5 == 0) grid.row();
        }
        return grid;
    }

    @Override
    public void render0(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update Logic
        if (worldLogicEnabled) {
            updateAI(delta);
        }

        uiStage.act(delta);
        uiStage.draw();

        drawGameScene(delta);
    }
    
    private void updateAI(float delta) {
        // 使用自定义 renderProxy，它会自动检查所有 mapLayers
        
        for (Monster m : monsters) {
            m.update(delta, null, renderProxy, monsters); // Player is null -> AI Wanders
            
            // Sync Visuals
            m.visualX = m.x * Constants.TILE_SIZE;
            m.visualY = m.y * Constants.TILE_SIZE;
        }
    }

    private void drawGameScene(float delta) {
        Vector2 pos = gameArea.localToStageCoordinates(new Vector2(0, 0));
        float areaW = gameArea.getWidth();
        float areaH = gameArea.getHeight();

        if (gameCamera.viewportWidth != areaW || gameCamera.viewportHeight != areaH) {
            gameCamera.viewportWidth = areaW;
            gameCamera.viewportHeight = areaH;
            gameCamera.update();
        }

        ScissorStack.calculateScissors(uiStage.getCamera(), uiStage.getBatch().getTransformMatrix(), 
            new Rectangle(pos.x, pos.y, areaW, areaH), scissorRect);
        
        if (ScissorStack.pushScissors(scissorRect)) {
            
            batch.setProjectionMatrix(gameCamera.combined);
            polyBatch.setProjectionMatrix(gameCamera.combined);
            shapeRenderer.setProjectionMatrix(gameCamera.combined);
            
            HdpiUtils.glViewport((int)pos.x, (int)pos.y, (int)areaW, (int)areaH);
            
            batch.begin();
            
            // 1. 地图渲染 (Multi-Layer)
            for (Tile[][] layer : mapLayers) {
                // [Modified] Use renderTileGrid directly
                dungeonRenderer.renderTileGrid(batch, layer, DungeonTheme.DEFAULT, true);
            }
            
            // 2. 实体 (Item, Monster Texture)
            renderEntities(batch, delta);
            
            batch.end();
            
            // 3. Spine Entities (Wolf)
            polyBatch.begin();
            renderSpineEntities(delta);
            polyBatch.end();
            
            // 4. Grid
            if (showGrid) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                shapeRenderer.setColor(1, 1, 1, 0.3f);
                int w = renderProxy.width * Constants.TILE_SIZE;
                int h = renderProxy.height * Constants.TILE_SIZE;
                for (int x=0; x<=w; x+=Constants.TILE_SIZE) shapeRenderer.line(x, 0, x, h);
                for (int y=0; y<=h; y+=Constants.TILE_SIZE) shapeRenderer.line(0, y, w, y);
                
                shapeRenderer.setColor(Color.RED);
                shapeRenderer.line(-20, 0, 20, 0);
                shapeRenderer.line(0, -20, 0, 20);
                shapeRenderer.end();
            }

            HdpiUtils.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            ScissorStack.popScissors();
        }
    }

    private void renderEntities(SpriteBatch batch, float delta) {
        for (Item item : items) {
            ItemRenderer.drawItem(batch, item.item, item.visualX + 4, item.visualY + 4, 24);
        }
        for (Monster m : monsters) {
            if (m.type == MonsterType.Wolf) continue;
            TextureRegion mTex = TextureManager.getInstance().getMonster(m.type.name());
            if (mTex == null) mTex = TextureManager.getInstance().getMonster(MonsterType.Slime.name());
            if (mTex != null) {
                batch.draw(mTex, m.visualX, m.visualY, Constants.TILE_SIZE, Constants.TILE_SIZE);
            }
        }
    }

    private void renderSpineEntities(float delta) {
        for (Monster m : monsters) {
            if (m.type == MonsterType.Wolf) {
                SpineState state = getSpineState(m);
                if (state != null && state.skeleton != null) {
                    state.skeleton.setPosition(m.visualX + Constants.TILE_SIZE / 2f, m.visualY);
                    if (worldLogicEnabled) {
                        state.animationState.update(delta);
                        state.animationState.apply(state.skeleton);
                    }
                    state.skeleton.updateWorldTransform();
                    skeletonRenderer.draw(polyBatch, state.skeleton);
                }
            }
        }
    }

    private SpineState getSpineState(Monster m) {
        if (spineCache.containsKey(m)) return spineCache.get(m);
        SpineState state = new SpineState();
        if (m.type == MonsterType.Wolf) {
            SkeletonData data = SpineManager.getInstance().get("Wolf");
            if (data != null) {
                state.skeleton = new Skeleton(data);
                AnimationStateData stateData = new AnimationStateData(data);
                state.animationState = new AnimationState(stateData);
                state.animationState.setAnimation(0, "idle", true);
            }
        }
        spineCache.put(m, state);
        return state;
    }

    @Override
    public void dispose() {
        batch.dispose();
        polyBatch.dispose();
        shapeRenderer.dispose();
        dungeonRenderer.dispose();
        uiStage.dispose();
    }

    class EditorInputHandler extends InputAdapter {
        @Override
        public boolean scrolled(float amountX, float amountY) {
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
            return false;
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
        Vector3 vec3 = new Vector3(screenX, screenY, 0);
        gameCamera.unproject(vec3, areaPos.x, areaPos.y, gameArea.getWidth(), gameArea.getHeight());
        
        int gx = (int) Math.floor(vec3.x / Constants.TILE_SIZE);
        int gy = (int) Math.floor(vec3.y / Constants.TILE_SIZE);

        if (gx < 0 || gx >= renderProxy.width || gy < 0 || gy >= renderProxy.height) return;

        if (currentMode == EditorMode.MAP) {
            if (isNewClick || (gx != lastGx || gy != lastGy)) {
                if (selectedMapLayerIndex >= 0 && selectedMapLayerIndex < mapLayers.size()) {
                    // [Modified] Check if tile type changed
                    Tile current = mapLayers.get(selectedMapLayerIndex)[gy][gx];
                    if (current == null || current.type != selectedTileType) {
                        Tile tile = new Tile(selectedTileType);
                        mapLayers.get(selectedMapLayerIndex)[gy][gx] = tile;
                        updateCollisionAt(gx, gy);
                    }
                }
                lastGx = gx; lastGy = gy;
            }
        } else if ((currentMode == EditorMode.ITEM || currentMode == EditorMode.ENTITY) && isNewClick && selectedEntity != null) {
            if (selectedEntity.type instanceof MonsterType) {
                MonsterType mt = (MonsterType) selectedEntity.type;
                Monster m = new Monster(gx, gy, mt);
                m.visualX = gx * Constants.TILE_SIZE;
                m.visualY = gy * Constants.TILE_SIZE;
                monsters.add(m);
            } else if (selectedEntity.type instanceof ItemData) {
                ItemData id = (ItemData) selectedEntity.type;
                InventoryItem ii = new InventoryItem(id);
                Item item = new Item(gx, gy, ii);
                item.visualX = gx * Constants.TILE_SIZE;
                item.visualY = gy * Constants.TILE_SIZE;
                items.add(item);
            }
        }
    }

    private void saveMap() {
        // Build LayerData
        LayerData data = new LayerData(50, 50);
        
        // Layer 0: Floor
        Tile[][] floorLayer = mapLayers.get(0);
        for(int y=0; y<50; y++) {
            for(int x=0; x<50; x++) {
                Tile t = floorLayer[y][x];
                if(t != null) data.floorIds[y*50+x] = t.type.name();
            }
        }
        
        // Layer 1: Block
        if (mapLayers.size() > 1) {
            Tile[][] blockLayer = mapLayers.get(1);
            for(int y=0; y<50; y++) {
                for(int x=0; x<50; x++) {
                    Tile t = blockLayer[y][x];
                    if(t != null) data.blockIds[y*50+x] = t.type.name();
                }
            }
        }
        
        // Entities
        for(Monster m : monsters) {
            data.monsters.add(new MonsterState(m.x, m.y, m.type.name(), m.hp, m.maxHp));
        }
        
        // Items
        for(Item item : items) {
            data.items.add(new ItemState(
                item.x, item.y, item.item.data.name(), item.item.quality.name(),
                item.item.atk, item.item.def, item.item.heal, item.item.manaRegen, item.item.count
            ));
        }
        
        if (currentScope == EditScope.INTERNAL) {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);
            String jsonStr = json.prettyPrint(data);
            
            String path = "assets/maps/" + currentAreaId + "/floor_" + currentFloor + ".json";
            
            FileHandle file = Gdx.files.local(path);
            file.parent().mkdirs();
            file.writeString(jsonStr, false);
            System.out.println("Map saved (Internal) to " + file.file().getAbsolutePath());
            
        } else {
            // Local Scope
            if (currentSaveName == null) {
                // Should show error or select save
                System.err.println("No save selected!");
                return;
            }
            SaveManager.saveLayerData(currentSaveName, currentAreaId, currentFloor, data);
            System.out.println("Map saved (Local) to " + currentSaveName + "/" + currentAreaId + "/" + currentFloor);
        }
    }

    private void loadMap() {
        LayerData data = null;
        
        if (currentScope == EditScope.INTERNAL) {
            String path = "assets/maps/" + currentAreaId + "/floor_" + currentFloor + ".json";
            FileHandle file = Gdx.files.local(path);
            if (!file.exists()) {
                System.err.println("File not found: " + path);
                return;
            }
            try {
                data = new Json().fromJson(LayerData.class, file);
            } catch (Exception e) {
                System.err.println("Error loading internal map: " + e.getMessage());
                return;
            }
        } else {
            if (currentSaveName == null) {
                System.err.println("No save selected!");
                return;
            }
            if (!SaveManager.hasLayerData(currentSaveName, currentAreaId, currentFloor)) {
                 System.err.println("Layer data not found for " + currentSaveName + " / " + currentAreaId + " / " + currentFloor);
                 return;
            }
            data = SaveManager.loadLayerData(currentSaveName, currentAreaId, currentFloor);
        }
        
        if (data == null) return;
        
        // Restore Map Layers
        // We expect fixed 2 layers
        // Clear current
        for(int i=0; i<mapLayers.size(); i++) {
            Tile[][] layer = mapLayers.get(i);
            for(int y=0; y<50; y++) {
                for(int x=0; x<50; x++) layer[y][x] = null;
            }
        }
        
        // Layer 0: Floor
        if (data.floorIds != null) {
            Tile[][] floorLayer = mapLayers.get(0);
            for(int i=0; i<data.floorIds.length; i++) {
                String id = data.floorIds[i];
                if (id != null) {
                    int x = i % data.width; // Assuming width is 50
                    int y = i / data.width;
                    if (x < 50 && y < 50) {
                        try {
                            floorLayer[y][x] = new Tile(TileType.valueOf(id));
                        } catch(Exception e){}
                    }
                }
            }
        }
        
        // Layer 1: Block
        if (data.blockIds != null) {
            Tile[][] blockLayer = mapLayers.get(1);
            for(int i=0; i<data.blockIds.length; i++) {
                String id = data.blockIds[i];
                if (id != null) {
                    int x = i % data.width;
                    int y = i / data.width;
                    if (x < 50 && y < 50) {
                        try {
                            blockLayer[y][x] = new Tile(TileType.valueOf(id));
                        } catch(Exception e){}
                    }
                }
            }
        }
        
        rebuildCollisionMap();
        
        // Restore Entities
        monsters.clear();
        items.clear();
        spineCache.clear();
        
        for(MonsterState ms : data.monsters) {
            try {
                MonsterType type = MonsterType.valueOf(ms.typeName);
                Monster m = new Monster(ms.x, ms.y, type);
                m.hp = ms.hp;
                m.maxHp = ms.maxHp;
                m.visualX = m.x * Constants.TILE_SIZE;
                m.visualY = m.y * Constants.TILE_SIZE;
                monsters.add(m);
            } catch(Exception e){}
        }
        
        for(ItemState is : data.items) {
             try {
                ItemData id = ItemData.valueOf(is.itemName); // Assuming ItemState stores ItemData enum name
                InventoryItem ii = new InventoryItem(id);
                ii.quality = com.goldsprite.magicdungeon.entities.ItemQuality.valueOf(is.quality);
                ii.atk = is.atk; ii.def = is.def; ii.heal = is.heal; ii.manaRegen = is.manaRegen;
                ii.count = is.count > 0 ? is.count : 1;
                
                Item item = new Item(is.x, is.y, ii);
                item.visualX = item.x * Constants.TILE_SIZE;
                item.visualY = item.y * Constants.TILE_SIZE;
                items.add(item);
             } catch(Exception e){}
        }
        
        System.out.println("Map loaded!");
    }
}
