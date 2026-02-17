package com.goldsprite.magicdungeon.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.magicdungeon.AppConstants;

public class TextureExporter {

	public static void exportToDisk(final Texture texture, final String filename) {
		if (texture == null) return;

		if (AppConstants.getLocalFile("TempTexes/" + filename + ".png").exists()) return; //存在则跳过

		// Use Gdx.app.postRunnable to ensure we are on the OpenGL thread
		Gdx.app.postRunnable(new Runnable() {
			@Override
			public void run() {
				FrameBuffer fbo = null;
				SpriteBatch batch = null;
				Pixmap pixmap = null;
				try {
					int w = texture.getWidth();
					int h = texture.getHeight();

					// 1. Create FrameBuffer
					fbo = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);

					// 2. Begin FBO
					fbo.begin();

					// 3. Clear (Transparent)
					Gdx.gl.glClearColor(0, 0, 0, 0);
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

					// 4. Draw Texture to FBO
					batch = new SpriteBatch();
					// Set projection matrix to match FBO size (0,0 bottom-left)
					batch.getProjectionMatrix().setToOrtho2D(0, 0, w, h);

					batch.begin();
					// batch.draw(texture, x, y, width, height, srcX, srcY, srcWidth, srcHeight, flipX, flipY)
					batch.draw(texture, 0, 0, w, h, 0, 0, w, h, false, true);
					batch.end();

					// 5. Read Pixmap from FBO
					pixmap = ScreenUtils.getFrameBufferPixmap(0, 0, w, h);

					// 6. End FBO
					fbo.end();

					// 7. Save
					FileHandle dir = AppConstants.getLocalFile("TempTexes");
					if (!dir.exists()) dir.mkdirs();
					FileHandle file = dir.child(filename + ".png");

					PixmapIO.writePNG(file, pixmap);
					Gdx.app.log("TextureExporter", "Exported texture via FBO to: " + file.file().getAbsolutePath());

				} catch (Exception e) {
					Gdx.app.error("TextureExporter", "Failed to export texture: " + filename, e);
				} finally {
					if (batch != null) batch.dispose();
					if (fbo != null) fbo.dispose();
					if (pixmap != null) pixmap.dispose();
				}
			}
		});
	}
}
