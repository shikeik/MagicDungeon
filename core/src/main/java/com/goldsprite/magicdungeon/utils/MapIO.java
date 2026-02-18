package com.goldsprite.magicdungeon.utils;

import com.goldsprite.magicdungeon.model.LayerData;
import com.goldsprite.magicdungeon.world.Tile;
import com.goldsprite.magicdungeon.world.TileType;
import com.goldsprite.magicdungeon.world.data.GameMapData;
import com.goldsprite.magicdungeon.core.ItemState;
import com.goldsprite.magicdungeon.core.MonsterState;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.world.data.MapEntityData;
import com.goldsprite.magicdungeon.world.data.MapItemData;

public class MapIO {

    public static Tile[][] toTileMap(LayerData data) {
        if (data == null) return null;
        int w = data.width;
        int h = data.height;
        Tile[][] map = new Tile[h][w];

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                
                // 1. Base Floor
                String floorId = (data.floorIds != null && index < data.floorIds.length) ? data.floorIds[index] : null;
                TileType floorType = null;
                if (floorId != null) {
                    try {
                        floorType = TileType.valueOf(floorId);
                    } catch (Exception e) {}
                }

                // 2. Block/Object
                String blockId = (data.blockIds != null && index < data.blockIds.length) ? data.blockIds[index] : null;
                TileType blockType = null;
                if (blockId != null) {
                    try {
                        blockType = TileType.valueOf(blockId);
                    } catch (Exception e) {}
                }

                // Combine: prioritize Block, fallback to Floor
                // Note: Current game logic uses single Tile per cell.
                // If we want dual layer (floor + object), we need to change Tile class or how we load it.
                // For now, if block is present, use block. If block is null/Air, use floor.
                // If both present, usually block covers floor, but in current Tile system we only have one type.
                // Wait, TileType has Wall, Floor, etc.
                // If data has Floor=Floor and Block=Wall, we should set Tile=Wall.
                // If data has Floor=Floor and Block=null, we set Tile=Floor.
                
                TileType finalType = null;
                if (blockType != null) {
                    finalType = blockType;
                } else if (floorType != null) {
                    finalType = floorType;
                }

                if (finalType != null) {
                    map[y][x] = new Tile(finalType);
                } else {
                    // map[y][x] remains null or Air?
                    // Existing code handles null as void/nothing
                }
            }
        }
        return map;
    }

    public static LayerData fromTileMap(Tile[][] map, int width, int height) {
        LayerData data = new LayerData(width, height);
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                Tile t = (y < map.length && x < map[0].length) ? map[y][x] : null;
                
                if (t != null) {
                    // This is lossy because Tile doesn't store if it's on a floor or not.
                    // But usually:
                    // Wall -> Block=Wall, Floor=Floor (implicitly)
                    // Floor -> Block=null, Floor=Floor
                    // Object -> Block=Object, Floor=Floor
                    
                    if (isFloor(t.type)) {
                        data.floorIds[index] = t.type.name();
                    } else {
                        // It's an object/wall
                        data.blockIds[index] = t.type.name();
                        // Assume standard floor underneath?
                        data.floorIds[index] = TileType.Floor.name(); 
                    }
                }
            }
        }
        return data;
    }

    public static LayerData fromGameMapData(GameMapData gameMapData) {
        if (gameMapData == null) return null;
        int w = gameMapData.width;
        int h = gameMapData.height;
        LayerData layerData = new LayerData(w, h);
        
        // 1. Convert Grid
        if (gameMapData.grid != null) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (y >= gameMapData.grid.length || x >= gameMapData.grid[y].length) continue;
                    
                    String typeName = gameMapData.grid[y][x];
                    if (typeName != null) {
                        int index = y * w + x;
                        try {
                            TileType type = TileType.valueOf(typeName);
                            if (isFloor(type)) {
                                layerData.floorIds[index] = typeName;
                            } else {
                                layerData.blockIds[index] = typeName;
                                // Fill floor underneath if it's a block
                                layerData.floorIds[index] = TileType.Floor.name();
                            }
                        } catch (Exception e) {
                            // Fallback for unknown types or just ignore
                        }
                    }
                }
            }
        }
        
        // 2. Convert Entities
        if (gameMapData.entities != null) {
            for (MapEntityData entity : gameMapData.entities) {
                 int maxHp = 100;
                 try {
                     MonsterType type = MonsterType.valueOf(entity.type);
                     maxHp = type.maxHp;
                 } catch(Exception e) {}
                 
                 layerData.monsters.add(new MonsterState(entity.x, entity.y, entity.type, maxHp, maxHp));
            }
        }
        
        // 3. Convert Items
        if (gameMapData.items != null) {
            for (MapItemData item : gameMapData.items) {
                 // Use default stats for items loaded from map
                 layerData.items.add(new ItemState(item.x, item.y, item.itemId, "COMMON", 0, 0, 0, 0, 1));
            }
        }
        
        return layerData;
    }

    private static boolean isFloor(TileType type) {
        return type == TileType.Floor || type == TileType.Dirt || type == TileType.Grass || type == TileType.Sand || 
               type == TileType.StonePath;
    }
}
