package com.goldsprite.magicdungeon.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;

public class SettingsManager {
    private static final String PREF_NAME = "magic_dungeon_settings";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_SFX_VOLUME = "sfx_volume";
    private static final String KEY_FULLSCREEN = "fullscreen";
    
    private static SettingsManager instance;
    private Preferences prefs;
    
    private float musicVolume;
    private float sfxVolume;
    private boolean isFullscreen;
    
    private SettingsManager() {
        prefs = Gdx.app.getPreferences(PREF_NAME);
        load();
    }
    
    public static SettingsManager getInstance() {
        if (instance == null) instance = new SettingsManager();
        return instance;
    }
    
    private void load() {
        musicVolume = prefs.getFloat(KEY_MUSIC_VOLUME, 0.5f);
        sfxVolume = prefs.getFloat(KEY_SFX_VOLUME, 0.5f);
        isFullscreen = prefs.getBoolean(KEY_FULLSCREEN, false);
    }
    
    public void save() {
        prefs.putFloat(KEY_MUSIC_VOLUME, musicVolume);
        prefs.putFloat(KEY_SFX_VOLUME, sfxVolume);
        prefs.putBoolean(KEY_FULLSCREEN, isFullscreen);
        prefs.flush();
    }
    
    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float v) { 
        this.musicVolume = v; 
        // Apply music volume change immediately if MusicManager exists
        // MusicManager.getInstance().setVolume(v);
    }
    
    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float v) { this.sfxVolume = v; }
    
    public boolean isFullscreen() { return isFullscreen; }
    public void setFullscreen(boolean fullscreen) {
        this.isFullscreen = fullscreen;
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(1280, 720);
        }
    }
}
