# FBO与TextureRegion坐标翻转坑

## 问题描述
在使用 `FrameBuffer` (FBO) 生成纹理，并使用 `TextureRegion` 包装后，通过 `SpriteBatch` 绘制时，发现图像是**倒立**的。

## 原因分析
1.  **FBO 坐标系**: OpenGL 默认 FBO 坐标系是 **Y-Up** (原点在左下角)。我们在绘制到 FBO 时也是按 Y-Up 逻辑绘制的 (正立)。
2.  **像素读取**: `Pixmap.createFromFrameBuffer(0, 0, w, h)` 从 FBO 读取像素。它会把 FBO 的第 0 行 (物理底部) 读取到 Pixmap 的内存第 0 行 (逻辑头部)。
3.  **纹理上传**: `new Texture(pixmap)` 将数据上传到 GPU。OpenGL 纹理坐标 (UV) 中，V=0 对应内存数据的头部。因此，**V=0 对应图像的物理底部**。
4.  **SpriteBatch 映射**: LibGDX 的 `SpriteBatch` 在绘制矩形时，默认将 **V=0 映射到矩形的顶部 (Top Edge)** (为了兼容常见的 Y-Down 图片存储格式)。
5.  **冲突**: 
    *   纹理 V=0 是 **底**。
    *   屏幕绘制 Top 是 **V=0**。
    *   结果：屏幕 Top 显示了 纹理底 -> **倒立**。

## 解决方案
在从 FBO 创建 `TextureRegion` 后，必须**手动翻转 Y 轴**，使 V=0 指向纹理的数据尾 (即 FBO 的顶部)。

```java
TextureRegion region = new TextureRegion(texture);
// flip(x, y)
region.flip(false, true); 
```

## 验证
翻转后：
- V=1 (指向数据头/FBO底)。
- SpriteBatch 绘制 Bottom (Y) 时使用 V=1 -> 显示 FBO 底部。
- SpriteBatch 绘制 Top (Y+H) 时使用 V=0 -> 显示 FBO 顶部。
- **正立**。
