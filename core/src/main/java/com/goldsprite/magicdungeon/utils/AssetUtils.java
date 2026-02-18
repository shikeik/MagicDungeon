package com.goldsprite.magicdungeon.utils;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.log.DLog;

/**
 * 资源清单工具类，通过 assets.txt 索引扫描资源文件
 * 用法与 Gdx.files 完全兼容，只需将 Gdx 改为 Gd（这里就是 AssetUtils）
 */
public class AssetUtils {
	private static final Set<String> fileIndex = new HashSet<>();
	private static boolean indexLoaded = false;

	/** 加载 assets.txt 索引 */
	public static void loadIndex() {
		if (indexLoaded) return;

		try {
			FileHandle indexFile = Gdx.files.internal("assets.txt");
			if (indexFile.exists()) {
				String content = indexFile.readString("UTF-8");
				String[] lines = content.split("\\r?\\n");
				for (String line : lines) {
					if (!line.trim().isEmpty()) {
						// 统一路径分隔符为 /
						fileIndex.add(line.trim().replace("\\", "/"));
					}
				}
				DLog.logT("AssetUtils", "✓ Assets Index Loaded: " + fileIndex.size() + " entries.");
			} else {
				DLog.logT("AssetUtils", "⚠️ Warning: assets.txt not found! Run 'gradle generateAssetList' first.");
			}
		} catch (Exception e) {
			DLog.logT("AssetUtils", "❌ Failed to load assets.txt: " + e.getMessage());
		}
		indexLoaded = true;
	}

	/**
	 * 从索引中查找子文件名称列表 (支持文件和文件夹)
	 * @param basePath 相对路径 (如 "ai_draw_cmds")
	 * @return 文件名数组 (如 ["metallic_orb.json", "stone_brick.json"])
	 */
	public static String[] listNames(String basePath) {
		loadIndex();

		String searchPath = basePath.replace("\\", "/");
		// 移除末尾斜杠以统一匹配逻辑
		if (searchPath.endsWith("/")) searchPath = searchPath.substring(0, searchPath.length() - 1);

		// 使用 Set 去重 (因为多个文件可能属于同一个子文件夹)
		HashSet<String> results = new HashSet<>();
		String prefix = searchPath.isEmpty() ? "" : searchPath + "/";
		int prefixLen = prefix.length();

		for (String path : fileIndex) {
			if (path.startsWith(prefix)) {
				// 获取去掉前缀后的剩余部分
				// 例如 path: "ai_draw_cmds/metallic_orb.json"
				// prefix: "ai_draw_cmds/"
				// suffix: "metallic_orb.json"
				String suffix = path.substring(prefixLen);

				int slashIndex = suffix.indexOf('/');
				if (slashIndex == -1) {
					// 没有斜杠，说明是直接子文件
					if (!suffix.isEmpty()) results.add(suffix);
				} else {
					// 有斜杠，说明是子文件夹，提取第一段作为文件夹名
					// "subfolder/file.json" -> "subfolder"
					String dirName = suffix.substring(0, slashIndex);
					if (!dirName.isEmpty()) results.add(dirName);
				}
			}
		}

		// 转为数组返回
		String[] arr = new String[results.size()];
		return results.toArray(arr);
	}

	/**
	 * 清空索引缓存（用于开发时刷新）
	 */
	public static void clearCache() {
		fileIndex.clear();
		indexLoaded = false;
	}
}
