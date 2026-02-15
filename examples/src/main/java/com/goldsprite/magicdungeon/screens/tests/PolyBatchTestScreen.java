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
import com.goldsprite.gdengine.screens.GScreen;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

public class PolyBatchTestScreen extends GScreen {
	// 渲染
	private SpriteBatch batch;
	private PolygonSpriteBatch polyBatch;
	private ShapeRenderer shapes;

	// 状态与数据
	public enum Mode { ALIGN, MESH, WEIGHT, STATIC_TEST, DYNAMIC_WAVE }
	public enum MeshTool { HULL, INTERIOR } // 子模式：轮廓/内部点
	private Mode currentMode = Mode.ALIGN;
	private MeshTool currentMeshTool = MeshTool.HULL;
	private CapeState capeState = new CapeState();

	// UI
	private Stage uiStage;
	private UIController uiController;
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
		shapes = new ShapeRenderer();
		
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
		imp.addProcessor(new EditorInputHandler());
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
		Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
		// calculateScissors 需要传入 Camera，这里使用 UI Camera
		com.badlogic.gdx.math.Rectangle scissor = new com.badlogic.gdx.math.Rectangle();
		com.badlogic.gdx.math.Rectangle clipBounds = new com.badlogic.gdx.math.Rectangle(x, y, w, h);
		com.badlogic.gdx.scenes.scene2d.utils.ScissorStack.calculateScissors(uiStage.getCamera(), uiStage.getBatch().getTransformMatrix(), clipBounds, scissor);
		com.badlogic.gdx.scenes.scene2d.utils.ScissorStack.pushScissors(scissor);
		
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
		// 我们的绘制逻辑 renderCapeByMode 是基于 (drawX, drawY) 进行绘制。
		// 只要 drawX = centerX, drawY = centerY，那么披风的“锚点”就在中心。
		// 披风纹理本身可能不是中心对齐的，这取决于纹理坐标和 Region 的使用方式。
		// 但只要 drawX/Y 是中心，且 batch.draw 是以左下角为基准，那么我们需要减去纹理宽高的一半才能真正居中。
		// 在 renderCapeByMode 里：
		// float drawX = baseX + capeState.offset.x;
		// float drawY = baseY + capeState.offset.y;
		// 如果 baseX, baseY 是中心点，那么 drawX, drawY 也是中心点。
		// 具体的 batch.draw(region, x, y) 是画在 x,y 的。
		// 所以如果要居中，我们需要在 draw 时减去 w/2, h/2。
		// 但目前的 renderCapeByMode 是直接 draw(region, drawX, drawY)。
		// 这意味着 drawX, drawY 是纹理的左下角。
		// 所以为了居中，baseX 应该等于 centerX - textureWidth/2。
		
		float cw = capeState.capeRegion.getRegionWidth();
		float ch = capeState.capeRegion.getRegionHeight();
		
		float baseX = (x + w / 2f) - cw / 2f;
		float baseY = (y + h / 2f) - ch / 2f;
		
		// 更新 CapeState 的渲染基准点 (用于 InputHandler 计算鼠标点击)
		// InputHandler 计算 localClick = worldPos - baseX - offset
		// 所以这里的 baseX 必须和 InputHandler 里用的一致。
		// 我们需要把这个计算好的基准点传给 UIController 或者直接存到 CapeState 里？
		// 为了解耦，我们最好在 InputHandler 里也动态获取 gameArea 的中心。
		// 但 InputHandler 访问 UIController 比较容易。
		uiController.renderBaseX = baseX;
		uiController.renderBaseY = baseY;

		batch.setProjectionMatrix(uiStage.getCamera().combined);
		polyBatch.setProjectionMatrix(uiStage.getCamera().combined);
		shapes.setProjectionMatrix(uiStage.getCamera().combined);
		
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
		com.badlogic.gdx.scenes.scene2d.utils.ScissorStack.popScissors();
		Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
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
		
		shapes.setProjectionMatrix(uiStage.getCamera().combined);
		
		// 1. 绘制纹理边框 (红色矩形)
		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.RED);
		float w = capeState.capeRegion.getRegionWidth();
		float h = capeState.capeRegion.getRegionHeight();
		shapes.rect(ox, oy, w, h);
		shapes.end();

		shapes.begin(ShapeRenderer.ShapeType.Line);
		shapes.setColor(Color.CYAN);

		// 绘制三角形网格线
		if (capeState.triangles != null) {
			float[] v = capeState.animatedVertices;
			
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
		boolean isWeightMode = (currentMode == Mode.WEIGHT);
		
		// 1. 绘制轮廓点 (蓝色)
		for(int i=0; i<capeState.hullPoints.size; i++) {
			ControlPoint p = capeState.hullPoints.get(i);
			
			if (isWeightMode) {
				drawWeightPoint(p, ox, oy);
			} else {
				shapes.setColor(Color.BLUE);
				shapes.circle(p.position.x + ox, p.position.y + oy, 4);
			}
			
			// 绘制轮廓连线
			if (i > 0) {
				ControlPoint prev = capeState.hullPoints.get(i-1);
				shapes.setColor(Color.BLUE);
				shapes.rectLine(prev.position.x+ox, prev.position.y+oy, p.position.x+ox, p.position.y+oy, 2);
			}
		}
		// 闭合轮廓线
		if (capeState.hullPoints.size > 2) {
			ControlPoint first = capeState.hullPoints.first();
			ControlPoint last = capeState.hullPoints.peek();
			shapes.setColor(Color.BLUE);
			shapes.rectLine(last.position.x+ox, last.position.y+oy, first.position.x+ox, first.position.y+oy, 2);
		}

		// 2. 绘制内部点 (黄色)
		for(ControlPoint p : capeState.interiorPoints) {
			if (isWeightMode) {
				drawWeightPoint(p, ox, oy);
			} else {
				shapes.setColor(Color.YELLOW);
				shapes.circle(p.position.x + ox, p.position.y + oy, 3);
			}
		}
		
		// 3. 绘制列表选中的高亮提示 (绿色大圆圈)
		PointItem item = uiController.getHighlightedItem();
		if (item != null) {
			shapes.setColor(Color.GREEN);
			ControlPoint p = null;
			if (item.isHull && item.index < capeState.hullPoints.size) {
				p = capeState.hullPoints.get(item.index);
			} else if (!item.isHull && item.index < capeState.interiorPoints.size) {
				p = capeState.interiorPoints.get(item.index);
			}
			
			if (p != null) {
				shapes.circle(p.position.x + ox, p.position.y + oy, 6); // 更大的圆
				shapes.rect(p.position.x + ox - 8, p.position.y + oy - 8, 16, 16); // 方框
			}
		}
		
		shapes.end();
	}

	private void drawWeightPoint(ControlPoint p, float ox, float oy) {
		Color c = new Color(1.0f - p.weight, p.weight, 0, 1);
		shapes.setColor(c);
		float outerR = 6;
		float innerR = 3;
		shapes.circle(p.position.x + ox, p.position.y + oy, outerR);
		shapes.setColor(Color.BLACK);
		shapes.circle(p.position.x + ox, p.position.y + oy, innerR);
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
		public boolean dirty = true; // 数据变更标记
		
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
		
		public void loadFromJson(com.badlogic.gdx.utils.JsonValue root) {
			// 1. 读取 Offset
			if (root.has("offset")) {
				offset.x = root.get("offset").getFloat("x");
				offset.y = root.get("offset").getFloat("y");
			}
			
			// 2. 读取 Hull Points
			hullPoints.clear();
			if (root.has("hullPoints")) {
				for (com.badlogic.gdx.utils.JsonValue v : root.get("hullPoints")) {
					float w = v.has("weight") ? v.getFloat("weight") : calculateDefaultWeight(v.getFloat("y"));
					hullPoints.add(new ControlPoint(v.getFloat("x"), v.getFloat("y"), w));
				}
			}
			
			// 3. 读取 Interior Points
			interiorPoints.clear();
			if (root.has("interiorPoints")) {
				for (com.badlogic.gdx.utils.JsonValue v : root.get("interiorPoints")) {
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
			if (capeRegion == null) return 0;
			float h = capeRegion.getRegionHeight();
			float factor = 1.0f - (y / h);
			return Math.max(0, factor);
		}
		
		public String toJson() {
			com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();
			json.setOutputType(com.badlogic.gdx.utils.JsonWriter.OutputType.json);
			
			java.util.HashMap<String, Object> map = new java.util.HashMap<>();
			map.put("offset", offset);
			// 序列化 ControlPoint
			map.put("hullPoints", serializePoints(hullPoints));
			map.put("interiorPoints", serializePoints(interiorPoints));
			map.put("windStrength", windStrength);
			map.put("bigWaveFreq", bigWaveFreq);
			map.put("bigWavePhase", bigWavePhase);
			map.put("bigWaveAmp", bigWaveAmp);
			map.put("smallWaveFreq", smallWaveFreq);
			map.put("smallWavePhase", smallWavePhase);
			map.put("smallWaveAmp", smallWaveAmp);
			map.put("transmissionAmp", transmissionAmp);
			
			return json.prettyPrint(map);
		}

		private Array<Object> serializePoints(Array<ControlPoint> points) {
			Array<Object> arr = new Array<>();
			for(ControlPoint p : points) {
				java.util.HashMap<String, Object> pm = new java.util.HashMap<>();
				pm.put("x", p.position.x);
				pm.put("y", p.position.y);
				pm.put("weight", p.weight);
				arr.add(pm);
			}
			return arr;
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
					falloff = com.badlogic.gdx.math.Interpolation.pow2Out.apply(falloff); // 非线性衰减
					
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

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			Vector2 screenPos = new Vector2(screenX, screenY);
			Vector2 stagePos = uiStage.screenToStageCoordinates(screenPos.cpy());
			com.badlogic.gdx.scenes.scene2d.Actor hitActor = uiStage.hit(stagePos.x, stagePos.y, true);

			if (hitActor != null && hitActor != uiController.gameArea && !hitActor.isDescendantOf(uiController.gameArea)) {
				return false;
			}

			Vector2 stageCoords = uiStage.screenToStageCoordinates(new Vector2(screenX, screenY));
			lastMousePos.set(stageCoords);
			
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
						if (button == com.badlogic.gdx.Input.Buttons.RIGHT) {
							if (isHullPoint) capeState.hullPoints.removeIndex(selectedPointIndex);
							else capeState.interiorPoints.removeIndex(selectedPointIndex);
							capeState.generateMesh();
							selectedPointIndex = -1;
						} else {
							isDraggingPoint = true;
						}
					} else if (currentMode == Mode.WEIGHT) {
						applyWeightBrush(localClick);
						isDraggingPoint = true;
					}
				} else {
					if (currentMode == Mode.MESH && button == com.badlogic.gdx.Input.Buttons.LEFT) {
						if (currentMeshTool == MeshTool.HULL) {
							capeState.hullPoints.add(new ControlPoint(localClick, 0));
						} else {
							capeState.interiorPoints.add(new ControlPoint(localClick, 0));
						}
						capeState.generateMesh();
					} else if (currentMode == Mode.WEIGHT) {
						applyWeightBrush(localClick);
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
				applyWeightBrush(localPos);
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
		
		private void applyWeightBrush(Vector2 localPos) {
			float radius = uiController.brushRadius;
			float target = uiController.brushTargetWeight;
			float strength = uiController.brushStrength;
			
			for (ControlPoint p : capeState.hullPoints) updatePointWeight(p, localPos, radius, target, strength);
			for (ControlPoint p : capeState.interiorPoints) updatePointWeight(p, localPos, radius, target, strength);
			
			// 触发 UI 列表刷新 (可选，可能会有点卡)
			// 为了流畅度，这里不全量刷新列表，只在松手或点击时刷新？
			// 或者，列表显示只在 update() 里做
			if (uiController.highlightedItem != null) {
				// 强制刷新一下选中的项的信息?
				// 暂时留空
			}
		}
		
		private void updatePointWeight(ControlPoint p, Vector2 brushPos, float radius, float target, float strength) {
			float dst = p.position.dst(brushPos);
			if (dst <= radius) {
				float alpha = strength * (1.0f - dst / radius);
				if (alpha < 0) alpha = 0;
				p.weight = p.weight + (target - p.weight) * alpha;
				p.weight = com.badlogic.gdx.math.MathUtils.clamp(p.weight, 0f, 1f);
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
						VisScrollPane sp = new VisScrollPane(createSlidersContent());
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
			
			VisScrollPane scrollPane = new VisScrollPane(pointListTable);
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
			
			VisScrollPane scrollPane = new VisScrollPane(createSlidersContent());
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
		
		private void addSlider(VisTable t, String label, float min, float max, float def, java.util.function.Consumer<Float> onChange) {
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
			if (!screen.capeState.dirty) return;
			screen.capeState.dirty = false;
			
			pointListTable.clear();
			
			float w = screen.capeState.capeRegion.getRegionWidth();
			float h = screen.capeState.capeRegion.getRegionHeight();
			
			// 1. 轮廓点部分
			if (screen.capeState.hullPoints.size > 0) {
				pointListTable.add(new VisLabel("[轮廓点 - 蓝色]")).left().padTop(5).row();
				for (int i = 0; i < screen.capeState.hullPoints.size; i++) {
					ControlPoint p = screen.capeState.hullPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					pointListTable.add(createPointItemWidget(true, i, text)).expandX().fillX().row();
				}
			}
			
			// 2. 内部点部分
			if (screen.capeState.interiorPoints.size > 0) {
				pointListTable.add(new VisLabel("[内部点 - 黄色]")).left().padTop(10).row();
				for (int i = 0; i < screen.capeState.interiorPoints.size; i++) {
					ControlPoint p = screen.capeState.interiorPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f) W:%.2f", i, p.position.x / w, p.position.y / h, p.weight);
					pointListTable.add(createPointItemWidget(false, i, text)).expandX().fillX().row();
				}
			}
			
			if (screen.capeState.hullPoints.size == 0 && screen.capeState.interiorPoints.size == 0) {
				pointListTable.add(new VisLabel("无顶点")).left().row();
			}
		}
		
		private void saveConfig() {
			try {
				com.badlogic.gdx.files.FileHandle file = Gdx.files.local("cape_config.json");
				file.writeString(screen.capeState.toJson(), false);
				Gdx.app.log("Config", "Saved to " + file.file().getAbsolutePath());
			} catch (Exception e) {
				Gdx.app.error("Config", "Save failed", e);
			}
		}
		
		private void loadConfig() {
			try {
				com.badlogic.gdx.files.FileHandle file = Gdx.files.local("cape_config.json");
				if (file.exists()) {
					com.badlogic.gdx.utils.JsonReader reader = new com.badlogic.gdx.utils.JsonReader();
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
		
		private Actor createPointItemWidget(boolean isHull, int index, String text) {
			VisTextButton btn = new VisTextButton(text, "toggle");
			btn.getLabel().setAlignment(com.badlogic.gdx.utils.Align.left);
			
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
