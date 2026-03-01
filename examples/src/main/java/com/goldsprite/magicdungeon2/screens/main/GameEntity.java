package com.goldsprite.magicdungeon2.screens.main;

import com.goldsprite.magicdungeon2.core.combat.WeaponRange;
import com.goldsprite.magicdungeon2.core.stats.StatCalculator;
import com.goldsprite.magicdungeon2.core.stats.StatData;
import com.goldsprite.magicdungeon2.core.stats.StatType;

/**
 * 游戏实体（半即时制）
 * 每个实体维护独立的移动冷却计时器和视觉插值坐标
 */
public class GameEntity {
	// --- 逻辑状态 ---
	public int x, y;            // 网格坐标（立即跳变）
	public String texName;
	public StatData stats;
	public float hp;             // 当前生命值（maxHp 由 stats.getHP() 驱动）
	public float mp;             // 当前魔法值（maxMp 由 stats.getMP() 驱动）
	public boolean alive = true;

	// --- 冷却系统 ---
	public float moveTimer = 0;  // 当前冷却剩余时间（秒）
	public float moveDelay;      // 基础冷却间隔（秒）

	// --- 视觉插值 ---
	public float visualX, visualY;   // 渲染像素坐标（平滑追赶逻辑坐标）
	public float bumpX, bumpY;       // Bump 攻击偏移（衰减动画）

	// --- 敌人AI ---
	public float aggroRange = 6f;    // 仇恨范围（格子距离）

	// --- 成长系统（主要用于玩家） ---
	public long totalXp = 0;    // 累计总经验
	public int gold = 0;        // 金币

	// --- 战斗扩展 ---
	public int xpReward;              // 击杀经验奖励（敌人用）
	public WeaponRange weaponRange;   // 武器范围类型
	public int faceDx = 0, faceDy = 1; // 面朝方向（默认朝上）

	public GameEntity(int x, int y, String texName, float hp, float atk, float def,
				   float moveDelay, int xpReward, WeaponRange weaponRange) {
		this.x = x;
		this.y = y;
		this.texName = texName;
		this.moveDelay = moveDelay;
		this.xpReward = xpReward;
		this.weaponRange = weaponRange;
		this.visualX = x * GameConfig.TILE;
		this.visualY = y * GameConfig.TILE;

		// 初始化 StatData 并反推 equipFixed，使 stats 成为属性唯一数据源
		stats = new StatData();
		stats.setLevel(0); // 0级对应 totalXp=0，确保等级与经验一致
		float fixedPts = StatCalculator.fixedPointsPerStat(0);
		stats.setEquipFixed(StatType.HP, hp - fixedPts * StatType.HP.valuePerPoint);
		stats.setEquipFixed(StatType.ATK, atk - fixedPts * StatType.ATK.valuePerPoint);
		stats.setEquipFixed(StatType.DEF, def - fixedPts * StatType.DEF.valuePerPoint);
		this.hp = getMaxHp();
		this.mp = getMaxMp();
	}

	/** 最大生命值（由 StatData 驱动） */
	public float getMaxHp() { return stats.getHP(); }
	/** 最大魔法值（由 StatData 驱动） */
	public float getMaxMp() { return stats.getMP(); }

	/** 获取移动冷却时间（受 MOV 属性加速） */
	public float getMoveCooldown() {
		return moveDelay / Math.max(stats.getMOV(), 0.1f);
	}
	/** 获取攻击冷却时间（受 ASP 属性加速） */
	public float getAttackCooldown() {
		return moveDelay / Math.max(stats.getASP(), 0.1f);
	}

	/** 更新视觉坐标（平滑追赶逻辑坐标） */
	public void updateVisuals(float dt) {
		float targetX = x * GameConfig.TILE;
		float targetY = y * GameConfig.TILE;

		// 线性插值到目标位置
		float distX = targetX - visualX;
		float distY = targetY - visualY;
		float move = GameConfig.VISUAL_SPEED * dt;

		if (Math.abs(distX) <= move) visualX = targetX;
		else visualX += Math.signum(distX) * move;

		if (Math.abs(distY) <= move) visualY = targetY;
		else visualY += Math.signum(distY) * move;

		// Bump 动画衰减
		bumpX += (0 - bumpX) * GameConfig.BUMP_DECAY * dt;
		bumpY += (0 - bumpY) * GameConfig.BUMP_DECAY * dt;
	}

	/** 触发 Bump 攻击动画（向目标方向弹一下） */
	public void triggerBump(int dx, int dy) {
		bumpX = dx * GameConfig.TILE * GameConfig.BUMP_OFFSET_RATIO;
		bumpY = dy * GameConfig.TILE * GameConfig.BUMP_OFFSET_RATIO;
	}
}
