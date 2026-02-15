package com.goldsprite.magicdungeon.systems;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.magicdungeon.core.EquipmentState;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.core.ItemState;
import com.goldsprite.magicdungeon.core.LevelState;
import com.goldsprite.magicdungeon.core.MonsterState;
import com.goldsprite.magicdungeon.entities.Item;
import com.goldsprite.magicdungeon.entities.Monster;
import com.goldsprite.magicdungeon.entities.Player;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.AppConstants;
import com.goldsprite.magicdungeon.BuildConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SaveManager {
	public static final String SAVE_DIR = AppConstants.STORAGE_ROOT + "saves/";
	private static final String SAVE_FILE_NAME = "game_save.json";
	private static final String SAVE_PATH = SAVE_DIR + SAVE_FILE_NAME;

	public static void saveGame(Player player, Dungeon dungeon, List<Monster> monsters, List<Item> items, Map<Integer, LevelState> visitedLevels, int maxDepth) {
		GameState state = new GameState();
		state.dungeonLevel = dungeon.level;
		state.maxDepth = maxDepth;
		state.seed = dungeon.globalSeed;
		state.playerStats = player.stats;
		state.inventory = player.inventory;
		state.playerX = player.x;
		state.playerY = player.y;
		
		// Save Equipment
		if (player.equipment != null) {
			state.equipment = new EquipmentState(
				player.equipment.mainHand,
				player.equipment.offHand,
				player.equipment.helmet,
				player.equipment.armor,
				player.equipment.boots,
				player.equipment.accessories
			);
		}
		
		// Save Visited Levels History
		if (visitedLevels != null) {
			state.visitedLevels = visitedLevels;
		}

		// Save Current Level State
		// Even if we are about to switch levels or exit, saving "current" monsters/items is crucial
		// Note: The caller might have already updated visitedLevels with current level, 
		// but GameState has dedicated fields for the "active" snapshot (monsters/items)
		// which are used when loading the save directly into the current screen.
		
		state.monsters = new ArrayList<>();
		for (Monster m : monsters) {
			if (m.hp > 0) { // Only save live monsters
				state.monsters.add(new MonsterState(m.x, m.y, m.type.name(), m.hp, m.maxHp));
			}
		}

		state.items = new ArrayList<>();
		for (Item item : items) {
			state.items.add(new ItemState(
				item.x, 
				item.y, 
				item.item.data.name(),
				item.item.quality.name(),
				item.item.atk,
				item.item.def,
				item.item.heal,
				item.item.manaRegen,
				item.item.count
			));
		}

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
