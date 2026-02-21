package com.goldsprite.magicdungeon2.ai;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;

/**
 * 命令到 NeonBatch 方法的动态注册器
 *
 * 启动时通过反射扫描 NeonBatch 所有 public 实例方法，
 * 以方法名（小写）为键构建 MethodHandle 注册表。
 * 运行时根据 AIDrawCommand.type 查找并动态调用，
 * 参数映射规则：
 *   float → args[] 按顺序取值
 *   Color → color / color2 按顺序取值
 *   int   → segments 优先，否则 args[]
 *   boolean → filled 优先，否则 args[]
 *   float[] → 整个 args 数组
 */
public class AIDrawMethodRegistry {

    /** 单个方法重载条目 */
    private static class Entry {
        final MethodHandle unboundHandle;
        final Class<?>[] paramTypes;

        Entry(MethodHandle unboundHandle, Class<?>[] paramTypes) {
            this.unboundHandle = unboundHandle;
            this.paramTypes = paramTypes;
        }
    }

    /** 方法名 (小写) → 所有重载的 Entry 列表 */
    private static final Map<String, List<Entry>> registry = new HashMap<>();

    /* 静态初始化：扫描 NeonBatch 所有 public 实例方法 */
    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Method[] methods = NeonBatch.class.getMethods();
            for (Method m : methods) {
                if (Modifier.isStatic(m.getModifiers())) continue;
                MethodHandle mh = lookup.unreflect(m);
                String name = m.getName().toLowerCase(Locale.ROOT);
                registry.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(new Entry(mh, m.getParameterTypes()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("AIDrawMethodRegistry 初始化失败", e);
        }
    }

    /**
     * 解析十六进制颜色字符串
     * 支持 6 位 (RGB)、8 位 (RGBA)、以及 Color.valueOf 风格
     */
    private static Color parseColor(String hex) {
        Color tmp = new Color();
        if (hex == null) return tmp.set(Color.WHITE);
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        try {
            if (s.length() == 6) {
                int v = Integer.parseInt(s, 16);
                return tmp.set(((v >> 16) & 0xFF) / 255f,
                               ((v >> 8) & 0xFF) / 255f,
                               (v & 0xFF) / 255f, 1f);
            } else if (s.length() == 8) {
                long v = Long.parseLong(s, 16);
                return tmp.set(((int) ((v >> 24) & 0xFF)) / 255f,
                               ((int) ((v >> 16) & 0xFF)) / 255f,
                               ((int) ((v >> 8) & 0xFF)) / 255f,
                               ((int) (v & 0xFF)) / 255f);
            } else {
                return tmp.set(Color.valueOf(s.toUpperCase(Locale.ROOT)));
            }
        } catch (Exception e) {
            return tmp.set(Color.WHITE);
        }
    }

    /**
     * 执行一条绘制命令
     * @param batch  NeonBatch 实例
     * @param cmd    绘制命令
     * @return 是否执行成功
     */
    public static boolean invoke(NeonBatch batch, AIDrawCommand cmd) {
        if (cmd == null || cmd.type == null) return false;
        List<Entry> list = registry.get(cmd.type.trim().toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) return false;

        // 收集所有能映射参数的候选重载
        List<Map.Entry<Entry, Object[]>> candidates = new ArrayList<>();
        for (Entry e : list) {
            try {
                Object[] mapped = tryMapArgs(cmd, e.paramTypes);
                if (mapped != null) {
                    candidates.add(new AbstractMap.SimpleEntry<>(e, mapped));
                }
            } catch (Throwable ex) {
                DLog.logT("AIDraw", "参数映射失败 %s: %s", cmd.type, ex.getMessage());
            }
        }

        if (candidates.isEmpty()) {
            DLog.logT("AIDraw", "无匹配重载: %s", cmd.type);
            return false;
        }

        // 按参数特异性排序（更具体的优先）
        candidates.sort((a, b) -> Integer.compare(
                specificityScore(b.getValue()),
                specificityScore(a.getValue())));

        // 按顺序尝试调用
        for (Map.Entry<Entry, Object[]> ent : candidates) {
            Entry e = ent.getKey();
            Object[] mapped = ent.getValue();
            try {
                MethodHandle bound = e.unboundHandle.bindTo(batch);
                bound.invokeWithArguments(mapped);
                return true;
            } catch (Throwable ex) {
                DLog.logT("AIDraw", "调用失败 %s: %s", cmd.type, ex.getMessage());
            }
        }

        DLog.logT("AIDraw", "所有候选调用均失败: %s", cmd.type);
        return false;
    }

    /**
     * 特异性评分：非默认值越多得分越高
     */
    private static int specificityScore(Object[] mapped) {
        int s = 0;
        for (Object o : mapped) {
            if (o == null) continue;
            if (o instanceof Float) {
                if (!Float.valueOf(0f).equals(o)) s += 1;
            } else if (o instanceof Double) {
                if (!Double.valueOf(0.0).equals(o)) s += 1;
            } else if (o instanceof Integer) {
                if (!Integer.valueOf(0).equals(o)) s += 1;
            } else if (o instanceof Boolean) {
                s += 1;
            } else if (o instanceof float[]) {
                if (((float[]) o).length > 0) s += 3;
            } else if (o instanceof Color) {
                s += 2;
            } else {
                s += 1;
            }
        }
        return s;
    }

    /**
     * 尝试将命令字段映射到方法参数
     * @return 映射后的参数数组，无法映射返回 null
     */
    private static Object[] tryMapArgs(AIDrawCommand cmd, Class<?>[] paramTypes) throws Exception {
        Object[] out = new Object[paramTypes.length];
        int numIdx = 0;    // cmd.args 的读取游标
        int colorIdx = 0;  // 颜色字段游标 (0→color, 1→color2)
        float[] args = cmd.args != null ? cmd.args : new float[0];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> p = paramTypes[i];

            if (p == float.class || p == Float.class) {
                out[i] = numIdx < args.length ? args[numIdx++] : 0f;

            } else if (p == double.class || p == Double.class) {
                out[i] = numIdx < args.length ? (double) args[numIdx++] : 0.0;

            } else if (p == int.class || p == Integer.class) {
                if (cmd.segments != null) {
                    out[i] = cmd.segments;
                } else if (numIdx < args.length) {
                    out[i] = (int) args[numIdx++];
                } else {
                    out[i] = 0;
                }

            } else if (p == boolean.class || p == Boolean.class) {
                if (cmd.filled != null) out[i] = cmd.filled;
                else if (numIdx < args.length) out[i] = args[numIdx++] != 0f;
                else out[i] = false;

            } else if (p == float[].class) {
                if (args.length > 0) out[i] = args;
                else return null; // float[] 为必需参数

            } else if (p == Color.class) {
                String colorStr = colorIdx == 0 ? cmd.color : cmd.color2;
                colorIdx++;
                if (colorStr == null) return null; // Color 参数必须显式提供
                out[i] = parseColor(colorStr);

            } else if (p == String.class) {
                if (cmd.filename != null) out[i] = cmd.filename;
                else if (cmd.meta != null) out[i] = cmd.meta;
                else return null;

            } else {
                // 不支持的参数类型
                return null;
            }
        }
        return out;
    }
}
