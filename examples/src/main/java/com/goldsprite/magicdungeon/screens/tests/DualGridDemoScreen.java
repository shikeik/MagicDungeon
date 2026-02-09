package com.goldsprite.gdengine.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisCheckBox;

/**
 * Dual-grid Tilemap System for libGDX (Android Optimized)
 * 逻辑层与显示层完全分离，支持多地形叠加。
 */
public class DualGridDemoScreen extends GScreen {

    // ==========================================
    // 1. 全局配置中心 (Config)
    // ==========================================
    public static class Config {
        public static final int TILE_SIZE = 16;
        public static final int GRID_W = 40;  // 逻辑网格宽度
        public static final int GRID_H = 30;  // 逻辑网格高度
        public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;
        
        // 渲染顺序：底层到顶层
        public static final TerrainType[] RENDER_ORDER = {
            TerrainType.GRASS, TerrainType.DIRT, TerrainType.SAND
        };
    }

    // ==========================================
    // 2. 地形类型与枚举
    // ==========================================
    public enum TerrainType {
        EMPTY(-1, null),
        GRASS(0, "grass.png"),
        DIRT(1, "dirt.png"),
        SAND(2, "sand.png");

        public final int id;
        public final String texPath;
        TerrainType(int id, String texPath) {
            this.id = id;
            this.texPath = texPath;
        }
    }

    // ==========================================
    // 3. 数据模型 (Grid Data)
    // ==========================================
    public static class GridData {
        private final int[][] data = new int[Config.GRID_W][Config.GRID_H];

        public GridData() {
            clear(TerrainType.EMPTY);
        }

        public void clear(TerrainType type) {
            for (int x = 0; x < Config.GRID_W; x++) {
                for (int y = 0; y < Config.GRID_H; y++) {
                    data[x][y] = type.id;
                }
            }
        }

        public void setTile(int x, int y, TerrainType type) {
            if (x >= 0 && x < Config.GRID_W && y >= 0 && y < Config.GRID_H) {
                data[x][y] = type.id;
            }
        }

        public int getTileId(int x, int y) {
            if (x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H) return -1;
            return data[x][y];
        }
    }

    // ==========================================
    // 4. 双格宫核心算法 (Renderer & Logic)
    // ==========================================
    public static class DualGridRenderer {
        private final TextureRegion[][] atlas = new TextureRegion[3][16];
        // 映射位掩码到图集坐标 (基于原Godot项目规则)
        // 掩码计算: (TL<<3 | TR<<2 | BL<<1 | BR)
        private static final int[] MASK_TO_ATLAS_X = { -1, 1, 0, 3, 0, 1, 3, 1, 3, 2, 3, 3, 1, 2, 0, 2 };
        private static final int[] MASK_TO_ATLAS_Y = { -1, 3, 0, 0, 2, 0, 2, 1, 3, 3, 0, 1, 2, 2, 1, 1 };

        public void load() {
            for (TerrainType type : Config.RENDER_ORDER) {
                Texture tex = new Texture(Gdx.files.internal(type.texPath));
                TextureRegion[][] temp = TextureRegion.split(tex, Config.TILE_SIZE, Config.TILE_SIZE);
                // 摊平 4x4 到 1x16 方便索引
                for (int i = 0; i < 16; i++) {
                    atlas[type.id][i] = temp[i / 4][i % 4];
                }
            }
        }

        public void render(SpriteBatch batch, GridData grid, TerrainType type) {
            // 显示网格比逻辑网格多出一圈
            for (int x = 0; x <= Config.GRID_W; x++) {
                for (int y = 0; y <= Config.GRID_H; y++) {
                    int mask = calculateMask(grid, x, y, type);
                    if (mask > 0) {
                        int atlasIndex = getAtlasIndex(mask);
                        if (atlasIndex != -1) {
                            // 关键点：向左下偏移半个瓦片
                            float drawX = x * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
                            float drawY = y * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
                            batch.draw(atlas[type.id][atlasIndex], drawX, drawY);
                        }
                    }
                }
            }
        }

        private int calculateMask(GridData grid, int x, int y, TerrainType target) {
            // 采样逻辑层周围的4个点
            int tl = grid.getTileId(x - 1, y) == target.id ? 1 : 0;
            int tr = grid.getTileId(x, y) == target.id ? 1 : 0;
            int bl = grid.getTileId(x - 1, y - 1) == target.id ? 1 : 0;
            int br = grid.getTileId(x, y - 1) == target.id ? 1 : 0;
            return (tl << 3) | (tr << 2) | (bl << 1) | br;
        }

        private int getAtlasIndex(int mask) {
            // 将位掩码转为 4x4 图集坐标对应的数组索引
            int tx = MASK_TO_ATLAS_X[mask];
            int ty = MASK_TO_ATLAS_Y[mask];
            if (tx == -1) return -1;
            return ty * 4 + tx;
        }
        
        public void dispose() {
            for (int i=0; i<3; i++) atlas[i][0].getTexture().dispose();
        }
    }


    // ==========================================
    // 5. 屏幕类实现 (Main Screen)
    // ==========================================
    private SpriteBatch batch;
    private ShapeRenderer debugRenderer;
    private GridData worldData;
    private DualGridRenderer dualRenderer;
    private Stage uiStage;
    
    // 交互状态
    private TerrainType selectedTerrain = TerrainType.DIRT;
    private boolean showGrid = true;

    public DualGridDemoScreen(ScreenManager sm) {
        super(sm);
        // 安卓端缩放优化
        this.worldScale = 0.5f; 
        this.autoCenterWorldCamera = true;
    }

    @Override
    public void create() {
        if (!VisUI.isLoaded()) VisUI.load();
        
        batch = new SpriteBatch();
        debugRenderer = new ShapeRenderer();
        worldData = new GridData();
        dualRenderer = new DualGridRenderer();
        dualRenderer.load();

        uiStage = new Stage(uiViewport);
        setupUI();

        // 交互逻辑
        imp = new InputMultiplexer();
        imp.addProcessor(uiStage);
        imp.addProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                handlePaint(screenX, screenY);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                handlePaint(screenX, screenY);
                return true;
            }
        });
    }

    private void handlePaint(int sx, int sy) {
        // 使用 GScreen 提供的 screenToWorldCoord
        Vector2 worldPos = screenToWorldCoord(sx, sy);
        int gx = Math.round(worldPos.x / Config.TILE_SIZE);
        int gy = Math.round(worldPos.y / Config.TILE_SIZE);
        
        // 适配安卓操作：如果是删除模式（右键模拟），可以设为 EMPTY 或 GRASS
        worldData.setTile(gx, gy, selectedTerrain);
    }

    private void setupUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.top().right();

        VisLabel title = new VisLabel("Dual-Grid Demo (libGDX)");
        
        // 地形选择按钮
        ButtonGroup<VisTextButton> group = new ButtonGroup<>();
        VisTextButton btnGrass = new VisTextButton("Grass", "toggle");
        VisTextButton btnDirt = new VisTextButton("Dirt", "toggle");
        VisTextButton btnSand = new VisTextButton("Sand", "toggle");
        
        group.add(btnGrass, btnDirt, btnSand);
        btnDirt.setChecked(true);

        btnGrass.addListener(e -> { if(btnGrass.isChecked()) selectedTerrain = TerrainType.GRASS; return true; });
        btnDirt.addListener(e -> { if(btnDirt.isChecked()) selectedTerrain = TerrainType.DIRT; return true; });
        btnSand.addListener(e -> { if(btnSand.isChecked()) selectedTerrain = TerrainType.SAND; return true; });

        VisCheckBox cbGrid = new VisCheckBox("Show Logic Grid", true);
        cbGrid.addListener(e -> { showGrid = cbGrid.isChecked(); return true; });

        VisTextButton btnClear = new VisTextButton("Clear All");
        btnClear.addListener(e -> { worldData.clear(TerrainType.EMPTY); return true; });

        // 布局
        Table menu = new Table();
        menu.add(title).padBottom(10).row();
        menu.add(btnGrass).fillX().pad(2).row();
        menu.add(btnDirt).fillX().pad(2).row();
        menu.add(btnSand).fillX().pad(2).row();
        menu.add(cbGrid).padTop(10).row();
        menu.add(btnClear).fillX().padTop(20);

        root.add(menu).pad(20).background(VisUI.getSkin().getDrawable("window-bg"));
        uiStage.addActor(root);
    }

    @Override
    public void render0(float delta) {
        // 1. 渲染游戏世界
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        // 按顺序渲染各层，实现堆叠感
        for (TerrainType layer : Config.RENDER_ORDER) {
            dualRenderer.render(batch, worldData, layer);
        }
        batch.end();

        // 2. 渲染辅助网格 (Debug)
        if (showGrid) {
            debugRenderer.setProjectionMatrix(worldCamera.combined);
            debugRenderer.begin(ShapeRenderer.ShapeType.Point);
            debugRenderer.setColor(Color.RED);
            for (int x = 0; x < Config.GRID_W; x++) {
                for (int y = 0; y < Config.GRID_H; y++) {
                    debugRenderer.point(x * Config.TILE_SIZE, y * Config.TILE_SIZE, 0);
                }
            }
            debugRenderer.end();
        }

        // 3. 渲染 UI
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        uiStage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        debugRenderer.dispose();
        dualRenderer.dispose();
        uiStage.dispose();
    }
}