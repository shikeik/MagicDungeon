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
import com.badlogic.gdx.scenes.scene2d.Stage;
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
			batch.begin();
			batch.setProjectionMatrix(worldCamera.combined);
			batch.draw(capeState.knightRegion, 100, 100);
			batch.end();
		}

		// 2. 根据模式渲染披风
		renderCapeByMode(delta);

		// 3. 绘制 UI
		uiStage.act(delta);
		uiStage.draw();
	}

	private void renderCapeByMode(float delta) {
		switch (currentMode) {
			case ALIGN:
				batch.begin();
				batch.draw(capeState.capeRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
				batch.end();
				break;
			case MESH:
				batch.begin();
				batch.draw(capeState.capeRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
				batch.end();
				drawMeshDebug();
				break;
			case STATIC_TEST:
			case DYNAMIC_WAVE:
				if (capeState.polyRegion != null) {
					if (currentMode == Mode.DYNAMIC_WAVE) capeState.updateAnimation(delta);
					polyBatch.begin();
					polyBatch.draw(capeState.polyRegion, 100 + capeState.offset.x, 100 + capeState.offset.y);
					polyBatch.end();
					drawMeshDebug();
				}
				break;
		}
	}

	private void drawMeshDebug() {
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
			FloatArray fa = new FloatArray();
			for (Vector2 v : points) fa.addAll(v.x, v.y);
			originalVertices = fa.toArray();
			animatedVertices = fa.toArray();
			triangles = new DelaunayTriangulator().computeTriangles(fa, false).toArray();
			polyRegion = new PolygonRegion(capeRegion, animatedVertices, triangles);
		}

		public void updateAnimation(float delta) {
			stateTime += delta;
			// ... 这里放入你之前的正弦波权重计算逻辑
		}
	}

	// --- 输入处理器 ---
	class EditorInputHandler extends InputAdapter {
		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			float worldY = Gdx.graphics.getHeight() - screenY;
			Vector2 click = new Vector2(screenX, worldY);

			if (currentMode == Mode.MESH) {
				// 记录相对于披风起始点的局部坐标
				capeState.points.add(click.sub(100 + capeState.offset.x, 100 + capeState.offset.y));
				capeState.generateMesh();
			}
			return true;
		}
		// ... 此处还需实现 touchDragged 逻辑来处理模式1的整体偏移和模式3的顶点拖动
	}

	// --- UI 控制类 ---
	static class UIController {
		public UIController(Stage stage, final PolyBatchTestScreen screen) {
			VisTable root = new VisTable();
			root.top().left().setFillParent(true);

			VisSelectBox<Mode> modeSelect = new VisSelectBox<>();
			modeSelect.setItems(Mode.values());
			modeSelect.addListener(event -> {
				if (modeSelect.getSelected() != null) screen.currentMode = modeSelect.getSelected();
				return true;
			});

			root.add(new VisLabel("模式切换: "));
			root.add(modeSelect).row();
			// ... 继续添加滑块 (Slider) 用于调节频率和幅度
			stage.addActor(root);
		}
	}
}
