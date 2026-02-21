package com.goldsprite.magicdungeon2.screens.main;

/**
 * 游戏配置常量
 * 集中管理 SimpleGameScreen 中的所有数值参数，消除魔法数字
 */
public final class GameConfig {

	private GameConfig() {} // 工具类禁止实例化

	// ============ 地图 ============
	/** 地图宽度（格子数） */
	public static final int MAP_W = 9;
	/** 地图高度（格子数） */
	public static final int MAP_H = 9;
	/** 格子像素尺寸 */
	public static final int TILE = 32;

	// ============ 图块类型 ============
	public static final int T_FLOOR = 0;
	public static final int T_WALL = 1;
	public static final int T_STAIRS = 2;

	// ============ 视觉 ============
	/** 视觉插值速度（像素/秒） */
	public static final float VISUAL_SPEED = 256f;
	/** Bump 攻击动画衰减系数 */
	public static final float BUMP_DECAY = 10f;
	/** Bump 攻击偏移比例（格子尺寸的百分比） */
	public static final float BUMP_OFFSET_RATIO = 0.3f;
	/** 世界相机视野基准尺寸（像素单位） */
	public static final float WORLD_VIEW_SIZE = 400f;

	// ============ 战斗 ============
	/** 魔法攻击MP消耗 */
	public static final int MAGIC_MP_COST = 10;
	/** MP不足时的短冷却倍率（防止连按） */
	public static final float MP_FAIL_CD_FACTOR = 0.3f;

	// ============ 输入 ============
	/** 摇杆死区阈值 */
	public static final float STICK_DEADZONE = 0.3f;

	// ============ 敌人AI ============
	/** 随机游荡概率 */
	public static final float WANDER_CHANCE = 0.3f;
	/** 空闲时冷却缩短倍率 */
	public static final float IDLE_CD_FACTOR = 0.5f;

	// ============ 成长系统 ============
	/** MP 自然回复速率（每秒恢复最大MP的百分比） */
	public static final float MP_REGEN_RATE = 0.05f;
	/** 击杀金币 = 经验奖励 × 此系数 */
	public static final float GOLD_XP_RATIO = 0.5f;

	// ============ 飘字 ============
	/** 飘字上升速度（像素/秒） */
	public static final float POPUP_RISE_SPEED = 30f;
}
