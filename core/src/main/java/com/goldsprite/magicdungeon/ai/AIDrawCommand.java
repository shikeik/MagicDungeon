package com.goldsprite.magicdungeon.ai;

/**
 * 一个简单的数据驱动绘图命令表示。
 * 设计为能被 LibGDX 的 Json 直接反序列化。
 */
public class AIDrawCommand {
    // 命令类型，例如: "fillRect", "strokeRect", "radialCircle", "line", "polygon", "gradientRect", "save"
    public String type;

    // 参数数组，自由解释。通常为坐标/尺寸/线宽等浮点数。
    public float[] args;

    // 主颜色（十六进制字符串，如 "#ff00ff" 或 "ff00ff"）
    public String color;

    // 可选第2颜色（用于渐变/描边等）
    public String color2;

    // 细分/分段数（例如圆的 segments）
    public Integer segments;

    // 是否填充（用于 polygon）
    public Boolean filled;

    // 保存文件名（用于 type="save"）
    public String filename;

    // 可选随机种子或风格字段
    public String meta;
}
