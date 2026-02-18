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
        
        int[] floorIds = decodeIds(data.compressedFloors, w * h);
        int[] blockIds = decodeIds(data.compressedBlocks, w * h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int index = y * w + x;
                
                // 1. Base Floor
                TileType floorType = TileType.fromId(floorIds[index]);

                // 2. Block/Object
                TileType blockType = TileType.fromId(blockIds[index]);

                // Combine: prioritize Block, fallback to Floor
                TileType finalType = TileType.Air;
                if (blockType != TileType.Air) {
                    finalType = blockType;
                } else if (floorType != TileType.Air) {
                    finalType = floorType;
                }

                if (finalType != TileType.Air) {
                    map[y][x] = new Tile(finalType);
                } else {
                    // map[y][x] remains null or Air
                }
            }
        }
        return map;
    }

    public static LayerData fromTileMap(Tile[][] map, int width, int height) {
        LayerData data = new LayerData(width, height);
        int[] floorIds = new int[width * height];
        int[] blockIds = new int[width * height];
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = y * width + x;
                Tile t = (y < map.length && x < map[0].length) ? map[y][x] : null;
                
                if (t != null) {
                    if (isFloor(t.type)) {
                        floorIds[index] = t.type.getId();
                    } else {
                        // It's an object/wall
                        blockIds[index] = t.type.getId();
                        // Assume standard floor underneath?
                        floorIds[index] = TileType.Floor.getId(); 
                    }
                }
            }
        }
        
        data.compressedFloors = encodeIds(floorIds);
        data.compressedBlocks = encodeIds(blockIds);
        return data;
    }

    public static LayerData fromGameMapData(GameMapData gameMapData) {
        if (gameMapData == null) return null;
        int w = gameMapData.width;
        int h = gameMapData.height;
        LayerData layerData = new LayerData(w, h);
        int[] floorIds = new int[w * h];
        int[] blockIds = new int[w * h];
        
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
                                floorIds[index] = type.getId();
                            } else {
                                blockIds[index] = type.getId();
                                // Fill floor underneath if it's a block
                                floorIds[index] = TileType.Floor.getId();
                            }
                        } catch (Exception e) {
                            // Fallback for unknown types or just ignore
                        }
                    }
                }
            }
        }
        
        layerData.compressedFloors = encodeIds(floorIds);
        layerData.compressedBlocks = encodeIds(blockIds);
        
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
    
    public static String encodeIds(int[] ids) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ids.length; i++) {
            sb.append(ids[i]);
            if (i < ids.length - 1) sb.append(" ");
        }
        return sb.toString();
    }
    
    public static int[] decodeIds(String data, int size) {
        int[] ids = new int[size];
        if (data == null || data.isEmpty()) return ids;
        
        String[] parts = data.split(" ");
        for (int i = 0; i < Math.min(ids.length, parts.length); i++) {
            try {
                ids[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                ids[i] = 0; // Air
            }
        }
        return ids;
    }

    private static boolean isFloor(TileType type) {
        return type == TileType.Floor || type == TileType.Dirt || type == TileType.Grass || type == TileType.Sand || 
               type == TileType.StonePath;
    }
}
