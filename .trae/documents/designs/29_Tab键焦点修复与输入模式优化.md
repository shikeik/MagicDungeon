# Tab键焦点修复与输入模式优化

## 问题描述
用户反馈Tab键焦点光标操作无效。经排查，主要原因如下：
1. **输入模式重置冲突**：`InventoryDialog`和`ChestDialog`的`act`方法中，每一帧都会检测鼠标移动（`getDeltaX/Y`）。由于鼠标可能存在微小漂移（Jitter）或高灵敏度，即使未有意移动鼠标，也会触发`updateInputMode(InputMode.MOUSE)`，导致键盘模式（`KEYBOARD`）被立即重置，焦点光标（Focus Border）被隐藏。
2. **Tab键处理逻辑缺失**：部分UI组件（如`InventoryDialog`）虽然处理了Tab键切换区域，但未强制切换回键盘输入模式，导致在鼠标模式下按下Tab键后，焦点虽然逻辑上切换了，但视觉上不可见。
3. **Toolbar输入冲突**：`handleToolbarInput`在`InventoryDialog`打开时仍处理Tab键，可能导致逻辑冲突（现已通过`hasModalUI`检查解决）。

## 解决方案

### 1. 增加鼠标移动检测阈值
在检测鼠标移动以切换输入模式时，增加阈值（>1像素），避免微小抖动导致模式误切换。

```java
// 修改前
if (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0) {
    GameHUD.this.updateInputMode(InputMode.MOUSE);
}

// 修改后
if (Math.abs(Gdx.input.getDeltaX()) > 1 || Math.abs(Gdx.input.getDeltaY()) > 1) {
    GameHUD.this.updateInputMode(InputMode.MOUSE);
}
```

### 2. 强制激活键盘模式
在所有处理Tab键导航的地方（`InventoryDialog`, `ChestDialog`, `handleToolbarInput`），显式调用`updateInputMode(InputMode.KEYBOARD)`。

### 3. 避免Toolbar冲突
在`handleToolbarInput`中添加模态UI检查：
```java
if (hasModalUI()) return;
```

## 验证
- 确认按下Tab键后，焦点光标出现且位置正确。
- 确认轻微触碰鼠标不会立即导致焦点消失。
- 确认打开背包时，Toolbar不再响应Tab键。
