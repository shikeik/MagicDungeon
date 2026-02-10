package com.goldsprite.magicdungeon.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import com.goldsprite.gdengine.log.Debug;

public class SettingsManager {
    private static final String SETTINGS_FILE = "MagicDungeon/settings.json";

    private static SettingsManager instance;

    private float musicVolume = 0.5f;
    private float sfxVolume = 0.5f;
    private boolean isFullscreen = false;

    private SettingsManager() {
        load();
    }

    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }

    private void load() {
        FileHandle file = Gdx.files.local(SETTINGS_FILE);
        if (!file.exists()) {
            Debug.logT("SettingsManager", "No settings file found, using defaults.");
            return;
        }

        try {
            Json json = new Json();
            JsonValue root = json.fromJson(null, file);

            if (root.has("musicVolume")) musicVolume = root.getFloat("musicVolume");
            if (root.has("sfxVolume")) sfxVolume = root.getFloat("sfxVolume");
            if (root.has("fullscreen")) isFullscreen = root.getBoolean("fullscreen");

            applySettings();
            Debug.logT("SettingsManager", "Settings loaded.");
        } catch (Exception e) {
            Debug.logErr("SettingsManager", "Failed to load settings: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Json json = new Json();
            json.setOutputType(JsonWriter.OutputType.json);

            // Use simple object serialization for cleaner JSON and to avoid manual construction
            // which was causing confusion. LibGDX Json can handle this POJO.
            FileHandle file = Gdx.files.local(SETTINGS_FILE);
            file.parent().mkdirs();
            file.writeString(json.prettyPrint(this), false);
            Debug.logT("SettingsManager", "Settings saved to " + file.path());
        } catch (Exception e) {
            Debug.logErr("SettingsManager", "Failed to save settings: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void applySettings() {
        // Apply Fullscreen
        if (isFullscreen) {
            if (!Gdx.graphics.isFullscreen())
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            if (Gdx.graphics.isFullscreen())
                Gdx.graphics.setWindowedMode(1280, 720);
        }

        // TODO: Apply Volume when AudioSystem is ready
    }

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float v) {
        this.musicVolume = v;
    }

    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float v) { this.sfxVolume = v; }

    public boolean isFullscreen() { return isFullscreen; }
    public void setFullscreen(boolean fullscreen) {
        this.isFullscreen = fullscreen;
        applySettings(); // Apply immediately for preview
    }
}
