package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon.AppConstants;
import com.goldsprite.magicdungeon.model.LayerData;
import com.goldsprite.magicdungeon.core.EquipmentState;
import com.goldsprite.magicdungeon.entities.PlayerStats;
import com.goldsprite.magicdungeon.model.PlayerData;
import com.goldsprite.magicdungeon.model.SaveData;
import com.goldsprite.magicdungeon.utils.AssetUtils;
import java.util.ArrayList;
import java.util.List;

import com.goldsprite.magicdungeon.utils.MapIO;
import com.goldsprite.magicdungeon.world.data.GameMapData;

public class SaveManager {
    public static final String SAVES_ROOT = AppConstants.STORAGE_ROOT + "saves/game_saves/";
    private static final String META_FILE = "meta.json";
    
    private static Json json = new Json();

    static {
        json.setOutputType(JsonWriter.OutputType.json);
        json.setUsePrototypes(false);
    }

    /**
     * 获取所有存档列表
     */
    public static List<SaveData> listSaves() {
        List<SaveData> saves = new ArrayList<>();
        FileHandle root = Gdx.files.local(SAVES_ROOT);
        if (!root.exists()) return saves;

        for (FileHandle entry : root.list()) {
            if (entry.isDirectory()) {
                FileHandle meta = entry.child(META_FILE);
                if (meta.exists()) {
                    try {
                        SaveData data = json.fromJson(SaveData.class, meta);
                        saves.add(data);
                    } catch (Exception e) {
                        DLog.logErr("SaveManager", "Failed to load save meta: " + entry.name() + " - " + e.getMessage());
                    }
                }
            }
        }
        return saves;
    }

    /**
     * 检查存档是否存在
     */
    public static boolean hasSave(String saveName) {
        return Gdx.files.local(SAVES_ROOT + saveName).exists();
    }

    /**
     * 删除存档
     */
    public static void deleteSave(String saveName) {
        FileHandle saveDir = Gdx.files.local(SAVES_ROOT + saveName);
        if (saveDir.exists()) {
            saveDir.deleteDirectory();
        }
    }

    /**
     * 创建新存档
     */
    public static void createSave(String saveName, String playerName) {
        String savePath = SAVES_ROOT + saveName + "/";
        FileHandle saveDir = Gdx.files.local(savePath);
        
        if (saveDir.exists()) {
            throw new RuntimeException("Save already exists: " + saveName);
        }
        saveDir.mkdirs();

        // 1. Create Meta
        SaveData meta = new SaveData(saveName, playerName);
        saveJson(saveDir.child(META_FILE), meta);

        // 2. Create Players Dir & Initial Player Data
        FileHandle playersDir = saveDir.child("players");
        playersDir.mkdirs();
        
        PlayerData player = new PlayerData();
        player.name = playerName;
        player.stats = new PlayerStats(); // Initialize default stats
        player.inventory = new ArrayList<>();
        player.equipment = new EquipmentState();
        
        saveJson(playersDir.child(playerName + ".json"), player);

        // 3. Create Areas Dir & Import Assets
        FileHandle areasDir = saveDir.child("areas");
        areasDir.mkdirs();
        importAssetsAreas(areasDir);
    }

    /**
     * 从 assets/maps 导入预设区域
     */
    private static void importAssetsAreas(FileHandle targetAreasDir) {
        String[] mapFiles = AssetUtils.listNames("maps");
        for (String fileName : mapFiles) {
            // Check if it's a directory by checking if it has children in index
            String[] subFiles = AssetUtils.listNames("maps/" + fileName);
            
            if (subFiles.length > 0) {
                // It is a directory (e.g. maps/stone_tower/)
                FileHandle targetSubDir = targetAreasDir.child(fileName);
                targetSubDir.mkdirs();
                
                for (String sub : subFiles) {
                    FileHandle subSource = Gdx.files.internal("maps/" + fileName + "/" + sub);
                    // Assume sub files are not directories (flat structure for now inside area folders)
                    processMapFile(subSource, targetSubDir.child(sub));
                }
            } else {
                // It is a file (e.g. maps/camp.json)
                if (fileName.endsWith(".json")) {
                    FileHandle source = Gdx.files.internal("maps/" + fileName);
                    String areaName = fileName.replace(".json", "");
                    FileHandle targetDir = targetAreasDir.child(areaName);
                    targetDir.mkdirs();
                    processMapFile(source, targetDir.child("floor_1.json"));
                }
            }
        }
    }

    private static void processMapFile(FileHandle source, FileHandle target) {
        // Try to load as GameMapData first (Legacy format)
        try {
            GameMapData gameMapData = json.fromJson(GameMapData.class, source);
            // Simple check: GameMapData usually has grid
            if (gameMapData != null && gameMapData.grid != null) {
                // Convert to LayerData
                LayerData layerData = MapIO.fromGameMapData(gameMapData);
                saveJson(target, layerData);
                DLog.logT("SaveManager", "Converted map " + source.name() + " to LayerData.");
                return;
            }
        } catch (Exception e) {
            // Not GameMapData or parse error
        }
        
        // Try to load as LayerData (New format)
        try {
            LayerData layerData = json.fromJson(LayerData.class, source);
            if (layerData != null && (layerData.floorIds != null || layerData.blockIds != null)) {
                // Valid LayerData, copy or save
                saveJson(target, layerData);
                 DLog.logT("SaveManager", "Imported LayerData map " + source.name());
                return;
            }
        } catch (Exception e) {
            // Not LayerData
        }
        
        DLog.logErr("SaveManager", "Failed to process map file: " + source.path());
    }

    public static SaveData loadSaveMeta(String saveName) {
        return loadJson(Gdx.files.local(SAVES_ROOT + saveName + "/" + META_FILE), SaveData.class);
    }
    
    public static void saveSaveMeta(SaveData data) {
         saveJson(Gdx.files.local(SAVES_ROOT + data.saveName + "/" + META_FILE), data);
    }

    public static PlayerData loadPlayerData(String saveName, String playerName) {
        return loadJson(Gdx.files.local(SAVES_ROOT + saveName + "/players/" + playerName + ".json"), PlayerData.class);
    }

    public static void savePlayerData(String saveName, PlayerData data) {
        saveJson(Gdx.files.local(SAVES_ROOT + saveName + "/players/" + data.name + ".json"), data);
    }

    public static LayerData loadLayerData(String saveName, String areaId, int floor) {
        FileHandle file = getLayerFile(saveName, areaId, floor);
        if (!file.exists()) return null;
        return loadJson(file, LayerData.class);
    }

    public static void saveLayerData(String saveName, String areaId, int floor, LayerData data) {
        FileHandle file = getLayerFile(saveName, areaId, floor);
        file.parent().mkdirs();
        saveJson(file, data);
    }
    
    public static boolean hasLayerData(String saveName, String areaId, int floor) {
        return getLayerFile(saveName, areaId, floor).exists();
    }

    private static FileHandle getLayerFile(String saveName, String areaId, int floor) {
        return Gdx.files.local(SAVES_ROOT + saveName + "/areas/" + areaId + "/floor_" + floor + ".json");
    }

    private static <T> void saveJson(FileHandle file, T object) {
        try {
            file.writeString(json.prettyPrint(object), false);
        } catch (Exception e) {
            DLog.logErr("SaveManager", "Error saving json to " + file.path() + ": " + e.getMessage());
            throw e;
        }
    }

    private static <T> T loadJson(FileHandle file, Class<T> type) {
        try {
            return json.fromJson(type, file);
        } catch (Exception e) {
            DLog.logErr("SaveManager", "Error loading json from " + file.path() + ": " + e.getMessage());
            return null;
        }
    }
}
