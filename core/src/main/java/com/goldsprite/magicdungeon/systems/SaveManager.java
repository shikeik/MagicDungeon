package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon.AppConstants;
import com.goldsprite.magicdungeon.model.LayerData;
import com.goldsprite.magicdungeon.model.PlayerData;
import com.goldsprite.magicdungeon.model.SaveData;
import com.goldsprite.magicdungeon.utils.AssetUtils;
import java.util.ArrayList;
import java.util.List;

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
        // Initial player data is created empty, will be populated by GameScreen logic or here?
        // Let's create a basic one
        PlayerData player = new PlayerData();
        player.name = playerName;
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
        // 使用 AssetUtils 扫描 assets/maps 下的文件
        // 假设结构: assets/maps/camp.json -> target/camp/floor_1.json (如果 camp.json 是单层)
        // 或者: assets/maps/camp/floor_1.json -> target/camp/floor_1.json
        
        String[] mapFiles = AssetUtils.listNames("maps");
        for (String fileName : mapFiles) {
            // 这里简化处理：假设 assets/maps 下的文件都是 LayerData 格式的 JSON
            // 且文件名格式为 <areaId>_<floor>.json 或者 <areaId>.json (默认为 floor_1)
            
            FileHandle source = Gdx.files.internal("maps/" + fileName);
            if (source.isDirectory()) {
                // 如果是目录，递归复制? 目前 AssetUtils.listNames 返回的是文件名或一级目录名
                // 如果是目录，需要再次 list
                String[] subFiles = AssetUtils.listNames("maps/" + fileName);
                FileHandle targetSubDir = targetAreasDir.child(fileName);
                targetSubDir.mkdirs();
                
                for (String sub : subFiles) {
                    FileHandle subSource = Gdx.files.internal("maps/" + fileName + "/" + sub);
                    if (!subSource.isDirectory()) {
                         subSource.copyTo(targetSubDir);
                    }
                }
            } else {
                // 是文件
                if (fileName.endsWith(".json")) {
                    // 假设是 areaName.json -> areaName/floor_1.json
                    String areaName = fileName.replace(".json", "");
                    FileHandle targetDir = targetAreasDir.child(areaName);
                    targetDir.mkdirs();
                    source.copyTo(targetDir.child("floor_1.json"));
                }
            }
        }
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
