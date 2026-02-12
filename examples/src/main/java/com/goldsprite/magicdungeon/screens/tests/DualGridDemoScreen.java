package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

/**
 * 动态多层双网格系统
 * 支持：指定层级绘画、同层互斥、多层叠加、Air擦除
 */
public class DualGridDemoScreen extends GScreen {

    public static class Config {
        public static final int TILE_SIZE = 32;
        public static final int GRID_W = 40;
        public static final int GRID_H = 30;
        public static final int TOTAL_LAYERS = 3; // 预设 3 个动态层
        public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;
    }

    public enum TerrainType {
        AIR(null),
        SAND("sprites/tilesets/sand_tiles.png"),
        DIRT("sprites/tilesets/dirt_tiles.png"),
        GRASS("sprites/tilesets/grass_tiles.png"),
		;

        public final int id;
        public final String texPath;
        TerrainType(String texPath) {
            this.id = ordinal()-1;
            this.texPath = texPath;
        }
    }

    public static class GridData {
        private final int[][][] data = new int[Config.TOTAL_LAYERS][Config.GRID_W][Config.GRID_H];

        public GridData() {
            clearAll();
        }

        public void clearAll() {
            for (int l = 0; l < Config.TOTAL_LAYERS; l++) {
                for (int x = 0; x < Config.GRID_W; x++) {
                    for (int y = 0; y < Config.GRID_H; y++) {
                        data[l][x][y] = -1;
                    }
                }
            }
        }

        public void setTile(int layer, int x, int y, TerrainType type) {
            if (layer < 0 || layer >= Config.TOTAL_LAYERS) return;
            if (x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H) return;
            data[layer][x][y] = type.id; // 同层 ID 互斥，直接覆盖
        }

        public int getTileId(int layer, int x, int y) {
            if (layer < 0 || layer >= Config.TOTAL_LAYERS || x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H) return -1;
            return data[layer][x][y];
        }
    }

    public static class DualGridRenderer {
        private final TextureRegion[][] atlas = new TextureRegion[3][16];
		private static final int[] MASK_TO_ATLAS_X = {
			-1, // 0: 0000 (全空)
			1, // 1: 0001 (只有BR) -> 外转角: 右下 (1,3)
			0, // 2: 0010 (只有BL) -> 外转角: 左下 (0,0)
			3, // 3: 0011 (BL+BR)  -> 底部边缘 (3,0)
			0, // 4: 0100 (只有TR) -> 外转角: 右上 (0,2)
			1, // 5: 0101 (TR+BR)  -> 右侧边缘 (1,0)
			2, // 6: 0110 (TR+BL)  -> 对角线 (2,3)
			1, // 7: 0111 (TR+BL+BR) -> 内转角: 左上 (3,1)
			3, // 8: 1000 (只有TL) -> 外转角: 左上 (3,3)
			0, // 9: 1001 (TL+BR)  -> 对角线 (0,1)
			3, // 10: 1010 (TL+BL) -> 左侧边缘 (3,2)  <-- 【修复你图中的错误！】
			2, // 11: 1011 (TL+BL+BR) -> 内转角: 右上 (2,2)
			1, // 12: 1100 (TL+TR) -> 顶部边缘 (1,2)
			2, // 13: 1101 (TL+TR+BR) -> 内转角: 左下 (2,0)
			3, // 14: 1110 (TL+TR+BL) -> 内转角: 右下 (1,1)
			2  // 15: 1111 (全满)  -> 中心块 (2,1)
		};
		private static final int[] MASK_TO_ATLAS_Y = {
			-1, // 0
			3, // 1
			0, // 2
			0, // 3
			2, // 4
			0, // 5
			3, // 6
			1, // 7
			3, // 8
			1, // 9
			2, // 10
			0, // 11
			2, // 12
			2, // 13
			1, // 14
			1  // 15
		};

        public void load() {
            for (TerrainType type : TerrainType.values()) {
                if (type == TerrainType.AIR) continue;
                Texture tex = new Texture(Gdx.files.internal(type.texPath));
                TextureRegion[][] temp = TextureRegion.split(tex, 16, 16);
                for (int i = 0; i < 16; i++) {
                    atlas[type.id][i] = temp[i / 4][i % 4];
                }
            }
        }

        public void renderLayer(SpriteBatch batch, GridData grid, int layerIndex) {
            // 在这一层里，我们需要对所有可能的 TerrainType 进行一遍 DualGrid 渲染
            for (TerrainType type : TerrainType.values()) {
                if (type == TerrainType.AIR) continue;

                for (int x = 0; x <= Config.GRID_W; x++) {
                    for (int y = 0; y <= Config.GRID_H; y++) {
                        int mask = calculateMask(grid, layerIndex, x, y, type);
                        if (mask <= 0) continue;

                        int tx = MASK_TO_ATLAS_X[mask];
                        int ty = MASK_TO_ATLAS_Y[mask];
                        if (tx == -1) continue;

                        float drawX = x * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
                        float drawY = y * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
                        batch.draw(atlas[type.id][ty * 4 + tx], drawX, drawY, Config.TILE_SIZE, Config.TILE_SIZE);
                    }
                }
            }
        }

        private int calculateMask(GridData grid, int layer, int x, int y, TerrainType target) {
            int tr = (grid.getTileId(layer, x, y) == target.id) ? 1 : 0;
            int tl = (grid.getTileId(layer, x - 1, y) == target.id) ? 1 : 0;
            int br = (grid.getTileId(layer, x, y - 1) == target.id) ? 1 : 0;
            int bl = (grid.getTileId(layer, x - 1, y - 1) == target.id) ? 1 : 0;
            return (tl << 3) | (tr << 2) | (bl << 1) | br;
        }

        public void dispose() {
            for (int i = 0; i < 3; i++) if (atlas[i][0] != null) atlas[i][0].getTexture().dispose();
        }
    }

    private SpriteBatch batch;
    private ShapeRenderer debugRenderer;
    private GridData worldData;
    private DualGridRenderer dualRenderer;
    private Stage uiStage;

    private TerrainType selectedTerrain = TerrainType.GRASS; // 初始选中改为 Grass
    private int selectedLayer = 0; // 当前选中的操作层
    private boolean showGrid = true;
    private int lastGx = -1, lastGy = -1;

    public DualGridDemoScreen() {
        this.worldScale = 0.6f;
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

        // [Fix] 预填充一些地形，避免初始全黑，让用户知道系统在工作
        for (int x = 5; x <= 15; x++) {
            for (int y = 3; y <= 10; y++) {
                worldData.setTile(0, x, y, TerrainType.GRASS);
            }
        }

        uiStage = new Stage(uiViewport);
        setupUI();

        imp = new InputMultiplexer(uiStage, new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					handlePaint(screenX, screenY, true);
					return true;
				}
				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					handlePaint(screenX, screenY, false);
					return true;
				}
				@Override
				public boolean touchUp(int screenX, int screenY, int pointer, int button) {
					lastGx = -1; lastGy = -1;
					return true;
				}
			});
    }

    private void handlePaint(int sx, int sy, boolean isNew) {
        Vector2 worldPos = screenToWorldCoord(sx, sy);
        int gx = (int) Math.floor(worldPos.x / Config.TILE_SIZE);
        int gy = (int) Math.floor(worldPos.y / Config.TILE_SIZE);

        if (isNew) {
            worldData.setTile(selectedLayer, gx, gy, selectedTerrain);
        } else if (lastGx != -1 && (gx != lastGx || gy != lastGy)) {
            paintPath(lastGx, lastGy, gx, gy);
        }
        lastGx = gx; lastGy = gy;
    }

    private void paintPath(int x1, int y1, int x2, int y2) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int sx = x1 < x2 ? 1 : -1, sy = y1 < y2 ? 1 : -1;
        int err = dx - dy;
        while (true) {
            worldData.setTile(selectedLayer, x1, y1, selectedTerrain);
            if (x1 == x2 && y1 == y2) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x1 += sx; }
            if (e2 < dx) { err += dx; y1 += sy; }
        }
    }

    private void setupUI() {
        VisTable root = new VisTable();
        root.setFillParent(true);
        root.top().right();

        VisTable menu = new VisTable(true);
//        menu.setBackground("window-bg");
        menu.setBackground("window-ten");
        menu.add(new VisLabel("Dual-Grid Editor")).pad(10).row();

        // 地形选择 (画刷)
        menu.add(new VisLabel("--- Brush ---")).row();
        ButtonGroup<VisTextButton> terrainGroup = new ButtonGroup<>();
        for (TerrainType type : TerrainType.values()) {
            VisTextButton btn = new VisTextButton(type.name(), "toggle");
            if (type == selectedTerrain) btn.setChecked(true);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if(btn.isChecked()) selectedTerrain = type;
                }
            });
            terrainGroup.add(btn);
            menu.add(btn).fillX().row();
        }

        // 层级选择
        menu.add(new VisLabel("--- Layer ---")).padTop(10).row();
        ButtonGroup<VisTextButton> layerGroup = new ButtonGroup<>();
        for (int i = 0; i < Config.TOTAL_LAYERS; i++) {
            final int idx = i;
            VisTextButton btn = new VisTextButton("Layer " + i, "toggle");
            if (i == 0) btn.setChecked(true);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if(btn.isChecked()) selectedLayer = idx;
                }
            });
            layerGroup.add(btn);
            menu.add(btn).fillX().row();
        }

        VisCheckBox cbGrid = new VisCheckBox("Show Grid", true);
        cbGrid.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showGrid = cbGrid.isChecked();
            }
        });
        menu.add(cbGrid).padTop(10).row();

        VisTextButton btnClear = new VisTextButton("Clear All");
        btnClear.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                worldData.clearAll();
            }
        });
        menu.add(btnClear).fillX().padBottom(10);

        root.add(menu).pad(10);
        uiStage.addActor(root);
    }

    @Override
    public void render0(float delta) {
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        // 渲染逻辑：按层级顺序 0 -> 1 -> 2 渲染，实现正确的遮挡关系
        for (int l = 0; l < Config.TOTAL_LAYERS; l++) {
            dualRenderer.renderLayer(batch, worldData, l);
        }
        batch.end();

        if (showGrid) {
            debugRenderer.setProjectionMatrix(worldCamera.combined);
            debugRenderer.begin(ShapeRenderer.ShapeType.Line);
            // 蓝色逻辑网格
            debugRenderer.setColor(Color.BLUE);
            for (int x = 0; x <= Config.GRID_W; x++) debugRenderer.line(x * Config.TILE_SIZE, 0, x * Config.TILE_SIZE, Config.GRID_H * Config.TILE_SIZE);
            for (int y = 0; y <= Config.GRID_H; y++) debugRenderer.line(0, y * Config.TILE_SIZE, Config.GRID_W * Config.TILE_SIZE, y * Config.TILE_SIZE);

            // 黄色渲染网格
            debugRenderer.setColor(Color.YELLOW);
            float off = Config.DISPLAY_OFFSET;
            for (int x = 0; x <= Config.GRID_W; x++) debugRenderer.line(x * Config.TILE_SIZE - off, -off, x * Config.TILE_SIZE - off, Config.GRID_H * Config.TILE_SIZE - off);
            for (int y = 0; y <= Config.GRID_H; y++) debugRenderer.line(-off, y * Config.TILE_SIZE - off, Config.GRID_W * Config.TILE_SIZE - off, y * Config.TILE_SIZE - off);
            debugRenderer.end();

            // 红色采样点：只显示当前选中层的内容
            debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
            debugRenderer.setColor(Color.RED);
            for (int x = 0; x < Config.GRID_W; x++) {
                for (int y = 0; y < Config.GRID_H; y++) {
                    if (worldData.getTileId(selectedLayer, x, y) != -1)
                        debugRenderer.circle(x * Config.TILE_SIZE + off, y * Config.TILE_SIZE + off, 4);
                }
            }
            debugRenderer.end();
        }

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void dispose() {
        batch.dispose();
        debugRenderer.dispose();
        dualRenderer.dispose();
        uiStage.dispose();
    }
}
