package com.goldsprite.magicdungeon.ai;

import java.util.Locale;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon.utils.texturegenerator.NeonGenerator;
import com.goldsprite.magicdungeon.utils.texturegenerator.TextureExporter;

/**
 * 将数据驱动的绘图计划映射为对 `NeonBatch` 的调用并执行，返回生成的 `TextureRegion`。
 */
public class AIDrawExecutor {

    private static Color parseColor(String hex, Color tmp) {
        if (hex == null) return tmp.set(Color.WHITE);
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        // 支持 6 或 8 位 (RGB 或 RGBA)
        try {
            if (s.length() == 6) {
                int v = Integer.parseInt(s, 16);
                int r = (v >> 16) & 0xFF;
                int g = (v >> 8) & 0xFF;
                int b = v & 0xFF;
                return tmp.set(r / 255f, g / 255f, b / 255f, 1f);
            } else if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                int r = (int) ((v >> 24) & 0xFF);
                int g = (int) ((v >> 16) & 0xFF);
                int b = (int) ((v >> 8) & 0xFF);
                int a = (int) (v & 0xFF);
                return tmp.set(r / 255f, g / 255f, b / 255f, a / 255f);
            } else {
                // 兜底支持 Color.valueOf 风格
                tmp.set(Color.valueOf(s.toUpperCase(Locale.ROOT)));
                return tmp;
            }
        } catch (Exception e) {
            tmp.set(Color.WHITE);
            return tmp;
        }
    }

    public static TextureRegion generateFromJson(String jsonText) {
        Json json = new Json();
        AIDrawPlan plan = json.fromJson(AIDrawPlan.class, jsonText);
        if (plan == null) return null;

        final int w = plan.width != null ? plan.width : 256;
        final int h = plan.height != null ? plan.height : 256;

        TextureRegion region = NeonGenerator.getInstance().generate(w, h, (NeonBatch batch) -> {
            if (plan.commands == null) return;
            for (AIDrawCommand cmd : plan.commands) {
                if (cmd == null || cmd.type == null) continue;
                // 使用 MethodHandle 注册器动态调用
                boolean ok = AIDrawMethodRegistry.invoke(batch, cmd);
                if (!ok) {
                    DLog.logT("AIDrawExecutor", "Failed to execute command: %s", cmd.type);
                }
            }
        });

        // 如果 plan 最后包含 save 指令，则写磁盘
        if (plan.commands != null) {
            for (AIDrawCommand cmd : plan.commands) {
                if (cmd != null && cmd.type != null && (cmd.type.equalsIgnoreCase("save") || cmd.type.equalsIgnoreCase("saves"))) {
                    if (cmd.filename != null && region != null) {
                        try {
                            // 去除可能的 .png 后缀，避免双后缀 (TextureExporter 会自动追加)
                            String fn = cmd.filename;
                            if (fn.endsWith(".png") || fn.endsWith(".PNG")) {
                                fn = fn.substring(0, fn.length() - 4);
                            }
                            TextureExporter.exportToDisk(region.getTexture(), fn);
                            DLog.logT("AIDrawExecutor", "Save command executed: %s", cmd.filename);
                        } catch (Exception e) {
                            DLog.logT("AIDrawExecutor", "Save command failed: %s (%s)", cmd.filename, e.getMessage());
                        }
                    }
                }
            }
        }

        return region;
    }
}
