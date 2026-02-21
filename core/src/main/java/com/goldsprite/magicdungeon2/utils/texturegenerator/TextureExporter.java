package com.goldsprite.magicdungeon2.utils.texturegenerator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.magicdungeon2.AppConstants;

/**
 * 纹理导出工具
 * 将 Texture 通过 FBO 读回像素并保存为 PNG 文件
 */
public class TextureExporter {

    /**
     * 将纹理导出到本地磁盘
     * @param texture 要导出的纹理
     * @param filename 文件名（不含 .png 后缀）
     */
    public static void exportToDisk(final Texture texture, final String filename) {
        if (texture == null) return;

        // 已存在则跳过
        if (AppConstants.getLocalFile("TempTexes/" + filename + ".png").exists()) return;

        // 确保在 OpenGL 线程执行
        Gdx.app.postRunnable(() -> {
            FrameBuffer fbo = null;
            SpriteBatch batch = null;
            Pixmap pixmap = null;
            try {
                int w = texture.getWidth();
                int h = texture.getHeight();

                fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
                fbo.begin();

                Gdx.gl.glClearColor(0, 0, 0, 0);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                batch = new SpriteBatch();
                batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
                batch.begin();
                batch.draw(texture, 0, 0, w, h, 0, 0, w, h, false, true);
                batch.end();

                pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);
                fbo.end();

                FileHandle dir = AppConstants.getLocalFile("TempTexes");
                if (!dir.exists()) dir.mkdirs();
                FileHandle file = dir.child(filename + ".png");
                PixmapIO.writePNG(file, pixmap);

                Gdx.app.log("TextureExporter", "已导出: " + file.file().getAbsolutePath());
            } catch (Exception e) {
                Gdx.app.error("TextureExporter", "导出失败: " + filename, e);
            } finally {
                if (batch != null) batch.dispose();
                if (fbo != null) fbo.dispose();
                if (pixmap != null) pixmap.dispose();
            }
        });
    }
}
