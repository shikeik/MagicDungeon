package com.goldsprite.magicdungeon2.core.combat;

/**
 * 武器范围类型。
 */
public enum WeaponRange {
    /** 普通近战：4方向邻接1格，无穿透 */
    MELEE(1, false),
    /** 长柄武器：4方向直线2格，全穿透 */
    POLEARM(2, true),
    /** 远程能量弹：4方向直线5格，无穿透 */
    ENERGY(5, false),
    /** 远程箭矢：4方向直线5格，全穿透 */
    ARROW(5, true);

    /** 攻击射程（格数） */
    public final int range;
    /** 是否可穿透多个目标 */
    public final boolean piercing;

    WeaponRange(int range, boolean piercing) {
        this.range = range;
        this.piercing = piercing;
    }
}
