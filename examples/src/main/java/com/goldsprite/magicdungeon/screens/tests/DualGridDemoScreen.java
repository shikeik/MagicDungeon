package com.goldsprite.magicdungeon.screens.tests;

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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 动态多层双网格系统
 * 支持：指定层级绘画、同层互斥、多层叠加、Air擦除
 */
public class DualGridDemoScreen extends GScreen {

    public static class Config {
        public static final int TILE_SIZE = 32;
        public static final int GRID_W = 40;
        public static final int GRID_H = 30;
        public static final int DEFAULT_LAYERS = 3; // 预设 3 个动态层
        public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;
    }

    public enum TerrainType {
        AIR(null),
        SAND("sprites/tilesets/sand_tiles.png"),
        DIRT("sprites/tilesets/dirt_tiles.png"),
        GRASS("sprites/tilesets/grass_tiles.png"),
        DUNGEON_BRICK("sprites/tilesets/dungeon_brick_tiles.png"),
        ;

        public final int id;
        public final String texPath;
        TerrainType(String texPath) {
            this.id = ordinal()-1;
            this.texPath = texPath;
        }
    }

    public static class GridData {
        private final List<int[][]> layers = new ArrayList<>();

        public GridData() {
            for (int i = 0; i < Config.DEFAULT_LAYERS; i++) {
                addLayer();
            }
        }

        public void addLayer() {
            int[][] layer = new int[Config.GRID_W][Config.GRID_H];
            for (int x = 0; x < Config.GRID_W; x++) {
                Arrays.fill(layer[x], -1);
            }
            layers.add(layer);
        }

        public boolean removeLayer(int index) {
            if (index >= 0 && index < layers.size()) {
                layers.remove(index);
                return true;
            }
            return false;
        }

        public int getLayerCount() {
            return layers.size();
        }

        public void clearAll() {
            for (int[][] layer : layers) {
                for (int x = 0; x < Config.GRID_W; x++) {
                    Arrays.fill(layer[x], -1);
                }
            }
        }

        public void setTile(int layer, int x, int y, TerrainType type) {
            if (layer < 0 || layer >= layers.size()) return;
            if (x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H) return;
            layers.get(layer)[x][y] = type.id; // 同层 ID 互斥，直接覆盖
        }

        public int getTileId(int layer, int x, int y) {
            if (layer < 0 || layer >= layers.size() || x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H) return -1;
            return layers.get(layer)[x][y];
        }
    }

    public static class DualGridRenderer {
        private final TextureRegion[][] atlas = new TextureRegion[TerrainType.values().length][16];
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
            3, // 10: 1010 (TL+BL) -> 左侧边缘 (3,2)
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
        
        public TextureRegion getIcon(TerrainType type) {
            if (type == TerrainType.AIR) return null;
            // Return the tile at (2,2) (index 10) as requested
            // Row 2 (0-indexed) is the 3rd row. Col 2 is 3rd col.
            // Index = 2 * 4 + 2 = 10.
            return atlas[type.id][10];
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
            for (int i = 0; i < atlas.length; i++) {
                if (atlas[i][0] != null && atlas[i][0].getTexture() != null) {
                    atlas[i][0].getTexture().dispose();
                }
            }
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
                private int dragButton = -1;
                private float lastX, lastY;

                @Override
                public boolean scrolled(float amountX, float amountY) {
                     worldCamera.zoom += amountY * 0.1f;
                     worldCamera.zoom = Math.max(0.1f, Math.min(worldCamera.zoom, 5f));
                     worldCamera.update();
                     return true;
                }

                @Override
                public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                    if (button == Input.Buttons.LEFT) {
                        handlePaint(screenX, screenY, true);
                        dragButton = Input.Buttons.LEFT;
                    } else if (button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) {
                        dragButton = button;
                        lastX = screenX;
                        lastY = screenY;
                    }
                    return true;
                }
                
                @Override
                public boolean touchDragged(int screenX, int screenY, int pointer) {
                    if (dragButton == Input.Buttons.LEFT) {
                        handlePaint(screenX, screenY, false);
                    } else if (dragButton == Input.Buttons.RIGHT || dragButton == Input.Buttons.MIDDLE) {
                        float dx = screenX - lastX;
                        float dy = screenY - lastY;
                        worldCamera.translate(-dx * worldCamera.zoom, dy * worldCamera.zoom);
                        worldCamera.update();
                        lastX = screenX;
                        lastY = screenY;
                    }
                    return true;
                }
                
                @Override
                public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                    if (button == dragButton) {
                        dragButton = -1;
                        if (button == Input.Buttons.LEFT) {
                            lastGx = -1; lastGy = -1;
                        }
                    }
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
        menu.setBackground("window-ten");
        
        // 创建一个内部容器来存放内容
        VisTable contentTable = new VisTable();
        contentTable.top();
        
        contentTable.add(new VisLabel("Dual-Grid Editor")).pad(10).row();

        // 地形选择 (网格布局)
        contentTable.add(new VisLabel("--- Terrain ---")).row();
        
        VisTable gridTable = new VisTable();
        ButtonGroup<Button> terrainGroup = new ButtonGroup<>();
        int col = 0;
        
        for (TerrainType type : TerrainType.values()) {
            if (type == TerrainType.AIR) continue;
            
            // 使用 VisImageButton 代替 VisImageTextButton 以避免样式问题
            // 我们将手动组合 Image 和 Label
            VisImageButton btn = new VisImageButton("default"); 
            // 设置为 Toggle 模式
            VisImageButton.VisImageButtonStyle style = new VisImageButton.VisImageButtonStyle(btn.getStyle());
            
            // 借用 TextButton 的 toggle 选中背景 (如果存在) 或简单使用 default
            // 这里我们手动处理 checked 逻辑，样式上先用 default
            
            TextureRegion icon = dualRenderer.getIcon(type);
            if (icon != null) {
                style.imageUp = new TextureRegionDrawable(icon);
            }
            btn.setStyle(style);
            
            // 重组布局：上图下文
            btn.clearChildren();
            btn.add(btn.getImage()).size(32).pad(5).row();
            VisLabel label = new VisLabel(type.name());
            label.setFontScale(0.8f);
            btn.add(label).padBottom(5).row();
            
            if (type == selectedTerrain) btn.setChecked(true);
            
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if(btn.isChecked()) selectedTerrain = type;
                }
            });
            
            terrainGroup.add(btn);
            gridTable.add(btn).size(80, 80).pad(4);
            
            col++;
            if (col % 3 == 0) gridTable.row(); // 3列换行
        }
        contentTable.add(gridTable).row();
        
        // 橡皮擦 (AIR)
        VisTextButton btnEraser = new VisTextButton("Eraser (Air)", "toggle");
        btnEraser.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(btnEraser.isChecked()) selectedTerrain = TerrainType.AIR;
            }
        });
        terrainGroup.add(btnEraser);
        contentTable.add(btnEraser).fillX().padTop(5).row();


        // 层级选择
        contentTable.add(new VisLabel("--- Layer ---")).padTop(10).row();
        
        VisTable layerTable = new VisTable();
        VisSelectBox<String> layerSelect = new VisSelectBox<>();
        
        Runnable updateLayerItems = new Runnable() {
            @Override
            public void run() {
                String[] items = new String[worldData.getLayerCount()];
                for(int i=0; i<worldData.getLayerCount(); i++) {
                    items[i] = "Layer " + i;
                }
                layerSelect.setItems(items);
                
                if (selectedLayer >= items.length) selectedLayer = items.length - 1;
                if (selectedLayer < 0 && items.length > 0) selectedLayer = 0;
                
                if (items.length > 0) {
                    layerSelect.setSelectedIndex(selectedLayer);
                }
            }
        };
        updateLayerItems.run();

        layerSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedLayer = layerSelect.getSelectedIndex();
            }
        });
        
        VisTextButton btnDelLayer = new VisTextButton("-");
        btnDelLayer.addListener(new ChangeListener() {
             @Override
             public void changed(ChangeEvent event, Actor actor) {
                 if (worldData.getLayerCount() > 1) {
                     int idx = layerSelect.getSelectedIndex();
                     worldData.removeLayer(idx);
                     // Adjust selected layer logic is handled in updateLayerItems, but we need to update selectedLayer before update UI or let UI handle it
                     // If we delete current layer, we should select the previous one or 0
                     if (selectedLayer >= idx) selectedLayer--; 
                     if (selectedLayer < 0) selectedLayer = 0;
                     
                     updateLayerItems.run();
                 }
             }
        });
        
        VisTextButton btnAddLayer = new VisTextButton("+");
        btnAddLayer.addListener(new ChangeListener() {
             @Override
             public void changed(ChangeEvent event, Actor actor) {
                 worldData.addLayer();
                 selectedLayer = worldData.getLayerCount() - 1;
                 updateLayerItems.run();
             }
        });

        layerTable.add(layerSelect).width(120);
        layerTable.add(btnDelLayer).padLeft(5).width(30);
        layerTable.add(btnAddLayer).padLeft(20).width(30); 
        
        contentTable.add(layerTable).row();

        VisCheckBox cbGrid = new VisCheckBox("Show Grid", true);
        cbGrid.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showGrid = cbGrid.isChecked();
            }
        });
        contentTable.add(cbGrid).padTop(10).row();

        VisTextButton btnClear = new VisTextButton("Clear All");
        btnClear.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                worldData.clearAll();
            }
        });
        contentTable.add(btnClear).fillX().padTop(10).padBottom(20).row();

        // 将 contentTable 放入 ScrollPane
        VisScrollPane scrollPane = new VisScrollPane(contentTable);
        scrollPane.setScrollingDisabled(true, false); // 只允许垂直滚动
        scrollPane.setFadeScrollBars(false);
        
        menu.add(scrollPane).width(300).growY(); // 限制宽度并填充高度
        
        root.add(menu).growY().top().right(); // 确保 menu 能够利用垂直空间
        uiStage.addActor(root);
    }

    @Override
    public void render0(float delta) {
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        // 渲染逻辑：按层级顺序 0 -> 1 -> 2 渲染，实现正确的遮挡关系
        for (int l = 0; l < worldData.getLayerCount(); l++) {
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
