package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.ArrayList;
import java.util.List;

public class TexturePreviewScreen extends GScreen {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private BitmapFont font;
    
    private List<PreviewItem> previews;
    private static final int GRID_SIZE = 128; // Display size for preview (larger than 32 to see details)
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
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        previews = new ArrayList<>();
        int col = 0;
        int row = 0;
        int startX = 50;
        int startY = Gdx.graphics.getHeight() - 150;
        int colsPerRow = 6;

        // 1. Tiles
        addPreview(SpriteGenerator.createFloor(), "Floor", col++, row, startX, startY);
        addPreview(SpriteGenerator.createWall(), "Wall", col++, row, startX, startY);
        addPreview(SpriteGenerator.createDoor(), "Door", col++, row, startX, startY);
        addPreview(SpriteGenerator.createStairs(false), "Stairs Down", col++, row, startX, startY);
        addPreview(SpriteGenerator.createStairs(true), "Stairs Up", col++, row, startX, startY);

        // New Row
        row++; col = 0;
        
        // 2. Player
        addPreview(SpriteGenerator.createPlayer(), "Player", col++, row, startX, startY);
        
        // 3. Monsters
        for (MonsterType type : MonsterType.values()) {
            if (col >= colsPerRow) { col = 0; row++; }
            addPreview(SpriteGenerator.createMonster(type.name), "Mon: " + type.name, col++, row, startX, startY);
        }

        // New Row
        row++; col = 0;

        // 4. Items
        for (ItemData item : ItemData.values()) {
            if (col >= colsPerRow) { col = 0; row++; }
            addPreview(SpriteGenerator.createItem(item.name), item.name, col++, row, startX, startY);
        }

        // Input Processor for Camera
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                float deltaX = -Gdx.input.getDeltaX();
                float deltaY = Gdx.input.getDeltaY();
                camera.translate(deltaX, deltaY);
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom += amountY * 0.1f;
                camera.zoom = Math.max(0.1f, Math.min(camera.zoom, 5f));
                return true;
            }
            
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    // Back to Main Menu logic would go here, or just exit
                    Gdx.app.exit();
                }
                return super.keyDown(keycode);
            }
        });
    }

    private void addPreview(Texture tex, String name, int col, int row, int startX, int startY) {
        int x = startX + col * (GRID_SIZE + PADDING);
        int y = startY - row * (GRID_SIZE + PADDING + 30); // Extra space for text
        previews.add(new PreviewItem(tex, name, x, y));
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        
        batch.begin();
        for (PreviewItem item : previews) {
            batch.draw(item.texture, item.x, item.y, GRID_SIZE, GRID_SIZE);
            font.draw(batch, item.name, item.x, item.y - 10);
        }
        batch.end();
        
        // HUD Batch for Instructions
        batch.setProjectionMatrix(batch.getProjectionMatrix().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        font.draw(batch, "Texture Preview Mode - Drag to Pan, Scroll to Zoom, ESC to Exit", 20, Gdx.graphics.getHeight() - 20);
        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
        for (PreviewItem item : previews) {
            item.texture.dispose();
        }
    }
}
