package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class PolyBatchTestScreen extends GScreen {
	// 渲染
	private SpriteBatch batch;
	private PolygonSpriteBatch polyBatch;
	private ShapeRenderer shapes;

	// 状态与数据
	public enum Mode { ALIGN, MESH, STATIC_TEST, DYNAMIC_WAVE }
	private Mode currentMode = Mode.ALIGN;
	private CapeState capeState = new CapeState();

	// UI
	private Stage uiStage;
	private UIController uiController;
	private boolean showTextureBounds = true;

	@Override protected void initViewport() {
		autoCenterWorldCamera = true;
		super.initViewport();
	}

	@Override
	public void create() {
		if(!VisUI.isLoaded()) VisUI.load();

		batch = new SpriteBatch();
		polyBatch = new PolygonSpriteBatch();
		shapes = new ShapeRenderer();
		uiStage = new Stage(getUIViewport());

		// 加载素材
		capeState.initTextures("packs/PolyBatchTest/Knight.png", "packs/PolyBatchTest/Cape.png");

		// 初始化 UI
		uiController = new UIController(uiStage, this);

		// 输入多路复用：先 UI，再场景
		InputMultiplexer multiplexer = new InputMultiplexer(uiStage, new EditorInputHandler());
		Gdx.input.setInputProcessor(multiplexer);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 1. 绘制背景骑士 (除网格标注模式外都要画)
		if (currentMode != Mode.MESH) {
			batch.setProjectionMatrix(worldCamera.combined);
			batch.begin();
			batch.draw(capeState.knightRegion, 100, 100);
			batch.end();
		}

		// 2. 根据模式渲染披风
		renderCapeByMode(delta);

		// 3. 绘制 UI
		uiController.update();
		uiStage.act(delta);
		uiStage.draw();
	}

	private void renderCapeByMode(float delta) {
		switch (currentMode) {
			case ALIGN:
				batch.setProjectionMatrix(worldCamera.combined);
				batch.begin();
				batch.draw(capeState.capeRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
				batch.end();
				drawMeshDebug();
				break;
			case MESH:
				batch.setProjectionMatrix(worldCamera.combined);
				batch.begin();
				batch.draw(capeState.capeRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
				batch.end();
				drawMeshDebug();
				break;
			case STATIC_TEST:
			case DYNAMIC_WAVE:
				if (capeState.polyRegion != null) {
					if (currentMode == Mode.DYNAMIC_WAVE) capeState.updateAnimation(delta);
					polyBatch.setProjectionMatrix(worldCamera.combined);
					polyBatch.begin();
					polyBatch.draw(capeState.polyRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
					polyBatch.end();
					drawMeshDebug();
				}
				break;
		}
	}

	private void drawMeshDebug() {
		shapes.setProjectionMatrix(worldCamera.combined);
		
		// 1. 绘制纹理边框 (红色矩形)
		if (showTextureBounds) {
			shapes.begin(ShapeRenderer.ShapeType.Line);
			shapes.setColor(Color.RED);
			float x = 100 + capeState.offset.x;
			float y = 100 + capeState.offset.y;
			float w = capeState.capeRegion.getRegionWidth();
			float h = capeState.capeRegion.getRegionHeight();
			shapes.rect(x, y, w, h);
			shapes.end();
		}

		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.CYAN);

		// 绘制三角形网格线
		if (capeState.triangles != null) {
			float[] v = capeState.animatedVertices;
			float ox = 100 + capeState.offset.x;
			float oy = 100 + capeState.offset.y;
			for (int i = 0; i < capeState.triangles.length; i += 3) {
				int i1 = capeState.triangles[i] * 2;
				int i2 = capeState.triangles[i+1] * 2;
				int i3 = capeState.triangles[i+2] * 2;
				shapes.line(v[i1]+ox, v[i1+1]+oy, v[i2]+ox, v[i2+1]+oy);
				shapes.line(v[i2]+ox, v[i2+1]+oy, v[i3]+ox, v[i3+1]+oy);
				shapes.line(v[i3]+ox, v[i3+1]+oy, v[i1]+ox, v[i1+1]+oy);
			}
		}
		shapes.end();

		// 绘制顶点 (点)
		shapes.begin(ShapeRenderer.ShapeType.Filled);
		shapes.setColor(Color.YELLOW);
		float ox = 100 + capeState.offset.x;
		float oy = 100 + capeState.offset.y;
		for(Vector2 p : capeState.points) {
			shapes.circle(p.x + ox, p.y + oy, 3);
		}
		shapes.end();
	}

	// --- 内部数据类 ---
	static class CapeState {
		public TextureRegion knightRegion, capeRegion;
		public Vector2 offset = new Vector2(0, 0);
		public Array<Vector2> points = new Array<>();
		public short[] triangles;
		public float[] originalVertices, animatedVertices;
		public PolygonRegion polyRegion;
		public float stateTime = 0;

		public void initTextures(String kPath, String cPath) {
			knightRegion = new TextureRegion(new Texture(kPath));
			capeRegion = new TextureRegion(new Texture(cPath));
		}

		public void generateMesh() {
			if (points.size < 3) return; // 至少三个点才能构成多边形
			FloatArray fa = new FloatArray();
			for (Vector2 v : points) fa.addAll(v.x, v.y);
			originalVertices = fa.toArray();
			animatedVertices = fa.toArray();
			try {
				triangles = new DelaunayTriangulator().computeTriangles(fa, false).toArray();
				polyRegion = new PolygonRegion(capeRegion, animatedVertices, triangles);
			} catch (Exception e) {
				Gdx.app.error("CapeState", "Failed to generate mesh", e);
			}
		}

		public void updateAnimation(float delta) {
			stateTime += delta;
			if (originalVertices == null) return;

			for (int i = 0; i < originalVertices.length; i += 2) {
				float oldX = originalVertices[i];
				float oldY = originalVertices[i + 1];

				// 越往右（X越大）的顶点，波动幅度越大，且有一定的相位延迟
				float factor = oldX / 100f; // 假设披风宽度大概100
				float wave = (float) Math.sin(stateTime * 5f + oldX * 0.05f) * 10f * factor;

				animatedVertices[i] = oldX;
				animatedVertices[i + 1] = oldY + wave;
			}
			// 关键：通知 polyRegion 顶点数据已更新
			// PolygonRegion 内部引用的是数组地址，通常直接修改数组即可，
			// 但某些版本可能需要重新 new PolygonRegion(capeRegion, animatedVertices, triangles);
		}

		public void reset() {
			stateTime = 0;
			if (originalVertices != null && animatedVertices != null) {
				System.arraycopy(originalVertices, 0, animatedVertices, 0, originalVertices.length);
			}
		}
	}

	// --- 输入处理器 ---
	class EditorInputHandler extends InputAdapter {
		private int selectedPointIndex = -1; // 当前选中的顶点索引
		private Vector2 lastMousePos = new Vector2();

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 worldPos = screenToWorldCoord(screenX, screenY);
			lastMousePos.set(worldPos);

			// 获取相对于披风左下角的局部坐标
			// 披风渲染位置是 (100 + offset.x, 100 + offset.y)
			float baseX = 100 + capeState.offset.x;
			float baseY = 100 + capeState.offset.y;
			Vector2 localClick = new Vector2(worldPos).sub(baseX, baseY);

			if (currentMode == Mode.MESH) {
				// 模式2：点击即添加点
				// 只有在点在图片范围内才允许添加（可选，或者允许在外部添加）
				// 这里直接添加，localClick 即为相对于纹理左下角的坐标
				capeState.points.add(localClick);
				capeState.generateMesh();
			}
			else if (currentMode == Mode.STATIC_TEST) {
				// 模式3：寻找最近的顶点进行拾取
				selectedPointIndex = -1;
				float minDst = 20f * worldCamera.zoom; // 拾取半径随缩放调整
				for (int i = 0; i < capeState.points.size; i++) {
					// 顶点的存储坐标是 local 坐标，比较时需要加上 base 转换回世界坐标，或者把鼠标转成 local
					// 这里我们比较 local 坐标距离
					float dst = capeState.points.get(i).dst(localClick);
					if (dst < minDst) {
						minDst = dst;
						selectedPointIndex = i;
					}
				}
			}
			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			Vector2 currentMouse = screenToWorldCoord(screenX, screenY);
			Vector2 delta = new Vector2(currentMouse).sub(lastMousePos);

			if (currentMode == Mode.ALIGN) {
				// 模式1：整体移动披风偏移
				capeState.offset.add(delta);
			}
			else if (currentMode == Mode.STATIC_TEST && selectedPointIndex != -1) {
				// 模式3：移动选中的顶点
				Vector2 p = capeState.points.get(selectedPointIndex);
				p.add(delta); // delta 是世界坐标差值，直接加在 local 坐标上也是对的（平移量一致）
				capeState.generateMesh(); // 顶点变了，必须重新生成网格数据
			}

			lastMousePos.set(currentMouse);
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			selectedPointIndex = -1;
			return true;
		}
		
		private Vector2 screenToWorldCoord(int screenX, int screenY) {
			Vector3 vec = new Vector3(screenX, screenY, 0);
			worldCamera.unproject(vec);
			return new Vector2(vec.x, vec.y);
		}
	}

	// --- UI 控制类 ---
	static class UIController {
		private final PolyBatchTestScreen screen;
		private final VisList<String> pointList;
		private final VisCheckBox boundsCheck;

		public UIController(Stage stage, final PolyBatchTestScreen screen) {
			this.screen = screen;
			
			VisTable root = new VisTable();
			root.setFillParent(true);

			VisTable panel = new VisTable(true);
			panel.setBackground("window");
			
			// 标题
			panel.add(new VisLabel("控制面板")).pad(10).row();

			// 模式切换
			VisSelectBox<Mode> modeSelect = new VisSelectBox<>();
			modeSelect.setItems(Mode.values());
			modeSelect.addListener(event -> {
				if (modeSelect.getSelected() != null) {
					screen.currentMode = modeSelect.getSelected();
					screen.capeState.reset();
				}
				return true;
			});
			panel.add(new VisLabel("模式:")).left();
			panel.add(modeSelect).expandX().fillX().row();
			
			// 调试选项
			boundsCheck = new VisCheckBox("显示纹理边框", true);
			boundsCheck.addListener(event -> {
				screen.showTextureBounds = boundsCheck.isChecked();
				return true;
			});
			panel.add(boundsCheck).colspan(2).left().padTop(5).row();

			// 顶点列表
			panel.add(new VisLabel("网格顶点:")).colspan(2).left().padTop(10).row();
			pointList = new VisList<>();
			VisScrollPane scrollPane = new VisScrollPane(pointList);
			scrollPane.setFadeScrollBars(false);
			panel.add(scrollPane).colspan(2).height(300).expandX().fillX().row();

			// 操作说明
			VisLabel tip = new VisLabel("操作说明:\nAlign: 拖动调整位置\nMesh: 点击添加点\nStatic: 拖动点\nDynamic: 观看演示");
			tip.setWrap(true);
			panel.add(tip).colspan(2).width(200).padTop(10).row();
			panel.pack(); // 让面板先计算出首选尺寸

			// 左侧作为游戏操作区，设置为不可触摸（让点击穿透到底层）
			VisTable gameArea = new VisTable();
			gameArea.setTouchable(Touchable.disabled);

			// 使用 VisSplitPane 实现左右分栏，右侧面板宽度可调
			VisSplitPane splitPane = new VisSplitPane(gameArea, panel, false);
			
			// 计算初始分割比例 (让面板恰好展示完整)
			float totalWidth = stage.getWidth();
			float panelPrefWidth = panel.getPrefWidth() + 20; // 稍微多给一点余量
			float split = 0.75f; // 默认值
			if (totalWidth > 0) {
				split = (totalWidth - panelPrefWidth) / totalWidth;
				// 限制范围，避免面板太宽或太窄
				split = Math.max(0.2f, Math.min(0.85f, split));
			}
			splitPane.setSplitAmount(split);
			splitPane.setMinSplitAmount(0.1f);
			splitPane.setMaxSplitAmount(0.9f);

			root.add(splitPane).fill().expand();
			stage.addActor(root);
		}

		public void update() {
			// 更新顶点列表显示
			if (screen.capeState.points.size > 0) {
				float baseX = 100 + screen.capeState.offset.x;
				float baseY = 100 + screen.capeState.offset.y;
				
				String[] items = new String[screen.capeState.points.size];
				for (int i = 0; i < screen.capeState.points.size; i++) {
					Vector2 p = screen.capeState.points.get(i);
					// 显示局部坐标和世界坐标
					items[i] = String.format("[%d] Local: (%.0f, %.0f)\n      World: (%.0f, %.0f)", 
						i, p.x, p.y, p.x + baseX, p.y + baseY);
				}
				pointList.setItems(items);
			} else {
				pointList.setItems(new String[]{"无顶点"});
			}
		}
	}
}
