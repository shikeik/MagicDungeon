package com.goldsprite.magicdungeon.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.goldsprite.magicdungeon.assets.TextureManager;

public class TexturePreviewScreen extends GScreen {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private Viewport viewport;
    private BitmapFont font;

    private List<PreviewItem> previews;
    private static final int GRID_SIZE = 128;
    private static final int PADDING = 20;

    private static class PreviewItem {
        Texture texture;
        String name;
        int x, y;

        public PreviewItem(Texture texture, String name, int x, int y) {
            this.texture = texture;
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    @Override
    public void create() {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        viewport = new ExtendViewport(1280, 720, camera); // Use ExtendViewport to prevent stretching
        viewport.apply(true);

        font = new BitmapFont();
        font.setColor(Color.WHITE);

        previews = new ArrayList<>();
        int col = 0;
        int row = 0;
        int startX = 50;
        int startY = 0; // Will be set in resize or dynamic
        // Let's keep fixed layout but center camera initially

        // Use TextureManager
        TextureManager tm = TextureManager.getInstance();

        // 1. Tiles
        addPreview(tm.get("FLOOR"), "Floor", col++, row);
        addPreview(tm.get("WALL"), "Wall", col++, row);
        addPreview(tm.get("DOOR"), "Door", col++, row);
        addPreview(tm.get("STAIRS_DOWN"), "Stairs Down", col++, row);
        addPreview(tm.get("STAIRS_UP"), "Stairs Up", col++, row);

        // New Row
        row++; col = 0;

        // 2. Player
        addPreview(tm.get("PLAYER"), "Player", col++, row);

        // 3. Monsters
        for (MonsterType type : MonsterType.values()) {
            if (col >= 6) { col = 0; row++; }
            addPreview(tm.get(type.name()), "Mon: " + type.name(), col++, row);
        }

        // New Row
        row++; col = 0;

        // 4. Items
        for (ItemData item : ItemData.values()) {
            if (col >= 6) { col = 0; row++; }
            addPreview(tm.get(item.name()), item.name(), col++, row);
        }

        // Camera Controller
        SimpleCameraController controller = new SimpleCameraController(camera);
        controller.setCoordinateMapper((x, y) -> viewport.unproject(new com.badlogic.gdx.math.Vector2(x, y)));
        getImp().addProcessor(controller);
    }

    private void addPreview(Texture tex, String name, int col, int row) {
        // Calculate position based on grid
        int x = 50 + col * (GRID_SIZE + PADDING);
        int y = -50 - row * (GRID_SIZE + PADDING + 30); // Grow downwards
        previews.add(new PreviewItem(tex, name, x, y));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

		viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        for (PreviewItem item : previews) {
            batch.draw(item.texture, item.x, item.y, GRID_SIZE, GRID_SIZE);
            font.draw(batch, item.name, item.x, item.y - 10);
        }
        batch.end();

        // HUD
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        font.draw(batch, "Texture Preview Mode - Drag to Pan, Scroll to Zoom, ESC to Exit", 20, Gdx.graphics.getHeight() - 20);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        // Reset camera position if needed or keep user position
    }

    // ... (dispose)
}
