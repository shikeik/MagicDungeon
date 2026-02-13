package com.goldsprite.magicdungeon.core.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.world.Tile;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DualGridDungeonRenderer implements Disposable {
    private static final int TILE_SIZE = Constants.TILE_SIZE;
    private static final float DISPLAY_OFFSET = TILE_SIZE / 2f;

    // 0-15 Mask to Atlas Index Mapping
    private static final int[] MASK_TO_ATLAS_X = {
        -1, 1, 0, 3, 0, 1, 2, 1, 3, 0, 3, 2, 1, 2, 3, 2
    };
    private static final int[] MASK_TO_ATLAS_Y = {
        -1, 3, 0, 0, 2, 0, 3, 1, 3, 1, 2, 0, 2, 2, 1, 1
    };

    private List<LayerConfig> layers = new ArrayList<>();
    private Map<String, Texture> textures = new HashMap<>();

    public DualGridDungeonRenderer() {
        loadResources();
    }

    private void loadResources() {
        // Load blob tilesets (4x4 tiles per texture, 16x16 pixels each)
        TextureRegion[] grassBlob = loadBlobTexture("sprites/tilesets/grass_tiles.png");
        TextureRegion[] sandBlob = loadBlobTexture("sprites/tilesets/sand_tiles.png");
        TextureRegion[] dirtBlob = loadBlobTexture("sprites/tilesets/dirt_tiles.png");
        TextureRegion[] brickBlob = loadBlobTexture("sprites/tilesets/dungeon_brick_tiles.png");

        // Layer 0: Dirt (Base layer)
        if (dirtBlob != null) layers.add(new LayerConfig(dirtBlob, "dirt"));
        
        // Layer 1: Brick (Indoor/Dungeon Floor)
        if (brickBlob != null) layers.add(new LayerConfig(brickBlob, "brick"));
        
        // Layer 2: Sand (Overlay)
        if (sandBlob != null) layers.add(new LayerConfig(sandBlob, "sand"));
        
        // Layer 3: Grass (Top layer)
        if (grassBlob != null) layers.add(new LayerConfig(grassBlob, "grass"));
    }

    private TextureRegion[] loadBlobTexture(String path) {
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("DualGridDungeonRenderer", "Texture not found: " + path);
            return null;
        }
        
        Texture tex = new Texture(Gdx.files.internal(path));
        textures.put(path, tex);
        
        TextureRegion[][] split = TextureRegion.split(tex, 16, 16);
        TextureRegion[] flat = new TextureRegion[16];
        for (int i = 0; i < 16; i++) {
            flat[i] = split[i / 4][i % 4];
        }
        return flat;
    }

    public void render(NeonBatch batch, Dungeon dungeon) {
        // Layer 0: Dirt (Base) - Draw dirt everywhere there is a valid tile
        renderLayer(batch, dungeon, "dirt", (t) -> t != null); 
        
        // Layer 1: Brick - Draw for indoor tiles (Floor, Wall, etc.)
        renderLayer(batch, dungeon, "brick", (t) -> t != null && (
            t.type == TileType.Floor || t.type == TileType.Wall || 
            t.type == TileType.Door || t.type == TileType.Stairs_Up || 
            t.type == TileType.Stairs_Down || t.type == TileType.Dungeon_Entrance
        ));

        // Layer 2: Sand
        renderLayer(batch, dungeon, "sand", (t) -> t != null && t.type == TileType.Sand);
        
        // Layer 3: Grass
        renderLayer(batch, dungeon, "grass", (t) -> t != null && t.type == TileType.Grass);
    }

    private interface TilePredicate {
        boolean match(Tile tile);
    }
    
    private static class LayerConfig {
        TextureRegion[] atlas;
        String name;
        
        public LayerConfig(TextureRegion[] atlas, String name) {
            this.atlas = atlas;
            this.name = name;
        }
    }

    private void renderLayer(NeonBatch batch, Dungeon dungeon, String layerName, TilePredicate predicate) {
        LayerConfig config = null;
        for (LayerConfig l : layers) {
            if (l.name.equals(layerName)) {
                config = l;
                break;
            }
        }
        if (config == null) return;
        TextureRegion[] atlas = config.atlas;

        // Iterate over all intersections
        for (int x = 0; x <= dungeon.width; x++) {
            for (int y = 0; y <= dungeon.height; y++) {
                int mask = calculateMask(dungeon, x, y, predicate);
                if (mask <= 0) continue;

                int tx = MASK_TO_ATLAS_X[mask];
                int ty = MASK_TO_ATLAS_Y[mask];
                if (tx == -1) continue;

                int index = ty * 4 + tx;
                if (index >= 0 && index < atlas.length) {
                    float drawX = x * TILE_SIZE - DISPLAY_OFFSET;
                    float drawY = y * TILE_SIZE - DISPLAY_OFFSET;
                    batch.draw(atlas[index], drawX, drawY, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }

    private int calculateMask(Dungeon dungeon, int x, int y, TilePredicate predicate) {
        boolean tr = predicate.match(dungeon.getTile(x, y));
        boolean tl = predicate.match(dungeon.getTile(x - 1, y));
        boolean br = predicate.match(dungeon.getTile(x, y - 1));
        boolean bl = predicate.match(dungeon.getTile(x - 1, y - 1));

        return ((tl ? 1 : 0) << 3) | 
               ((tr ? 1 : 0) << 2) | 
               ((bl ? 1 : 0) << 1) | 
               (br ? 1 : 0);
    }

    @Override
    public void dispose() {
        for (Texture tex : textures.values()) {
            tex.dispose();
        }
        textures.clear();
        layers.clear();
    }
}
