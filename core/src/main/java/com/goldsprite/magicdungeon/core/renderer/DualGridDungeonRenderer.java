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
import com.goldsprite.magicdungeon.utils.texturegenerator.SpriteGenerator;
import com.goldsprite.magicdungeon.world.DungeonTheme;

public class DualGridDungeonRenderer implements Disposable {
    private static final int TILE_SIZE = Constants.TILE_SIZE;
    public static final float DISPLAY_OFFSET = TILE_SIZE / 2f;

    // 0-15 掩码到图集索引映射
    private static final int[] MASK_TO_ATLAS_X = {
        -1, 1, 0, 3, 0, 1, 2, 1, 3, 0, 3, 2, 1, 2, 3, 2
    };
    private static final int[] MASK_TO_ATLAS_Y = {
        -1, 3, 0, 0, 2, 0, 3, 1, 3, 1, 2, 0, 2, 2, 1, 1
    };

    private List<LayerConfig> layers = new ArrayList<>();
    private Map<String, TextureRegion> regions = new HashMap<>();
    private TextureRegion[] dungeonFloors;
    private TextureRegion torchTex;
    private TextureRegion windowTex;

    // Base blobs that don't change
    private TextureRegion[] dirtBlob;
    private TextureRegion[] sandBlob;
    private TextureRegion[] grassBlob;
    private DungeonTheme currentTheme = null;

    public DualGridDungeonRenderer() {
        initBaseResources();
    }

    private void initBaseResources() {
        // Load static blobs
        grassBlob = loadBlobTexture("sprites/tilesets/grass_tiles.png");
        sandBlob = loadBlobTexture("sprites/tilesets/sand_tiles.png");
        dirtBlob = loadBlobTexture("sprites/tilesets/dirt_tiles.png");

        // Load Decor
        if (Gdx.files.internal("sprites/tilesets/torch.png").exists()) {
            Texture tex = new Texture(Gdx.files.internal("sprites/tilesets/torch.png"));
            TextureRegion tr = new TextureRegion(tex);
            regions.put("sprites/tilesets/torch.png", tr);
            torchTex = tr;
        }
        if (Gdx.files.internal("sprites/tilesets/wall_window.png").exists()) {
            Texture tex = new Texture(Gdx.files.internal("sprites/tilesets/wall_window.png"));
            TextureRegion tr = new TextureRegion(tex);
            regions.put("sprites/tilesets/wall_window.png", tr);
            windowTex = tr;
        }
    }

    private void updateTheme(DungeonTheme theme) {
        if (currentTheme == theme) return;

        // Clean up old theme textures
        if (regions.containsKey("theme_wall")) {
            regions.get("theme_wall").getTexture().dispose();
            regions.remove("theme_wall");
        }
        for(int i=0; i<7; i++) {
            String key = "theme_floor_" + i;
            if (regions.containsKey(key)) {
                regions.get(key).getTexture().dispose();
                regions.remove(key);
            }
        }

        // 1. Generate Wall (Brick)
        TextureRegion wallTex = SpriteGenerator.createDungeonWallTileset(
            theme.primaryColor, theme.secondaryColor
        );
        regions.put("theme_wall", wallTex);

        int size = wallTex.getRegionWidth() / 4;
        TextureRegion[][] split = wallTex.split(size, size);
        TextureRegion[] brickBlob = new TextureRegion[16];
        for (int i = 0; i < 16; i++) {
            brickBlob[i] = split[i / 4][i % 4];
        }

        // 2. Generate Floors
        dungeonFloors = new TextureRegion[7];
        for(int i=0; i<7; i++) {
            TextureRegion region = SpriteGenerator.createFloor(
                theme.floorBase, theme.floorDark, theme.floorHighlight
            );
            regions.put("theme_floor_" + i, region);
            dungeonFloors[i] = new TextureRegion(region);
        }

        // Rebuild Layers List
        layers.clear();
        if (dirtBlob != null) layers.add(new LayerConfig(dirtBlob, "dirt"));
        if (brickBlob != null) layers.add(new LayerConfig(brickBlob, "brick"));
        if (sandBlob != null) layers.add(new LayerConfig(sandBlob, "sand"));
        if (grassBlob != null) layers.add(new LayerConfig(grassBlob, "grass"));

        currentTheme = theme;
        Gdx.app.log("DualGridDungeonRenderer", "Theme updated to: " + theme.name);
    }

    private TextureRegion[] loadBlobTexture(String path) {
        if (!Gdx.files.internal(path).exists()) {
            Gdx.app.error("DualGridDungeonRenderer", "Texture not found: " + path);
            return null;
        }

        Texture tex = new Texture(Gdx.files.internal(path));
        regions.put(path, new TextureRegion(tex));

        int size = tex.getWidth() / 4;
        TextureRegion[][] split = TextureRegion.split(tex, size, size);
        TextureRegion[] flat = new TextureRegion[16];
        for (int i = 0; i < 16; i++) {
            flat[i] = split[i / 4][i % 4];
        }
        return flat;
    }

    public void render(NeonBatch batch, Dungeon dungeon) {
        renderTileGrid(batch, dungeon.map, dungeon.theme, dungeon.level == 0);
    }

    public void renderTileGrid(NeonBatch batch, Tile[][] map, DungeonTheme theme, boolean isCamp) {
        if (map == null || map.length == 0) return;
        int height = map.length;
        int width = map[0].length;

        if (currentTheme != theme) {
            updateTheme(theme);
        }

        if (isCamp) {
            // === 营地模式 (Level 0) ===
            // 恢复完整的自然地形渲染逻辑

            // Layer 0: Dirt (Base) - Only draw if TileType is explicitly Dirt
            renderLayer(batch, map, "dirt", (t) -> t != null && t.type == TileType.Dirt);

            // Layer 0.5: Floor (Standard)
            TextureRegion floorTex = com.goldsprite.magicdungeon.assets.TextureManager.getInstance().getTile(TileType.Floor);
            if (floorTex != null) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        Tile t = getTileFromArray(map, x, y);
                        if (t != null && t.type == TileType.Floor) {
                            batch.draw(floorTex, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }
            }

            // Layer 1: Brick - Draw for Wall only (removed Floor, Door, Stairs)
            renderLayer(batch, map, "brick", (t) -> t != null && (
                t.type == TileType.Wall || t.type == TileType.Torch || t.type == TileType.Window
            ));

            // Layer 2: Sand
            renderLayer(batch, map, "sand", (t) -> t != null && t.type == TileType.Sand);

            // Layer 3: Grass
            renderLayer(batch, map, "grass", (t) -> t != null && t.type == TileType.Grass);

            // Layer 4: Simple Objects (Tree, Door, Stairs, StonePath, etc.)
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = getTileFromArray(map, x, y);
                    if (t == null) continue;
                    
                    if (t.type == TileType.Tree || t.type == TileType.Door || 
                        t.type == TileType.Stairs_Up || t.type == TileType.Stairs_Down || 
                        t.type == TileType.StonePath || t.type == TileType.Dungeon_Entrance ||
                        t.type == TileType.Pillar) {
                            
                        TextureRegion region = com.goldsprite.magicdungeon.assets.TextureManager.getInstance().getTile(t.type);
                        if (region != null) {
                             batch.draw(region, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }
            }

        } else {
            // === 地牢模式 (Level > 0) ===

            // 1. 渲染地板 (Floor) - 支持随机变种
            // 使用确定的哈希算法确保同一位置总是显示相同的地板变种
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = getTileFromArray(map, x, y);
                    if (t != null) {
                        // 只要不是 null，就绘制地板背景（或者是只在可行走区域绘制？通常地牢是铺满的）
                        // 这里我们假设所有有效 Tile 下面都有地板
                        if (dungeonFloors != null && dungeonFloors.length > 0) {
                            int variantIndex = Math.abs((x * 73856093 ^ y * 19349663) % dungeonFloors.length);
                            TextureRegion floorTex = dungeonFloors[variantIndex];
                            if (floorTex != null) {
                                float drawX = x * TILE_SIZE;
                                float drawY = y * TILE_SIZE;
                                batch.draw(floorTex, drawX, drawY, TILE_SIZE, TILE_SIZE);
                            }
                        }
                    }
                }
            }

            // 2. 双网格渲染器现在主要用于渲染地牢墙壁 (dungeon_brick)
            // 我们只在有 Wall 的地方渲染 "brick" 层，这样墙壁会有双网格边缘效果
            // 我们将 Torch 和 Window 也视为墙壁，以便它们下面也有墙体渲染

            renderLayer(batch, map, "brick", (t) -> t != null && (
                t.type == TileType.Wall || t.type == TileType.Torch || t.type == TileType.Window
            ));

            // 3. 渲染墙壁装饰 (火把, 窗户)
            // 这些装饰物是叠加在双网格墙壁之上的
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = getTileFromArray(map, x, y);
                    if (t == null) continue;

                    if (t.type == TileType.Torch && torchTex != null) {
                        batch.draw(torchTex, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    } else if (t.type == TileType.Window && windowTex != null) {
                        batch.draw(windowTex, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                    }
                }
            }
            
            // 4. Simple Tiles (Tree, Door, Stairs, StonePath, etc.)
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    Tile t = getTileFromArray(map, x, y);
                    if (t == null) continue;
                    
                    if (t.type == TileType.Tree || t.type == TileType.Door || 
                        t.type == TileType.Stairs_Up || t.type == TileType.Stairs_Down || 
                        t.type == TileType.StonePath || t.type == TileType.Dungeon_Entrance ||
                        t.type == TileType.Pillar) {
                            
                        TextureRegion region = com.goldsprite.magicdungeon.assets.TextureManager.getInstance().getTile(t.type);
                        if (region != null) {
                             batch.draw(region, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
                        }
                    }
                }
            }
        }
    }

    private Tile getTileFromArray(Tile[][] map, int x, int y) {
        if (map == null) return null;
        int h = map.length;
        if (h == 0) return null;
        int w = map[0].length;
        if (x < 0 || x >= w || y < 0 || y >= h) return null;
        return map[y][x];
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

    private void renderLayer(NeonBatch batch, Tile[][] map, String layerName, TilePredicate predicate) {
        LayerConfig config = null;
        for (LayerConfig l : layers) {
            if (l.name.equals(layerName)) {
                config = l;
                break;
            }
        }
        if (config == null) return;
        TextureRegion[] atlas = config.atlas;

        if (map == null || map.length == 0) return;
        int height = map.length;
        int width = map[0].length;

        // Iterate over all intersections
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                int mask = calculateMask(map, x, y, predicate);
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

    private int calculateMask(Tile[][] map, int x, int y, TilePredicate predicate) {
        boolean tr = match(map, x, y, predicate);
        boolean tl = match(map, x - 1, y, predicate);
        boolean br = match(map, x, y - 1, predicate);
        boolean bl = match(map, x - 1, y - 1, predicate);

        return ((tl ? 1 : 0) << 3) |
               ((tr ? 1 : 0) << 2) |
               ((bl ? 1 : 0) << 1) |
               (br ? 1 : 0);
    }

    private boolean match(Tile[][] map, int x, int y, TilePredicate predicate) {
        return predicate.match(getTileFromArray(map, x, y));
    }

    @Override
    public void dispose() {
        for (TextureRegion region : regions.values()) {
            if (region.getTexture() != null) {
                region.getTexture().dispose();
            }
        }
        regions.clear();
        layers.clear();
    }
}
