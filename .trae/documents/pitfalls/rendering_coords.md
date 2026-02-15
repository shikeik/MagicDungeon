# 渲染坐标对齐坑

## 问题描述
在开发过程中，经常会误解 LibGDX 的坐标对齐逻辑。
例如：`左侧渲染区x1/4,y1/2`，原本以为是“以该点为中心对齐”，结果实现成了“以该点为左下角”。
导致渲染的披风或其他元素位置偏移。

## 正确理解
- LibGDX 的 `batch.draw(texture, x, y)` 默认是以 `(x, y)` 为图片的左下角进行绘制。
- 如果需要以 `(centerX, centerY)` 为中心绘制，计算公式应为：
  `x = centerX - textureWidth / 2`
  `y = centerY - textureHeight / 2`
- 在处理 UI 布局或相对定位时，必须明确当前的 `(x, y)` 到底是锚点还是左下角。

## 预防措施
1. 在变量命名时明确含义，如 `drawX`, `centerX`。
2. 封装常用的对齐绘制方法，如 `drawCentered(batch, texture, x, y)`。
3. 在注释中明确说明坐标原点。
