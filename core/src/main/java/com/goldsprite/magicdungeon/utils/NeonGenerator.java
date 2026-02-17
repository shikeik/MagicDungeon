package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.log.DLog;
import java.util.function.Consumer;

/**
 * Neon 风格纹理生成器
 * 职责：
 * 1. 提供基于 NeonBatch 的矢量绘图环境
 * 2. 将矢量绘制结果烘焙 (Bake) 为 TextureRegion
 * 3. 替代原有的 SpriteGenerator (像素操作)
 */
public class NeonGenerator {
    private static NeonGenerator instance;
    private NeonBatch batch;
    private FrameBuffer frameBuffer;
    private Matrix4 projectionMatrix = new Matrix4();

    // 默认生成尺寸，如果不够大可以动态调整
    private int currentBufferSize = 512;

    private NeonGenerator() {
        batch = new NeonBatch();
        resizeBuffer(currentBufferSize);
    }

    public static NeonGenerator getInstance() {
        if (instance == null) {
            instance = new NeonGenerator();
        }
        return instance;
    }

    private void resizeBuffer(int size) {
        if (frameBuffer != null) frameBuffer.dispose();
        try {
            frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, size, size, false);
            currentBufferSize = size;
        } catch (Exception e) {
            DLog.logErr("NeonGenerator", "Failed to create FrameBuffer of size " + size);
        }
    }

    /**
     * 核心生成方法
     * @param width 生成纹理宽度
     * @param height 生成纹理高度
     * @param drawer 绘制逻辑回调
     * @return 生成的纹理区域 (翻转了Y轴，可以直接绘制)
     */
    public TextureRegion generate(int width, int height, Consumer<NeonBatch> drawer) {
        // 确保 Buffer 够大
        int maxSize = Math.max(width, height);
        if (maxSize > currentBufferSize) {
            resizeBuffer(Math.max(maxSize, currentBufferSize * 2));
        }

        if (frameBuffer == null) return null;

        // Setup Projection
        // 使用标准化正交投影 (0~1)。
        // 原点 (0,0) 在左下角，(1,1) 在右上角。
        // 这意味着所有绘制指令都应该使用 0.0~1.0 的归一化坐标。
        projectionMatrix.setToOrtho2D(0, 0, 1, 1);

        TextureRegion region = null;

        try {
            frameBuffer.begin();

            // [Fix] 设置 Viewport 为 FBO 的实际大小，确保投影正确映射到纹理像素
            Gdx.gl.glViewport(0, 0, width, height);

            // Clear with transparent
            ScreenUtils.clear(0, 0, 0, 0);

            batch.setProjectionMatrix(projectionMatrix);
            batch.begin();
            drawer.accept(batch);
            batch.end();

            // Extract BEFORE ending FrameBuffer
            region = extractTextureRegion(width, height);

        } catch (Exception e) {
            DLog.logErr("NeonGenerator", "Error during generation: " + e.getMessage());
            e.printStackTrace();
            if (batch.isDrawing()) batch.end();
        } finally {
            frameBuffer.end();
        }

        return region;
    }

    /**
     * 从 FrameBuffer 中提取指定区域为新的 Texture
     */
    private TextureRegion extractTextureRegion(int width, int height) {
        // 方法 A: 读取像素生成新 Texture (CPU heavy, but safe)
        // ScreenUtils.getFrameBufferPixmap 读取的是当前绑定的 FB (左下角原点)
        Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
		pixmap.setFilter(Pixmap.Filter.NearestNeighbour);
        // 生成 Texture
        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        // 创建 Region
		TextureRegion region = new TextureRegion(texture);
        // 由于 createFromFrameBuffer 读取的像素数据是 Y-down (第0行是FBO底部)，
        // 而 Texture 默认 V=0 对应数据头。
        // SpriteBatch 绘制时，V=0 对应矩形顶部。
        // 所以必须翻转 Y 轴，让 V=0 对应数据尾 (FBO顶部)，
        // 这样 SpriteBatch 绘制矩形顶部时，采样到的才是 FBO 的顶部内容。
        region.flip(false, true);

        return region;
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
    }
}
