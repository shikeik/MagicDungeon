
# 背包 UI 重构与美化方案

## 1. 需求分析
当前的背包界面存在以下问题：
- **布局局限**：使用垂直列表布局，空间利用率低，一屏只能显示少量物品。
- **视觉简陋**：纯文本列表，缺乏现代游戏（H5 风格）的视觉吸引力。
- **操作不便**：在大量物品时需要频繁滚动。

## 2. 目标效果
- **网格布局**：采用 Grid 布局，每行显示 4-5 个物品，大幅提高同屏显示数量。
- **H5 风格美化**：
    - 物品显示为“图标+背景框”的形式。
    - 装备状态通过图标上的角标或边框高亮显示。
    - 点击物品显示详情或直接操作（保持现有逻辑，但优化反馈）。
- **资源生成**：使用代码动态生成圆角矩形或边框背景（9-patch），无需引入外部图片文件。

## 3. 实现方案

### 3.1 资源准备
在 `GameHUD` 初始化时，使用 `Pixmap` 动态生成以下纹理：
- **Slot Background**：一个半透明的圆角矩形或带边框的矩形，作为物品格子的底板。
- **Selected/Equipped Border**：一个高亮的边框，用于标识已装备的物品。

### 3.2 UI 结构调整
修改 `GameHUD.java` 中的 `InventoryDialog` 和 `updateInventory` 方法。

**原有结构**：
```
Dialog
  └─ VisTable (Vertical)
       ├─ Item Table 1
       ├─ Item Table 2
       └─ ...
```

**新结构**：
```
Dialog
  └─ VisScrollPane
       └─ VisTable (Grid)
            ├─ Item Cell 1 | Item Cell 2 | Item Cell 3 | Item Cell 4
            ├─ Item Cell 5 | ...
```

### 3.3 物品格子 (Item Slot) 设计
每个格子包含：
1.  **背景**：动态生成的 `ui.9.png` 风格背景。
2.  **图标**：物品的贴图（缩放到合适大小，如 32x32 或 48x48）。
3.  **装备标识**：如果 `isEquipped` 为真，在右上角显示一个 "E" 标签或绘制金色边框。
4.  **数量/信息**：(可选) 点击后弹出 Tip 或在下方显示名称。鉴于移动端/H5 风格，可以直接在格子下方显示简短名称。

### 3.4 交互逻辑
- **点击**：保持原有的“点击即装备/使用”逻辑，或者改为“点击选中 -> 显示详情 -> 再次点击装备”。
- **为了简化且保持流畅**：采用“点击即操作”，并通过 Toast (`showMessage`) 提示结果。

## 4. 技术细节
- 使用 `Table.add().width().height()` 固定格子大小。
- 使用 `Table.row()` 在每添加 N 个物品后换行。
- 动态生成 Texture 代码示例：
  ```java
  Pixmap pixmap = new Pixmap(64, 64, Format.RGBA8888);
  pixmap.setColor(Color.DARK_GRAY);
  pixmap.fillRectangle(0, 0, 64, 64);
  // Draw border...
  Texture texture = new Texture(pixmap);
  NinePatch patch = new NinePatch(texture, ...);
  ```

## 5. 后续更新计划
- 编写代码实现上述 UI 变更。
- 验证在不同分辨率下的适配性。
