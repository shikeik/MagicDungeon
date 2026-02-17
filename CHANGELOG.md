# 更新日志 (Changelog)

本项目的所有显著更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本控制 (Semantic Versioning)](https://semver.org/lang/zh-CN/)。

## [未发布] (Unreleased)

### 新增 (Added)
- 文档: 建立了 Minor 版本更新流程 (`.trae/rules/flow-version-minor.md`)。
- 计划: 启动了 [Neon 生成与艺术化升级计划] (版本 0.x.Next)。

### 修复 (Fixed)
- 修复了 `NeonGenTestScreen` 中的闪退问题 (FrameBuffer 销毁逻辑与异常捕获)。
- 修复了 `TextureExporter` 导致的显存泄漏问题。
- 修复了 `GameScreen` 渲染循环中频繁切换 Batch 导致的性能损耗。

### 变更 (Changed)
- 重构: 将 `GameHUD` 中的战斗日志逻辑提取为 `CombatLogUI`。
- 优化: 引入 `ThemeConfig` 以减少绘图代码中的硬编码。
- 整理: 重命名并规范化了 `.trae/optimization` 目录结构。
