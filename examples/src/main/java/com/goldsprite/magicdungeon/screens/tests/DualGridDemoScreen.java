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
	// 1. 全局配置中心
	// ==========================================
	public static class Config {
		public static final int TILE_SIZE = 32;
		public static final int GRID_W = 40;
		public static final int GRID_H = 30;
		public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;

		// 自动计算总共需要多少层
		public static int getMaxLayers() {
			int max = 0;
			for (TerrainType t : TerrainType.values()) {
				if (t.layer > max)
					max = t.layer;
			}
			return max + 1;
		}
	}

	// ==========================================
	// 2. 地形类型 (支持自定义 layer 参数)
	// ==========================================
	public enum TerrainType {
		// id, layer, assetPath
		EMPTY(-1, -1, null), 
		DIRT(0, 0, "sprites/tilesets/dirt_tiles.png"), // Layer 0
		GRASS(1, 0, "sprites/tilesets/grass_tiles.png"), // Layer 1
		SAND(2, 0, "sprites/tilesets/sand_tiles.png"); // Layer 1 (与草地互斥)

		public final int id;
		public final int layer;
		public final String texPath;
		TerrainType(int id, int layer, String texPath) {
			this.id = id;
			this.layer = layer;
			this.texPath = texPath;
		}
	}

	// ==========================================
	// 3. 数据模型 (支持动态层级)
	// ==========================================
	public static class GridData {
		private final int[][][] data;
		private final int numLayers;

		public GridData() {
			this.numLayers = Config.getMaxLayers();
			this.data = new int[numLayers][Config.GRID_W][Config.GRID_H];
			clearAll();
		}

		public void clearAll() {
			for (int l = 0; l < numLayers; l++) {
				for (int x = 0; x < Config.GRID_W; x++) {
					for (int y = 0; y < Config.GRID_H; y++) {
						data[l][x][y] = -1;
					}
				}
			}
		}

		// 修复报错：添加此方法以适配按钮点击清空
		public void clear(TerrainType type) {
			clearAll();
		}

		public void setTile(int x, int y, TerrainType type) {
			if (x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H)
				return;

			if (type == TerrainType.EMPTY) {
				// 如果是空，清空该坐标的所有层级
				for (int l = 0; l < numLayers; l++)
					data[l][x][y] = -1;
			} else {
				// 否则，只设置对应的层级
				data[type.layer][x][y] = type.id;
			}
		}

		// 修复报错：保持三参数签名 (layer, x, y)
		public int getTileId(int layer, int x, int y) {
			if (layer < 0 || layer >= numLayers || x < 0 || x >= Config.GRID_W || y < 0 || y >= Config.GRID_H)
				return -1;
			return data[layer][x][y];
		}
	}

	// ==========================================
	// 4. 双格宫核心算法
	// ==========================================
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
				if (type.texPath == null)
					continue;
				Texture tex = new Texture(Gdx.files.internal(type.texPath));
				TextureRegion[][] temp = TextureRegion.split(tex, 16, 16);
				for (int i = 0; i < 16; i++) {
					atlas[type.id][i] = temp[i / 4][i % 4];
				}
			}
		}

		// 修复报错：补全 getAtlasIndex 方法
		private int getAtlasIndex(int mask) {
			if (mask <= 0 || mask >= 16)
				return -1;
			int tx = MASK_TO_ATLAS_X[mask];
			int ty = MASK_TO_ATLAS_Y[mask];
			if (tx == -1)
				return -1;
			return ty * 4 + tx;
		}

		public void render(SpriteBatch batch, GridData grid, TerrainType type) {
			for (int x = 0; x <= Config.GRID_W; x++) {
				for (int y = 0; y <= Config.GRID_H; y++) {
					// 注意：现在采样需要传入地形所属的 layer
					int mask = calculateMask(grid, type.layer, x, y, type);
					int atlasIndex = getAtlasIndex(mask);
					if (atlasIndex != -1) {
						float drawX = x * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
						float drawY = y * Config.TILE_SIZE - Config.DISPLAY_OFFSET;
						batch.draw(atlas[type.id][atlasIndex], drawX, drawY, Config.TILE_SIZE, Config.TILE_SIZE);
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
			for (TerrainType type : TerrainType.values()) {
				if (type.id >= 0 && atlas[type.id][0] != null)
					atlas[type.id][0].getTexture().dispose();
			}
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
		if (!VisUI.isLoaded())
			VisUI.load();

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
			if (x1 == x2 && y1 == y2)
				break;
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

		btnGrass.addListener(e -> {
			if (btnGrass.isChecked())
				selectedTerrain = TerrainType.GRASS;
			return true;
		});
		btnDirt.addListener(e -> {
			if (btnDirt.isChecked())
				selectedTerrain = TerrainType.DIRT;
			return true;
		});
		btnSand.addListener(e -> {
			if (btnSand.isChecked())
				selectedTerrain = TerrainType.SAND;
			return true;
		});

		VisCheckBox cbGrid = new VisCheckBox("Show Logic Grid", true);
		cbGrid.addListener(e -> {
			showGrid = cbGrid.isChecked();
			return true;
		});

		VisTextButton btnClear = new VisTextButton("Clear All");
		btnClear.addListener(e -> {
			worldData.clear(TerrainType.EMPTY);
			return true;
		});

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
		batch.setProjectionMatrix(worldCamera.combined);
		batch.begin();
        // 动态根据层级渲染：先画 layer 0, 再画 layer 1...
        int numLayers = Config.getMaxLayers();
        for (int l = 0; l < numLayers; l++) {
            for (TerrainType type : TerrainType.values()) {
                if (type.layer == l) {
                    dualRenderer.render(batch, worldData, type);
                }
            }
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
				debugRenderer.rectLine(x * Config.TILE_SIZE, 0, x * Config.TILE_SIZE, Config.GRID_H * Config.TILE_SIZE,
						blueWidth);
			}
			for (int y = 0; y <= Config.GRID_H; y++) {
				debugRenderer.rectLine(0, y * Config.TILE_SIZE, Config.GRID_W * Config.TILE_SIZE, y * Config.TILE_SIZE,
						blueWidth);
			}

			// B. 黄色：渲染瓦片边界 (相对于逻辑网格偏移了 0.5)
			debugRenderer.setColor(Color.YELLOW);
			float off = Config.DISPLAY_OFFSET;
			for (int x = 0; x <= Config.GRID_W; x++) {
				debugRenderer.rectLine(x * Config.TILE_SIZE - off, -off, x * Config.TILE_SIZE - off,
						Config.GRID_H * Config.TILE_SIZE - off, yellowWidth);
			}
			for (int y = 0; y <= Config.GRID_H; y++) {
				debugRenderer.rectLine(-off, y * Config.TILE_SIZE - off, Config.GRID_W * Config.TILE_SIZE - off,
						y * Config.TILE_SIZE - off, yellowWidth);
			}
			
			// 修正红色采样点显示逻辑
            debugRenderer.setColor(Color.RED);
            for (int x = 0; x < Config.GRID_W; x++) {
                for (int y = 0; y < Config.GRID_H; y++) {
                    boolean hasSomething = false;
                    for (int l = 0; l < numLayers; l++) {
                        if (worldData.getTileId(l, x, y) != -1) {
                            hasSomething = true;
                            break;
                        }
                    }
                    if (hasSomething) {
                        debugRenderer.circle(x * Config.TILE_SIZE + Config.DISPLAY_OFFSET, y * Config.TILE_SIZE + Config.DISPLAY_OFFSET, 4);
                    }
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

