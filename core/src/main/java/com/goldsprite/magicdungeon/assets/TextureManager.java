package com.goldsprite.magicdungeon.assets;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.goldsprite.magicdungeon.entities.ItemData;
import com.goldsprite.magicdungeon.entities.MonsterType;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonItemGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonSpriteGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonTileGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.SpriteGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.TextureExporter;
import com.goldsprite.magicdungeon.world.TileType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TextureManager implements Disposable {
	private static TextureManager instance;

	private Map<String, TextureRegion> regionCache;
	// Track underlying textures for disposal
	private Set<Texture> managedTextures;

	private TextureManager() {
		regionCache = new HashMap<>();
		managedTextures = new HashSet<>();
		loadAll();
	}

	public static TextureManager getInstance() {
		if (instance == null) {
			instance = new TextureManager();
		}
		return instance;
	}

	private void loadAll() {
		// 1. Load Assets (Sprite Sheets)
		loadSheet("sprites/all_blocks_sheet.json");
		loadSheet("sprites/all_entities_sheet.json");
		loadSheet("sprites/all_items_sheet.json");

		// 2. Generate Missing Textures (Fallback)

		// Tiles
		// [修改] 切换到 NeonTileGenerator
		checkAndGenerate(TileType.Wall.name(), () -> NeonTileGenerator.createDungeonWallTileset(ThemeConfig.WALL_TOP, ThemeConfig.WALL_FACE));
		checkAndGenerate(TileType.Floor.name(), () -> NeonTileGenerator.createFloor(ThemeConfig.FLOOR_BASE, ThemeConfig.FLOOR_DARK, ThemeConfig.FLOOR_HIGHLIGHT));

		checkAndGenerate(TileType.Door.name(), () -> SpriteGenerator.createDoor());
		checkAndGenerate(TileType.Stairs_Down.name(), () -> SpriteGenerator.createStairs(false));
		checkAndGenerate(TileType.Stairs_Up.name(), () -> SpriteGenerator.createStairs(true));

		// Camp Tiles
		checkAndGenerate(TileType.Tree.name(), () -> SpriteGenerator.createTree());
		checkAndGenerate(TileType.Grass.name(), () -> SpriteGenerator.createGrass());
		checkAndGenerate(TileType.Sand.name(), () -> SpriteGenerator.createSand());
		checkAndGenerate(TileType.StonePath.name(), () -> SpriteGenerator.createStonePath());
		checkAndGenerate(TileType.Dungeon_Entrance.name(), () -> SpriteGenerator.createDungeonEntrance());

		// Decor
		checkAndGenerate(TileType.Pillar.name(), () -> SpriteGenerator.createPillar());
		checkAndGenerate(TileType.Torch.name(), () -> SpriteGenerator.createTorch());
		checkAndGenerate(TileType.Window.name(), () -> SpriteGenerator.createWindow());

		// Player
		// [修改] 切换到 NeonSpriteGenerator
		checkAndGenerate("PLAYER", () -> NeonSpriteGenerator.createPlayer());

		// Monsters
		for (MonsterType type : MonsterType.values()) {
			final String enumName = type.name();
			
			// [New Feature] 尝试加载自定义纹理文件 (assets/textures/monsters/Boss.png)
			try {
				com.badlogic.gdx.files.FileHandle customFile = com.badlogic.gdx.Gdx.files.internal("textures/monsters/" + enumName + ".png");
				if (customFile.exists()) {
					checkAndGenerate(enumName, () -> new TextureRegion(new Texture(customFile)));
					continue;
				}
			} catch (Exception e) {
				com.badlogic.gdx.Gdx.app.error("TextureManager", "Failed to check custom texture for " + enumName, e);
			}

			// [Integration] 尝试使用 NeonSpriteGenerator (针对 Boss/Dragon)
			TextureRegion region = NeonSpriteGenerator.createMonster(type.name);
			if (region != null) {
				checkAndGenerate(type.name(), () -> region);
			} else {
				// 回退到旧的生成器
				checkAndGenerate(type.name(), () -> SpriteGenerator.createMonster(type.name()));
			}
		}

		// Items
		// [修改] 切换到 NeonItemGenerator
		for (ItemData item : ItemData.values()) {
			checkAndGenerate(item.name(), () -> NeonItemGenerator.createItem(item.name));
		}
	}

	private void loadSheet(String path) {
		Map<String, TextureRegion> sheet = SpriteSheetLoader.load(path);
		if (!sheet.isEmpty()) {
			// Add all regions to cache
			regionCache.putAll(sheet);
			// Track the texture (all regions in a sheet share the same texture)
			Texture texture = sheet.values().iterator().next().getTexture();
			managedTextures.add(texture);
		}
	}

	private interface TextureProducer {
		TextureRegion produce();
	}

	// [Refactor] 修改方法签名以支持 Lambda 中包含逻辑判断
	// 原来的 TextureProducer 接口方法是 TextureRegion produce();
	// 但我们需要能在 checkAndGenerate 外部先判断，或者让 producer 可以返回 null (虽然 checkAndGenerate 已经做了 null 检查)
	// 实际上上面的修改 loop 中已经处理了 null 逻辑，这里只需稍微调整 checkAndGenerate 即可。
	// 但为了更清晰，我们可以在这里直接接受 TextureRegion 参数的重载？
	// 不，保持原样即可，我们在 loop 里做的逻辑是：
	/*
		TextureRegion region = NeonSpriteGenerator.createMonster(type.name);
		if (region != null) {
			checkAndGenerate(type.name(), () -> region);
		} else {
			checkAndGenerate(type.name(), () -> SpriteGenerator.createMonster(type.name()));
		}
	*/
	// 这样是没问题的。
	
	private void checkAndGenerate(String key, TextureProducer producer) {
		String lowerKey = key.toLowerCase();
		if (!regionCache.containsKey(lowerKey)) {
			TextureRegion region = producer.produce();
			
			// 如果 producer 返回 null，则不缓存
			if (region == null) return;

			// DEBUG: Export generated textures to disk for inspection
			// This helps user to see what procedural textures look like
			// [Fix] 只有在非 null 时才导出
			TextureExporter.exportToDisk(region.getTexture(), lowerKey);

			managedTextures.add(region.getTexture());
			regionCache.put(lowerKey, new TextureRegion(region));
		}
	}

	public void updateTexture(String key, Texture newTexture) {
		String lowerKey = key.toLowerCase();

		// If exists, we might need to dispose old texture if it was managed and generated
		// But be careful if it's shared from Atlas.
		// Our "generated" textures are individual Texture objects in managedTextures.

		TextureRegion oldRegion = regionCache.get(lowerKey);
		if (oldRegion != null) {
			Texture oldTex = oldRegion.getTexture();
			if (managedTextures.contains(oldTex)) {
				// Only dispose if it's one of our managed individual textures
				// And check if no other region uses it?
				// For generated textures, usually 1 region = 1 texture.
				managedTextures.remove(oldTex);
				oldTex.dispose();
			}
		}

		managedTextures.add(newTexture);
		regionCache.put(lowerKey, new TextureRegion(newTexture));
	}

	public Map<String, TextureRegion> getAllTextures() {
		return regionCache;
	}

	public TextureRegion get(String key) {
		if (key == null) return null;
		return regionCache.get(key.toLowerCase());
	}

	public TextureRegion getTile(TileType type) {
		return get(type.name());
	}

	public TextureRegion getMonster(String name) {
		return get(name);
	}

	public TextureRegion getItem(String name) {
		return get(name);
	}

	public TextureRegion getQualityStar() {
		String key = "quality_star";
		if (!regionCache.containsKey(key)) {
			TextureRegion region = SpriteGenerator.createQualityStar();
			managedTextures.add(region.getTexture());
			regionCache.put(key, region);
		}
		return regionCache.get(key);
	}

	public TextureRegion getPlayer() {
		return get("PLAYER");
	}

	@Override
	public void dispose() {
		for (Texture t : managedTextures) {
			t.dispose();
		}
		managedTextures.clear();
		regionCache.clear();
		instance = null;
	}
}
