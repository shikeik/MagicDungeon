package com.goldsprite.magicdungeon.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.Debug;
import com.goldsprite.magicdungeon.AppConstants;

public class SettingsManager {
    private static final String SETTINGS_FILE = "settings.json";

    private static SettingsManager instance;

    // Data Object for Serialization
    public static class SettingsData {
        public float musicVolume = 0.5f;
        public float sfxVolume = 0.5f;
        public boolean isFullscreen = false;
    }

    private SettingsData data = new SettingsData();

    private SettingsManager() {
        load();
    }

    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }

    private void load() {
        FileHandle file = AppConstants.getLocalFile(SETTINGS_FILE);
        if (!file.exists()) {
            Debug.logT("SettingsManager", "No settings file found, using defaults.");
            return;
        }

        try {
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            SettingsData loadedData = json.fromJson(SettingsData.class, file);
            
            if (loadedData != null) {
                this.data = loadedData;
                applySettings();
                Debug.logT("SettingsManager", "Settings loaded.");
            }
        } catch (Exception e) {
            Debug.logErr("SettingsManager", "Failed to load settings: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);

            FileHandle file = AppConstants.getLocalFile(SETTINGS_FILE);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(data), false);
            Debug.logT("SettingsManager", "Settings saved to " + file.path());
        } catch (Exception e) {
            Debug.logErr("SettingsManager", "Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void applySettings() {
        // Apply Fullscreen
        if (data.isFullscreen) {
            if (!Gdx.graphics.isFullscreen())
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            if (Gdx.graphics.isFullscreen())
                Gdx.graphics.setWindowedMode(1280, 720);
        }

        // TODO: Apply Volume when AudioSystem is ready
    }

    public float getMusicVolume() { return data.musicVolume; }
    public void setMusicVolume(float v) {
        data.musicVolume = v;
    }

    public float getSfxVolume() { return data.sfxVolume; }
    public void setSfxVolume(float v) { data.sfxVolume = v; }

    public boolean isFullscreen() { return data.isFullscreen; }
    public void setFullscreen(boolean fullscreen) {
        data.isFullscreen = fullscreen;
        applySettings(); // Apply immediately for preview
    }
}
