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
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTable;

/**
 * Dual-grid Tilemap System for libGDX (Android Optimized)
 * 逻辑层与显示层完全分离，支持多地形叠加。
 */
public class DualGridDemoScreen extends GScreen {

    // ==========================================
    // 1. 全局配置中心 - 加大 Unit 尺寸
    // ==========================================
    public static class Config {
        public static final int TILE_SIZE = 32; // 从 16 加大到 32
        public static final int GRID_W = 30;
        public static final int GRID_H = 20;
        public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;

        public static final TerrainType[] RENDER_ORDER = {
            TerrainType.GRASS, TerrainType.DIRT, TerrainType.SAND
        };
    }

    // ==========================================
    // 2. 地形类型与枚举
    // ==========================================
    public enum TerrainType {
        EMPTY(-1, null),
        GRASS(0, "sprites/tilesets/grass_tiles.png"),
        DIRT(1, "sprites/tilesets/dirt_tiles.png"),
        SAND(2, "sprites/tilesets/sand_tiles.png");

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
    // 4. 修正后的双格宫核心算法 (Renderer)
    // ==========================================
    public static class DualGridRenderer {
        private final TextureRegion[][] atlas = new TextureRegion[3][16];

        // 修正后的掩码映射表 (对应 libGDX Y-Up 环境)
        // 索引顺序: 0:Empty, 1:BR, 2:BL, 3:BottomEdge, 4:TR, 5:RightEdge, 6:TR+BL, 7:Inner-TL...
        // 这里直接使用 0-15 的二进制顺序
        private static final int[] MASK_TO_ATLAS_X = {2, 0, 3, 3, 0, 1, 0, 2, 3, 2, 3, 3, 1, 1, 1, 2};
        private static final int[] MASK_TO_ATLAS_Y = {3, 0, 3, 0, 2, 0, 1, 0, 2, 3, 2, 1, 2, 3, 1, 1};

        public void load() {
            for (TerrainType type : Config.RENDER_ORDER) {
                Texture tex = new Texture(Gdx.files.internal(type.texPath));
                TextureRegion[][] temp = TextureRegion.split(tex, 16, 16); // 原始素材是 16x16
                for (int i = 0; i < 16; i++) {
                    atlas[type.id][i] = temp[i / 4][i % 4];
                }
            }
        }

        public void render(SpriteBatch batch, GridData grid, TerrainType type) {
            // 显示层坐标遍历
            for (int x = 0; x <= Config.GRID_W; x++) {
                for (int y = 0; y <= Config.GRID_H; y++) {
                    int mask = calculateMask(grid, x, y, type);
                    if (mask == 0) continue; // 全空不画

                    int tx = MASK_TO_ATLAS_X[mask];
                    int ty = MASK_TO_ATLAS_Y[mask];

                    // 渲染位置：以 (x,y) 为中心，向左下偏移半格
                    float drawX = x * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
                    float drawY = y * Config.TILE_SIZE - Config.DISPLAY_OFFSET;

                    batch.draw(atlas[type.id][ty * 4 + tx], drawX, drawY, Config.TILE_SIZE, Config.TILE_SIZE);
                }
            }
        }

        private int calculateMask(GridData grid, int x, int y, TerrainType target) {
            // 在 libGDX (Y-Up) 中，(x,y) 是采样区域的右上方
            // 我们检查以 (x,y) 交叉点为中心的四个逻辑象限
            int tr = (grid.getTileId(x, y) == target.id) ? 1 : 0;         // Top Right
            int tl = (grid.getTileId(x - 1, y) == target.id) ? 1 : 0;     // Top Left
            int br = (grid.getTileId(x, y - 1) == target.id) ? 1 : 0;     // Bottom Right
            int bl = (grid.getTileId(x - 1, y - 1) == target.id) ? 1 : 0; // Bottom Left

            // 构造 4 位掩码 (对应映射表顺序)
            return (tl << 3) | (tr << 2) | (bl << 1) | br;
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
	
	// 1. 在类成员变量中增加记录上一次坐标的变量
	private int lastGx = -1;
	private int lastGy = -1;
	

    public DualGridDemoScreen() {
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
        // 4. 更新输入监听器
		imp.addProcessor(new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					handlePaint(screenX, screenY, true); // 新的点击
					return true;
				}

				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					handlePaint(screenX, screenY, false); // 拖拽中
					return true;
				}

				@Override
				public boolean touchUp(int screenX, int screenY, int pointer, int button) {
					lastGx = -1; // 重置
					lastGy = -1;
					return true;
				}
			});
    }
	

	// 2. 提取一个设置瓦片的方法，包含线性插值逻辑
	private void paintPath(int x1, int y1, int x2, int y2) {
		// Bresenham 直线算法：填充两点之间的所有逻辑格子
		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);
		int sx = x1 < x2 ? 1 : -1;
		int sy = y1 < y2 ? 1 : -1;
		int err = dx - dy;

		while (true) {
			worldData.setTile(x1, y1, selectedTerrain);
			if (x1 == x2 && y1 == y2) break;
			int e2 = 2 * err;
			if (e2 > -dy) {
				err -= dy;
				x1 += sx;
			}
			if (e2 < dx) {
				err += dx;
				y1 += sy;
			}
		}
	}

    // 3. 修改 handlePaint 处理逻辑
	private void handlePaint(int sx, int sy, boolean isNewTouch) {
		Vector2 worldPos = screenToWorldCoord(sx, sy);

		// 使用 floor 代替 round，这样点击位置会更符合逻辑网格的采样点
		int gx = (int) Math.floor(worldPos.x / Config.TILE_SIZE);
		int gy = (int) Math.floor(worldPos.y / Config.TILE_SIZE);

		if (isNewTouch) {
			worldData.setTile(gx, gy, selectedTerrain);
		} else if (lastGx != -1 && (gx != lastGx || gy != lastGy)) {
			// 如果不是第一次点击，且坐标发生了变化，则插值填充
			paintPath(lastGx, lastGy, gx, gy);
		}

		lastGx = gx;
		lastGy = gy;
	}

    private void setupUI() {
        VisTable root = new VisTable();
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
        VisTable menu = new VisTable();
		menu.setBackground("window-bg");
        menu.add(title).padBottom(10).row();
        menu.add(btnGrass).fillX().pad(2).row();
        menu.add(btnDirt).fillX().pad(2).row();
        menu.add(btnSand).fillX().pad(2).row();
        menu.add(cbGrid).padTop(10).row();
        menu.add(btnClear).fillX().padTop(20);

        root.add(menu).pad(20);
        uiStage.addActor(root);
    }

    // ==========================================
    // 5. 增强的渲染逻辑与网格辅助
    // ==========================================
    @Override
    public void render0(float delta) {
        // 1. 绘制地形
        batch.setProjectionMatrix(worldCamera.combined);
        batch.begin();
        for (TerrainType layer : Config.RENDER_ORDER) {
            dualRenderer.render(batch, worldData, layer);
        }
        batch.end();

        // 2. 绘制辅助网格
        if (showGrid) {
            debugRenderer.setProjectionMatrix(worldCamera.combined);
            debugRenderer.begin(ShapeRenderer.ShapeType.Filled);

			float blueWidth = 1.0f, yellowWidth = 0.5f;
            // A. 蓝色：逻辑网格 (逻辑格点是交汇点)
            debugRenderer.setColor(Color.BLUE);
            for (int x = 0; x <= Config.GRID_W; x++) {
                debugRenderer.rectLine(x * Config.TILE_SIZE, 0, x * Config.TILE_SIZE, Config.GRID_H * Config.TILE_SIZE, blueWidth);
            }
            for (int y = 0; y <= Config.GRID_H; y++) {
                debugRenderer.rectLine(0, y * Config.TILE_SIZE, Config.GRID_W * Config.TILE_SIZE, y * Config.TILE_SIZE, blueWidth);
            }

            // B. 黄色：渲染瓦片边界 (相对于逻辑网格偏移了 0.5)
            debugRenderer.setColor(Color.YELLOW);
            float off = Config.DISPLAY_OFFSET;
            for (int x = 0; x <= Config.GRID_W; x++) {
                debugRenderer.rectLine(x * Config.TILE_SIZE - off, -off, x * Config.TILE_SIZE - off, Config.GRID_H * Config.TILE_SIZE - off, yellowWidth);
            }
            for (int y = 0; y <= Config.GRID_H; y++) {
                debugRenderer.rectLine(-off, y * Config.TILE_SIZE - off, Config.GRID_W * Config.TILE_SIZE - off, y * Config.TILE_SIZE - off, yellowWidth);
            }

            // C. 红色：逻辑采样点 (点击生效的位置)
            debugRenderer.end();
            debugRenderer.begin(ShapeRenderer.ShapeType.Filled);
            debugRenderer.setColor(Color.RED);
            for (int x = 0; x < Config.GRID_W; x++) {
                for (int y = 0; y < Config.GRID_H; y++) {
                    if (worldData.getTileId(x, y) != -1)
                        debugRenderer.circle(x * Config.TILE_SIZE + off, y * Config.TILE_SIZE + off, 4);
                }
            }
            debugRenderer.end();
        }

        // 3. UI
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
        uiStage.dispose();
    }
}
