package com.goldsprite.magicdungeon2.assets;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.magicdungeon2.ai.AIDrawExecutor;
import com.goldsprite.magicdungeon2.utils.AssetUtils;

/**
 * JSON 驱动的纹理管理器
 *
 * 所有游戏纹理均通过 assets/ai_draw_cmds/ 目录下的 JSON 绘制计划生成。
 * 不使用任何硬编码生成器，所有视觉表现由数据驱动。
 *
 * 使用方式:
 *   TextureManager.init();           // 启动时调用一次
 *   TextureRegion tex = TextureManager.get("player");  // 按名称获取纹理
 *
 * JSON 文件命名规则:
 *   assets/ai_draw_cmds/player.json → 名称 "player"
 *   assets/ai_draw_cmds/slime.json  → 名称 "slime"
 */
public class TextureManager {
    private static final String TAG = "TextureManager";

    /** JSON 绘制计划目录（相对 assets 根目录） */
    private static final String DRAW_CMD_DIR = "ai_draw_cmds";

    /** 名称 → 生成的 TextureRegion 缓存 */
    private static final Map<String, TextureRegion> cache = new HashMap<>();

    /** 是否已初始化 */
    private static boolean initialized = false;

    /**
     * 初始化纹理管理器
     * 扫描 ai_draw_cmds/ 目录下所有 .json 文件，
     * 逐一执行 AIDrawExecutor 生成 TextureRegion 并缓存。
     */
    public static void init() {
        if (initialized) return;

        AssetUtils.loadIndex();
        String[] files = AssetUtils.listNames(DRAW_CMD_DIR);

        int loaded = 0;
        for (String fileName : files) {
            if (!fileName.endsWith(".json")) continue;

            // 提取名称（去除 .json 后缀）
            String name = fileName.substring(0, fileName.length() - 5);

            try {
                String jsonText = Gdx.files.internal(DRAW_CMD_DIR + "/" + fileName).readString("UTF-8");
                TextureRegion region = AIDrawExecutor.generateFromJson(jsonText);
                if (region != null) {
                    cache.put(name, region);
                    loaded++;
                } else {
                    DLog.logT(TAG, "⚠ 生成返回 null: %s", name);
                }
            } catch (Exception e) {
                DLog.logT(TAG, "❌ 加载失败 [%s]: %s", name, e.getMessage());
            }
        }

        initialized = true;
        DLog.logT(TAG, "✓ 纹理管理器初始化完成: 已加载 %d/%d", loaded, files.length);
    }

    /**
     * 按名称获取纹理
     * @param name 纹理名称（对应 JSON 文件名，不含 .json）
     * @return TextureRegion，未找到返回 null
     */
    public static TextureRegion get(String name) {
        if (!initialized) {
            DLog.logT(TAG, "⚠ 尚未初始化，正在自动初始化...");
            init();
        }
        return cache.get(name);
    }

    /**
     * 动态加载单个 JSON 绘制计划（热加载）
     * @param name JSON 文件名（不含 .json 后缀）
     * @return 生成的 TextureRegion，失败返回 null
     */
    public static TextureRegion loadSingle(String name) {
        try {
            String path = DRAW_CMD_DIR + "/" + name + ".json";
            String jsonText = Gdx.files.internal(path).readString("UTF-8");
            TextureRegion region = AIDrawExecutor.generateFromJson(jsonText);
            if (region != null) {
                cache.put(name, region);
                DLog.logT(TAG, "✓ 热加载成功: %s", name);
            }
            return region;
        } catch (Exception e) {
            DLog.logT(TAG, "❌ 热加载失败 [%s]: %s", name, e.getMessage());
            return null;
        }
    }

    /**
     * 从 JSON 字符串直接生成并缓存纹理（编辑器实时预览用）
     * @param name 缓存键名
     * @param jsonText JSON 绘制计划文本
     * @return 生成的 TextureRegion
     */
    public static TextureRegion loadFromJsonText(String name, String jsonText) {
        try {
            TextureRegion region = AIDrawExecutor.generateFromJson(jsonText);
            if (region != null) {
                cache.put(name, region);
            }
            return region;
        } catch (Exception e) {
            DLog.logT(TAG, "❌ 从文本加载失败 [%s]: %s", name, e.getMessage());
            return null;
        }
    }

    /**
     * 检查纹理是否已加载
     */
    public static boolean has(String name) {
        return cache.containsKey(name);
    }

    /**
     * 获取已加载纹理数量
     */
    public static int getLoadedCount() {
        return cache.size();
    }

    /**
     * 获取所有已加载纹理名称
     */
    public static String[] getLoadedNames() {
        return cache.keySet().toArray(new String[0]);
    }

    /**
     * 重新加载所有纹理
     */
    public static void reload() {
        dispose();
        init();
    }

    /**
     * 释放所有纹理资源
     */
    public static void dispose() {
        for (TextureRegion region : cache.values()) {
            if (region != null && region.getTexture() != null) {
                region.getTexture().dispose();
            }
        }
        cache.clear();
        initialized = false;
        DLog.logT(TAG, "已释放所有纹理");
    }
}
