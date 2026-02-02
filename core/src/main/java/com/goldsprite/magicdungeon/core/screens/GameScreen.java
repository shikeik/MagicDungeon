package com.goldsprite.magicdungeon.core.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.ScreenUtils;
import com.goldsprite.magicdungeon.core.NewDungeonGame;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.goldsprite.magicdungeon.entities.Item;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.Monster;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.entities.Player;
import com.goldsprite.magicdungeon.ui.GameHUD;
import com.goldsprite.magicdungeon.systems.AudioSystem;
import com.goldsprite.magicdungeon.systems.SaveManager;
import com.goldsprite.magicdungeon.core.GameState;
import com.goldsprite.magicdungeon.utils.Constants;
import com.goldsprite.magicdungeon.utils.SpriteGenerator;
import com.goldsprite.magicdungeon.world.Dungeon;
import com.goldsprite.magicdungeon.world.Tile;
import com.goldsprite.magicdungeon.world.TileType;
import com.badlogic.gdx.math.GridPoint2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameScreen implements Screen {
    final NewDungeonGame game;
    private Dungeon dungeon;
    private Player player;
    private List<Monster> monsters;
    private List<Item> items;
    private FitViewport viewport;
    private OrthographicCamera camera;
    private Map<TileType, Texture> tileTextures;
    private Texture playerTexture;
    private Map<String, Texture> monsterTextures;
    private Map<String, Texture> itemTextures;
    private GameHUD hud;
    private AudioSystem audio;

    public GameScreen(NewDungeonGame game) {
        System.out.println("GameScreen Constructor Started");
        this.game = game;
        this.dungeon = new Dungeon(50, 50);
        this.player = new Player(dungeon.startPos.x, dungeon.startPos.y);

        this.monsters = new ArrayList<>();
        this.items = new ArrayList<>();
        spawnEntities();

        camera = new OrthographicCamera();
        viewport = new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT, camera);
        camera.setToOrtho(false, Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT);
        updateCamera();

        // Scene2D HUD
        hud = new GameHUD(new FitViewport(Constants.VIEWPORT_WIDTH, Constants.VIEWPORT_HEIGHT));

        // Audio
        audio = new AudioSystem();

        generateTextures();
        System.out.println("GameScreen Constructor Finished");
    }

    private void nextLevel() {
        dungeon.level++;
        dungeon.generate();
        player.x = dungeon.startPos.x;
        player.y = dungeon.startPos.y;
        player.visualX = player.x * Constants.TILE_SIZE;
        player.visualY = player.y * Constants.TILE_SIZE;
        spawnEntities();
        hud.showMessage("Descended to Floor " + dungeon.level + "!");
        audio.playLevelUp(); // Reusing sound for now
    }

    private void spawnEntities() {
        monsters.clear();
        items.clear();

        // Difficulty Multiplier
        float difficulty = 1.0f + (dungeon.level - 1) * 0.2f; // 20% harder per level

        // Monsters
        int numMonsters = 10 + (dungeon.level / 2); // More monsters per level
        for (int i = 0; i < numMonsters; i++) {
            GridPoint2 pos = dungeon.getRandomWalkableTile();
            if (pos != null) {
                if (Math.abs(pos.x - player.x) < 5 && Math.abs(pos.y - player.y) < 5) continue;

                // Varied Monster Types based on Level
                MonsterType type = MonsterType.SLIME;
                if (dungeon.level >= 2 && Math.random() < 0.3) type = MonsterType.BAT;
                if (dungeon.level >= 3 && Math.random() < 0.3) type = MonsterType.SKELETON;
                if (dungeon.level >= 5 && Math.random() < 0.3) type = MonsterType.ORC;
                if (dungeon.level % 10 == 0) type = MonsterType.BOSS;

                Monster m = new Monster(pos.x, pos.y, type);
                // Apply difficulty
                m.maxHp = (int)(m.maxHp * difficulty);
                m.hp = m.maxHp;
                m.atk = (int)(m.atk * difficulty);

                monsters.add(m);
            }
        }

        // Items
        int numItems = 5;
        for (int i = 0; i < numItems; i++) {
            GridPoint2 pos = dungeon.getRandomWalkableTile();
            if (pos != null) {
                // Random item type
                ItemData itemData = ItemData.HEALTH_POTION;
                double r = Math.random();
                if (r < 0.4) itemData = ItemData.HEALTH_POTION;
                else if (r < 0.6) itemData = ItemData.MANA_POTION;
                else if (r < 0.8) itemData = ItemData.RUSTY_SWORD;
                // Add more logic for better loot later

                items.add(new Item(pos.x, pos.y, itemData));
            }
        }
    }

    private void updateCamera() {
        // Camera follows player (lerp could be added later)
        camera.position.set(
            player.visualX + Constants.TILE_SIZE / 2f,
            player.visualY + Constants.TILE_SIZE / 2f,
            0
        );
        camera.update();
    }

    private void generateTextures() {
        System.out.println("Generating Textures...");
        tileTextures = new HashMap<>();
        tileTextures.put(TileType.WALL, SpriteGenerator.createWall());
        tileTextures.put(TileType.FLOOR, SpriteGenerator.createFloor());
        tileTextures.put(TileType.DOOR, SpriteGenerator.createDoor());
        tileTextures.put(TileType.STAIRS_DOWN, SpriteGenerator.createStairs());

        playerTexture = SpriteGenerator.createPlayer();
        if (playerTexture == null) System.err.println("Player texture is null!");

        monsterTextures = new HashMap<>();
        monsterTextures.put(MonsterType.SLIME.name, SpriteGenerator.createMonster("SLIME"));
        monsterTextures.put(MonsterType.SKELETON.name, SpriteGenerator.createMonster("SKELETON"));
        monsterTextures.put(MonsterType.ORC.name, SpriteGenerator.createMonster("ORC"));
        monsterTextures.put(MonsterType.BAT.name, SpriteGenerator.createMonster("BAT"));
        monsterTextures.put(MonsterType.BOSS.name, SpriteGenerator.createMonster("BOSS"));

        itemTextures = new HashMap<>();
        itemTextures.put(ItemData.HEALTH_POTION.name, SpriteGenerator.createItem("Health Potion"));
        itemTextures.put(ItemData.MANA_POTION.name, SpriteGenerator.createItem("Mana Potion"));
        itemTextures.put(ItemData.RUSTY_SWORD.name, SpriteGenerator.createItem("Rusty Sword"));
        System.out.println("Textures Generated.");
    }

    private Texture createTexture(Color color) {
        // Deprecated: Use SpriteGenerator
        return null;
    }

    @Override
    public void show() {
    }

    @Override
    public void render(float delta) {
        ScreenUtils.clear(0, 0, 0, 1);

        // Input Handling
        int dx = 0;
        int dy = 0;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) dx = -1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) dx = 1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) dy = 1;
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) dy = -1;

        if (dx != 0 || dy != 0) {
            audio.playMove(); // Note: playMove is silent in H5 but we can enable it
        }

        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.SPACE)) {
            if (player.stats.mana >= 10) {
                player.useSkill();
                audio.playLevelUp(); // Use LevelUp sound as skill sound for now
                hud.showMessage("Used Heal!");
            } else {
                hud.showMessage("Not enough Mana!");
            }
        }

        // Enter Next Level
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.E)) {
            Tile tile = dungeon.getTile(player.x, player.y);
            if (tile != null && tile.type == TileType.STAIRS_DOWN) {
                nextLevel();
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
                if (item.data.type == com.goldsprite.magicdungeon.entities.ItemType.POTION) {
                    if (item.data == ItemData.HEALTH_POTION) {
                        player.stats.hp = Math.min(player.stats.hp + item.data.heal, player.stats.maxHp);
                        hud.showMessage("Used Health Potion!");
                    } else if (item.data == ItemData.MANA_POTION) {
                        player.stats.mana = Math.min(player.stats.mana + item.data.heal, player.stats.maxMana);
                        hud.showMessage("Used Mana Potion!");
                    }
                } else {
                    player.inventory.add(item.data);
                    hud.showMessage("Picked up " + item.data.name + "!");
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
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
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
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F5)) {
            SaveManager.saveGame(player, dungeon);
            hud.showMessage("Game Saved!");
        }

        // Load Game
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F9)) {
            loadGame();
        }

        // Toggle Inventory
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.I)) {
            hud.toggleInventory();
        }

        // Update HUD
        hud.update(player, dungeon.level);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Calculate visible range (optimization)
        int startX = (int)((camera.position.x - camera.viewportWidth / 2) / Constants.TILE_SIZE) - 1;
        int startY = (int)((camera.position.y - camera.viewportHeight / 2) / Constants.TILE_SIZE) - 1;
        int endX = (int)((camera.position.x + camera.viewportWidth / 2) / Constants.TILE_SIZE) + 1;
        int endY = (int)((camera.position.y + camera.viewportHeight / 2) / Constants.TILE_SIZE) + 1;

        startX = Math.max(0, startX);
        startY = Math.max(0, startY);
        endX = Math.min(dungeon.width, endX);
        endY = Math.min(dungeon.height, endY);

        // Render Map
        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                Tile tile = dungeon.getTile(x, y);
                if (tile != null) {
                    Texture texture = tileTextures.get(tile.type);
                    if (texture != null) {
                        game.batch.draw(texture, x * Constants.TILE_SIZE, y * Constants.TILE_SIZE);
                    }
                }
            }
        }

        // Render Items
        for (Item item : items) {
            Texture itemTex = itemTextures.get(item.data.name);
            if (itemTex != null) {
                // Render slightly smaller to distinguish from tiles
                game.batch.draw(itemTex, item.visualX + 4, item.visualY + 4, 24, 24);
            } else {
                // Fallback debug
                // game.batch.draw(playerTexture, item.visualX + 8, item.visualY + 8, 16, 16);
            }
        }

        // Render Monsters
        for (Monster m : monsters) {
            if (m.hp > 0) {
                Texture mTex = monsterTextures.get(m.name);
                if (mTex != null) {
                    game.batch.draw(mTex, m.visualX + m.bumpX, m.visualY + m.bumpY);
                } else {
                    // Fallback to Slime if texture missing
                    Texture fallback = monsterTextures.get(MonsterType.SLIME.name);
                    if (fallback != null) {
                        game.batch.draw(fallback, m.visualX + m.bumpX, m.visualY + m.bumpY);
                    } else {
                         System.err.println("Slime texture is missing!");
                    }
                }
            }
        }

        // Render Player
        if (playerTexture != null) {
            game.batch.draw(playerTexture, player.visualX + player.bumpX, player.visualY + player.bumpY);
        } else {
             System.err.println("Player texture is null during render!");
        }

        game.batch.end();

        // Draw HUD Stage last
        // Stage uses its own camera, usually y-up.
        // If we flipped the main camera to y-down, HUD might need adjustment or just keep it separate.
        // Stage handles its own projection matrix.
        hud.render(delta);
    }

    public void loadGame() {
        GameState state = SaveManager.loadGame();
        if (state != null) {
            player.stats = state.playerStats;
            player.inventory = state.inventory;
            dungeon.level = state.dungeonLevel;

            // Regenerate world
            dungeon.generate();
            player.x = dungeon.startPos.x;
            player.y = dungeon.startPos.y;
            player.visualX = player.x * Constants.TILE_SIZE;
            player.visualY = player.y * Constants.TILE_SIZE;
            spawnEntities();

            hud.showMessage("Game Loaded!");
        } else {
            hud.showMessage("No Save Found!");
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
        hud.resize(width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        for (Texture texture : tileTextures.values()) {
            texture.dispose();
        }
        if (playerTexture != null) playerTexture.dispose();
        for (Texture texture : monsterTextures.values()) {
            texture.dispose();
        }
        for (Texture texture : itemTextures.values()) {
            texture.dispose();
        }
        hud.dispose();
    }
}
