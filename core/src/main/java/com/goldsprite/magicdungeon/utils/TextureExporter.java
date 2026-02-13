package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;

public class TextureExporter {

    public static void exportToDisk(final Texture texture, final String filename) {
        if (texture == null) return;

        // Use Gdx.app.postRunnable to ensure we are on the OpenGL thread
        Gdx.app.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    int w = texture.getWidth();
                    int h = texture.getHeight();

                    // 1. Create FrameBuffer
                    FrameBuffer fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);

                    // 2. Begin FBO
                    fbo.begin();

                    // 3. Clear (Transparent)
                    Gdx.gl.glClearColor(0, 0, 0, 0);
                    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                    // 4. Draw Texture to FBO
                    // Note: FrameBuffer coordinates are usually y-up, same as batch.
                    // But ScreenUtils.getFrameBufferPixmap reads from bottom-up or top-down depending on system?
                    // Usually batch.draw(tex, 0, 0) draws at bottom-left.
                    // But texture coordinates in FBO might be flipped when read back?
                    // Let's draw it normally first.
                    
                    SpriteBatch batch = new SpriteBatch();
                    // Set projection matrix to match FBO size (0,0 bottom-left)
                    batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);
                    
                    batch.begin();
                    // Draw texture flipping Y because FrameBuffer textures are often flipped in memory when read back?
                    // Actually, if we draw it normally (0,0), it appears at bottom-left.
                    // ScreenUtils.getFrameBufferPixmap reads pixels.
                    // If result is flipped, we can flip pixmap.
                    // The user reported images are upside down. So we need to flip Y.
                    // batch.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
                    batch.draw(texture, 0, 0, w, h, 0, 0, w, h, false, true);
                    batch.end();

                    // 5. Read Pixmap from FBO
                    // ScreenUtils.getFrameBufferPixmap gets pixels from current bound framebuffer
                    Pixmap pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);

                    // 6. End FBO
                    fbo.end();
                    
                    // 7. Save
                    FileHandle dir = Gdx.files.local("MagicDungeon/TempTexes");
                    if (!dir.exists()) dir.mkdirs();
                    FileHandle file = dir.child(filename + ".png");
                    
                    PixmapIO.writePNG(file, pixmap);
                    Gdx.app.log("TextureExporter", "Exported texture via FBO to: " + file.file().getAbsolutePath());

                    // Cleanup
                    batch.dispose();
                    fbo.dispose();
                    pixmap.dispose();

                } catch (Exception e) {
                    Gdx.app.error("TextureExporter", "Failed to export texture: " + filename, e);
                }
            }
        });
    }
}
