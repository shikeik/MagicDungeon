# Neon 纹理生成与坐标系详解

本文档详细解析 `NeonGenerator` 的纹理生成逻辑以及 `NeonGenTestScreen` 的渲染逻辑，重点阐述坐标系原点处理，确保在 LibGDX 环境下获得正确的视觉效果。

## 1. 核心结论 (TL;DR)

**LibGDX 默认采用 Y-Up (左下角为原点) 坐标系**。为了保证逻辑统一且不产生混淆，我们的 Neon 生成系统全流程均遵循 Y-Up 标准：

1.  **生成器 (NeonGenerator)**：使用 `setToOrtho2D(0, 0, 1, 1)`，定义 (0,0) 为左下角，(1,1) 为右上角。
2.  **绘制指令 (NeonBatch)**：所有绘制指令 (如 `drawRect`) 的 Y 坐标均基于 **左下角**。Y=0 是底部，Y=1 是顶部。
3.  **纹理提取 (FBO -> Texture)**：FBO 也是 Y-Up 的。提取出的 `TextureRegion` **不需要翻转 (flip)**，即 `flip(false, false)`，保持原始数据方向。
4.  **渲染 (SpriteBatch/Stage)**：使用默认的 Y-Up 相机。绘制时，纹理的底部 (V=0) 对齐屏幕的底部。

**只要全流程坚持 Y-Up，就不需要任何额外的 Flip 操作。**

---

## 2. 详细流程解析

### 2.1 生成端: `NeonGenerator.java`

这个类负责创建一个 FrameBuffer (FBO)，设置投影矩阵，执行绘制回调，然后提取结果。

#### 关键代码逐行解析

```java
// [NeonGenerator.java]

// 1. 设置投影矩阵
// setToOrtho2D(x, y, width, height)
// 这里设置视口为 x=0, y=0, w=1, h=1。
// 在 OpenGL 默认行为中，Projection Matrix 决定了坐标如何映射到 NDC (标准化设备坐标)。
// LibGDX 的 setToOrtho2D 默认产生 Y-Up 的投影 (y=0 在下, y=h 在上)。
// 所以这里 (0,0) 对应 FBO 的左下角，(1,1) 对应 FBO 的右上角。
projectionMatrix.setToOrtho2D(0, 0, 1, 1);

// ...

frameBuffer.begin();
// 2. 设置视口
// 必须设置 Viewport 匹配 FBO 尺寸，否则绘制会错位或缩放。
// 0,0 是 FBO 左下角。
Gdx.gl.glViewport(0, 0, width, height);

ScreenUtils.clear(0, 0, 0, 0);

batch.setProjectionMatrix(projectionMatrix);
batch.begin();
// 3. 执行绘制
// drawer 里的代码 (如 drawCharacter) 使用 0~1 坐标。
// 例如 drawRect(0, 0, 1, 0.5) 会画在 FBO 的下半部分。
drawer.accept(batch);
batch.end();

// 4. 提取纹理
region = extractTextureRegion(width, height);
```

#### 纹理提取逻辑

```java
// [NeonGenerator.java] -> extractTextureRegion

// 1. 读取像素
// Pixmap.createFromFrameBuffer(0, 0, w, h) 从当前绑定的 FBO 读取像素。
// FBO 的 (0,0) 是左下角像素。
// Pixmap 的内存布局通常是：第 0 行对应图像顶部 (Y-Down)。
// 但是！createFromFrameBuffer 读取时，OpenGL 的第 0 行 (底部) 会被读入 Pixmap 的第 0 行。
// 如果我们把这个 Pixmap 存成 PNG，图像是倒立的 (因为 PNG 期望第 0 行是顶部，但我们给的是底部数据)。
Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);

// 2. 创建纹理
// new Texture(pixmap) 将像素上传回 GPU。
// 如果 Pixmap 第 0 行是“底部数据”，上传后 OpenGL 纹理的第 0 行也是“底部数据”。
// 在 OpenGL 中，纹理坐标 V=0 对应第 0 行。
// 所以，V=0 对应图像底部。
Texture texture = new Texture(pixmap);

// 3. 创建 Region
// TextureRegion 默认 UV 为 (0,0) 到 (1,1)。
// U=0 (左), V=0 (底)。
TextureRegion region = new TextureRegion(texture);

// 4. 翻转设置
// 我们的纹理数据：V=0 是底部内容。
// 我们的渲染目标 (SpriteBatch 默认)：期望 V=0 是底部内容 (因为 SpriteBatch 也是 Y-Up 的)。
// 所以：方向一致，不需要翻转。
region.flip(false, false); 
```

---

### 2.2 消费端: `NeonGenTestScreen.java`

测试屏幕负责展示生成结果。它有两种模式：实时矢量 (Live Vector) 和 烘焙纹理 (Baked Texture)。

#### 模式 A: 实时矢量 (Live Vector)

此模式直接在屏幕上绘制矢量图形，不经过 FBO。

```java
// [NeonGenTestScreen.java]

// 1. 设置投影矩阵
// stage.getCamera() 是 Y-Up 的 (0,0 在屏幕左下角)。
neonBatch.setProjectionMatrix(stage.getCamera().combined);

neonBatch.begin();

// 2. 设置变换矩阵 (Transform)
// 我们需要把 0~1 的生成器坐标映射到屏幕上的具体位置和大小。
// cx, cy 是目标矩形的左下角 (因为 Stage 是 Y-Up)。
// cw, ch 是宽高。
neonBatch.getTransformMatrix().idt()
    .translate(cx, cy, 0) // 移到屏幕位置
    .scale(cw, ch, 1f);   // 放大到目标尺寸 (0~1 -> 0~cw/ch)

// 3. 绘制内容
// 生成器代码 (如 drawRect) 在 0~1 空间绘制。
// 经过 Matrix 变换后，(0,0) 变成了屏幕上的 (cx, cy)，(1,1) 变成了 (cx+cw, cy+ch)。
// 结果：正立的图像。
drawContent(neonBatch);

neonBatch.end();
```

#### 模式 B: 烘焙纹理 (Baked Texture)

此模式绘制 `NeonGenerator` 生成的 `TextureRegion`。

```java
// [NeonGenTestScreen.java]

// 1. 获取 Region
// 这个 region 来自 NeonGenerator，如前所述，它是 Y-Up 的 (V=0 是底)。
// 且没有翻转 (flip=false, false)。

// 2. 绘制
// batch.draw(region, x, y, w, h)
// SpriteBatch 使用 Stage 的 Camera (Y-Up)。
// draw 方法会将 region 映射到一个矩形上。
// 矩形左下角 (x, y) 对应 Region 的 (u, v2) 还是 (u, v)?
// LibGDX SpriteBatch 默认逻辑：
// 顶点 0 (左下): u, v2 (默认 v2=1) -> 对应纹理顶部? 等等。
// 让我们看 TextureRegion 的默认值：u=0, v=0, u2=1, v2=1。
// SpriteBatch 绘制时的顶点 UV 映射：
// Bottom-Left  (x, y)     -> u, v2 (0, 1)  => 对应纹理 Top (V=1)
// Top-Left     (x, y+h)   -> u, v  (0, 0)  => 对应纹理 Bottom (V=0)
// Bottom-Right (x+w, y)   -> u2, v2(1, 1)  => 对应纹理 Top (V=1)
// Top-Right    (x+w, y+h) -> u2, v (1, 0)  => 对应纹理 Bottom (V=0)

// ！！！注意！！！
// 这里有一个极其容易混淆的点：
// SpriteBatch 默认假定纹理坐标 V=0 是图片的上边缘 (Top)，V=1 是下边缘 (Bottom)。
// 这是为了兼容通常的图片加载方式 (图片数据通常是从上到下存储的)。
// 
// 但是，我们的 FBO 纹理数据是 V=0 为底部内容 (因为 createFromFrameBuffer 是倒着读的)。
// 所以：
// 屏幕 Top (y+h) 采样 V=0 (底部内容) -> 屏幕上方显示了图像底部 -> **倒立**！
// 屏幕 Bottom (y) 采样 V=1 (顶部内容) -> 屏幕下方显示了图像顶部。
```

### 2.3 修正结论：需要 Flip！

根据上述深入分析 (特别是 SpriteBatch 的默认 UV 映射行为)，如果 `TextureRegion` 保持 `flip(false, false)`，**在 `SpriteBatch` (Y-Up) 中绘制出来的结果将是倒立的**。

**原因链条：**
1.  **FBO**: 0,0 是左下角。绘制正立图像。
2.  **Pixmap**: `createFromFrameBuffer` 把 FBO 第 0 行 (底) 读入 Pixmap 第 0 行 (数据头)。
3.  **Texture**: 上传 Pixmap。OpenGL 纹理 V=0 对应数据头 (即图像底)。
4.  **SpriteBatch**: 绘制时，将矩形**上方** (y+h) 映射到 V=0。
5.  **结果**: 矩形上方显示了图像底部 -> **倒立**。

**解决方案：**
必须在 `NeonGenerator.extractTextureRegion` 中执行 `region.flip(false, true)`。
翻转后：
*   V=1, V2=0。
*   SpriteBatch 矩形上方 (y+h) 映射到 V (1)。V=1 是 OpenGL 纹理的尾部 (数据尾) = FBO 的顶部。-> **正确 (顶对顶)**。
*   SpriteBatch 矩形下方 (y) 映射到 V2 (0)。V2=0 是 OpenGL 纹理的头部 (数据头) = FBO 的底部。-> **正确 (底对底)**。

## 3. 最终代码规范

### NeonGenerator.java (修正版)

```java
private TextureRegion extractTextureRegion(int width, int height) {
    Pixmap pixmap = Pixmap.createFromFrameBuffer(0, 0, width, height);
    // ... filter settings ...
    Texture texture = new Texture(pixmap);
    pixmap.dispose();

    TextureRegion region = new TextureRegion(texture);
    
    // 关键修正：必须翻转 Y 轴！
    // 因为 createFromFrameBuffer 读取的数据导致 V=0 指向图像底部。
    // 而 SpriteBatch 期望 V=0 指向图像顶部。
    region.flip(false, true); 

    return region;
}
```

### NeonGenTestScreen.java

**Live Vector 模式** 不需要任何翻转，因为它是直接的几何绘制，不涉及纹理采样。

```java
// 正确
neonBatch.getTransformMatrix().idt().translate(cx, cy, 0).scale(cw, ch, 1f);
// 不需要 scale(1, -1) 或其他翻转
```

**Baked Texture 模式** 使用 `batch.draw`，依赖 `TextureRegion` 的 flip 状态。只要 `NeonGenerator` 里 flip 了，这里就正常画。

```java
// 正确
batch.draw(bakedRegion, cx, cy, cw, ch);
```

## 4. 总结

为了得到“正确的左下原点纹理”效果：
1.  **生成**：在 FBO 里按 Y-Up 绘制 (0 在下)。
2.  **提取**：从 FBO 读出后，必须 `flip(false, true)`。
3.  **显示**：直接绘制即可。

如果 `NeonGenerator` 目前是 `flip(false, false)`，那么你在屏幕上看到的烘焙结果应该是倒立的。如果看起来是正的，那一定有其他地方负负得正了 (比如生成代码本身是倒着画的，或者 Batch 设置了特殊的 Projection)。但根据标准推演，应该 flip。
