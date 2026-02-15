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
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.ShortArray;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.AppConstants;
import com.goldsprite.magicdungeon.BuildConfig;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;
import com.goldsprite.gdengine.ui.widget.HoverFocusScrollPane;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.ObjectMap;
import java.io.IOException;
import java.io.StringWriter;
import java.util.function.Consumer;

public class PolyBatchTestScreen extends GScreen {
	// 渲染
	private SpriteBatch batch;
	private PolygonSpriteBatch polyBatch;
	private NeonBatch neonBatch;

	// 状态与数据
	public enum Mode { ALIGN, MESH, WEIGHT, STATIC_TEST, DYNAMIC_WAVE }
	public enum MeshTool { HULL, INTERIOR } // 子模式：轮廓/内部点
	private Mode currentMode = Mode.ALIGN;
	private MeshTool currentMeshTool = MeshTool.HULL;
	private CapeState capeState = new CapeState();
	private static float deafultWeight = 0.5f;

	// UI
	private Stage uiStage;
	private UIController uiController;
	private EditorInputHandler inputHandler;
	private boolean showDebugInfo = true; // [修改] 控制所有调试信息的显隐

	@Override protected void initViewport() {
		// 移除 worldCamera 相关的设置，现在全部使用 UI viewport
		// autoCenterWorldCamera = true;
		uiViewportScale = PlatformImpl.isDesktopUser() ? 1.5f : 2f;
		super.initViewport();
	}

	@Override
	public void create() {
		if(!VisUI.isLoaded()) VisUI.load();

		batch = new SpriteBatch();
		polyBatch = new PolygonSpriteBatch();
		neonBatch = new NeonBatch();

		// [重构] 移除独立的 worldCamera，统一使用 uiViewport
		// UI Stage 默认使用 ScreenViewport，我们需要确保它能正确缩放
		// 这里使用 GScreen 的 uiViewport，或者直接创建一个新的 FitViewport 给 Stage？
		// 为了保持一致，我们使用 uiViewport
		uiStage = new Stage(getUIViewport());
		// 注意：GScreen.getUIViewport() 返回的是一个 ScreenViewport (通常)，
		// 这里的需求是“UI 视口绘制”，即所有坐标基于 Stage 坐标系。

		// 加载素材
		capeState.initTextures("packs/PolyBatchTest/Knight.png", "packs/PolyBatchTest/Cape.png");

		// 初始化 UI
		uiController = new UIController(uiStage, this);

		// 使用基类 GScreen 的 imp
		imp.addProcessor(uiStage);
		inputHandler = new EditorInputHandler();
		imp.addProcessor(inputHandler);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.15f, 0.15f, 0.15f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 1. 在 gameArea 区域内绘制场景
		// 由于我们要在 UI 布局中绘制，我们需要获取 gameArea 的位置和尺寸
		// 并设置裁剪区域 (ScissorStack)

		// 刷新 UI 数据 (如顶点列表)
		if (uiController != null) uiController.update();

		uiStage.act(delta);
		uiStage.draw();

		// [重构] 场景绘制移到 UI 绘制之后？不，应该是在 gameArea 内部绘制
		// 或者我们在 render 中手动计算坐标。
		// 为了简单起见，我们仍然在 render 中绘制，但坐标系变换为 UI 坐标系，并限制在 gameArea 区域。

		drawSceneInGameArea(delta);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (batch != null) batch.dispose();
		if (polyBatch != null) polyBatch.dispose();
		if (neonBatch != null) neonBatch.dispose();
		if (uiStage != null) uiStage.dispose();

		// Dispose textures
		if (capeState.knightRegion != null && capeState.knightRegion.getTexture() != null) {
			capeState.knightRegion.getTexture().dispose();
		}
		if (capeState.capeRegion != null && capeState.capeRegion.getTexture() != null) {
			capeState.capeRegion.getTexture().dispose();
		}
	}

	private void drawSceneInGameArea(float delta) {
		Actor area = uiController.gameArea;
		if (area == null) return;

		// 获取 gameArea 在屏幕上的坐标和尺寸
		Vector2 pos = area.localToStageCoordinates(new Vector2(0, 0));
		float x = pos.x;
		float y = pos.y;
		float w = area.getWidth();
		float h = area.getHeight();

		// 设置裁剪区域
		Rectangle scissor = new Rectangle();
		Rectangle clipBounds = new Rectangle(x, y, w, h);
		ScissorStack.calculateScissors(uiStage.getCamera(), uiStage.getBatch().getTransformMatrix(), clipBounds, scissor);
		boolean isScissorsPushed = ScissorStack.pushScissors(scissor);

		if (isScissorsPushed) {
			Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);

			// 绘制内容
			// 坐标系原点移到 gameArea 左下角 + 居中偏移
			// 为了使渲染内容在 gameArea 中心，我们需要：
			// CenterX = x + w/2
			// CenterY = y + h/2
			// 渲染偏移量 = capeState.offset

			// [修复] 渲染图像位置问题
			// 之前可能因为 coordinate system 的理解偏差。
			// UI Stage 的 (0,0) 通常在屏幕左下角。
			// gameArea.localToStageCoordinates(0,0) 返回的是 gameArea 左下角在 Stage 中的位置。
			// 所以 centerX, centerY 就是 gameArea 的几何中心。
			float baseX = x + w / 2f;
			float baseY = y + h / 2f;

			// 为了解耦，我们最好在 InputHandler 里也动态获取 gameArea 的中心。
			// 但 InputHandler 访问 UIController 比较容易。
			uiController.renderBaseX = baseX;
			uiController.renderBaseY = baseY;

			batch.setProjectionMatrix(uiStage.getCamera().combined);
			polyBatch.setProjectionMatrix(uiStage.getCamera().combined);

			// 1. 绘制背景骑士
			if (currentMode != Mode.MESH) {
				batch.begin();
				// 骑士稍微大一点，假设 100x100，也居中
				// 既然披风是基于 baseX, baseY 画的，骑士也应该基于此相对位置画
				// 假设骑士和披风的相对位置是固定的 (0,0)
				batch.draw(capeState.knightRegion, baseX, baseY);
				batch.end();
			}

			// 2. 绘制披风
			renderCapeByMode(delta, baseX, baseY);

			// 结束裁剪
			ScissorStack.popScissors();
			Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
		}
	}

	private void renderCapeByMode(float delta, float baseX, float baseY) {
		float drawX = baseX + capeState.offset.x;
		float drawY = baseY + capeState.offset.y;

		switch (currentMode) {
			case ALIGN:
				batch.begin();
				batch.draw(capeState.capeRegion, drawX, drawY);
				batch.end();
				drawMeshDebug(drawX, drawY);
				break;
			case MESH:
			case WEIGHT:
				batch.begin();
				batch.draw(capeState.capeRegion, drawX, drawY);
				batch.end();
				drawMeshDebug(drawX, drawY);
				break;
			case STATIC_TEST:
			case DYNAMIC_WAVE:
				if (capeState.polyRegion != null) {
					if (currentMode == Mode.DYNAMIC_WAVE) capeState.updateAnimation(delta);
					polyBatch.begin();
					polyBatch.draw(capeState.polyRegion, drawX, drawY);
					polyBatch.end();
				drawMeshDebug(drawX, drawY);
				}
				break;
		}
	}

		private void drawMeshDebug(float ox, float oy) {
		if (!showDebugInfo) return; // [修改] 只有开启调试才绘制任何线框/点

		// 使用 NeonBatch 进行加粗绘制
		neonBatch.setProjectionMatrix(uiStage.getCamera().combined);
		neonBatch.begin();

		// 1. 绘制纹理边框 (红色矩形)
		float w = capeState.capeRegion.getRegionWidth();
		float h = capeState.capeRegion.getRegionHeight();
		neonBatch.drawRect(ox, oy, w, h, 0, 2, Color.RED, false);

		// 绘制三角形网格线
		if (capeState.triangles != null) {
			float[] v = capeState.animatedVertices;
			Color meshColor = Color.CYAN.cpy();
			meshColor.a = 0.6f;

			for (int i = 0; i < capeState.triangles.length; i += 3) {
				int i1 = capeState.triangles[i] * 2;
				int i2 = capeState.triangles[i+1] * 2;
				int i3 = capeState.triangles[i+2] * 2;
				neonBatch.drawLine(v[i1]+ox, v[i1+1]+oy, v[i2]+ox, v[i2+1]+oy, 1.5f, meshColor);
				neonBatch.drawLine(v[i2]+ox, v[i2+1]+oy, v[i3]+ox, v[i3+1]+oy, 1.5f, meshColor);
				neonBatch.drawLine(v[i3]+ox, v[i3+1]+oy, v[i1]+ox, v[i1+1]+oy, 1.5f, meshColor);
			}
		}

		boolean isWeightMode = (currentMode == Mode.WEIGHT);

		// 1. 绘制轮廓连线 (先画线，避免遮挡点)
		for(int i=0; i<capeState.hullPoints.size; i++) {
			ControlPoint p = capeState.hullPoints.get(i);
			if (i > 0) {
				ControlPoint prev = capeState.hullPoints.get(i-1);
				neonBatch.drawLine(prev.position.x+ox, prev.position.y+oy, p.position.x+ox, p.position.y+oy, 3, Color.BLUE);
			}
		}
		// 闭合轮廓线
		if (capeState.hullPoints.size > 2) {
			ControlPoint first = capeState.hullPoints.first();
			ControlPoint last = capeState.hullPoints.peek();
			neonBatch.drawLine(last.position.x+ox, last.position.y+oy, first.position.x+ox, first.position.y+oy, 3, Color.BLUE);
		}

		// 2. 绘制轮廓点 (蓝色)
		for(int i=0; i<capeState.hullPoints.size; i++) {
			ControlPoint p = capeState.hullPoints.get(i);

			if (isWeightMode) {
				drawWeightPoint(p, ox, oy);
			} else {
				// 加粗轮廓点
				neonBatch.drawCircle(p.position.x + ox, p.position.y + oy, 5, 2, Color.BLUE, 16, false);
				// 中心点实心
				neonBatch.drawCircle(p.position.x + ox, p.position.y + oy, 2, 0, Color.BLUE, 8, true);
			}
		}

		// 3. 绘制内部点 (黄色)
		for(ControlPoint p : capeState.interiorPoints) {
			if (isWeightMode) {
				drawWeightPoint(p, ox, oy);
			} else {
				neonBatch.drawCircle(p.position.x + ox, p.position.y + oy, 4, 0, Color.YELLOW, 12, true);
			}
		}

		// 3. 绘制列表选中的高亮提示 (绿色矩形)
		PointItem item = uiController.getHighlightedItem();
		if (item != null) {
			ControlPoint p = null;
			if (item.isHull && item.index < capeState.hullPoints.size) {
				p = capeState.hullPoints.get(item.index);
			} else if (!item.isHull && item.index < capeState.interiorPoints.size) {
				p = capeState.interiorPoints.get(item.index);
			}

			if (p != null) {
				// 选中点框4倍并改为简单空心矩形
				float rectSize = 40;
				neonBatch.drawRect(p.position.x + ox - rectSize/2, p.position.y + oy - rectSize/2, rectSize, rectSize, 0, 3, Color.GREEN, false);
			}
		}

		// 4. [新增] 绘制笔刷光标 (在 WEIGHT 模式下)
		if (isWeightMode && inputHandler != null) {
			neonBatch.drawCircle(inputHandler.currentMousePos.x, inputHandler.currentMousePos.y, uiController.brushRadius, 2, Color.WHITE, 32, false);
		}

		neonBatch.end();
	}

	private void drawWeightPoint(ControlPoint p, float ox, float oy) {
		float outerR = 12; // 2x
		float x = p.position.x + ox;
		float y = p.position.y + oy;

		// 1. 绘制背景圆 (深色)
		neonBatch.drawCircle(x, y, outerR, 0, Color.DARK_GRAY, 16, true);

		// 2. 绘制扇形进度条 (绿色)
		if (p.weight > 0) {
			neonBatch.drawSector(x, y, outerR, 90, 360 * p.weight, Color.GREEN, 16);
		}
	}

	// --- 内部数据类 ---
	public static class ControlPoint {
		public Vector2 position;
		public float weight; // 0.0 - 1.0

		public ControlPoint() {
			this(0, 0, 0);
		}

		public ControlPoint(float x, float y, float weight) {
			this.position = new Vector2(x, y);
			this.weight = weight;
		}

		public ControlPoint(Vector2 pos, float weight) {
			this.position = new Vector2(pos);
			this.weight = weight;
		}
	}

	// 移出 PointItem 到外部或静态类，方便引用
	public static class PointItem {
		boolean isHull;
		int index;

		public PointItem(boolean isHull, int index) {
			this.isHull = isHull;
			this.index = index;
		}
	}

	static class CapeState {
		public TextureRegion knightRegion, capeRegion;
		public Vector2 offset = new Vector2(0, 0);
		public boolean dirty = true; // 结构变更标记 (增删点)
		public boolean weightChanged = false; // 权重变更标记 (刷权重)

		// 1. 数据结构拆分
		public Array<ControlPoint> hullPoints = new Array<>(); // 轮廓点 (有序)
		public Array<ControlPoint> interiorPoints = new Array<>(); // 内部点 (无序)

		// 所有的点 (hull + interior)，用于构建网格顶点数组
		// 注意：PolygonRegion 的 vertices 数组必须和这个列表的顺序一致
		private Array<ControlPoint> allPoints = new Array<>();

		public short[] triangles;
		public float[] originalVertices, animatedVertices;
		public PolygonRegion polyRegion;
		public float stateTime = 0;

		// 动态波动参数
		public float windStrength = -15f;
		public float bigWaveFreq = 2f;
		public float bigWavePhase = 0.02f;
		public float bigWaveAmp = 20f;
		public float smallWaveFreq = 8f;
		public float smallWavePhase = 0.1f;
		public float smallWaveAmp = 4f;

		// 传导幅度 (从上到下的放大倍率)
		public float transmissionAmp = 1f;

		public void initTextures(String kPath, String cPath) {
			knightRegion = new TextureRegion(new Texture(kPath));
			capeRegion = new TextureRegion(new Texture(cPath));
		}

		public void loadFromJson(JsonValue root) {
			if (root == null) return;
			// 1. 读取 Offset
			if (root.has("offset")) {
				offset.x = root.get("offset").getFloat("x", 0);
				offset.y = root.get("offset").getFloat("y", 0);
			}

			// 2. 读取 Hull Points
			hullPoints.clear();
			if (root.has("hullPoints")) {
				for (JsonValue v : root.get("hullPoints")) {
					float w = v.has("weight") ? v.getFloat("weight") : calculateDefaultWeight(v.getFloat("y"));
					hullPoints.add(new ControlPoint(v.getFloat("x"), v.getFloat("y"), w));
				}
			}

			// 3. 读取 Interior Points
			interiorPoints.clear();
			if (root.has("interiorPoints")) {
				for (JsonValue v : root.get("interiorPoints")) {
					float w = v.has("weight") ? v.getFloat("weight") : calculateDefaultWeight(v.getFloat("y"));
					interiorPoints.add(new ControlPoint(v.getFloat("x"), v.getFloat("y"), w));
				}
			}

			// 4. 读取参数
			windStrength = root.getFloat("windStrength", -15f);
			bigWaveFreq = root.getFloat("bigWaveFreq", 2f);
			bigWavePhase = root.getFloat("bigWavePhase", 0.02f);
			bigWaveAmp = root.getFloat("bigWaveAmp", 20f);
			smallWaveFreq = root.getFloat("smallWaveFreq", 8f);
			smallWavePhase = root.getFloat("smallWavePhase", 0.1f);
			smallWaveAmp = root.getFloat("smallWaveAmp", 4f);
			transmissionAmp = root.getFloat("transmissionAmp", 1f); // 默认 1

			generateMesh();
		}

		private float calculateDefaultWeight(float y) {
			return deafultWeight;
		}

		public String toJson() {
			StringWriter stringWriter = new StringWriter();
			JsonWriter writer = new JsonWriter(stringWriter);
			writer.setOutputType(JsonWriter.OutputType.json);

			try {
				writer.object();

				writer.name("offset").object()
					.name("x").value(offset.x)
					.name("y").value(offset.y)
				.pop();

				writer.name("hullPoints").array();
				for(ControlPoint p : hullPoints) {
					writer.object()
						.name("x").value(p.position.x)
						.name("y").value(p.position.y)
						.name("weight").value(p.weight)
					.pop();
				}
				writer.pop();

				writer.name("interiorPoints").array();
				for(ControlPoint p : interiorPoints) {
					writer.object()
						.name("x").value(p.position.x)
						.name("y").value(p.position.y)
						.name("weight").value(p.weight)
					.pop();
				}
				writer.pop();

				writer.name("windStrength").value(windStrength);
				writer.name("bigWaveFreq").value(bigWaveFreq);
				writer.name("bigWavePhase").value(bigWavePhase);
				writer.name("bigWaveAmp").value(bigWaveAmp);
				writer.name("smallWaveFreq").value(smallWaveFreq);
				writer.name("smallWavePhase").value(smallWavePhase);
				writer.name("smallWaveAmp").value(smallWaveAmp);
				writer.name("transmissionAmp").value(transmissionAmp);

				writer.pop();
				writer.close();
			} catch (IOException e) {
				Gdx.app.error("CapeState", "Failed to write json", e);
			}

			return new Json().prettyPrint(stringWriter.toString());
		}

		private Array<Object> serializePoints(Array<ControlPoint> points) {
			// Deprecated: used by old toJson
			return null;
		}

		public void clear() {
			hullPoints.clear();
			interiorPoints.clear();
			allPoints.clear();
			triangles = null;
			originalVertices = null;
			animatedVertices = null;
			polyRegion = null;
			dirty = true;
		}

		public void generateMesh() {
			// 至少要有3个轮廓点才能构成多边形
			if (hullPoints.size < 3) return;

			// 1. 合并所有点
			allPoints.clear();
			allPoints.addAll(hullPoints);
			allPoints.addAll(interiorPoints);
			dirty = true;

			FloatArray fa = new FloatArray();
			for (ControlPoint p : allPoints) fa.addAll(p.position.x, p.position.y);

			originalVertices = fa.toArray();
			animatedVertices = fa.toArray();

			try {
				// 2. 使用 Delaunay 生成全凸包网格
				ShortArray allTriangles = new DelaunayTriangulator().computeTriangles(fa, false);

				// 3. 剔除重心在轮廓外部的三角形 (实现凹包)
				// 准备轮廓多边形数据用于检测
				Array<Vector2> hullVectors = new Array<>();
				for(ControlPoint p : hullPoints) hullVectors.add(p.position);

				ShortArray validTriangles = new ShortArray();
				for (int i = 0; i < allTriangles.size; i += 3) {
					int p1 = allTriangles.get(i) * 2;
					int p2 = allTriangles.get(i+1) * 2;
					int p3 = allTriangles.get(i+2) * 2;

					// 计算三角形重心
					float cx = (originalVertices[p1] + originalVertices[p2] + originalVertices[p3]) / 3f;
					float cy = (originalVertices[p1+1] + originalVertices[p2+1] + originalVertices[p3+1]) / 3f;

					// 判断重心是否在轮廓内
					if (Intersector.isPointInPolygon(hullVectors, new Vector2(cx, cy))) {
						validTriangles.add(allTriangles.get(i));
						validTriangles.add(allTriangles.get(i+1));
						validTriangles.add(allTriangles.get(i+2));
					}
				}

				triangles = validTriangles.toArray();
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

				float finalOffset = 0;

				// 获取权重
				int pointIndex = i / 2;
				float weight = 0;
				if (pointIndex < allPoints.size) {
					weight = allPoints.get(pointIndex).weight;
				}

				// 1. 权重计算
				float factor = weight * transmissionAmp;

				// A. 基础持续风力
				float baseWind = windStrength * factor;

				finalOffset = baseWind;

				// B & C. 波动 (仅 Dynamic)
				float bigWave = (float) Math.sin(stateTime * bigWaveFreq - oldY * bigWavePhase) * bigWaveAmp * factor;
				float smallRipple = (float) Math.sin(stateTime * smallWaveFreq - oldY * smallWavePhase) * smallWaveAmp * factor;
				finalOffset += bigWave + smallRipple;

				animatedVertices[i] = oldX + finalOffset;
				animatedVertices[i + 1] = oldY;
			}
		}

		// 新增：重置动画顶点到原始状态 (用于进入 STATIC_TEST 时)
		public void resetAnimatedVertices() {
			if (originalVertices != null && animatedVertices != null) {
				System.arraycopy(originalVertices, 0, animatedVertices, 0, originalVertices.length);
			}
		}

		// 新增：应用临时形变 (用于 STATIC_TEST 鼠标拖拽)
		public void applyTemporaryDeformation(Vector2 pullPos, Vector2 pullDelta) {
			if (animatedVertices == null) return;

			// 简单的径向衰减变形：距离 pullPos 越近的点，受到的 pullDelta 影响越大
			float radius = 100f; // 影响半径

			for (int i = 0; i < animatedVertices.length; i += 2) {
				float vx = animatedVertices[i];
				float vy = animatedVertices[i+1];

				float dist = Vector2.dst(vx, vy, pullPos.x, pullPos.y);
				if (dist < radius) {
					float falloff = (radius - dist) / radius; // 1.0 at center, 0.0 at edge
					falloff = Interpolation.pow2Out.apply(falloff); // 非线性衰减

					animatedVertices[i] += pullDelta.x * falloff;
					animatedVertices[i+1] += pullDelta.y * falloff;
				}
			}
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
		private boolean isHullPoint = false; // 选中的是轮廓点还是内部点
		private Vector2 lastMousePos = new Vector2();
		private boolean isDraggingPoint = false;
		// 当前鼠标位置 (用于绘制笔刷)
		public Vector2 currentMousePos = new Vector2();

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
			currentMousePos.set(stageCoords);
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 screenPos = new Vector2(screenX, screenY);
			Vector2 stagePos = uiStage.screenToStageCoordinates(screenPos.cpy());
			Actor hitActor = uiStage.hit(stagePos.x, stagePos.y, true);

			if (hitActor != null && hitActor != uiController.gameArea && !hitActor.isDescendantOf(uiController.gameArea)) {
				return false;
			}

			Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
			lastMousePos.set(stageCoords);
			currentMousePos.set(stageCoords);

			float baseX = uiController.renderBaseX + capeState.offset.x;
			float baseY = uiController.renderBaseY + capeState.offset.y;

			Vector2 localClick = new Vector2(stageCoords).sub(baseX, baseY);

			if (currentMode == Mode.MESH || currentMode == Mode.WEIGHT) {
				selectedPointIndex = -1;
				isDraggingPoint = false;
				float minDst = 15f;

				float closestDst = Float.MAX_VALUE;
				int closestIndex = -1;
				boolean closestIsHull = false;

				for (int i = 0; i < capeState.hullPoints.size; i++) {
					float dst = capeState.hullPoints.get(i).position.dst(localClick);
					if (dst < minDst && dst < closestDst) {
						closestDst = dst;
						closestIndex = i;
						closestIsHull = true;
					}
				}
				for (int i = 0; i < capeState.interiorPoints.size; i++) {
					float dst = capeState.interiorPoints.get(i).position.dst(localClick);
					if (dst < minDst && dst < closestDst) {
						closestDst = dst;
						closestIndex = i;
						closestIsHull = false;
					}
				}

				if (closestIndex != -1) {
					selectedPointIndex = closestIndex;
					isHullPoint = closestIsHull;

					uiController.highlightedItem = new PointItem(isHullPoint, selectedPointIndex);
					uiController.update();

					if (currentMode == Mode.MESH) {
						if (button == Input.Buttons.RIGHT) {
							if (isHullPoint) capeState.hullPoints.removeIndex(selectedPointIndex);
							else capeState.interiorPoints.removeIndex(selectedPointIndex);
							capeState.generateMesh();
							selectedPointIndex = -1;
						} else {
							isDraggingPoint = true;
						}
					} else if (currentMode == Mode.WEIGHT) {
						applyWeightBrush(localClick, button == Input.Buttons.RIGHT);
						isDraggingPoint = true;
					}
				} else {
					if (currentMode == Mode.MESH && button == Input.Buttons.LEFT) {
						if (currentMeshTool == MeshTool.HULL) {
							capeState.hullPoints.add(new ControlPoint(localClick, deafultWeight));
						} else {
							capeState.interiorPoints.add(new ControlPoint(localClick, deafultWeight));
						}
						capeState.generateMesh();
					} else if (currentMode == Mode.WEIGHT) {
						applyWeightBrush(localClick, button == Input.Buttons.RIGHT);
						isDraggingPoint = true;
					}
				}
			}
			else if (currentMode == Mode.STATIC_TEST) {
				isDraggingPoint = true;
			}
			return true;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			Vector2 currentMouse = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
			currentMousePos.set(currentMouse);
			Vector2 delta = new Vector2(currentMouse).sub(lastMousePos);

			if (currentMode == Mode.ALIGN) {
				capeState.offset.add(delta);
			}
			else if (currentMode == Mode.MESH && isDraggingPoint && selectedPointIndex != -1) {
				ControlPoint p = isHullPoint ? capeState.hullPoints.get(selectedPointIndex) : capeState.interiorPoints.get(selectedPointIndex);
				p.position.add(delta);
				capeState.generateMesh();
			}
			else if (currentMode == Mode.WEIGHT && isDraggingPoint) {
				float baseX = uiController.renderBaseX + capeState.offset.x;
				float baseY = uiController.renderBaseY + capeState.offset.y;
				Vector2 localPos = new Vector2(currentMouse).sub(baseX, baseY);
				applyWeightBrush(localPos, Gdx.input.isButtonPressed(Input.Buttons.RIGHT));
			}
			else if (currentMode == Mode.STATIC_TEST && isDraggingPoint) {
				float baseX = uiController.renderBaseX + capeState.offset.x;
				float baseY = uiController.renderBaseY + capeState.offset.y;
				Vector2 pullPos = new Vector2(lastMousePos).sub(baseX, baseY);
				capeState.applyTemporaryDeformation(pullPos, delta);
			}

			lastMousePos.set(currentMouse);
			return true;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			isDraggingPoint = false;
			return true;
		}

		private void applyWeightBrush(Vector2 localPos, boolean isEraser) {
			float radius = uiController.brushRadius;
			float target = isEraser ? 0f : uiController.brushTargetWeight;
			float strength = uiController.brushStrength;

			for (ControlPoint p : capeState.hullPoints) updatePointWeight(p, localPos, radius, target, strength);
			for (ControlPoint p : capeState.interiorPoints) updatePointWeight(p, localPos, radius, target, strength);

			// 标记权重变更，以便 UIController 刷新显示
			if (uiController.brushStrength > 0) { // 只有确实刷了才标记? 简单点总是标记吧
				capeState.weightChanged = true;
			}
		}

		private void updatePointWeight(ControlPoint p, Vector2 brushPos, float radius, float target, float strength) {
			float dst = p.position.dst(brushPos);
			if (dst <= radius) {
				float alpha = strength * (1.0f - dst / radius);
				if (alpha < 0) alpha = 0;
				// 如果是橡皮擦 (target=0)，且 strength 通常较小，
				// 这里使用 lerp 效果是可以的。
				// 如果希望橡皮擦更强力，可以根据 strength 调整。
				p.weight = p.weight + (target - p.weight) * alpha;
				p.weight = MathUtils.clamp(p.weight, 0f, 1f);
			}
		}
	}

	// --- UI 控制类 ---
	static class UIController {
		private final PolyBatchTestScreen screen;
		private final VisTable pointListTable; // 替换 VisList 为 Table 容器
		private final VisCheckBox boundsCheck;
		public final VisTable gameArea;

		// 渲染基准点 (用于 InputHandler 坐标转换)
		public float renderBaseX, renderBaseY;

		// 笔刷设置
		public float brushRadius = 50f;
		public float brushTargetWeight = 1.0f;
		public float brushStrength = 0.1f;

		// 当前高亮的点信息
		private PointItem highlightedItem = null;

		// 缓存 ControlPoint 到 UI 组件的映射，用于快速更新文本
		private ObjectMap<ControlPoint, VisTextButton> pointWidgetMap = new ObjectMap<>();

		public UIController(Stage stage, final PolyBatchTestScreen screen) {
			this.screen = screen;

			VisTable root = new VisTable();
			root.setFillParent(true);
			root.setTouchable(Touchable.childrenOnly);

			VisTable panel = new VisTable(true);
			panel.setBackground("window");

			// 标题
			panel.add(new VisLabel("控制面板")).pad(10).row();

			// 模式切换
			VisSelectBox<Mode> modeSelect = new VisSelectBox<>();
			modeSelect.setItems(Mode.values());
			modeSelect.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (modeSelect.getSelected() != null) {
						screen.currentMode = modeSelect.getSelected();
						screen.capeState.reset();
					}
				}
			});
			panel.add(new VisLabel("模式:")).left();
			panel.add(modeSelect).expandX().fillX().row();

			// Mesh 子模式切换 (仅在 MESH 模式下显示)
			VisTable toolTable = new VisTable();
			VisTextButton btnHull = new VisTextButton("轮廓 (蓝)", "toggle");
			VisTextButton btnInterior = new VisTextButton("内部 (黄)", "toggle");
			ButtonGroup<VisTextButton> toolGroup = new ButtonGroup<>(btnHull, btnInterior);

			btnHull.setChecked(true);
			btnHull.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					if(btnHull.isChecked()) screen.currentMeshTool = MeshTool.HULL;
				}
			});
			btnInterior.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					if(btnInterior.isChecked()) screen.currentMeshTool = MeshTool.INTERIOR;
				}
			});

			toolTable.add(btnHull).expandX().fillX().padRight(5);
			toolTable.add(btnInterior).expandX().fillX();

			// 按钮栏
			VisTable btnTable = new VisTable();
			VisTextButton btnClear = new VisTextButton("清空");
			btnClear.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					screen.capeState.clear();
				}
			});

			VisTextButton btnSave = new VisTextButton("保存");
			btnSave.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					saveConfig();
				}
			});

			VisTextButton btnLoad = new VisTextButton("加载");
			btnLoad.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					loadConfig();
					VisTable paramPanel = (VisTable) screen.uiStage.getRoot().findActor("ParamPanel");
					if (paramPanel != null) {
						paramPanel.clearChildren();
						paramPanel.add(new VisLabel("参数调节")).pad(10).row();
						HoverFocusScrollPane sp = new HoverFocusScrollPane(createSlidersContent());
						sp.setFadeScrollBars(false);
						sp.setScrollingDisabled(true, false);
						paramPanel.add(sp).expand().fill().row();
					}
				}
			});

			btnTable.add(btnClear).padRight(5);
			btnTable.add(btnSave).padRight(5);
			btnTable.add(btnLoad);

			panel.add(toolTable).colspan(2).expandX().fillX().padTop(5).row();
			panel.add(btnTable).colspan(2).fillX().padTop(5).row();

			// 调试选项
			boundsCheck = new VisCheckBox("显示调试信息", true);
			boundsCheck.addListener(event -> {
				screen.showDebugInfo = boundsCheck.isChecked();
				return true;
			});
			panel.add(boundsCheck).colspan(2).left().padTop(5).row();

			// 顶点列表
			panel.add(new VisLabel("网格顶点 (UV):")).colspan(2).left().padTop(10).row();

			// 使用 Table 作为列表容器
			pointListTable = new VisTable();
			pointListTable.top().left();

			HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(pointListTable);
			scrollPane.setFadeScrollBars(false);
			scrollPane.setScrollingDisabled(true, false);
			panel.add(scrollPane).colspan(2).height(300).expandX().fillX().row();

			// 操作说明
			VisLabel tip = new VisLabel("操作说明:\nAlign: 拖动调整位置\nMesh: 点击添加点\nWeight: 拖动刷权重\nDynamic: 观看演示");
			tip.setWrap(true);
			panel.add(tip).colspan(2).width(200).padTop(10).row();
			panel.pack();

			// 左侧作为游戏操作区，设置为可触摸，以便在 InputHandler 中识别
			gameArea = new VisTable();
			gameArea.setTouchable(Touchable.enabled);
			gameArea.setBackground(VisUI.getSkin().newDrawable("white", new Color(0.2f, 0.2f, 0.2f, 1f)));

			VisTable paramPanel = createParamPanel();
			VisSplitPane rightSideSplit = new VisSplitPane(paramPanel, panel, false);
			rightSideSplit.setSplitAmount(0.5f);
			rightSideSplit.setMinSplitAmount(0.1f);
			rightSideSplit.setMaxSplitAmount(0.9f);

			VisSplitPane mainSplit = new VisSplitPane(gameArea, rightSideSplit, false);

			float totalWidth = stage.getWidth();
			float rightSidePrefWidth = panel.getPrefWidth() * 2 + 40;
			float split = 0.6f;
			if (totalWidth > 0) {
				split = (totalWidth - rightSidePrefWidth) / totalWidth;
				split = Math.max(0.2f, Math.min(0.85f, split));
			}
			mainSplit.setSplitAmount(split);
			mainSplit.setMinSplitAmount(0.1f);
			mainSplit.setMaxSplitAmount(0.9f);

			root.add(mainSplit).fill().expand();
			stage.addActor(root);
		}

		private VisTable createParamPanel() {
			VisTable table = new VisTable(true);
			table.setName("ParamPanel"); // 设置 Name 以便查找
			table.setBackground("window");
			table.add(new VisLabel("参数调节")).pad(10).row();

			HoverFocusScrollPane scrollPane = new HoverFocusScrollPane(createSlidersContent());
			scrollPane.setFadeScrollBars(false);
			scrollPane.setScrollingDisabled(true, false);

			table.add(scrollPane).expand().fill().row();
			return table;
		}

		private VisTable createSlidersContent() {
			VisTable t = new VisTable();
			t.defaults().left().expandX().fillX().pad(2);

			t.add(new VisLabel("--- 权重刷设置 ---")).padTop(10).row();
			addSlider(t, "目标权重", 0, 1, brushTargetWeight, v -> brushTargetWeight = v);
			addSlider(t, "笔刷半径", 10, 200, brushRadius, v -> brushRadius = v);
			addSlider(t, "笔刷强度", 0, 1, brushStrength, v -> brushStrength = v);

			t.add(new VisLabel("--- 波动参数 ---")).padTop(10).row();
			addSlider(t, "基础风力", -50, 50, screen.capeState.windStrength, v -> screen.capeState.windStrength = v);
			addSlider(t, "传导幅度 (垂直增益)", 0, 10, screen.capeState.transmissionAmp, v -> screen.capeState.transmissionAmp = v);

			t.add(new VisLabel("--- 大波浪 (主体) ---")).padTop(10).row();
			addSlider(t, "频率 (速度)", 0, 10, screen.capeState.bigWaveFreq, v -> screen.capeState.bigWaveFreq = v);
			addSlider(t, "幅度 (摆幅)", 0, 50, screen.capeState.bigWaveAmp, v -> screen.capeState.bigWaveAmp = v);
			addSlider(t, "相位 (波长)", 0, 0.2f, screen.capeState.bigWavePhase, v -> screen.capeState.bigWavePhase = v);

			t.add(new VisLabel("--- 小颤动 (细节) ---")).padTop(10).row();
			addSlider(t, "频率 (速度)", 0, 20, screen.capeState.smallWaveFreq, v -> screen.capeState.smallWaveFreq = v);
			addSlider(t, "幅度 (摆幅)", 0, 20, screen.capeState.smallWaveAmp, v -> screen.capeState.smallWaveAmp = v);
			addSlider(t, "相位 (波长)", 0, 0.5f, screen.capeState.smallWavePhase, v -> screen.capeState.smallWavePhase = v);

			return t;
		}

		private void addSlider(VisTable t, String label, float min, float max, float def, Consumer<Float> onChange) {
			t.add(new VisLabel(label)).row();
			VisSlider slider = new VisSlider(min, max, (max-min)/100f, false);
			slider.setValue(def);

			final VisLabel valLabel = new VisLabel(String.format("%.2f", def));

			slider.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					float v = slider.getValue();
					valLabel.setText(String.format("%.2f", v));
					onChange.accept(v);
				}
			});

			VisTable row = new VisTable();
			row.add(slider).expandX().fillX();
			row.add(valLabel).width(40).padLeft(5);
			t.add(row).row();
		}

		public void update() {
			if (screen.capeState.dirty) {
				rebuildPointList();
				screen.capeState.dirty = false;
				screen.capeState.weightChanged = false;
			} else if (screen.capeState.weightChanged) {
				refreshPointWeights();
				screen.capeState.weightChanged = false;
			}
		}

		private void rebuildPointList() {
			pointListTable.clear();
			pointWidgetMap.clear();

			float w = screen.capeState.capeRegion.getRegionWidth();
			float h = screen.capeState.capeRegion.getRegionHeight();

			// 1. 轮廓点部分
			if (screen.capeState.hullPoints.size > 0) {
				pointListTable.add(new VisLabel("[轮廓点 - 蓝色]")).left().padTop(5).row();
				for (int i = 0; i < screen.capeState.hullPoints.size; i++) {
					ControlPoint p = screen.capeState.hullPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					VisTextButton btn = createPointItemWidget(true, i, text);
					pointWidgetMap.put(p, btn);
					pointListTable.add(btn).expandX().fillX().row();
				}
			}

			// 2. 内部点部分
			if (screen.capeState.interiorPoints.size > 0) {
				pointListTable.add(new VisLabel("[内部点 - 黄色]")).left().padTop(10).row();
				for (int i = 0; i < screen.capeState.interiorPoints.size; i++) {
					ControlPoint p = screen.capeState.interiorPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					VisTextButton btn = createPointItemWidget(false, i, text);
					pointWidgetMap.put(p, btn);
					pointListTable.add(btn).expandX().fillX().row();
				}
			}

			if (screen.capeState.hullPoints.size == 0 && screen.capeState.interiorPoints.size == 0) {
				pointListTable.add(new VisLabel("无顶点")).left().row();
			}
		}

		private void refreshPointWeights() {
			float w = screen.capeState.capeRegion.getRegionWidth();
			float h = screen.capeState.capeRegion.getRegionHeight();

			// 重新遍历以获取 index 并更新文本
			for (int i = 0; i < screen.capeState.hullPoints.size; i++) {
				ControlPoint p = screen.capeState.hullPoints.get(i);
				VisTextButton btn = pointWidgetMap.get(p);
				if (btn != null) {
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					btn.setText(text);
				}
			}
			for (int i = 0; i < screen.capeState.interiorPoints.size; i++) {
				ControlPoint p = screen.capeState.interiorPoints.get(i);
				VisTextButton btn = pointWidgetMap.get(p);
				if (btn != null) {
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					btn.setText(text);
				}
			}
		}

		private void saveConfig() {
			try {
				FileHandle file = AppConstants.getLocalFile("cape_config.json");
				file.writeString(screen.capeState.toJson(), false);
				Gdx.app.log("Config", "Saved to " + file.file().getAbsolutePath());
			} catch (Exception e) {
				Gdx.app.error("Config", "Save failed", e);
			}
		}

		private void loadConfig() {
			try {
				FileHandle file = AppConstants.getLocalFile("cape_config.json");
				if (file.exists()) {
					JsonReader reader = new JsonReader();
					screen.capeState.loadFromJson(reader.parse(file));

					// 刷新 UI 显示
					screen.capeState.dirty = true;
					update();
					Gdx.app.log("Config", "Loaded from " + file.file().getAbsolutePath());
				}
			} catch (Exception e) {
				Gdx.app.error("Config", "Load failed", e);
			}
		}

		private VisTextButton createPointItemWidget(boolean isHull, int index, String text) {
			VisTextButton btn = new VisTextButton(text, "toggle");
			btn.getLabel().setAlignment(Align.left);

			// 检查当前按钮是否应该是选中状态
			if (highlightedItem != null && highlightedItem.isHull == isHull && highlightedItem.index == index) {
				btn.setChecked(true);
			}

			btn.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					Gdx.app.log("UI", "Button clicked: " + text + ", checked: " + btn.isChecked());
					if (btn.isChecked()) {
						// 取消其他所有按钮的选中状态 (手动实现单选组逻辑，因为跨类别不好用 ButtonGroup)
						uncheckAllOthers(btn);
						highlightedItem = new PointItem(isHull, index);
					} else {
						// 如果取消选中，则清空高亮
						if (highlightedItem != null && highlightedItem.isHull == isHull && highlightedItem.index == index) {
							highlightedItem = null;
						}
					}
				}
			});
			return btn;
		}

		private void uncheckAllOthers(VisTextButton current) {
			for (Actor child : pointListTable.getChildren()) {
				if (child instanceof VisTextButton && child != current) {
					((VisTextButton) child).setChecked(false);
				}
			}
		}

		public PointItem getHighlightedItem() {
			return highlightedItem;
		}
	}
}
