package com.goldsprite.magicdungeon2.ai;

/**
 * JSON 绘制计划数据结构
 * 由 LibGDX Json 直接反序列化
 * 包含纹理名称、尺寸和绘制指令列表
 */
public class AIDrawPlan {
    /** 纹理名称标识 */
    public String name;
    /** 生成纹理宽度（像素），默认 256 */
    public Integer width;
    /** 生成纹理高度（像素），默认 256 */
    public Integer height;
    /** 绘制指令数组 */
    public AIDrawCommand[] commands;
}
