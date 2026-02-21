package com.goldsprite.magicdungeon2.screens.main;

import com.badlogic.gdx.utils.Array;
import com.goldsprite.magicdungeon2.core.combat.WeaponRange;

/**
 * 敌人定义工厂
 * 集中管理所有敌人的属性配置，便于后续数据驱动扩展
 */
public class EnemyDefs {

	/** 生成默认一批敌人实体（地牢基础怪物组） */
	public static Array<GameEntity> createDefaultEnemies() {
		Array<GameEntity> list = new Array<>();
		// 史莱姆：低攻低防，最慢
		list.add(new GameEntity(2, 2, "slime",    20,  4, 1, 1.0f, 15, WeaponRange.MELEE));
		// 骷髅：中等属性，长柄穿透武器
		list.add(new GameEntity(6, 6, "skeleton", 35,  8, 3, 0.8f, 25, WeaponRange.POLEARM));
		// 蝙蝠：低血低防，最快
		list.add(new GameEntity(2, 6, "bat",      15,  6, 1, 0.4f, 20, WeaponRange.MELEE));
		// 狼：高攻高经验
		list.add(new GameEntity(6, 2, "wolf",     30, 10, 2, 0.6f, 30, WeaponRange.MELEE));
		return list;
	}

	/** 创建玩家实体（初始属性） */
	public static GameEntity createPlayer() {
		return new GameEntity(4, 4, "player", 100, 12, 5, 0.2f, 0, WeaponRange.MELEE);
	}
}
