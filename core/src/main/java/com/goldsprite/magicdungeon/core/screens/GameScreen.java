package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.gdengine.PlatformImpl;
import com.goldsprite.gdengine.assets.VisUIHelper;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.GScreen;
import com.goldsprite.gdengine.utils.SimpleCameraController;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.entities.*;
import com.goldsprite.magicdungeon.entities.InventoryItem;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.ui.GameHUD;
import com.goldsprite.magicdungeon.utils.Constants;
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
import com.goldsprite.magicdungeon.model.LayerData;
import com.goldsprite.magicdungeon.model.PlayerData;
import com.goldsprite.magicdungeon.model.SaveData;
import com.goldsprite.magicdungeon.utils.MapIO;
import com.goldsprite.magicdungeon.core.EquipmentState;

import com.goldsprite.magicdungeon.input.InputManager;
import com.goldsprite.magicdungeon.input.InputAction;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.goldsprite.magicdungeon.ui.ItemRenderer;
import com.goldsprite.magicdungeon.core.renderer.DualGridDungeonRenderer;
import com.esotericsoftware.spine.AnimationState;
import com.esotericsoftware.spine.AnimationStateData;
import com.esotericsoftware.spine.Skeleton;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonRenderer;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.esotericsoftware.spine.Animation;
import com.goldsprite.magicdungeon.screens.WorldMapScreen;
import com.goldsprite.magicdungeon.world.DungeonTheme;
import com.goldsprite.magicdungeon.assets.AudioAssets;
import com.goldsprite.magicdungeon.assets.SpineManager;
import com.goldsprite.magicdungeon.vfx.VFXManager;
import com.goldsprite.magicdungeon.progress.ProgressScreen;

import com.goldsprite.magicdungeon.world.data.GameMapData;
import com.goldsprite.magicdungeon.world.data.MapEntityData;
import com.goldsprite.magicdungeon.world.data.MapItemData;

public class GameScreen extends GScreen {
	private Dungeon dungeon;
	private DualGridDungeonRenderer dungeonRenderer;
	private Player player;
	private List<Monster> monsters;
	private List<Item> items;
	private List<Chest> chests;
	// Removed redundant viewport and camera
	private GameHUD hud;
	private AudioSystem audio;
	private NeonBatch batch;
	private PolygonSpriteBatch polyBatch;
	private long seed;

    private VFXManager vfxManager;

	private int maxDepth = 1;

	// History of visited levels
    // Deprecated: Visited levels are now saved to disk immediately via SaveManager
	// private Map<Integer, LevelState> visitedLevels = new HashMap<>();

	private String cheatCodeBuffer = "";
	public static boolean isPaused = false;
	private boolean isGameOver = false;

	private boolean wasAttackPressed = false;
	private boolean wasInteractPressed = false;

	// Spine Resources
	private SkeletonRenderer spineRenderer;
	private SkeletonData wolfSkeletonData;

	private static class SpineState {
		Skeleton skeleton;
		AnimationState state;
	}

    private String saveName = "default_save";

    public GameScreen() {
        this(MathUtils.random(Long.MIN_VALUE, Long.MAX_VALUE));
    }

    public GameScreen(String saveName) {
        this(MathUtils.random(Long.MIN_VALUE, Long.MAX_VALUE));
        this.saveName = saveName;
    }

    public GameScreen(long seed) {
        this.seed = seed;
    }

	public Player getPlayer() {
		return player;
	}

	public GameHUD getHud() {
		return hud;
	}
	
	@Override
	public Stage getStage() {
	    return hud != null ? hud.getStage() : null;
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
		this.worldScale = PlatformImpl.isDesktopUser() ? 0.3f : 0.36f;

		this.uiViewportScale = PlatformImpl.isDesktopUser() ? 1 : 1.4f;

		super.initViewport();

		// Camera Controller
		SimpleCameraController controller = new SimpleCameraController(worldCamera);
		controller.setCoordinateMapper((x, y) -> uiViewport.unproject(new Vector2(x, y)));
//		controller.setActivationCondition(() -> ); // 这里需要完成逻辑: 仅在光标落在ui区域外时, 注意SplitPane的空面板也需要识别为落空在ui区域外(好像可以用GSplitPane他处理了这种问题, 查看分析源码来确认这一点)
		getImp().addProcessor(controller);
	}

	@Override
	public void create() {
		batch = new NeonBatch();
		polyBatch = new PolygonSpriteBatch();
		dungeonRenderer = new DualGridDungeonRenderer();
        vfxManager = new VFXManager();

		// Spine Init
		spineRenderer = new SkeletonRenderer();
		spineRenderer.setPremultipliedAlpha(false);

		wolfSkeletonData = SpineManager.getInstance().get("Wolf");
		if (wolfSkeletonData == null) {
			Gdx.app.error("GameScreen", "Wolf spine data not found!");
		}

		System.out.println("GameScreen Constructor Started");
		this.dungeon = new Dungeon(50, 50, seed);
		// IMPORTANT: Set level to 0 immediately to prevent enterCamp() from saving an empty Level 1 state
		this.dungeon.level = 0;

		this.player = new Player(0, 0); // Temp pos, will be set by enterCamp
		this.player.visualX = this.player.x * Constants.TILE_SIZE;
		this.player.visualY = this.player.y * Constants.TILE_SIZE;

		this.monsters = new ArrayList<>();
		this.items = new ArrayList<>();
		this.chests = new ArrayList<>();

		// Start Game
		loadGame();
		
		// If level is 0, ensure we are in Camp mode correctly
		if (dungeon.level == 0) {
		    enterCamp(false);
		}

		// Ensure initial visual is generated
		player.updateVisuals();

		// Scene2D HUD
		// 传递 uiViewport 给 HUD
		hud = new GameHUD(this);

		// Set save listener for HUD save button
		hud.setSaveListener(() -> {
			saveGameData();
			hud.showMessage("游戏已保存!");
		});

		hud.setReturnToCampListener(() -> {
			enterCamp(false);
		});
		getImp().addProcessor(hud.stage);

		// Audio
		audio = AudioSystem.getInstance();
		// Initial Music (Camp)
		audio.playMusic(AudioAssets.MUSIC_QUANTUM);

		// 初始化相机位置
		updateCamera();

		System.out.println("GameScreen Constructor Finished");
	}

	private void saveGameData() {
		if (saveName == null) return;

		// 1. Save Meta
		SaveData meta = new SaveData();
		meta.saveName = saveName;
		meta.currentPlayerName = player.name;
		
		// Better load meta first to preserve createTime
		SaveData oldMeta = SaveManager.loadSaveMeta(saveName);
		if (oldMeta != null) {
			meta.createTime = oldMeta.createTime;
		} else {
			meta.createTime = System.currentTimeMillis();
		}
		meta.lastPlayedTime = System.currentTimeMillis();
		meta.currentFloor = dungeon.level;
		meta.currentAreaId = dungeon.areaId;
		meta.maxDepth = this.maxDepth;
		SaveManager.saveSaveMeta(meta);

		// 2. Save Player Data
		EquipmentState equipState = new EquipmentState();
		equipState.mainHand = player.equipment.mainHand;
		equipState.offHand = player.equipment.offHand;
		equipState.helmet = player.equipment.helmet;
		equipState.armor = player.equipment.armor;
		equipState.boots = player.equipment.boots;
		equipState.accessories = player.equipment.accessories;

		PlayerData pd = new PlayerData(
			player.name,
			player.stats,
			player.inventory,
			equipState,
			player.x,
			player.y
		);
		SaveManager.savePlayerData(saveName, pd);

		// 3. Save Current Level Data
		saveCurrentLevelState();
	}

	private void saveCurrentLevelState() {
		// Save current level state to file
		// if (dungeon.level == 0) return; // Allow saving camp too! User said so.

		List<MonsterState> monsterStates = new ArrayList<>();
		for (Monster m : monsters) {
			if (m.hp > 0) {
				monsterStates.add(new MonsterState(m.x, m.y, m.type.name(), m.hp, m.maxHp));
			}
		}

		List<ItemState> itemStates = new ArrayList<>();
		for (Item item : items) {
			itemStates.add(new ItemState(
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

		// Build LayerData
		LayerData data = MapIO.fromTileMap(dungeon.map, dungeon.width, dungeon.height);
		data.monsters = monsterStates;
		data.items = itemStates;

		SaveManager.saveLayerData(saveName, dungeon.areaId, dungeon.level, data);
	}

	private void enterCamp(boolean fromStairs) {
		boolean wasInDungeon = dungeon.level > 0;
		if (wasInDungeon) {
			saveCurrentLevelState();
		}

		dungeon.level = 0;
        dungeon.areaId = "camp"; // Switch to Camp Area
		loadCurrentLevel();

		if (fromStairs && wasInDungeon) {
			// Find Dungeon Entrance position
			// Assuming loadCurrentLevel loaded the map
			GridPoint2 entPos = findTilePosition(TileType.Dungeon_Entrance);
			if (entPos != null) {
				player.x = entPos.x;
				player.y = entPos.y;
			} else {
				player.x = dungeon.startPos.x;
				player.y = dungeon.startPos.y;
			}
		} else {
			// If just started or teleported, use start pos
			// But if we loaded a save, player pos is already set in loadGame()
			// However, enterCamp is called when transitioning.
			// If fromStairs=false, it means "Return to Camp" spell or similar?
			player.x = dungeon.startPos.x;
			player.y = dungeon.startPos.y;
		}

		player.visualX = player.x * Constants.TILE_SIZE;
		player.visualY = player.y * Constants.TILE_SIZE;

		updateCamera();

		if (hud != null) hud.showMessage("回到了营地.");
		if (audio != null) audio.playMusic(AudioAssets.MUSIC_QUANTUM);

		updateHUDDungeonInfo();
	}

	public void enterDungeonFromMap(WorldMapScreen.DungeonNode node) {
		rebuildScene(node, null);
	}

    /**
     * 重构场景 (In-Place Rebuild)
     * 用于在不销毁 GameScreen 实例的情况下切换关卡/区域
     */
    public void rebuildScene(WorldMapScreen.DungeonNode node, Runnable onFinish) {
        Gdx.app.postRunnable(() -> {
            try {
                dungeon.level = Math.max(1, node.minLv);
                dungeon.areaId = node.id; // Set correct Area ID
                dungeon.theme = node.theme;
                dungeon.generate();

                player.x = dungeon.startPos.x;
                player.y = dungeon.startPos.y;
                player.visualX = player.x * Constants.TILE_SIZE;
                player.visualY = player.y * Constants.TILE_SIZE;

                monsters.clear();
                items.clear();
                chests.clear();

                spawnEntities();
                updateCamera();

                hud.showMessage("进入了 " + node.name);
                
                // Update BGM
                if (audio != null) {
                    if (dungeon.level % 5 == 0) audio.playMusic(AudioAssets.MUSIC_TAKE_COVER);
                    else audio.playMusic(AudioAssets.MUSIC_LASER_QUEST);
                }
                
                if (onFinish != null) onFinish.run();
            } catch (Exception e) {
                Gdx.app.error("GameScreen", "Failed to rebuild scene", e);
                if (onFinish != null) onFinish.run();
            }
        });
    }

	private void enterDungeon(int level) {
		int prevLevel = dungeon.level;
		if (dungeon.level >= 0) {
			saveCurrentLevelState();
		}

		dungeon.level = level;

		loadCurrentLevel();

		// Set Player Position
		if (level < prevLevel) {
			// Going UP (e.g. 2 -> 1): Spawn at Stairs Down
			GridPoint2 downPos = findTilePosition(TileType.Stairs_Down);
			if (downPos != null) {
				player.x = downPos.x;
				player.y = downPos.y;
			} else {
				player.x = dungeon.startPos.x; // Fallback
				player.y = dungeon.startPos.y;
			}
		} else {
			// Going DOWN (e.g. 1 -> 2) or from Camp (0 -> X): Spawn at Start (Stairs Up)
			player.x = dungeon.startPos.x;
			player.y = dungeon.startPos.y;
		}

		player.visualX = player.x * Constants.TILE_SIZE;
		player.visualY = player.y * Constants.TILE_SIZE;

		updateCamera();

		if (level > prevLevel) {
			hud.showMessage("Descended to Floor " + level);
			if (audio != null) {
				if (level % 5 == 0) audio.playMusic(AudioAssets.MUSIC_TAKE_COVER);
				else audio.playMusic(AudioAssets.MUSIC_LASER_QUEST);
			}
		} else {
			hud.showMessage("Ascended to Floor " + level);
			if (audio != null) audio.playLevelUp();
		}

		if (level > maxDepth) maxDepth = level;
	}

	private GridPoint2 findTilePosition(TileType type) {
		for (int y = 0; y < dungeon.height; y++) {
			for (int x = 0; x < dungeon.width; x++) {
				Tile t = dungeon.getTile(x, y);
				if (t != null && t.type == type) {
					return new GridPoint2(x, y);
				}
			}
		}
		return null;
	}

	private void returnToCamp() {
		// Check conditions:
		// 1. Must be in Dungeon (Level > 0)
		if (dungeon.level == 0) {
			hud.showMessage("你已经在营地了。");
			return;
		}

		// 2. Must be on Stairs Up (changed from Stairs Down)
		Tile tile = dungeon.getTile(player.x, player.y);
		if (tile == null || tile.type != TileType.Stairs_Up) {
			hud.showMessage("你需要站在上层入口处才能返回营地。");
			return;
		}

		// 3. No monsters in room?
		// Simple check: visible range? or active monsters?
		// For simplicity, check if any monster is within 3 tiles.
		boolean safe = true;
		for (Monster m : monsters) {
			if (m.hp > 0 && Math.abs(m.x - player.x) < 3 && Math.abs(m.y - player.y) < 3) {
				safe = false;
				break;
			}
		}

		if (!safe) {
			hud.showMessage("附近有怪物，无法传送!");
			return;
		}

		enterCamp(false);
	}






    private void nextLevel() {
        saveCurrentLevelState();

        dungeon.level++;
        if (dungeon.level > maxDepth) maxDepth = dungeon.level;

        loadCurrentLevel();

        // Place player at stairs up
        GridPoint2 stairsPos = findTilePosition(TileType.Stairs_Up);
        if (stairsPos != null) {
            player.x = stairsPos.x;
            player.y = stairsPos.y;
        } else {
            player.x = dungeon.startPos.x;
            player.y = dungeon.startPos.y;
        }
        
        player.visualX = player.x * Constants.TILE_SIZE;
        player.visualY = player.y * Constants.TILE_SIZE;

        updateCamera();
        updateHUDDungeonInfo();
        playCurrentBGM();
    }

    private void prevLevel() {
        if (dungeon.level <= 0) return;

        saveCurrentLevelState();

        dungeon.level--;

        if (dungeon.level == 0) {
            enterCamp(true);
            return;
        }

        loadCurrentLevel();

        // Place player at stairs down
        GridPoint2 stairsPos = findTilePosition(TileType.Stairs_Down);
        if (stairsPos != null) {
            player.x = stairsPos.x;
            player.y = stairsPos.y;
        } else {
            player.x = dungeon.startPos.x;
            player.y = dungeon.startPos.y;
        }
        
        player.visualX = player.x * Constants.TILE_SIZE;
        player.visualY = player.y * Constants.TILE_SIZE;

        updateCamera();
        updateHUDDungeonInfo();
        playCurrentBGM();
    }

	private void spawnEntities() {
        monsters.clear();
        items.clear();

        // 1. Load from Data (Editor generated)
        if (dungeon.loadedMapData != null) {
            GameMapData data = dungeon.loadedMapData;
            
            // Spawn Monsters
            if (data.entities != null) {
                for(MapEntityData ed : data.entities) {
                    try {
                        MonsterType type = MonsterType.valueOf(ed.type);
                        Monster m = new Monster(ed.x, ed.y, type);
                        m.visualX = ed.x * Constants.TILE_SIZE;
                        m.visualY = ed.y * Constants.TILE_SIZE;
                        monsters.add(m);
                    } catch(Exception e) {
                         Gdx.app.error("GameScreen", "Failed to spawn entity: " + ed.type);
                    }
                }
            }
            
            // Spawn Items
            if (data.items != null) {
                for(MapItemData id : data.items) {
                     try {
                        ItemData iData = ItemData.valueOf(id.itemId);
                        InventoryItem ii = new InventoryItem(iData);
                        Item item = new Item(id.x, id.y, ii);
                        item.visualX = id.x * Constants.TILE_SIZE;
                        item.visualY = id.y * Constants.TILE_SIZE;
                        items.add(item);
                     } catch(Exception e) {
                        Gdx.app.error("GameScreen", "Failed to spawn item: " + id.itemId);
                     }
                }
            }
            return;
        }

		if (dungeon.level == 0) return;

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

				// Use theme allowed monsters
				MonsterType[] allowed = dungeon.theme.allowedMonsters;
				if (allowed == null || allowed.length == 0) {
					allowed = MonsterType.values();
				}

				MonsterType type = allowed[monsterRng.nextInt(allowed.length)];

				// Handle Boss Logic
				if (type == MonsterType.Boss) {
					boolean isBossLevel = (dungeon.level % 6 == 0);
					if (!isBossLevel) {
						// Reroll
						for(int k=0; k<5; k++) {
							if (type != MonsterType.Boss) break;
							type = allowed[monsterRng.nextInt(allowed.length)];
						}
					} else {
						// Boss Level: Ensure only 1 boss or rare
						boolean hasBoss = false;
						for(Monster m : monsters) if(m.type == MonsterType.Boss) hasBoss = true;
						if(hasBoss) {
							for(int k=0; k<5; k++) {
								if (type != MonsterType.Boss) break;
								type = allowed[monsterRng.nextInt(allowed.length)];
							}
						}
					}
				}

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

				items.add(new Item(pos.x, pos.y, new InventoryItem(itemData, itemRng)));
			}
		}

		// Chests
		chests.clear();
		int numChests = 2 + (dungeon.level / 2);
		List<ItemData> rareLoot = new ArrayList<>();
		for (ItemData d : ItemData.values()) {
			// Exclude common items already spawned on floor (optional, but requested to spawn "new items")
			if (d == ItemData.Health_Potion || d == ItemData.Mana_Potion || d == ItemData.Rusty_Sword) continue;
			rareLoot.add(d);
		}

		for (int i = 0; i < numChests; i++) {
			GridPoint2 pos = dungeon.getRandomWalkableTile(itemRng);
			if (pos != null) {
				// Avoid spawning on player
				if (pos.x == player.x && pos.y == player.y) continue;

				// Avoid spawning on other entities (simple check)
				boolean overlap = false;
				for (Monster m : monsters) if (m.x == pos.x && m.y == pos.y) overlap = true;
				for (Item it : items) if (it.x == pos.x && it.y == pos.y) overlap = true;
				for (Chest c : chests) if (c.x == pos.x && c.y == pos.y) overlap = true;

				if (!overlap) {
					Chest chest = new Chest(pos.x, pos.y);
					// Add 1-3 random items
					int lootCount = 1 + itemRng.nextInt(3);
					for (int j = 0; j < lootCount; j++) {
						if (!rareLoot.isEmpty()) {
							ItemData lootData = rareLoot.get(itemRng.nextInt(rareLoot.size()));
							chest.addItem(new InventoryItem(lootData, itemRng));
						}
					}
					// Always add some coins
					if (itemRng.nextFloat() < 0.5f) {
						InventoryItem coins = new InventoryItem(ItemData.Gold_Coin, itemRng);
						coins.count = 10 + itemRng.nextInt(50);
						chest.addItem(coins);
					}

					chests.add(chest);
				}
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
	public boolean handleBackKey() {
		return hud.handleBackKey();
	}

	private void updateHUDDungeonInfo() {
		if (hud == null || dungeon == null) return;
		float difficulty = 1.0f;
		if (dungeon.level > 0) {
			difficulty = 1.0f + (dungeon.level - 1) * 0.2f;
		}
		String themeName = dungeon.theme != null ? dungeon.theme.name : "未知";
		hud.updateDungeonInfo(dungeon.level, themeName, difficulty);
	}

	@Override
	public void render(float delta) {
		handleInput(delta);

		if (!isPaused && !getScreenManager().isTransitioning()) {
			updateLogic(delta);
		}

		draw(delta);
	}

	private void handleInput(float delta) {
		if (getScreenManager().isTransitioning()) return;

        InputManager input = InputManager.getInstance();

		// Toggle Pause
		if (input.isJustPressed(InputAction.PAUSE)) {
			isPaused = !isPaused;
			hud.setPaused(isPaused);
			// Optional: Pause audio or other systems
		}

		// Toggle Inventory (Always allowed)
		if (input.isJustPressed(InputAction.BAG)) {
			hud.toggleInventory();
		}

		// Toggle Progress Screen
		if (input.isJustPressed(InputAction.PROGRESS)) {
			getScreenManager().goScreen(ProgressScreen.class);
		}

		// Save Game
        if (input.isJustPressed(InputAction.SAVE)) {
            saveGameData();
            hud.showMessage("游戏已保存!");
        }

        // Load Game (F9 - Default keyboard only for now, not mapped in JSON yet, adding fallback)
        if (input.isJustPressed(InputAction.LOAD_GAME)) {
            loadGame();
        }

		// Regenerate map (Reset) - R (Keyboard only for now)
		if (input.isJustPressed(InputAction.RESET_MAP)) {
			dungeon.generate();
			player.x = dungeon.startPos.x;
			player.y = dungeon.startPos.y;
			player.visualX = player.x * Constants.TILE_SIZE;
			player.visualY = player.y * Constants.TILE_SIZE;
			player.stats.hp = player.stats.maxHp;
			spawnEntities();
			hud.showMessage("地图已重置!");
		}

		// Cheat Codes
		handleCheatInput();

		// Mouse Click Selection
		if (Gdx.input.justTouched()) {
			// Convert screen coordinates to world coordinates
			Vector3 touchPos = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
			worldCamera.unproject(touchPos);

			float wx = touchPos.x;
			float wy = touchPos.y;

			// Check collisions with Monsters
			boolean clickedMonster = false;
			for (Monster m : monsters) {
				if (m.hp > 0) {
					// Monster visual area
					float mx = m.visualX;
					float my = m.visualY;
					// Add some tolerance or use exact tile bounds
					if (wx >= mx && wx < mx + Constants.TILE_SIZE &&
						wy >= my && wy < my + Constants.TILE_SIZE) {
						hud.showMonsterInfo(m);
						clickedMonster = true;
						break;
					}
				}
			}

			// If clicked elsewhere, maybe hide info?
			// But user might want to keep it open while fighting.
			// Let's keep it open unless explicitly closed or clicked on empty space?
			// For now, only open on click.
		}
	}

	private void handleCheatInput() {
		if (InputManager.getInstance().isKeyJustPressed(Input.Keys.GRAVE)) {
			// Placeholder
		}

		if (InputManager.getInstance().isKeyJustPressed(Input.Keys.NUMPAD_6) || InputManager.getInstance().isKeyJustPressed(Input.Keys.NUM_6)) {
			cheatCodeBuffer += "6";
		} else if (InputManager.getInstance().isKeyJustPressed(Input.Keys.C)) {
			cheatCodeBuffer += "c";
		} else if (InputManager.getInstance().isKeyJustPressed(Input.Keys.H)) {
			cheatCodeBuffer += "h";
		} else if (InputManager.getInstance().isKeyJustPressed(Input.Keys.E)) {
			cheatCodeBuffer += "e";
		} else if (InputManager.getInstance().isKeyJustPressed(Input.Keys.A)) {
			cheatCodeBuffer += "a";
		} else if (InputManager.getInstance().isKeyJustPressed(Input.Keys.T)) {
			cheatCodeBuffer += "t";
		}

		if (cheatCodeBuffer.endsWith("cheat666")) {
			cheatCodeBuffer = "";
			for (ItemData data : ItemData.values()) {
				for (ItemQuality quality : ItemQuality.values()) {
					int atk = Math.max(0, Math.round(data.atk * quality.multiplier));
					int def = Math.max(0, Math.round(data.def * quality.multiplier));
					int heal = Math.max(0, Math.round(data.heal * quality.multiplier));
					int manaRegen = 0;
					if (data.name().contains("Mana") || data.name().contains("Magic") || data.name().contains("Ring")) {
						manaRegen = Math.max(1, Math.round(1 * quality.multiplier));
					}
					player.inventory.add(new InventoryItem(data, quality, atk, def, heal, manaRegen));
				}
			}
			hud.showMessage("作弊已激活: 所有品质物品已添加!");
			hud.updateInventory(player);
			audio.playLevelUp();
		}
		if (cheatCodeBuffer.length() > 20) cheatCodeBuffer = "";
	}

	private void updateLogic(float delta) {
		if (isGameOver) {
			// Even if game over, we must update HUD so stage can act (process input/animations for dialogs)
			hud.update(player, dungeon.level);
			return;
		}

		InputManager input = InputManager.getInstance();

		// Game Input Handling (WASD)
		int dx = 0;
		int dy = 0;

		// Only allow movement if NO modal UI is open
		if (!hud.hasModalUI()) {
			if (input.isPressed(InputAction.MOVE_LEFT)) dx = -1;
			if (input.isPressed(InputAction.MOVE_RIGHT)) dx = 1;
			if (input.isPressed(InputAction.MOVE_UP)) dy = 1;
			if (input.isPressed(InputAction.MOVE_DOWN)) dy = -1;

			// Android Touchpad Support
			Vector2 pad = hud.getMovementDirection();
			if (pad.len() > 0.3f) {
				if (Math.abs(pad.x) > Math.abs(pad.y)) {
					dx = pad.x > 0 ? 1 : -1;
					dy = 0;
				} else {
					dy = pad.y > 0 ? 1 : -1;
					dx = 0;
				}
			}
		}

		if (dx != 0 || dy != 0) {
			int nextX = player.x + dx;
			int nextY = player.y + dy;

			// Auto-select target monster if attacking
			for (Monster m : monsters) {
				if (m.hp > 0 && m.x == nextX && m.y == nextY) {
					hud.showMonsterInfo(m);
					break;
				}
			}

			audio.playMove();
		}

		// Interact / Next Level - Space (Changed from E)
		boolean isInteractPressed = input.isJustPressed(InputAction.INTERACT);
		boolean isInteractBtnJustPressed = hud.isInteractPressed() && !wasInteractPressed;
		wasInteractPressed = hud.isInteractPressed();

		boolean interactTriggered = isInteractPressed || isInteractBtnJustPressed;
		boolean handledInteract = false;

		if (interactTriggered) {
			Tile tile = dungeon.getTile(player.x, player.y);
			if (tile != null) {
				if (tile.type == TileType.Stairs_Down) {
					// Go deeper
                    getScreenManager().playLoadingTransition((finishCallback) -> {
                        enterDungeon(dungeon.level + 1);
                        finishCallback.run();
                    }, "进入下层 (Lv." + (dungeon.level + 1) + ")...", 1.0f);
					handledInteract = true;
				} else if (tile.type == TileType.Stairs_Up) {
					// Go back up
					if (dungeon.level > 1) {
                        getScreenManager().playLoadingTransition((finishCallback) -> {
                            enterDungeon(dungeon.level - 1);
                            finishCallback.run();
                        }, "返回上层 (Lv." + (dungeon.level - 1) + ")...", 1.0f);
					} else {
						// Level 1 -> Camp
                        getScreenManager().playLoadingTransition((finishCallback) -> {
                            enterCamp(true);
                            finishCallback.run();
                        }, "返回营地...", 1.0f);
					}
					handledInteract = true;
				} else if (tile.type == TileType.Dungeon_Entrance) {
					// Save current progress before switching to World Map
					saveGameData();

					// Switch to WorldMapScreen with Fade Transition
					getScreenManager().playTransition(() -> {
                        WorldMapScreen mapScreen = new WorldMapScreen((node) -> {
                             // Callback when area selected
                             
                             // 2. Play Loading Transition & Rebuild Scene
                             // Update tip text
                             String areaName = node.name; // Assuming node has name
                             getScreenManager().playLoadingTransition((finishCallback) -> {
                                 // 1. Return to GameScreen (pop map) - Do this while black
                                 getScreenManager().popLastScreen();
                                 
                                 rebuildScene(node, finishCallback);
                             }, "去往 " + areaName + " 中...", 1.5f);
                         });
                         getScreenManager().goScreen(mapScreen);
                     });
					
					handledInteract = true;
				}
			}

			// Check Chests (Interact)
			for (Chest chest : chests) {
				if (chest.x == player.x && chest.y == player.y) {
					hud.showChestDialog(chest, player);
					chest.isOpen = true;
					handledInteract = true;
					break;
				}
			}
		}

		// Use Potion (Health) - H (Changed from SPACE)
		boolean isSkillKeyPressed = input.isJustPressed(InputAction.SKILL);
		boolean isAttackBtnJustPressed = hud.isAttackPressed() && !wasAttackPressed;
		wasAttackPressed = hud.isAttackPressed();

		if (isSkillKeyPressed || isAttackBtnJustPressed) {
			// Simple heal logic (if no items)
			if (player.useSkill(audio)) {
				hud.showMessage("使用了治疗术! 回复了 20 点生命!");
			} else {
				hud.showMessage("法力不足!");
			}
		}

        // Map
        if (input.isJustPressed(InputAction.MAP)) {
            // Placeholder for Map
            hud.showMessage("Map not implemented yet.");
        }

		// Update Player
		player.update(delta, dungeon, dx, dy, monsters, audio, vfxManager);


		// Item Pickup
		for (int i = 0; i < items.size(); i++) {
			Item item = items.get(i);
			if (item.x == player.x && item.y == player.y) {
				// Apply Item Effect
				boolean added = player.addItem(item.item);
				if (added) {
					items.remove(i);
					i--;
					audio.playItem();
					hud.showMessage("拾取了 [" + item.item.quality.name + "] " + item.item.data.name + "!");

                    if (vfxManager != null) {
                        vfxManager.spawnFloatingText(player.visualX + 16, player.visualY + 32, item.item.data.name, Color.GREEN);
                    }

					// Update inventory dialog if it's open
					hud.updateInventory(player);
				} else {
					hud.showMessage("背包已满!");
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
				hud.showMessage("受到来自 " + m.name + " 的 " + damage + " 点伤害!");

                if (vfxManager != null) {
                    vfxManager.spawnExplosion(player.visualX + 16, player.visualY + 16, Color.RED, 10);
                    vfxManager.spawnFloatingText(player.visualX + 16, player.visualY + 32, "-" + damage, Color.RED);
                }

				if (player.stats.hp <= 0) {
					triggerGameOver();
				}
			}
			m.updateVisuals(delta);
		}

		// Update Camera
		updateCamera();

		// Update HUD Data
		hud.update(player, dungeon.level);
	}

	private void triggerGameOver() {
		if (isGameOver) return;
		isGameOver = true;

		// Play Game Over Sound and Stop BGM
		if (audio != null) {
			audio.stopBGM();
			audio.playGameOver();
		}

		hud.showGameOver(new Runnable() {
			@Override
			public void run() {
				// Restart (Respawn at Camp with reset progress)
                getScreenManager().playLoadingTransition((finishCallback) -> {
					isGameOver = false;
					playCurrentBGM();

					// [修改] 死亡惩罚: 掉级掉装备，而不是重置
					player.applyDeathPenalty();

					// Reset World History
					maxDepth = 1;

					// Enter Camp
					enterCamp(false);

					hud.showMessage("你已复活。等级和物品已掉落。");
                    finishCallback.run();
                }, "复活中...", 1.0f);
			}
		}, new Runnable() {
			@Override
			public void run() {
				// Quit
				Gdx.app.postRunnable(new Runnable() {
					@Override
					public void run() {
						getScreenManager().playTransition(() -> {
							getScreenManager().goScreen(new MainMenuScreen());
						});
					}
				});
			}
		});
	}

	private void restartGame() {
		isGameOver = false;
		isPaused = false;

		// 1. Reset Dungeon World
		dungeon.level = 1;
		dungeon.globalSeed = System.currentTimeMillis(); // New random seed
		dungeon.generate();

		// 2. Clear History
		// visitedLevels.clear(); // Deprecated

		// 3. Reset Player
		// Re-create player to reset all stats and inventory
		player = new Player(dungeon.startPos.x, dungeon.startPos.y);
		player.visualX = player.x * Constants.TILE_SIZE;
		player.visualY = player.y * Constants.TILE_SIZE;

		// 4. Reset Entities
		spawnEntities();

		// 5. Update UI
		hud.reset();
		hud.showMessage("游戏重新开始!");
		// Important: Update HUD with new player instance
		hud.update(player, dungeon.level);
		// If HUD caches player reference (it does: currentPlayer), update ensures it's refreshed.
		// However, check if HUD has other references that need update.
		// HUD.update() does: this.currentPlayer = player; so it's fine.
		// But inventory dialog? toggleInventory() uses currentPlayer, so it's fine.
	}

	float wolfScl = 0.75f;
	private void renderSpineMonster(PolygonSpriteBatch polyBatch, Monster m, float delta) {
		SpineState spineState = (SpineState) m.visualState;

		if (spineState == null) {
			try {
				Skeleton skeleton = new Skeleton(wolfSkeletonData);
				AnimationStateData stateData = new AnimationStateData(wolfSkeletonData);
				AnimationState state = new AnimationState(stateData);

				// Try to find idle animation
				String defaultAnim = "idle";
				if (wolfSkeletonData.findAnimation(defaultAnim) == null) {
					for(Animation anim : wolfSkeletonData.getAnimations()) {
						if (anim.getName().toLowerCase().contains("idle") || anim.getName().toLowerCase().contains("stand")) {
							defaultAnim = anim.getName();
							break;
						}
					}
					if (wolfSkeletonData.findAnimation(defaultAnim) == null && wolfSkeletonData.getAnimations().size > 0) {
						defaultAnim = wolfSkeletonData.getAnimations().get(0).getName();
					}
				}

				if (defaultAnim != null && wolfSkeletonData.findAnimation(defaultAnim) != null) {
					state.setAnimation(0, defaultAnim, true);
				}

				spineState = new SpineState();
				spineState.skeleton = skeleton;
				spineState.state = state;
				m.visualState = spineState;
				spineState.skeleton.setScale(wolfScl, wolfScl);
			} catch (Exception e) {
				Gdx.app.error("GameScreen", "Failed to initialize Spine for Wolf", e);
				wolfSkeletonData = null; // Disable Spine rendering for Wolf globally on error
				return;
			}
		}

		// Position: center bottom
		// TILE_SIZE is 32.
		float drawX = m.visualX + Constants.TILE_SIZE / 2f + m.bumpX;
		float drawY = m.visualY + m.bumpY;

		spineState.skeleton.setPosition(drawX, drawY);

		// Facing direction (Assume asset faces Right)
		if (player.x < m.x) {
			spineState.skeleton.setScaleX(-1*wolfScl); // Face Left
		} else {
			spineState.skeleton.setScaleX(1*wolfScl); // Face Right
		}

		spineState.state.update(delta);
		spineState.state.apply(spineState.skeleton);
		spineState.skeleton.updateWorldTransform();

		// Hit Flash
		if (m.applyHitFlash()) {
			spineState.skeleton.setColor(Color.RED);
		} else {
			spineState.skeleton.setColor(Color.WHITE);
		}

		spineRenderer.draw(polyBatch, spineState.skeleton);
		// Reset batch color
		polyBatch.setColor(Color.WHITE);
	}

	private void draw(float delta) {
		ScreenUtils.clear(0, 0, 0, 1);

		// 使用 GScreen 的 worldCamera
		batch.setProjectionMatrix(worldCamera.combined);
		batch.begin();

		// Calculate visible range (optimization)
		int startX = 0;
		int startY = 0;
		int endX = dungeon.width;
		int endY = dungeon.height;

		// Render Map Terrain (Dual Grid)
		// 使用 DualGridDungeonRenderer 渲染地形（地板和双网格墙体）
		dungeonRenderer.render(batch, dungeon);

		// Render Objects (Walls, Doors, Stairs, etc.)
		for (int y = startY; y < endY; y++) {
			for (int x = startX; x < endX; x++) {
				Tile tile = dungeon.getTile(x, y);
				if (tile != null) {
					// Draw Objects that are NOT handled by Dual Grid (Terrain)
					TileType t = tile.type;
					// 注意：Wall 已经被 DualGrid 接管，这里不再绘制单图块 Wall
					// Torch 和 Window 现在也由 DualGridDungeonRenderer 负责渲染，以使用新纹理
					if (t == TileType.Door || t == TileType.Stairs_Up ||
						t == TileType.Stairs_Down || t == TileType.Dungeon_Entrance ||
						t == TileType.Tree || t == TileType.StonePath ||
						t == TileType.Pillar) {

						TextureRegion texture = TextureManager.getInstance().getTile(t);
						if (texture != null) {
							batch.draw(texture, x * Constants.TILE_SIZE, y * Constants.TILE_SIZE, Constants.TILE_SIZE, Constants.TILE_SIZE);
						}
					}
				}
			}
		}

		// Render Items
		for (Item item : items) {
			// Use unified ItemRenderer for map rendering
			// Center the item slightly in the tile (size 24 vs tile 32)
			ItemRenderer.drawItem(
				batch,
				item.item,
				item.visualX + 4,
				item.visualY + 4,
				24
			);
		}

		// Render Chests
		for (Chest chest : chests) {
			Color c = chest.isOpen ? Color.DARK_GRAY : Color.GOLD;
			// Draw a simple box for chest
			// Using batch.drawRect which uses a white pixel texture internally if configured, or we can use a known texture
			// Assuming drawRect works as used in line 1002
			batch.drawRect(chest.visualX + 4, chest.visualY + 4, 24, 24, 0, 0, c, true);

			// Optional: Draw a "border" or detail to look like a chest
			if (!chest.isOpen) {
				batch.drawRect(chest.visualX + 10, chest.visualY + 10, 12, 4, 0, 0, Color.BLACK, true);
			}
		}

		// Render Monsters
		for (Monster m : monsters) {
			if (m.hp > 0) {
				if (m.type == MonsterType.Wolf && wolfSkeletonData != null) {
					continue;
				}

				TextureRegion mTex = TextureManager.getInstance().get(m.type.name());
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
		TextureRegion playerTex = TextureManager.getInstance().getPlayer();
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

		// If paused, draw overlay
		if (isPaused) {
			batch.setProjectionMatrix(getUICamera().combined);
			batch.drawRect(0, 0, getViewSize().x, getViewSize().y, 0, 0, new Color(0, 0, 0, 0.5f), true);
		}

        // Update and Render VFX
        if (!isPaused) {
            vfxManager.update(delta);
        }
        // Use world camera for VFX
        batch.setProjectionMatrix(worldCamera.combined);
        vfxManager.render(batch);

		batch.end();

		// Render Spine Monsters (PolyBatch)
		polyBatch.setProjectionMatrix(worldCamera.combined);
		polyBatch.begin();
		for (Monster m : monsters) {
			if (m.hp > 0) {
				if (m.type == MonsterType.Wolf && wolfSkeletonData != null) {
					renderSpineMonster(polyBatch, m, delta);
				}
			}
		}
		polyBatch.end();

        // Render VFX Text (Batch)
		batch.setProjectionMatrix(worldCamera.combined);
		batch.begin();
        if (vfxManager != null) {
             vfxManager.renderText(batch, VisUIHelper.cnFont);
        }
		batch.end();

		// HUD Render
		hud.render();
	}

	public void loadGame() {
		// Load Meta
		SaveData meta = SaveManager.loadSaveMeta(saveName);
		if (meta == null) {
			Gdx.app.error("GameScreen", "Save meta not found for: " + saveName);
			return;
		}

		// Load Player Data
		PlayerData pd = SaveManager.loadPlayerData(saveName, meta.currentPlayerName);
		if (pd != null) {
			if (pd.stats != null) {
				player.stats = pd.stats;
			}
			if (pd.inventory != null) {
				player.inventory = pd.inventory;
			}

			// Restore Equipment
			if (pd.equipment != null) {
				player.equipment.mainHand = pd.equipment.mainHand;
				player.equipment.offHand = pd.equipment.offHand;
				player.equipment.helmet = pd.equipment.helmet;
				player.equipment.armor = pd.equipment.armor;
				player.equipment.boots = pd.equipment.boots;
				player.equipment.accessories = pd.equipment.accessories;

				if (player.equipment.accessories == null) {
					player.equipment.accessories = new InventoryItem[3];
				}
			}

			player.x = (int)pd.x;
			player.y = (int)pd.y;
			player.visualX = player.x * Constants.TILE_SIZE;
			player.visualY = player.y * Constants.TILE_SIZE;
		}

		// Set current level
		dungeon.level = meta.currentFloor;
        dungeon.areaId = meta.currentAreaId;
        if (dungeon.areaId == null || dungeon.areaId.isEmpty()) {
            dungeon.areaId = (dungeon.level == 0) ? "camp" : "dungeon";
        }
		this.maxDepth = Math.max(1, meta.maxDepth); // Ensure at least 1

		loadCurrentLevel();

		updateHUDDungeonInfo();
		playCurrentBGM();
	}

	private void loadCurrentLevel() {
		String areaId = dungeon.areaId;

		if (SaveManager.hasLayerData(saveName, areaId, dungeon.level)) {
			LayerData data = SaveManager.loadLayerData(saveName, areaId, dungeon.level);
			if (data != null) {
				// Restore Map
				dungeon.width = data.width;
				dungeon.height = data.height;
				dungeon.map = MapIO.toTileMap(data);

				// Restore Entities
				monsters.clear();
				for (MonsterState ms : data.monsters) {
					MonsterType type = MonsterType.Slime;
					try {
						type = MonsterType.valueOf(ms.typeName);
					} catch (IllegalArgumentException e) {
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
				for (ItemState is : data.items) {
					ItemData iData = null;
					try {
						iData = ItemData.valueOf(is.itemName);
					} catch(Exception e) {
						continue;
					}

					InventoryItem invItem = new InventoryItem(iData);
					invItem.quality = ItemQuality.valueOf(is.quality);
					invItem.atk = is.atk;
					invItem.def = is.def;
					invItem.heal = is.heal;
					invItem.manaRegen = is.manaRegen;
					invItem.count = is.count > 0 ? is.count : 1;

					Item item = new Item(is.x, is.y, invItem);
					items.add(item);
				}

				chests.clear();
				return;
			}
		}

		// If load failed or no data, generate
		dungeon.generate();
		spawnEntities();
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (hud != null) {
			hud.resize(width, height);
		}
	}

	private void playCurrentBGM() {
		if (audio == null) return;
		if (dungeon.level == 0) {
			audio.playMusic(AudioAssets.MUSIC_QUANTUM);
		} else {
			if (dungeon.level % 5 == 0) audio.playMusic(AudioAssets.MUSIC_TAKE_COVER);
			else audio.playMusic(AudioAssets.MUSIC_LASER_QUEST);
		}
	}

	@Override
	public void show() {
		super.show();
		// Resume BGM when screen is shown (if not game over)
		if (!isGameOver) {
			playCurrentBGM();
		}
	}

	@Override
	public void hide() {
		super.hide();
		// Stop BGM when screen is hidden (e.g. going to main menu)
		if (audio != null) {
			audio.stopBGM();
		}
	}

	@Override
	public void dispose() {
		if (batch != null) batch.dispose();
		if (polyBatch != null) polyBatch.dispose();
		if (dungeonRenderer != null) dungeonRenderer.dispose();
		if (hud != null) hud.dispose();
		if (audio != null) {
			audio.stopBGM();
			audio.dispose();
		}
		super.dispose();
	}
}
