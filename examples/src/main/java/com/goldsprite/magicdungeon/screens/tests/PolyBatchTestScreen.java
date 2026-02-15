package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.PolygonRegion;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.log.Debug;

public class PolyBatchTestScreen extends GScreen {
	// 渲染工具
	private SpriteBatch spriteBatch;
	private PolygonSpriteBatch polyBatch;
	private ShapeRenderer shapeRenderer;

	// 素材
	private Texture knightTex, capeTex;
	private TextureRegion knightRegion, capeRegion;
	private PolygonRegion polyRegion;

	// 交互数据
	private Array<Vector2> contourPoints = new Array<>();
	private Array<Vector2> internalPoints = new Array<>();
	private boolean isContourFinished = false;

	// 动态网格数据
	private float[] originalVertices; // 原始UV对应的位置
	private float[] animatedVertices; // 实时计算的位置
	private short[] triangles;
	private float stateTime = 0;

	// 调节参数 (对应UI区域)
	private float amplitude = 20f; // 摆动幅度
	private float frequency = 5f; // 频率
	private float waveSpeed = 3f; // 波速

	@Override
	public void create() {
		spriteBatch = new SpriteBatch();
		polyBatch = new PolygonSpriteBatch();
		shapeRenderer = new ShapeRenderer();

		// 路径请确保正确：assets/packs/PolyBatchTest/
		knightTex = new Texture("packs/PolyBatchTest/Knight.png");
		capeTex = new Texture("packs/PolyBatchTest/Cape.png");
		knightRegion = new TextureRegion(knightTex);
		capeRegion = new TextureRegion(capeTex);

		setupInput();
	}

	private void setupInput() {
		Gdx.input.setInputProcessor(new InputAdapter() {
			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				float y = Gdx.graphics.getHeight() - screenY; // 坐标转换
				Vector2 p = new Vector2(screenX, y);

				if (!isContourFinished) {
					contourPoints.add(p);
				} else {
					internalPoints.add(p);
					generateMesh(); // 每次点内部点都重新生成网格
				}
				return true;
			}

			@Override
			public boolean keyDown(int keycode) {
				Debug.log("keycode: %s", keycode);
				if (!isContourFinished && contourPoints.size > 2) {
					isContourFinished = true; // 按任意键结束轮廓标注
					generateMesh();
				}
				return true;
			}
		});
	}

	private void generateMesh() {
		// 1. 合并所有点
		FloatArray allPoints = new FloatArray();
		for (Vector2 v : contourPoints) {
			allPoints.addAll(v.x, v.y);
		}
		for (Vector2 v : internalPoints) {
			allPoints.addAll(v.x, v.y);
		}

		// 2. 三角剖分 (Delaunay 算法)
		DelaunayTriangulator triangulator = new DelaunayTriangulator();
		triangles = triangulator.computeTriangles(allPoints, false).toArray();

		// 3. 构建 PolygonRegion
		// 注意：这里的坐标需要转换为相对于贴图的 0-1 坐标或像素坐标
		originalVertices = allPoints.toArray();
		animatedVertices = new float[originalVertices.length];

		polyRegion = new PolygonRegion(capeRegion, originalVertices, triangles);
	}

	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stateTime += Gdx.graphics.getDeltaTime();

		// --- 区域 1 & 3：绘制骑士和披风 ---
		spriteBatch.begin();
		spriteBatch.draw(knightRegion, 100, 100); // 绘制骑士
		spriteBatch.end();

		if (polyRegion != null) {
			updateCapeAnimation();
			polyBatch.begin();
			// 直接传入更新后的顶点进行绘制
			polyBatch.draw(polyRegion, 0, 0);
			polyBatch.end();
		}

		// --- 区域 2：绘制标注线和控制点 ---
		drawDebugInfo();
	}

	private void updateCapeAnimation() {
		float minY = Float.MAX_VALUE;
		float maxY = Float.MIN_VALUE;
		for (int i = 1; i < originalVertices.length; i += 2) {
			minY = Math.min(minY, originalVertices[i]);
			maxY = Math.max(maxY, originalVertices[i]);
		}

		float totalHeight = maxY - minY;

		for (int i = 0; i < originalVertices.length; i += 2) {
			float x = originalVertices[i];
			float y = originalVertices[i + 1];

			// 渐进权重计算：顶部(maxY)权重为0，底部权重为1
			float weight = (maxY - y) / totalHeight;

			// X方向正弦波
			float offsetX = (float) Math.sin(stateTime * waveSpeed + y * 0.05f) * amplitude * weight;

			animatedVertices[i] = x + offsetX;
			animatedVertices[i + 1] = y;
		}

		// 将计算好的顶点存回 polyRegion 进行渲染
		System.arraycopy(animatedVertices, 0, polyRegion.getVertices(), 0, animatedVertices.length);
	}

	private void drawDebugInfo() {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(Color.CYAN);
		// 绘制轮廓
		for (int i = 0; i < contourPoints.size; i++) {
			Vector2 p1 = contourPoints.get(i);
			Vector2 p2 = contourPoints.get((i + 1) % contourPoints.size);
			shapeRenderer.line(p1, p2);
		}
		shapeRenderer.setColor(Color.YELLOW);
		for (Vector2 p : internalPoints)
			shapeRenderer.circle(p.x, p.y, 3);
		shapeRenderer.end();
	}
}

