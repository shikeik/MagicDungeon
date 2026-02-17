# Neon 生成与艺术化升级计划 (Version 0.x.Next)

> 版本目标：将所有程序化纹理生成迁移到 `NeonBatch` 矢量绘图，并大幅提升美术质量，实现“伪手绘”风格。

## 1. 背景与动机
目前项目的 `SpriteGenerator` 严重依赖 `Pixmap` 进行像素级操作。
- **缺点**:
  - 代码硬编码，难以维护。
  - 缩放失真 (锯齿或模糊)。
  - 缺乏抗锯齿、透明混合、复杂形状支持。
  - 每一帧修改都需要上传纹理到 GPU，性能较差 (如果是动态生成)。
- **优势**:
  - `NeonBatch` 支持矢量绘制 (Rect, Circle, Polygon, Bezier)。
  - 理论上支持更好的缩放 (取决于实现)。
  - 支持任意分辨率生成。

## 2. 核心技术方案 (验证清单)

### 2.1 架构调整
- [ ] **NeonGenerator 类**: 创建 `NeonGenerator` 替代 `SpriteGenerator`。
  - [ ] **输入**: `NeonBatch`, `ThemeConfig`, 参数 (Size, Seed, Type)。
  - [ ] **输出**: `FrameBuffer` -> `TextureRegion` 转换逻辑。
  - [ ] **缓存**: `TextureManager` 缓存机制适配。

### 2.2 艺术风格升级 (可行性验证)
利用 `NeonBatch` 的特性实现以下效果：
- [ ] **轮廓线 (Outline)**: 
  - [ ] 验证: 是否可以通过绘制两层 (底层黑色稍大) 来模拟描边?
  - [ ] 验证: 复杂多边形的描边算法是否支持?
- [ ] **渐变填充 (Gradients)**: 
  - [x] 验证: `drawSkewGradientRect` 支持水平渐变。
  - [x] 验证: `drawTriangleStrip` 支持顶点颜色渐变。
  - [ ] 验证: 是否需要实现径向渐变 (Radial Gradient)? (目前 NeonBatch 未原生支持，可能需要 Shader 或多边形拟合)。
- [ ] **抗锯齿 (Anti-aliasing)**:
  - [ ] 验证: 目前 `NeonBatch` 使用 `drawSolidTriangle`，依赖 GPU 光栅化，无内置 AA。需验证是否开启 MSAA 或引入 Shader 实现平滑边缘。
- [ ] **细节纹理 (Details)**: 
  - [ ] 验证: 使用 `drawLine` / `drawCircle` 绘制噪点和划痕的性能开销。
- [ ] **阴影 (Shadows)**: 
  - [ ] 验证: 手动绘制半透明黑色图层的效果。

### 2.3 迁移计划 (Phase Breakdown)

#### Phase 1: 基础设施建设
- [x] 修复 `NeonGenTestScreen` 闪退问题 (已完成)。
- [ ] **NeonBatch 功能补全**:
  - [ ] 实现/验证 `pathStroke` 的连接点 (Join) 效果 (Miter/Round/Bevel)。
  - [ ] 考虑添加 `drawShape(Shape shape)` 统一接口。
- [ ] 创建 `NeonGenerator` 基类框架。

#### Phase 2: 核心纹理迁移
- [ ] **Player**: 重写 `createPlayer`。
  - [ ] 支持纸娃娃系统 (换装) 的矢量图层叠加。
- [ ] **Tiles**: 地板、墙壁、门、楼梯。
- [ ] **Items**: 药水、武器、防具图标。

#### Phase 3: 视觉增强
- [ ] **光照**: 为生成的物体添加“伪光照” (高光/阴影层)。
- [ ] **配色**: 优化 `ThemeConfig`。

## 3. 具体实施细节

### 3.1 NeonGenerator 接口设计
```java
public class NeonGenerator {
    private FrameBuffer fbo;
    private NeonBatch batch;
    
    // [ ] Verify: FBO 资源管理策略 (池化 vs 单例 vs 每次创建)
    public TextureRegion generate(int width, int height, Consumer<NeonBatch> drawer) {
        // Setup FBO, Camera, Batch
        // drawer.accept(batch);
        // Extract Texture
        // Return Region
    }
}
```

### 3.2 性能优化
- [ ] **显存监控**: 监控 FrameBuffer 即使释放。
- [ ] **图集打包**: 验证 `PixmapPacker` 能否在运行时高效打包由 FBO 生成的纹理。

## 4. 风险评估
- **显存占用**: 大量使用 FrameBuffer 可能会增加显存压力。
- **生成耗时**: 复杂的矢量绘制可能比 Pixmap 慢 (涉及大量 GPU 提交)。
- **AA 缺失**: 如果没有 Shader/MSAA，矢量图在低分辨率下可能有明显锯齿。

## 5. 里程碑
- **M1**: `NeonGenTestScreen` 能稳定展示高质量的 Player 矢量图 (含描边、渐变)。
- **M2**: 游戏内替换所有 `SpriteGenerator` 调用。
- **M3**: 视觉风格统一化调整。
