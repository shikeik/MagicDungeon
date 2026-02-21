package com.goldsprite.magicdungeon2.input;

/**
 * 虚拟输入提供者接口
 *
 * UI 层（虚拟摇杆/虚拟按钮）实现此接口，
 * 将触摸事件转化为 InputAction 信号注入到 InputManager。
 *
 * 设计理念：虚拟控件仅作为"信号发生器"，
 * 业务层始终通过 InputManager 查询，无需关心信号来源。
 */
public interface VirtualInputProvider {

	/**
	 * 每帧更新，用于持续注入轴向信号
	 * @param delta 帧间隔时间
	 */
	void update(float delta);

	/**
	 * 设置可见性（当检测到手柄/键盘输入时可隐藏）
	 */
	void setVisible(boolean visible);

	/**
	 * 是否当前可见
	 */
	boolean isVisible();

	/**
	 * 释放资源
	 */
	void dispose();
}
