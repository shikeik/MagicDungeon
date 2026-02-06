package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.entities.*;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.ItemType;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.ui.GameHUD;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.world.Tile;
import com.goldsprite.magicdungeon.world.TileType;
import com.goldsprite.magicdungeon.assets.TextureManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.RandomXS128;

import com.goldsprite.magicdungeon.core.ItemState;
import com.goldsprite.magicdungeon.core.LevelState;
import com.goldsprite.magicdungeon.core.MonsterState;

public class GameScreen extends GScreen {
	private Dungeon dungeon;
	private Player player;
	private List<Monster> monsters;
	private List<Item> items;
	// Removed redundant viewport and camera
	private GameHUD hud;
	private AudioSystem audio;
	private SpriteBatch batch;
	private long seed;
	
	// History of visited levels
	private Map<Integer, LevelState> visitedLevels = new HashMap<>();

	private String cheatCodeBuffer = "";

	public GameScreen() {
		this(MathUtils.random(Long.MIN_VALUE, Long.MAX_VALUE));
	}

	public GameScreen(long seed) {
		this.seed = seed;
	}

	@Override
	protected void initViewport() {
		// 使用 GScreen 的机制，这里设定视口参数
		// 假设我们希望世界可视范围也是 960x540 或者基于 TILE_SIZE 调整
		// 这里沿用 Constants 的配置，但通过 worldScale 控制
		// worldScale = 1.0f 意味着 1 world unit = 1 pixel
		// 之前是 Constants.VIEWPORT_WIDTH = 800, VIEWPORT_HEIGHT = 600
		// 如果想保持像素感，可以设置较小的 viewSizeShort
		this.viewSizeShort = Constants.VIEWPORT_HEIGHT;
		this.viewSizeLong = Constants.VIEWPORT_WIDTH;

		// 保持世界相机的缩放比例为 1.0 (1 unit = 1 pixel)
		// 或者如果需要像素放大，可以调整 worldScale, 例如 0.5f 会放大两倍
		this.worldScale = 0.3f;

		super.initViewport();

//		// Camera Controller
//		SimpleCameraController controller = new SimpleCameraController(worldCamera);
//		controller.setCoordinateMapper((x, y) -> uiViewport.unproject(new Vector2(x, y)));
//		getImp().addProcessor(controller);
	}

	@Override
	public void create() {
		batch = new SpriteBatch();

		System.out.println("GameScreen Constructor Started");
		this.dungeon = new Dungeon(50, 50, seed);
		this.player = new Player(dungeon.startPos.x, dungeon.startPos.y);

		this.monsters = new ArrayList<>();
		this.items = new ArrayList<>();
		spawnEntities();

		// Scene2D HUD
		// 传递 uiViewport 给 HUD
		hud = new GameHUD(getUIViewport());

		// Set save listener for HUD save button
		hud.setSaveListener(() -> {
			SaveManager.saveGame(player, dungeon, monsters, items, visitedLevels);
			hud.showMessage("Game Saved!");
		});
		getImp().addProcessor(hud.stage);

		// Audio
		audio = new AudioSystem();

		// 初始化相机位置
		updateCamera();

		System.out.println("GameScreen Constructor Finished");
	}

	private void saveCurrentLevelState() {
		// Save current level state to history
		List<MonsterState> monsterStates = new ArrayList<>();
		for (Monster m : monsters) {
			if (m.hp > 0) {
				monsterStates.add(new MonsterState(m.x, m.y, m.type.name(), m.hp, m.maxHp));
			}
		}
		
		List<ItemState> itemStates = new ArrayList<>();
		for (Item item : items) {
			itemStates.add(new ItemState(item.x, item.y, item.data.name()));
		}
		
		visitedLevels.put(dungeon.level, new LevelState(monsterStates, itemStates));
	}
	
	private void loadLevelState(LevelState state) {
		monsters.clear();
		for(MonsterState ms : state.monsters) {
			MonsterType type = MonsterType.Slime;
			try {
				type = MonsterType.valueOf(ms.typeName);
			} catch(IllegalArgumentException e) {
				for(MonsterType t : MonsterType.values()) {
					if(t.name.equals(ms.typeName)) {
						type = t;
						break;
					}
				}
			}
			
			Monster m = new Monster(ms.x, ms.y, type);
			m.hp = ms.hp;
			m.maxHp = ms.maxHp;
			monsters.add(m);
		}
		
		items.clear();
		for(ItemState is : state.items) {
			ItemData data = ItemData.Health_Potion;
			try {
				data = ItemData.valueOf(is.itemName);
			} catch(IllegalArgumentException e) {
			}
			items.add(new Item(is.x, is.y, data));
		}
	}

	private void nextLevel() {
		saveCurrentLevelState();
		
		dungeon.level++;
		dungeon.generate();
		
		// Check if we visited this level before
		if (visitedLevels.containsKey(dungeon.level)) {
			loadLevelState(visitedLevels.get(dungeon.level));
		} else {
			spawnEntities();
		}

		player.x = dungeon.startPos.x;
		player.y = dungeon.startPos.y;
		player.visualX = player.x * Constants.TILE_SIZE;
		player.visualY = player.y * Constants.TILE_SIZE;
		
		hud.showMessage("Descended to Floor " + dungeon.level + "!");
		audio.playLevelUp(); // Reusing sound for now
	}

	private void prevLevel() {
		if (dungeon.level > 1) {
			saveCurrentLevelState();
			
			dungeon.level--;
			dungeon.generate();
			
			// Check if we visited this level before
			if (visitedLevels.containsKey(dungeon.level)) {
				loadLevelState(visitedLevels.get(dungeon.level));
			} else {
				// Should theoretically always be visited if going back, but handle just in case
				spawnEntities();
			}
			
            // When going up, we might want to spawn at the down stairs of the previous level?
            // But currently MapGenerator places stairs down at end room and stairs up at start room.
            // If we assume linear progression, going UP should land us at the DOWN stairs of the upper floor.
            // However, our MapGenerator randomizes everything each time.
            // So for now, let's just spawn at startPos (which is where Stairs Up is) to be safe,
            // OR we can find the Stairs Down and spawn there to simulate "coming up from below".

            // Let's spawn at Stairs Down to simulate coming back up.
            // Find Stairs Down
            GridPoint2 stairsDownPos = null;
            for(int y=0; y<dungeon.height; y++) {
                for(int x=0; x<dungeon.width; x++) {
                    Tile t = dungeon.getTile(x, y);
                    if(t != null && t.type == TileType.Stairs_Down) {
                        stairsDownPos = new GridPoint2(x, y);
                        break;
                    }
                }
                if(stairsDownPos != null) break;
            }
            
            if (stairsDownPos != null) {
                player.x = stairsDownPos.x;
                player.y = stairsDownPos.y;
            } else {
                player.x = dungeon.startPos.x;
                player.y = dungeon.startPos.y;
            }

			player.visualX = player.x * Constants.TILE_SIZE;
			player.visualY = player.y * Constants.TILE_SIZE;
			
			hud.showMessage("Ascended to Floor " + dungeon.level + "!");
			audio.playLevelUp();
		}
	}

	private void spawnEntities() {
		monsters.clear();
		items.clear();

		// Difficulty Multiplier
		float difficulty = 1.0f + (dungeon.level - 1) * 0.2f; // 20% harder per level

		RandomXS128 monsterRng = dungeon.getMonsterRNG();
		RandomXS128 itemRng = dungeon.getItemRNG();

		// Monsters
		int numMonsters = 10 + (dungeon.level / 2); // More monsters per level
		for (int i = 0; i < numMonsters; i++) {
			GridPoint2 pos = dungeon.getRandomWalkableTile(monsterRng);
			if (pos != null) {
				if (Math.abs(pos.x - player.x) < 5 && Math.abs(pos.y - player.y) < 5) continue;

				// Varied Monster Types based on Level
				MonsterType type = MonsterType.Slime;
				if (dungeon.level >= 2 && monsterRng.nextFloat() < 0.3) type = MonsterType.Bat;
				if (dungeon.level >= 3 && monsterRng.nextFloat() < 0.3) type = MonsterType.Skeleton;
				if (dungeon.level >= 5 && monsterRng.nextFloat() < 0.3) type = MonsterType.Orc;
				if (dungeon.level % 6 == 0 && monsterRng.nextFloat() < 0.3) type = MonsterType.Boss;

				Monster m = new Monster(pos.x, pos.y, type);
				// Apply difficulty
				m.maxHp = (int) (m.maxHp * difficulty);
				m.hp = m.maxHp;
				m.atk = (int) (m.atk * difficulty);

				monsters.add(m);
			}
		}

		// Items
		int numItems = 5;
		for (int i = 0; i < numItems; i++) {
			GridPoint2 pos = dungeon.getRandomWalkableTile(itemRng);
			if (pos != null) {
				// Random item type
				ItemData itemData = ItemData.Health_Potion;
				double r = itemRng.nextDouble();
				if (r < 0.4) itemData = ItemData.Health_Potion;
				else if (r < 0.6) itemData = ItemData.Mana_Potion;
				else if (r < 0.8) itemData = ItemData.Rusty_Sword;
				// Add more logic for better loot later

				items.add(new Item(pos.x, pos.y, itemData));
			}
		}
	}

	private void updateCamera() {
		// 使用 GScreen 的 worldCamera
		worldCamera.position.set(
			player.visualX + Constants.TILE_SIZE / 2f,
			player.visualY + Constants.TILE_SIZE / 2f,
			0
		);
		worldCamera.update();
	}



	@Override
	public void render(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		// Input Handling
		int dx = 0;
		int dy = 0;
		if (Gdx.input.isKeyPressed(Input.Keys.A)) dx = -1;
		if (Gdx.input.isKeyPressed(Input.Keys.D)) dx = 1;
		if (Gdx.input.isKeyPressed(Input.Keys.W)) dy = 1;
		if (Gdx.input.isKeyPressed(Input.Keys.S)) dy = -1;

		if (dx != 0 || dy != 0) {
			audio.playMove();
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
			if (player.stats.mana >= 10) {
				player.useSkill();
				audio.playLevelUp();
				hud.showMessage("Used Heal!");
			} else {
				hud.showMessage("Not enough Mana!");
			}
		}

		// Enter Next Level
		if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {
			Tile tile = dungeon.getTile(player.x, player.y);
			// Debug: Check current position and tile type
			if (tile != null) {
				System.out.println("Player position: " + player.x + "," + player.y + ", Tile type: " + tile.type);
				if (tile.type == TileType.Stairs_Down) {
					System.out.println("Stairs found! Current level: " + dungeon.level);
					nextLevel();
					System.out.println("New level: " + dungeon.level);
				} else if (tile.type == TileType.Stairs_Up) {
                    System.out.println("Stairs Up found! Current level: " + dungeon.level);
                    prevLevel();
                    System.out.println("New level: " + dungeon.level);
                }
			} else {
				System.out.println("Tile is null at position: " + player.x + "," + player.y);
			}
		}

		// Update Player
		player.update(delta, dungeon, dx, dy, monsters, audio);

		// Item Pickup
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (item.x == player.x && item.y == player.y) {
				items.remove(i);
				i--;
				audio.playItem();
				// Apply Item Effect
				if (item.data.type == ItemType.POTION) {
					if (item.data == ItemData.Health_Potion) {
						player.stats.hp = Math.min(player.stats.hp + item.data.heal, player.stats.maxHp);
						hud.showMessage("Used Health Potion!");
					} else if (item.data == ItemData.Mana_Potion) {
						player.stats.mana = Math.min(player.stats.mana + item.data.heal, player.stats.maxMana);
						hud.showMessage("Used Mana Potion!");
					}
				} else {
					player.inventory.add(new InventoryItem(item.data));
					hud.showMessage("Picked up " + item.data.name + "!");
					// Update inventory dialog if it's open
					hud.updateInventory(player);
				}
			}
		}

		player.updateVisuals(delta);

		// Update Monsters
		for (Monster m : monsters) {
			int damage = m.update(delta, player, dungeon, monsters);
			if (damage > 0) {
				audio.playHit();
				player.stats.hp -= damage;
				player.hitFlashTimer = 0.2f; // Trigger red flash
				hud.showMessage("Took " + damage + " damage!");
				if (player.stats.hp <= 0) {
					hud.showMessage("GAME OVER!");
				}
			}
			m.updateVisuals(delta);
		}

		// Update Camera
		updateCamera();

		// Regenerate map
		if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
			dungeon.generate();
			player.x = dungeon.startPos.x;
			player.y = dungeon.startPos.y;
			player.visualX = player.x * Constants.TILE_SIZE;
			player.visualY = player.y * Constants.TILE_SIZE;
			player.stats.hp = player.stats.maxHp;
			spawnEntities();
			hud.showMessage("Map Regenerated!");
		}

		// Save Game
		if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
			SaveManager.saveGame(player, dungeon, monsters, items, visitedLevels);
			hud.showMessage("Game Saved!");
		}

		// Load Game
		if (Gdx.input.isKeyJustPressed(Input.Keys.F9)) {
			loadGame();
		}

		// Toggle Inventory
		if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
			hud.toggleInventory();
		}

		// Cheat Code: Get All Items
		if (Gdx.input.isKeyJustPressed(Input.Keys.GRAVE)) { // Tilde key (~) for easier access, or check for sequence if strictly "cheat666" needed
			 // Simple key binding for now, implementing "cheat666" sequence would require buffer
			 // Let's use F8 as "Cheat Key" or handle "cheat666" via input processor if strict
		}

		// Simple cheat trigger for testing: Press P to get all items (P for Presents/Power)
		// Or strictly follow user request "input cheat666" - typically means typing.
		// For simplicity in LibGDX without UI input field, let's use a debug key combo like CTRL+L
		// But user asked for "input cheat666". Let's assume typing blind.

		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) {
			 // Basic cheat implementation - Adds all items when F6 is pressed (easier than typing)
			 // Or better:
		}

		// Implementing "cheat666" typing detection
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUM_6)) {
			 cheatCodeBuffer += "6";
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
			 cheatCodeBuffer += "c";
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
			 cheatCodeBuffer += "h";
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
			 cheatCodeBuffer += "e";
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.A)) {
			 cheatCodeBuffer += "a";
		} else if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
			 cheatCodeBuffer += "t";
		} else {
			 // Reset buffer if too long or check on timer?
			 // Simplified: Check end of string
		}

		if (cheatCodeBuffer.endsWith("cheat666")) {
			cheatCodeBuffer = "";
			for (ItemData data : ItemData.values()) {
				player.inventory.add(new InventoryItem(data));
			}
			hud.showMessage("Cheat Activated: All Items Added!");
			hud.updateInventory(player);
			audio.playLevelUp();
		}
		if (cheatCodeBuffer.length() > 20) cheatCodeBuffer = "";

		// Update HUD
		hud.update(player, dungeon.level);
		// hud.updateInventory(player);

		// 使用 GScreen 的 worldCamera
		batch.setProjectionMatrix(worldCamera.combined);
		batch.begin();

		// Calculate visible range (optimization)
		int startX = 0;
		int startY = 0;
		int endX = dungeon.width;
		int endY = dungeon.height;

		// Render Map
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				Tile tile = dungeon.getTile(x, y);
				if (tile != null) {
					// Fix: Draw Floor under transparent tiles (Stairs, Door) to show background properly
					if (tile.type == TileType.Stairs_Down || tile.type == TileType.Stairs_Up || tile.type == TileType.Door) {
						Texture floorTex = TextureManager.getInstance().getTile(TileType.Floor);
						if (floorTex != null) {
							batch.draw(floorTex, x * Constants.TILE_SIZE, y * Constants.TILE_SIZE, Constants.TILE_SIZE, Constants.TILE_SIZE);
						}
					}

					Texture texture = TextureManager.getInstance().getTile(tile.type);
					if (texture != null) {
						batch.draw(texture, x * Constants.TILE_SIZE, y * Constants.TILE_SIZE, Constants.TILE_SIZE, Constants.TILE_SIZE);
					}
				}
			}
		}

		// Render Items
		for (Item item : items) {
			Texture itemTex = TextureManager.getInstance().get(item.data.name());
			if (itemTex != null) {
				// Render slightly smaller to distinguish from tiles
				batch.draw(itemTex, item.visualX + 4, item.visualY + 4, 24, 24);
			}
		}

		// Render Monsters
		for (Monster m : monsters) {
			if (m.hp > 0) {
				Texture mTex = TextureManager.getInstance().get(m.type.name());
				if (mTex == null) {
					mTex = TextureManager.getInstance().get(MonsterType.Slime.name());
				}

				if (mTex != null) {
					// Apply hit flash effect if needed
					boolean appliedFlash = m.applyHitFlash();
					if (appliedFlash) {
						batch.setColor(1f, 1f, 1f, 0.5f); // 半透明效果
					}
					batch.draw(mTex, m.visualX + m.bumpX, m.visualY + m.bumpY, Constants.TILE_SIZE, Constants.TILE_SIZE);
					// Reset color if we changed it
					if (appliedFlash) {
						batch.setColor(1f, 1f, 1f, 1f);
					}
				}
			}
		}

		// Render Player
		Texture playerTex = TextureManager.getInstance().getPlayer();
		if (playerTex != null) {
			// Apply hit flash effect using base class method
			boolean appliedFlash = player.applyHitFlash();
			if (appliedFlash) {
				batch.setColor(1f, 1f, 1f, 0.5f); // 半透明效果
			}
			batch.draw(playerTex, player.visualX + player.bumpX, player.visualY + player.bumpY, Constants.TILE_SIZE, Constants.TILE_SIZE);
			if (appliedFlash) {
				batch.setColor(1f, 1f, 1f, 1f);
			}
		}

		batch.end();

		// HUD Render
		hud.render();
	}

	public void loadGame() {
		GameState state = SaveManager.loadGame();
		if (state != null) {
			player.stats = state.playerStats;
			player.inventory = state.inventory;
			dungeon.level = state.dungeonLevel;
			dungeon.globalSeed = state.seed; // Restore seed
			
			// Restore Equipment
			if (state.equipment != null) {
				player.equipment.weapon = state.equipment.weapon;
				player.equipment.armor = state.equipment.armor;
			}
			
			// Restore History
			if (state.visitedLevels != null) {
				visitedLevels = state.visitedLevels;
			} else {
				visitedLevels = new HashMap<>();
			}

			// Regenerate world
			dungeon.generate();
			
			// Restore Player Position
			// If save has coordinates (new save), use them. Otherwise (old save), use startPos.
			if (state.playerX != 0 || state.playerY != 0) {
				player.x = state.playerX;
				player.y = state.playerY;
			} else {
				player.x = dungeon.startPos.x;
				player.y = dungeon.startPos.y;
			}
			
			player.visualX = player.x * Constants.TILE_SIZE;
			player.visualY = player.y * Constants.TILE_SIZE;
			
			// Restore Entities
			if (state.monsters != null && state.items != null) {
				// New save system: restore from snapshot
				monsters.clear();
				for(MonsterState ms : state.monsters) {
					MonsterType type = MonsterType.Slime;
					try {
						// Try to find by Enum name first
						type = MonsterType.valueOf(ms.typeName);
					} catch(IllegalArgumentException e) {
						// Fallback: search by display name (chinese) or just default
						for(MonsterType t : MonsterType.values()) {
							if(t.name.equals(ms.typeName)) {
								type = t;
								break;
							}
						}
					}
					
					Monster m = new Monster(ms.x, ms.y, type);
					m.hp = ms.hp;
					m.maxHp = ms.maxHp;
					monsters.add(m);
				}
				
				items.clear();
				for(ItemState is : state.items) {
					ItemData data = ItemData.Health_Potion;
					try {
						data = ItemData.valueOf(is.itemName);
					} catch(IllegalArgumentException e) {
						// Fallback logic if needed
					}
					items.add(new Item(is.x, is.y, data));
				}
				
			} else {
				// Old save system compatibility: regenerate randomly
				spawnEntities();
			}

			hud.showMessage("Game Loaded!");
		} else {
			hud.showMessage("No Save Found!");
		}
	}
}
