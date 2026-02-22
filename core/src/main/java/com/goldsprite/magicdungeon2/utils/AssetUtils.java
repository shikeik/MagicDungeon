package com.goldsprite.magicdungeon2.utils;

import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.goldsprite.gdengine.log.DLog;

/**
 * 资源清单工具类
 *
 * 通过 assets.txt 索引扫描资源文件列表。
 * 在 pc internal内部 无法直接列目录，
 * 因此统一使用预生成的 assets.txt 作为文件索引。
 */
public class AssetUtils {
    private static final Set<String> fileIndex = new HashSet<>();
    private static boolean indexLoaded = false;

    /** 加载 assets.txt 索引（重复调用自动跳过） */
    public static void loadIndex() {
        if (indexLoaded) return;

        try {
            FileHandle indexFile = Gdx.files.internal("assets.txt");
            if (indexFile.exists()) {
                String content = indexFile.readString("UTF-8");
                String[] lines = content.split("\\r?\\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        fileIndex.add(line.trim().replace("\\", "/"));
                    }
                }
                DLog.logT("AssetUtils", "✓ 索引已加载: %d 条", fileIndex.size());
            } else {
                DLog.logT("AssetUtils", "⚠ assets.txt 不存在，请先运行 gradle generateAssetList");
            }
        } catch (Exception e) {
            DLog.logT("AssetUtils", "❌ 加载 assets.txt 失败: %s", e.getMessage());
        }
        indexLoaded = true;
    }

    /**
     * 列出指定目录下的直接子文件/子文件夹名称
     * @param basePath 相对路径（如 "ai_draw_cmds"）
     * @return 文件名数组（如 ["metallic_orb.json", "stone_brick.json"]）
     */
    public static String[] listNames(String basePath) {
        loadIndex();

        String searchPath = basePath.replace("\\", "/");
        if (searchPath.endsWith("/")) searchPath = searchPath.substring(0, searchPath.length() - 1);

        HashSet<String> results = new HashSet<>();
        String prefix = searchPath.isEmpty() ? "" : searchPath + "/";
        int prefixLen = prefix.length();

        for (String path : fileIndex) {
            if (path.startsWith(prefix)) {
                String suffix = path.substring(prefixLen);
                int slashIndex = suffix.indexOf('/');
                if (slashIndex == -1) {
                    // 直接子文件
                    if (!suffix.isEmpty()) results.add(suffix);
                } else {
                    // 子文件夹名
                    String dirName = suffix.substring(0, slashIndex);
                    if (!dirName.isEmpty()) results.add(dirName);
                }
            }
        }

        return results.toArray(new String[0]);
    }

    /** 清空索引缓存（开发时刷新用） */
    public static void clearCache() {
        fileIndex.clear();
        indexLoaded = false;
    }
}
