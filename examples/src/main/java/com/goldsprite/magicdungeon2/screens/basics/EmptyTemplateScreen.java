package com.goldsprite.magicdungeon2.screens.basics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.utils.SimpleCameraController;

public class EmptyTemplateScreen extends GScreen {
	private NeonBatch batch;
	private OrthographicCamera camera;
	private Viewport viewport;
	private BitmapFont font;

	@Override
	public void create() {
		batch = new NeonBatch();
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(1280, 720, camera); // Use ExtendViewport to prevent stretching
		viewport.apply(true);

		font = FontUtils.generate(14, 3); // fontSize, clarity
		font.setColor(Color.WHITE);

		// Camera Controller: 安卓/pc端交互适配的相机移动缩放控制查看器
		SimpleCameraController controller = new SimpleCameraController(camera);
		controller.setCoordinateMapper((x, y) -> viewport.unproject(new Vector2(x, y)));
		getImp().addProcessor(controller);
	}

	@Override
	public void render(float delta) {
		ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

		viewport.apply();
		camera.update();
		batch.setProjectionMatrix(camera.combined);

		// HUD
		batch.setProjectionMatrix(
				batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		batch.begin();
		font.draw(batch, "Texture Preview Mode - Drag to Pan, Scroll to Zoom, ESC to Exit", 20,
				Gdx.graphics.getHeight() - 20);
		batch.end();
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		// Reset camera position if needed or keep user position
	}

	// ... (dispose)
}

