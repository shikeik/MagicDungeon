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
        projectionMatrix.setToOrtho2D(0, 0, width, height);
        
        TextureRegion region = null;

        try {
            frameBuffer.begin();
            
            // [Fix] 必须手动设置 Viewport，否则默认使用 FBO 全尺寸 (例如 512x512)，导致小尺寸生成 (例如 128x128) 被拉伸到全 FBO，
            // 进而导致 extractTextureRegion 只截取了内容的左下角 (看起来是放大了且偏了)。
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
        // ScreenUtils.getFrameBufferPixmap 读取的是当前绑定的 FB
        // 注意：getFrameBufferPixmap 读取的是 (x, y, w, h)，其中 x,y 是左下角。
        // 因为我们上面设置了 Viewport (0,0,w,h)，所以内容就在 (0,0,w,h) 区域。
        Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, width, height);
        
        // 生成 Texture
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        // 创建 Region 并翻转 Y (因为 OpenGL 纹理坐标 Y 向上，而通常 TextureRegion 期望 Y 向下匹配屏幕/UI)
        TextureRegion region = new TextureRegion(texture);
        region.flip(false, true); 
        
        return region;
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (frameBuffer != null) frameBuffer.dispose();
    }
}
