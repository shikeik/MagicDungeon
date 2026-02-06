package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.entities.Player;
import com.goldsprite.magicdungeon.world.Dungeon;

public class SaveManager {
	private static final String SAVE_DIR = "MagicDungeon/saves/";
	private static final String SAVE_FILE_NAME = "game_save.json";
	private static final String SAVE_PATH = SAVE_DIR + SAVE_FILE_NAME;

	public static void saveGame(Player player, Dungeon dungeon) {
		GameState state = new GameState();
		state.dungeonLevel = dungeon.level;
		state.seed = dungeon.globalSeed;
		state.playerStats = player.stats;
		state.inventory = player.inventory;

		Json json = new Json();
		String stateJson = json.prettyPrint(state);

		// Create the saves directory if it doesn't exist
		FileHandle dirHandle = Gdx.files.local(SAVE_DIR);
		if (!dirHandle.exists()) {
			dirHandle.mkdirs();
			System.out.println("Created save directory: " + dirHandle.path());
		}

		// Write the save file
		FileHandle saveFile = Gdx.files.local(SAVE_PATH);
		saveFile.writeString(stateJson, false);

		System.out.println("Game Saved to: " + saveFile.path());
	}

	public static GameState loadGame() {
		FileHandle saveFile = Gdx.files.local(SAVE_PATH);
		if (!saveFile.exists()) {
			System.out.println("No save file found at: " + saveFile.path());
			return null;
		}

		try {
			String stateJson = saveFile.readString();
			Json json = new Json();
			return json.fromJson(GameState.class, stateJson);
		} catch (Exception e) {
			System.err.println("Failed to load save: " + e.getMessage());
			return null;
		}
	}

	public static boolean hasSave() {
		FileHandle saveFile = Gdx.files.local(SAVE_PATH);
		return saveFile.exists();
	}
}
