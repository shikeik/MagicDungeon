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
	public enum MeshTool { HULL, INTERIOR } // 子模式：轮廓/内部点
	private Mode currentMode = Mode.ALIGN;
	private MeshTool currentMeshTool = MeshTool.HULL;
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
		// InputMultiplexer multiplexer = new InputMultiplexer(uiStage, new EditorInputHandler());
		// Gdx.input.setInputProcessor(multiplexer);
		
		// 使用基类 GScreen 的 imp (InputMultiplexer)
		imp.addProcessor(uiStage);
		imp.addProcessor(new EditorInputHandler());
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
		// 修正：offset 应理解为披风左下角的偏移量，而非中心点偏移
		// 骑士绘制在 (100, 100)，披风如果也要在骑士背后，
		// 它的左下角起始点应该是 (100 + offset.x, 100 + offset.y)
		// 之前的 drawX = cx - w/2 导致披风被居中到了 (100, 100)，所以偏左下了
		
		float drawX = 100 + capeState.offset.x;
		float drawY = 100 + capeState.offset.y;

		switch (currentMode) {
			case ALIGN:
				batch.setProjectionMatrix(worldCamera.combined);
				batch.begin();
				batch.draw(capeState.capeRegion, drawX, drawY);
				batch.end();
				drawMeshDebug(drawX, drawY);
				break;
			case MESH:
				batch.setProjectionMatrix(worldCamera.combined);
				batch.begin();
				batch.draw(capeState.capeRegion, drawX, drawY);
				batch.end();
				drawMeshDebug(drawX, drawY);
				break;
			case STATIC_TEST:
			case DYNAMIC_WAVE:
				if (capeState.polyRegion != null) {
					if (currentMode == Mode.DYNAMIC_WAVE) capeState.updateAnimation(delta);
					polyBatch.setProjectionMatrix(worldCamera.combined);
					polyBatch.begin();
					polyBatch.draw(capeState.polyRegion, drawX, drawY);
					polyBatch.end();
					drawMeshDebug(drawX, drawY);
				}
				break;
		}
	}

	private void drawMeshDebug(float ox, float oy) {
		shapes.setProjectionMatrix(worldCamera.combined);
		
		// 1. 绘制纹理边框 (红色矩形)
		if (showTextureBounds) {
			shapes.begin(ShapeRenderer.ShapeType.Line);
			shapes.setColor(Color.RED);
			float w = capeState.capeRegion.getRegionWidth();
			float h = capeState.capeRegion.getRegionHeight();
			shapes.rect(ox, oy, w, h);
			shapes.end();
		}

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
		
		// 1. 绘制轮廓点 (蓝色)
		shapes.setColor(Color.BLUE);
		for(int i=0; i<capeState.hullPoints.size; i++) {
			Vector2 p = capeState.hullPoints.get(i);
			shapes.circle(p.x + ox, p.y + oy, 4);
			
			// 绘制轮廓连线
			if (i > 0) {
				Vector2 prev = capeState.hullPoints.get(i-1);
				shapes.rectLine(prev.x+ox, prev.y+oy, p.x+ox, p.y+oy, 2);
			}
		}
		// 闭合轮廓线
		if (capeState.hullPoints.size > 2) {
			Vector2 first = capeState.hullPoints.first();
			Vector2 last = capeState.hullPoints.peek();
			shapes.rectLine(last.x+ox, last.y+oy, first.x+ox, first.y+oy, 2);
		}

		// 2. 绘制内部点 (黄色)
		shapes.setColor(Color.YELLOW);
		for(Vector2 p : capeState.interiorPoints) {
			shapes.circle(p.x + ox, p.y + oy, 3);
		}
		
		// 3. 绘制列表选中的高亮提示 (绿色大圆圈)
		UIController.PointItem item = uiController.getHighlightedItem();
		if (item != null) {
			shapes.setColor(Color.GREEN);
			Vector2 p = null;
			if (item.isHull && item.index < capeState.hullPoints.size) {
				p = capeState.hullPoints.get(item.index);
			} else if (!item.isHull && item.index < capeState.interiorPoints.size) {
				p = capeState.interiorPoints.get(item.index);
			}
			
			if (p != null) {
				shapes.circle(p.x + ox, p.y + oy, 6); // 更大的圆
				shapes.rect(p.x + ox - 8, p.y + oy - 8, 16, 16); // 方框
			}
		}
		
		shapes.end();
	}

	// --- 内部数据类 ---
	static class CapeState {
		public TextureRegion knightRegion, capeRegion;
		public Vector2 offset = new Vector2(0, 0);
		public boolean dirty = true; // 数据变更标记
		
		// 1. 数据结构拆分
		public Array<Vector2> hullPoints = new Array<>(); // 轮廓点 (有序)
		public Array<Vector2> interiorPoints = new Array<>(); // 内部点 (无序)
		
		// 所有的点 (hull + interior)，用于构建网格顶点数组
		// 注意：PolygonRegion 的 vertices 数组必须和这个列表的顺序一致
		private Array<Vector2> allPoints = new Array<>(); 
		
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

		public void initTextures(String kPath, String cPath) {
			knightRegion = new TextureRegion(new Texture(kPath));
			capeRegion = new TextureRegion(new Texture(cPath));
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
			for (Vector2 v : allPoints) fa.addAll(v.x, v.y);
			
			originalVertices = fa.toArray();
			animatedVertices = fa.toArray();

			try {
				// 2. 使用 Delaunay 生成全凸包网格
				ShortArray allTriangles = new DelaunayTriangulator().computeTriangles(fa, false);
				
				// 3. 剔除重心在轮廓外部的三角形 (实现凹包)
				// 准备轮廓多边形数据用于检测
				float[] hullVerts = new float[hullPoints.size * 2];
				for(int i=0; i<hullPoints.size; i++) {
					hullVerts[i*2] = hullPoints.get(i).x;
					hullVerts[i*2+1] = hullPoints.get(i).y;
				}
				
				ShortArray validTriangles = new ShortArray();
				for (int i = 0; i < allTriangles.size; i += 3) {
					int p1 = allTriangles.get(i) * 2;
					int p2 = allTriangles.get(i+1) * 2;
					int p3 = allTriangles.get(i+2) * 2;
					
					// 计算三角形重心
					float cx = (originalVertices[p1] + originalVertices[p2] + originalVertices[p3]) / 3f;
					float cy = (originalVertices[p1+1] + originalVertices[p2+1] + originalVertices[p3+1]) / 3f;
					
					// 判断重心是否在轮廓内
					if (Intersector.isPointInPolygon(hullPoints, new Vector2(cx, cy))) {
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
			
			float h = capeRegion.getRegionHeight();

			for (int i = 0; i < originalVertices.length; i += 2) {
				float oldX = originalVertices[i];
				float oldY = originalVertices[i + 1];

				// 1. 权重计算：从顶部往下逐级增加
				// 假设 Y=h 是顶部，Y=0 是底部 (LibGDX 默认坐标系)
				// 权重：0.0 (顶) -> 1.0 (底)
				float factor = 1.0f - (oldY / h);
				if (factor < 0) factor = 0; 
				
				// 2. 披风摆动：大风摆动 + 细节颤动 (修改 X)
				// 模拟风从右边吹向左边 -> 整体向左偏 (windBias) + 波浪 (wave)
				
				// A. 基础持续风力 (让披风整体往左飘，不仅仅是摆动)
				float baseWind = windStrength * factor;
				
				// B. 大摆动 (低频、大幅度、相位差小) -> 模拟风的主体方向变化
				// 频率 2f，相位差 0.02f (波长长)，幅度 20f
				float bigWave = (float) Math.sin(stateTime * bigWaveFreq - oldY * bigWavePhase) * bigWaveAmp * factor;
				
				// C. 小摆动 (高频、小幅度、相位差大) -> 模拟布料褶皱和湍流
				// 频率 8f，相位差 0.1f (波长短)，幅度 4f
				float smallRipple = (float) Math.sin(stateTime * smallWaveFreq - oldY * smallWavePhase) * smallWaveAmp * factor;
				
				// 叠加所有效果
				// 注意：bigWave 的正值可能会抵消 baseWind，导致披风偶尔回到右边，
				// 如果希望一直吹向左边，可以调整 baseWind 的大小大于 bigWave 的幅度。
				float finalOffset = baseWind + bigWave + smallRipple;

				animatedVertices[i] = oldX + finalOffset;
				animatedVertices[i + 1] = oldY; // Y 轴保持稳定
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
		private boolean isHullPoint = false; // 选中的是轮廓点还是内部点
		private Vector2 lastMousePos = new Vector2();

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			// 如果点击在 UI 上，则拦截输入，不进行场景操作
			Vector2 screenPos = new Vector2(screenX, screenY);
			Vector2 stagePos = uiStage.screenToStageCoordinates(screenPos.cpy());
			// 使用 hit 检测，并且确保命中的不是 gameArea (游戏操作区)
			com.badlogic.gdx.scenes.scene2d.Actor hitActor = uiStage.hit(stagePos.x, stagePos.y, true);
			if (hitActor != null && hitActor != uiController.gameArea) {
				return false;
			}

			Vector2 worldPos = screenToWorldCoord(screenX, screenY);
			lastMousePos.set(worldPos);

			// 获取相对于披风左下角的局部坐标
			// 披风渲染位置是 (100 + offset.x, 100 + offset.y)
			float baseX = 100 + capeState.offset.x;
			float baseY = 100 + capeState.offset.y;
			
			Vector2 localClick = new Vector2(worldPos).sub(baseX, baseY);

			if (currentMode == Mode.MESH) {
				if (currentMeshTool == MeshTool.HULL) {
					// 轮廓模式：直接添加轮廓点
					capeState.hullPoints.add(localClick);
				} else {
					// 内部点模式：添加内部控制点
					capeState.interiorPoints.add(localClick);
				}
				capeState.generateMesh();
			}
			else if (currentMode == Mode.STATIC_TEST) {
				// 模式3：寻找最近的顶点进行拾取 (搜索轮廓和内部点)
				selectedPointIndex = -1;
				float minDst = 20f * worldCamera.zoom; // 拾取半径随缩放调整
				
				// 1. 搜索轮廓点
				for (int i = 0; i < capeState.hullPoints.size; i++) {
					float dst = capeState.hullPoints.get(i).dst(localClick);
					if (dst < minDst) {
						minDst = dst;
						selectedPointIndex = i;
						isHullPoint = true;
					}
				}
				
				// 2. 搜索内部点 (如果有更近的，覆盖)
				for (int i = 0; i < capeState.interiorPoints.size; i++) {
					float dst = capeState.interiorPoints.get(i).dst(localClick);
					if (dst < minDst) {
						minDst = dst;
						selectedPointIndex = i;
						isHullPoint = false;
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
				Vector2 p = isHullPoint ? capeState.hullPoints.get(selectedPointIndex) : capeState.interiorPoints.get(selectedPointIndex);
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
		private final VisTable pointListTable; // 替换 VisList 为 Table 容器
		private final VisCheckBox boundsCheck;
		public final VisTable gameArea;
		
		// 当前高亮的点信息
		private PointItem highlightedItem = null;
		
		private static class PointItem {
			boolean isHull;
			int index;
			
			PointItem(boolean isHull, int index) {
				this.isHull = isHull;
				this.index = index;
			}
		}

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
			// 使用 ChangeListener 替代通用 Listener，仅在值实际改变时触发
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
			
			// 清空按钮
			VisTextButton btnClear = new VisTextButton("清空网格");
			btnClear.addListener(new ChangeListener() {
				@Override public void changed(ChangeEvent event, Actor actor) {
					screen.capeState.clear();
				}
			});
			
			panel.add(toolTable).colspan(2).expandX().fillX().padTop(5).row();
			panel.add(btnClear).colspan(2).fillX().padTop(5).row();

			// 调试选项
			boundsCheck = new VisCheckBox("显示纹理边框", true);
			boundsCheck.addListener(event -> {
				screen.showTextureBounds = boundsCheck.isChecked();
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
			VisLabel tip = new VisLabel("操作说明:\nAlign: 拖动调整位置\nMesh: 点击添加点\nStatic: 拖动点\nDynamic: 观看演示");
			tip.setWrap(true);
			panel.add(tip).colspan(2).width(200).padTop(10).row();
			panel.pack(); 

			// 左侧作为游戏操作区，设置为可触摸，以便在 InputHandler 中识别
			gameArea = new VisTable();
			gameArea.setTouchable(Touchable.enabled);

			// 使用 VisSplitPane 实现左右分栏
			// 布局结构：[游戏区] | [参数面板] | [控制面板]
			// 为了实现三栏，我们需要嵌套 SplitPane
			// SplitPane1 = [参数面板] | [控制面板]
			// SplitPane2 = [游戏区] | [SplitPane1]
			
			// 1. 参数面板 (左边那个侧边栏)
			VisTable paramPanel = createParamPanel();
			
			// 2. 右侧组合栏 (参数 + 控制)
			VisSplitPane rightSideSplit = new VisSplitPane(paramPanel, panel, false);
			rightSideSplit.setSplitAmount(0.5f); // 均分
			rightSideSplit.setMinSplitAmount(0.1f);
			rightSideSplit.setMaxSplitAmount(0.9f);

			// 3. 主布局 (游戏区 + 右侧组合栏)
			VisSplitPane mainSplit = new VisSplitPane(gameArea, rightSideSplit, false);
			
			// 计算初始分割比例
			float totalWidth = stage.getWidth();
			float rightSidePrefWidth = panel.getPrefWidth() * 2 + 40; // 估算宽度
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
			table.setBackground("window");
			table.add(new VisLabel("波动参数调节")).pad(10).row();
			
			VisScrollPane scrollPane = new VisScrollPane(createSlidersContent());
			scrollPane.setFadeScrollBars(false);
			scrollPane.setScrollingDisabled(true, false);
			
			table.add(scrollPane).expand().fill().row();
			return table;
		}
		
		private VisTable createSlidersContent() {
			VisTable t = new VisTable();
			t.defaults().left().expandX().fillX().pad(2);
			
			addSlider(t, "基础风力", -50, 50, screen.capeState.windStrength, v -> screen.capeState.windStrength = v);
			
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
					Vector2 p = screen.capeState.hullPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f)", i, p.x / w, p.y / h);
					pointListTable.add(createPointItemWidget(true, i, text)).expandX().fillX().row();
				}
			}
			
			// 2. 内部点部分
			if (screen.capeState.interiorPoints.size > 0) {
				pointListTable.add(new VisLabel("[内部点 - 黄色]")).left().padTop(10).row();
				for (int i = 0; i < screen.capeState.interiorPoints.size; i++) {
					Vector2 p = screen.capeState.interiorPoints.get(i);
					String text = String.format("%d: UV(%.2f, %.2f)", i, p.x / w, p.y / h);
					pointListTable.add(createPointItemWidget(false, i, text)).expandX().fillX().row();
				}
			}
			
			if (screen.capeState.hullPoints.size == 0 && screen.capeState.interiorPoints.size == 0) {
				pointListTable.add(new VisLabel("无顶点")).left().row();
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
