package com.goldsprite.magicdungeon.progress;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.magicdungeon.assets.TextureManager;
import java.util.Collection;

public class ProgressScreen extends GScreen {
    private OrthographicCamera camera;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private BitmapFont font;
    private ProgressManager manager;
    
    // Config
    private static final float NODE_RADIUS = 30f;
    private static final Color COL_LOCKED = Color.GRAY;
    private static final Color COL_AVAILABLE = Color.YELLOW;
    private static final Color COL_UNLOCKED = Color.CYAN;
    private static final Color COL_LINE = Color.WHITE;

    private boolean isDragging = false;
    private float lastTouchX, lastTouchY;

    public ProgressScreen() {
        this.manager = ProgressManager.getInstance();
    }

    @Override
    public void create() {
        super.create();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0, 100, 0); 
        camera.update();

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        font = new BitmapFont(); // Default font
    }

    @Override
    public void show() {
        super.show();
        // Override input processor
        getImp().addProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.P) {
                    getScreenManager().popLastScreen();
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                Vector3 worldPos = camera.unproject(new Vector3(screenX, screenY, 0));
                
                // Check click on nodes
                for (ProgressNode node : manager.getAllNodes()) {
                    if (Vector2.dst(worldPos.x, worldPos.y, node.x, node.y) < NODE_RADIUS) {
                        handleNodeClick(node);
                        return true;
                    }
                }
                
                isDragging = true;
                lastTouchX = screenX;
                lastTouchY = screenY;
                return true;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                isDragging = false;
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (isDragging) {
                    float dx = screenX - lastTouchX;
                    float dy = screenY - lastTouchY; 
                    camera.translate(-dx * camera.zoom, dy * camera.zoom); 
                    camera.update();
                    lastTouchX = screenX;
                    lastTouchY = screenY;
                }
                return true;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom += amountY * 0.1f;
                if (camera.zoom < 0.1f) camera.zoom = 0.1f;
                if (camera.zoom > 5f) camera.zoom = 5f;
                camera.update();
                return true;
            }
        });
    }

    private void handleNodeClick(ProgressNode node) {
        DLog.log("Clicked node: " + node.id);
        if (manager.isAvailable(node.id)) {
            manager.unlock(node.id);
            DLog.log("Unlocked: " + node.id);
        } else if (manager.isUnlocked(node.id)) {
             DLog.log("Already unlocked: " + node.id);
        } else {
             DLog.log("Locked (Parents not ready): " + node.id);
        }
    }

    @Override
    public void render0(float delta) {
        // GScreen clears screen usually, but we can do it again if needed or rely on GScreen
        // GScreen uses getClearScreenColor()
        
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        Collection<ProgressNode> nodes = manager.getAllNodes();

        // Draw connections
        shapeRenderer.setColor(COL_LINE);
        for (ProgressNode node : nodes) {
            for (String pid : node.parentIds) {
                ProgressNode parent = manager.getNode(pid);
                if (parent != null) {
                    shapeRenderer.rectLine(parent.x, parent.y, node.x, node.y, 2f);
                }
            }
        }

        // Draw nodes
        for (ProgressNode node : nodes) {
            if (manager.isUnlocked(node.id)) {
                shapeRenderer.setColor(COL_UNLOCKED);
            } else if (manager.isAvailable(node.id)) {
                shapeRenderer.setColor(COL_AVAILABLE);
            } else {
                shapeRenderer.setColor(COL_LOCKED);
            }
            shapeRenderer.circle(node.x, node.y, NODE_RADIUS);
        }
        shapeRenderer.end();
        
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (ProgressNode node : nodes) {
             font.draw(batch, node.title, node.x - 20, node.y + NODE_RADIUS + 15);
        }
        batch.end();
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        batch.dispose();
        font.dispose();
        super.dispose();
    }
}
