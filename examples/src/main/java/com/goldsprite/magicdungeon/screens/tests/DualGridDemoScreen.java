package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
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
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.DualGridConfig;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.DualGridRenderer;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.GridData;
import com.goldsprite.magicdungeon.screens.tests.dualgrid.TerrainType;

/**
 * 动态多层双网格系统
 * 支持：指定层级绘画、同层互斥、多层叠加、Air擦除
 */
public class DualGridDemoScreen extends GScreen {

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
        int gx = (int) Math.floor(worldPos.x / DualGridConfig.TILE_SIZE);
        int gy = (int) Math.floor(worldPos.y / DualGridConfig.TILE_SIZE);

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
        menu.setBackground("window");

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

            VisImageButton btn = new VisImageButton("default");
            VisImageButton.VisImageButtonStyle style = new VisImageButton.VisImageButtonStyle(btn.getStyle());

            TextureRegion icon = dualRenderer.getIcon(type);
            if (icon != null) {
                style.imageUp = new TextureRegionDrawable(icon);
            }
            btn.setStyle(style);

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
        HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(contentTable);
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
            for (int x = 0; x <= DualGridConfig.GRID_W; x++) debugRenderer.line(x * DualGridConfig.TILE_SIZE, 0, x * DualGridConfig.TILE_SIZE, DualGridConfig.GRID_H * DualGridConfig.TILE_SIZE);
            for (int y = 0; y <= DualGridConfig.GRID_H; y++) debugRenderer.line(0, y * DualGridConfig.TILE_SIZE, DualGridConfig.GRID_W * DualGridConfig.TILE_SIZE, y * DualGridConfig.TILE_SIZE);

            // 黄色渲染网格
            debugRenderer.setColor(Color.YELLOW);
            float off = DualGridConfig.DISPLAY_OFFSET;
            for (int x = 0; x <= DualGridConfig.GRID_W; x++) debugRenderer.line(x * DualGridConfig.TILE_SIZE - off, -off, x * DualGridConfig.TILE_SIZE - off, DualGridConfig.GRID_H * DualGridConfig.TILE_SIZE - off);
            for (int y = 0; y <= DualGridConfig.GRID_H; y++) debugRenderer.line(-off, y * DualGridConfig.TILE_SIZE - off, DualGridConfig.GRID_W * DualGridConfig.TILE_SIZE - off, y * DualGridConfig.TILE_SIZE - off);
            debugRenderer.end();

            // 红色采样点：只显示当前选中层的内容
            debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
            debugRenderer.setColor(Color.RED);
            for (int x = 0; x < DualGridConfig.GRID_W; x++) {
                for (int y = 0; y < DualGridConfig.GRID_H; y++) {
                    if (worldData.getTileId(selectedLayer, x, y) != -1)
                        debugRenderer.circle(x * DualGridConfig.TILE_SIZE + off, y * DualGridConfig.TILE_SIZE + off, 4);
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
