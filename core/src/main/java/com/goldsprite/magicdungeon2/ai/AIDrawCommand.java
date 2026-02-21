package com.goldsprite.magicdungeon2.ai;

/**
 * 数据驱动的绘图命令
 * 由 LibGDX Json 直接反序列化，每个字段对应 JSON 键
 *
 * type 对应 NeonBatch 的方法名（如 "drawRect", "drawCircle"）
 * args 按顺序映射到方法的 float 参数
 * color/color2 映射到 Color 参数
 * segments 映射到 int 参数
 * filled 映射到 boolean 参数
 */
public class AIDrawCommand {
    /** 命令类型，对应 NeonBatch 方法名（如 "drawRect", "drawCircle", "drawLine"） */
    public String type;

    /** 浮点参数数组，按方法签名顺序填充（坐标/尺寸/线宽等） */
    public float[] args;

    /** 主颜色，十六进制字符串（如 "#ff00ff" 或 "ff00ff"） */
    public String color;

    /** 副颜色，用于渐变/描边等 */
    public String color2;

    /** 细分/分段数（用于圆形等图元） */
    public Integer segments;

    /** 是否填充（用于多边形等） */
    public Boolean filled;

    /** 保存文件名（当 type="save" 时使用） */
    public String filename;

    /** 可选元数据字段 */
    public String meta;
}
