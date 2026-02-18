package com.goldsprite.magicdungeon.ai;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.log.DLog;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/**
 * 命令到 `NeonBatch` 方法的动态注册器。
 * 使用 MethodHandle 做高性能动态调用，参数映射通过 ParamMapper 完成。
 */
public class AIDrawMethodRegistry {

    private static class Entry {
        final MethodHandle unboundHandle;
        final Class<?>[] paramTypes;

        Entry(MethodHandle unboundHandle, Class<?>[] paramTypes) {
            this.unboundHandle = unboundHandle;
            this.paramTypes = paramTypes;
        }
    }

    private static final Map<String, List<Entry>> registry = new HashMap<>();

    static {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Method[] methods = NeonBatch.class.getMethods();
            for (Method m : methods) {
                // only consider instance (non-static) public methods declared in NeonBatch or inherited
                if (Modifier.isStatic(m.getModifiers())) continue;
                MethodHandle mh = lookup.unreflect(m);
                String name = m.getName().toLowerCase(Locale.ROOT);
                registry.computeIfAbsent(name, k -> new ArrayList<>()).add(new Entry(mh, m.getParameterTypes()));
            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to initialize dynamic AIDrawMethodRegistry", e);
        }
    }

    private static Color parseColor(String hex) {
        Color tmp = new Color();
        if (hex == null) return tmp.set(Color.WHITE);
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
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
                tmp.set(Color.valueOf(s.toUpperCase(Locale.ROOT)));
                return tmp;
            }
        } catch (Exception e) {
            tmp.set(Color.WHITE);
            return tmp;
        }
    }

    public static boolean invoke(NeonBatch batch, AIDrawCommand cmd) {
        if (cmd == null || cmd.type == null) return false;
        List<Entry> list = registry.get(cmd.type.trim().toLowerCase(Locale.ROOT));
        if (list == null || list.isEmpty()) return false;
        DLog.logT("AIDraw", "Invoking command: %s", cmd.type);

        // Try to map args for each overload and score candidates
        List<Map.Entry<Entry, Object[]>> candidates = new ArrayList<>();
        for (Entry e : list) {
            try {
                Object[] mapped = tryMapArgs(cmd, e.paramTypes);
                if (mapped != null) {
                    candidates.add(new AbstractMap.SimpleEntry<>(e, mapped));
                }
            } catch (Throwable ex) {
                // mapping failed for this overload; skip
                DLog.logT("AIDraw", "Mapping failed for %s candidate: %s", cmd.type, ex.getMessage());
            }
        }

        if (candidates.isEmpty()) {
            DLog.logT("AIDraw", "No matching overload (mapping) for command: %s", cmd.type);
            return false;
        }

        // Simple heuristic: prefer candidates with more non-null provided args (higher specificity)
        candidates.sort((a, b) -> {
            int sa = specificityScore(a.getValue());
            int sb = specificityScore(b.getValue());
            return Integer.compare(sb, sa);
        });

        // Try invoking in order
        for (Map.Entry<Entry, Object[]> ent : candidates) {
            Entry e = ent.getKey();
            Object[] mapped = ent.getValue();
            try {
                MethodHandle bound = e.unboundHandle.bindTo(batch);
                bound.invokeWithArguments(mapped);
                DLog.logT("AIDraw", "Command succeeded: %s -> %s", cmd.type, e.unboundHandle.type().toMethodDescriptorString());
                return true;
            } catch (Throwable ex) {
                DLog.logT("AIDraw", "Invocation failed for %s candidate: %s", cmd.type, ex.getMessage());
            }
        }

        DLog.logT("AIDraw", "All candidate invocations failed for command: %s", cmd.type);
        return false;
    }

    // Compute a simple specificity score: count non-null and non-default args
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
                float[] a = (float[]) o;
                if (a.length > 0) s += 3;
            } else if (o instanceof Color) {
                s += 2;
            } else if (o instanceof String) {
                s += 1;
            } else {
                s += 1;
            }
        }
        return s;
    }

    /**
     * Try to map command fields to parameter types. Returns mapped args or null if mapping not possible.
     */
    private static Object[] tryMapArgs(AIDrawCommand cmd, Class<?>[] paramTypes) throws Exception {
        Object[] out = new Object[paramTypes.length];
        int numIdx = 0; // index into cmd.args for numeric params
        int colorIdx = 0; // index for color fields (color,color2)

        float[] args = cmd.args != null ? cmd.args : new float[0];

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> p = paramTypes[i];
            if (p == float.class || p == Float.class) {
                if (numIdx < args.length) {
                    out[i] = args[numIdx++];
                } else {
                    // no numeric available; allow default 0 but mark as less specific
                    out[i] = 0f;
                }
            } else if (p == double.class || p == Double.class) {
                if (numIdx < args.length) {
                    out[i] = (double) args[numIdx++];
                } else {
                    out[i] = 0.0;
                }
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
                else return null; // cannot satisfy required float[]
            } else if (p == Color.class) {
                String colorStr = colorIdx == 0 ? cmd.color : cmd.color2;
                colorIdx++;
                if (colorStr == null) return null; // require explicit color for Color param
                out[i] = parseColor(colorStr);
            } else if (p == String.class) {
                if (cmd.filename != null) out[i] = cmd.filename;
                else if (cmd.meta != null) out[i] = cmd.meta;
                else return null;
            } else {
                // Unsupported param type; fail mapping
                return null;
            }
        }
        return out;
    }

    private static Object[] mapArgs(AIDrawCommand cmd, Class<?>[] paramTypes) throws Exception {
        Object[] out = new Object[paramTypes.length];
        int numIdx = 0; // index into cmd.args for numeric params
        int colorIdx = 0; // index for color fields (color,color2)

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> p = paramTypes[i];
            if (p == float.class || p == Float.class) {
                float v = 0f;
                if (cmd.args != null && numIdx < cmd.args.length) v = cmd.args[numIdx++];
                out[i] = v;
            } else if (p == double.class || p == Double.class) {
                double v = 0.0;
                if (cmd.args != null && numIdx < cmd.args.length) v = cmd.args[numIdx++];
                out[i] = v;
            } else if (p == int.class || p == Integer.class) {
                int v = 0;
                if (cmd.args != null && numIdx < cmd.args.length) v = (int) cmd.args[numIdx++];
                out[i] = v;
            } else if (p == boolean.class || p == Boolean.class) {
                Boolean b = null;
                if (cmd.filled != null) b = cmd.filled;
                else if (cmd.args != null && numIdx < cmd.args.length) b = cmd.args[numIdx++] != 0f;
                out[i] = b != null ? b : false;
            } else if (p == float[].class) {
                out[i] = cmd.args != null ? cmd.args : new float[0];
            } else if (p == Color.class) {
                String colorStr = colorIdx == 0 ? cmd.color : cmd.color2;
                colorIdx++;
                out[i] = parseColor(colorStr);
            } else if (p == String.class) {
                // supply filename or meta
                out[i] = cmd.filename != null ? cmd.filename : cmd.meta;
            } else if (p == Integer.TYPE || p == Integer.class) {
                int v = 0;
                if (cmd.segments != null) v = cmd.segments;
                else if (cmd.args != null && numIdx < cmd.args.length) v = (int) cmd.args[numIdx++];
                out[i] = v;
            } else {
                // Unsupported param type; fail mapping
                throw new IllegalArgumentException("Unsupported parameter type: " + p.getName());
            }
        }
        return out;
    }
}
