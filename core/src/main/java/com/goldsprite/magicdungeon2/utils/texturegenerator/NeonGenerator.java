package com.goldsprite.magicdungeon2.utils.texturegenerator;

import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * Neon 风格纹理生成器
 * 将 NeonBatch 矢量绘制烘焙为 TextureRegion
 */
public class NeonGenerator {
	private static NeonGenerator instance;
	private NeonBatch batch;
	private FrameBuffer frameBuffer;
	private Matrix4 projectionMatrix = new Matrix4();
	private int currentBufferSize = 512;

	private NeonGenerator() {
		batch = new NeonBatch();
		resizeBuffer(currentBufferSize);
	}

	public static NeonGenerator getInstance() {
		if (instance == null) instance = new NeonGenerator();
		return instance;
	}

	private void resizeBuffer(int size) {
		if (frameBuffer != null) frameBuffer.dispose();
		try {
			frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, size, size, false);
			currentBufferSize = size;
		} catch (Exception e) {
			DLog.logErr("NeonGenerator", "无法创建 FrameBuffer, 大小: " + size);
		}
	}

	/**
	 * 核心生成方法
	 * @param width 纹理宽度
	 * @param height 纹理高度
	 * @param drawer 绘制逻辑回调 (坐标 0~1)
	 * @return 生成的纹理区域
	 */
	public TextureRegion generate(int width, int height, Consumer<NeonBatch> drawer) {
		int maxSize = Math.max(width, height);
		if (maxSize > currentBufferSize) {
			resizeBuffer(Math.max(maxSize, currentBufferSize * 2));
		}
		if (frameBuffer == null) return null;

		projectionMatrix.setToOrtho2D(0, 0, 1, 1);
		TextureRegion region = null;

		try {
			frameBuffer.begin();
			Gdx.gl.glViewport(0, 0, width, height);
			ScreenUtils.clear(0, 0, 0, 0);

			batch.setProjectionMatrix(projectionMatrix);
			batch.begin();
			drawer.accept(batch);
			batch.end();

			region = extractTextureRegion(width, height);
		} catch (Exception e) {
			DLog.logErr("NeonGenerator", "生成出错: " + e.getMessage());
			if (batch.isDrawing()) batch.end();
		} finally {
			frameBuffer.end();
		}
		return region;
	}

	private TextureRegion extractTextureRegion(int width, int height) {
		Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
		pixmap.setFilter(Pixmap.Filter.NearestNeighbour);
		Texture texture = new Texture(pixmap);
		pixmap.dispose();
		TextureRegion region = new TextureRegion(texture);
		region.flip(false, true);
		return region;
	}

	public void dispose() {
		if (batch != null) batch.dispose();
		if (frameBuffer != null) frameBuffer.dispose();
	}
}
