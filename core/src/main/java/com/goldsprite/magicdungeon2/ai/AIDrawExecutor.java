package com.goldsprite.magicdungeon2.ai;

import java.util.Locale;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Json;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.magicdungeon2.utils.texturegenerator.NeonGenerator;
import com.goldsprite.magicdungeon2.utils.texturegenerator.TextureExporter;

/**
 * AI 绘制执行器
 *
 * 将 JSON 文本解析为 AIDrawPlan，
 * 通过 AIDrawMethodRegistry 动态调用 NeonBatch 方法，
 * 最终烘焙为 TextureRegion。
 *
 * JSON 格式示例:
 * {
 *   "name": "fire_crystal",
 *   "width": 256,
 *   "height": 256,
 *   "commands": [
 *     { "type": "drawRect", "args": [0,0,1,1,0,0], "color": "#ff0000", "filled": true },
 *     { "type": "drawCircle", "args": [0.5,0.5,0.3,0,0], "color": "#ffdd00", "segments": 32 }
 *   ]
 * }
 */
public class AIDrawExecutor {

    /**
     * 从 JSON 文本生成纹理
     * @param jsonText JSON 绘制计划文本
     * @return 生成的 TextureRegion，失败返回 null
     */
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
                // 跳过 save 指令（save 不是绘制命令）
                if (cmd.type.equalsIgnoreCase("save") || cmd.type.equalsIgnoreCase("saves")) continue;
                boolean ok = AIDrawMethodRegistry.invoke(batch, cmd);
                if (!ok) {
                    DLog.logT("AIDrawExecutor", "执行命令失败: %s", cmd.type);
                }
            }
        });

        // 处理 save 指令：将纹理导出到磁盘
        if (plan.commands != null && region != null) {
            for (AIDrawCommand cmd : plan.commands) {
                if (cmd != null && cmd.type != null
                        && (cmd.type.equalsIgnoreCase("save") || cmd.type.equalsIgnoreCase("saves"))) {
                    if (cmd.filename != null) {
                        try {
                            String fn = cmd.filename;
                            // 去除 .png 后缀避免重复
                            if (fn.toLowerCase(Locale.ROOT).endsWith(".png")) {
                                fn = fn.substring(0, fn.length() - 4);
                            }
                            TextureExporter.exportToDisk(region.getTexture(), fn);
                            DLog.logT("AIDrawExecutor", "已保存: %s", cmd.filename);
                        } catch (Exception e) {
                            DLog.logT("AIDrawExecutor", "保存失败: %s (%s)", cmd.filename, e.getMessage());
                        }
                    }
                }
            }
        }

        return region;
    }

    /**
     * 从 AIDrawPlan 对象直接生成纹理（无需 JSON 解析）
     * @param plan 绘制计划
     * @return 生成的 TextureRegion
     */
    public static TextureRegion generateFromPlan(AIDrawPlan plan) {
        if (plan == null) return null;
        final int w = plan.width != null ? plan.width : 256;
        final int h = plan.height != null ? plan.height : 256;

        return NeonGenerator.getInstance().generate(w, h, (NeonBatch batch) -> {
            if (plan.commands == null) return;
            for (AIDrawCommand cmd : plan.commands) {
                if (cmd == null || cmd.type == null) continue;
                if (cmd.type.equalsIgnoreCase("save") || cmd.type.equalsIgnoreCase("saves")) continue;
                AIDrawMethodRegistry.invoke(batch, cmd);
            }
        });
    }
}
