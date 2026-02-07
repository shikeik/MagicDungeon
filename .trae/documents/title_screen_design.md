# 标题画面与动效设计策划案 (NeonBatch 版)

## 1. 概述
本设计案旨在重构《Magic Dungeon》的标题画面，利用项目特有的 `NeonBatch` 几何绘制引擎，通过纯代码生成具有风格化（Low-Poly 或 Neon Cyberpunk 风格）的动态视差场景。

**核心视觉主题**：勇者持剑对抗魔龙（几何抽象风）。
**核心技术**：`NeonBatch` 实时几何绘制 + 视差滚动 + 移入式 UI。

## 2. 视觉与场景设计 (基于 NeonBatch)

不再使用静态纹理，而是每一帧通过 `NeonBatch` 的 API 绘制几何图形。利用 `NeonBatch` 的描边、填充、渐变和贝塞尔曲线能力。

### 2.1 整体构图与分层
场景分为 4 个视差层，由后向前绘制。

#### 第一层：背景层 (Background Layer) - 极慢速
*   **夜空**：使用 `drawRect` 填充全屏，使用深蓝到黑色的垂直渐变。
*   **月亮**：
    *   主体：使用 `drawCircle` 绘制一个巨大的白色/淡黄色实心圆。
    *   光晕：在月亮周围绘制多个同心圆环 (`drawCircle` 描边模式)，透明度逐渐降低，利用 `NeonBatch` 的叠加产生发光感。
*   **星星**：随机分布的小 `drawStar` 或 `drawCircle`，带闪烁动画（改变 Alpha 值）。

#### 第二层：远景层 (Far Layer) - 慢速
*   **魔塔剪影**：
    *   塔身：使用 `drawPolygon` 绘制几个高耸的梯形或矩形堆叠。
    *   塔尖：使用三角形 (`drawPolygon`)。
    *   窗户：在塔身上绘制排列整齐的小 `drawRect`，颜色为亮黄或血红（表示塔内有魔物）。
*   **远山**：使用 `drawPolygon` 绘制连绵的深色三角形山脉。

#### 第三层：中景层 (Mid Layer) - 静态/微动 (核心焦点)
*   **魔龙 (The Dragon)** - 位于屏幕右侧：
    *   **身体**：使用 `drawCubicBezier` 绘制 S 型的龙颈和龙尾，模拟蜿蜒感。线条可以加粗。
    *   **头部**：组合几何体。`drawPolygon` (菱形/三角形) 构成头部轮廓。
    *   **眼睛**：高亮的 `drawCircle` 或 `drawStar`，颜色为猩红。
    *   **翅膀**：使用 `drawPolygon` (扇形骨架) + 半透明填充，或者使用 `drawSector`。
    *   **动画**：通过在 `update` 中微调贝塞尔曲线的控制点，实现龙的呼吸（身体起伏）或翅膀的缓慢扇动。
*   **勇者 (The Hero)** - 位于屏幕左侧下方：
    *   **身体**：抽象几何体。矩形躯干，圆形头部。
    *   **武器**：细长的 `drawRect` (剑身) + 三角形 (剑尖)。
    *   **披风**：使用 `drawQuadraticBezier` 绘制飘动的披风，并在 `update` 中根据时间正弦波改变终点，模拟风吹效果。

#### 第四层：前景层 (Foreground Layer) - 快速
*   **地面**：使用 `drawTriangleStrip` 绘制起伏的地面轮廓，颜色为深黑/深灰。
*   **环境粒子**：
    *   **余烬/火星**：大量微小的 `drawRect` 或 `drawLine`，从下往上飘动，颜色为橙红，模拟战场氛围。
    *   **迷雾**：使用低 Alpha 值的宽 `drawLine` 或 `drawRect` 快速水平滑过。

## 3. UI 交互与动效设计

### 3.1 移入式 UI (Entrance Animation)
进入标题画面时，UI 元素不直接显示，而是执行入场动画。

*   **初始状态**：
    *   UI 舞台 (`Stage`) 中的 Table 或 Window 初始 X 坐标设为 `-StageWidth` (屏幕左侧外)。
*   **入场逻辑** (`show()` 方法中触发)：
    *   使用 Scene2D 的 `MoveToAction`。
    *   **Target**: 屏幕左侧 1/4 处或原本的居中位置。
    *   **Duration**: 0.8秒 ~ 1.0秒。
    *   **Interpolation**: `Interpolation.swingOut` (带回弹) 或 `Interpolation.exp10Out` (强烈的减速感)。
*   **错位 (Stagger)**:
    *   如果按钮是分组在同一个 Table 中，可以移动整个 Table。
    *   为了更精致的效果，可以让按钮单独作为 Actor，分别设置 Action，每个按钮的 `delay` 递增 0.1s。

### 3.2 交互反馈
*   **Hover**: 按钮放大 + 颜色变亮 (Scene2D 标准行为)。
*   **Click**: 播放音效，画面可能执行一个快速的 `FadeOut` (黑屏) 或 `ZoomIn` (摄像机推向塔或门)，然后切换场景。

## 4. 技术实现方案

### 4.1 类结构
*   `TitleScreen.java`: 继承自 `GScreen`。
    *   `NeonBatch batch`: 用于绘制背景。
    *   `Stage stage`: 用于 UI。
    *   `ParallaxRenderer parallaxRenderer`: 封装绘制逻辑。
*   `ParallaxRenderer.java`: 负责所有几何绘制。
    *   方法: `render(NeonBatch batch, float delta, float cameraX)`
    *   内部类 `Dragon`, `Hero`, `Tower` 封装各自的 `draw` 方法。

### 4.2 资源需求
*   无位图纹理需求。
*   需要 `NeonBatch` 支持 `BlendFunction` (用于光晕叠加)，通常 `SpriteBatch` 默认支持。
*   颜色定义：在 `Constants` 或 `Colors` 类中定义一套配色方案（夜空蓝、月光白、魔能红、勇者金）。

### 4.3 开发步骤
1.  **基础设施**：创建 `TitleScreen`，初始化 `NeonBatch` 和 `Stage`。
2.  **背景实现**：编写 `ParallaxRenderer`，先实现背景层和远景层（塔）。调试视差滚动速度。
3.  **角色绘制**：使用 `NeonBatch` 调试绘制龙和勇者的几何造型。这是最耗时的部分，需要调整坐标和比例使其美观。
4.  **UI 实现**：重建主菜单 UI，应用移入动画。
5.  **打磨**：添加粒子系统（余烬），调整动画曲线。

## 5. 验收标准
*   [ ] 画面完全由代码绘制，无缺失图片占位符。
*   [ ] 龙和勇者的形象虽然抽象但可识别，且具有动态（呼吸/飘动）。
*   [ ] 视差滚动流畅。
*   [ ] UI 进场动画平滑且有弹性。
