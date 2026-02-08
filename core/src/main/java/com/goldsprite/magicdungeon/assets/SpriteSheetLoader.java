package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.HashMap;
import java.util.Map;

public class SpriteSheetLoader {

    public static Map<String, TextureRegion> load(String jsonPath) {
        Map<String, TextureRegion> regions = new HashMap<>();
        
        try {
            // Load JSON
            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal(jsonPath));
            
            int gridW = root.getInt("gridWidth");
            int gridH = root.getInt("gridHeight");
            
            // Load Texture (Assume same name as json but .png)
            String pngPath = jsonPath.replace(".json", ".png");
            if (!Gdx.files.internal(pngPath).exists()) {
                Gdx.app.error("SpriteSheetLoader", "Texture file not found: " + pngPath);
                return regions;
            }
            
            Texture texture = new Texture(Gdx.files.internal(pngPath));
            
            // Split Texture
            TextureRegion[][] splits = TextureRegion.split(texture, gridW, gridH);
            if (splits.length == 0) return regions;
            
            int rows = splits.length;
            int cols = splits[0].length;
            
            // Map regions
            JsonValue regionsJson = root.get("regions");
            for (JsonValue entry : regionsJson) {
                String key = entry.name(); // e.g. "rusty_sword"
                // The value is an array: ["index", "desc"]
                // But JsonValue iterator iterates over children. 
                // If entry is a child of object "regions", entry.name is the key.
                // entry itself is the value (array).
                
                int index = Integer.parseInt(entry.getString(0));
                
                // Calculate row/col from linear index
                int row = index / cols;
                int col = index % cols;
                
                if (row < rows && col < cols) {
                    // Use lowercase key for consistency
                    regions.put(key.toLowerCase(), splits[row][col]);
                } else {
                    Gdx.app.error("SpriteSheetLoader", "Index out of bounds: " + index + " for " + key);
                }
            }
            
        } catch (Exception e) {
            Gdx.app.error("SpriteSheetLoader", "Failed to load sheet: " + jsonPath, e);
        }
        
        return regions;
    }
}
