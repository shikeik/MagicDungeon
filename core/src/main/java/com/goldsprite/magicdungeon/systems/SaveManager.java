package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.entities.Player;
import com.goldsprite.magicdungeon.world.Dungeon;

public class SaveManager {
    private static final String PREF_NAME = "NewDungeonSave";
    private static final String KEY_STATE = "gameState";

    public static void saveGame(Player player, Dungeon dungeon) {
        GameState state = new GameState();
        state.dungeonLevel = dungeon.level;
        state.playerStats = player.stats;
        state.inventory = player.inventory;

        Json json = new Json();
        String stateJson = json.toJson(state);

        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        prefs.putString(KEY_STATE, stateJson);
        prefs.flush();

        System.out.println("Game Saved!");
    }

    public static GameState loadGame() {
        Preferences prefs = Gdx.app.getPreferences(PREF_NAME);
        if (!prefs.contains(KEY_STATE)) return null;

        String stateJson = prefs.getString(KEY_STATE);
        Json json = new Json();
        try {
            return json.fromJson(GameState.class, stateJson);
        } catch (Exception e) {
            System.err.println("Failed to load save: " + e.getMessage());
            return null;
        }
    }

    public static boolean hasSave() {
        return Gdx.app.getPreferences(PREF_NAME).contains(KEY_STATE);
    }
}
