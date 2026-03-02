feat(netcode): 坦克屏取消硬编码改用InputManager, 新增 Android虚拟摇杆+攻击按钮

- 新增 TankVirtualControls: 左侧四向摇杆(AXIS_LEFT) + 右下攻击按钮(ATTACK)
- OnlineScreen Host/Client 输入全面改用 InputManager.getAxis + isJustPressed
- SandboxScreen P1 输入改用 InputManager, P2 保留方向键调试用
- VirtualButton 增加 fallback 圆形绘制(无纹理时显示半透明红色圆)
- Android默认显示虚拟控件, PC默认隐藏, 自动淡入淡出