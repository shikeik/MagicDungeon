---
alwaysApply: false
description: AI 自动开发流程 (简明版)
---

# AI 自动开发流程 (简明版)

> **核心循环**: 分析 -> 计划 -> 实现 -> 验证 -> 提交

## 1. 开发五步曲
1. **明确需求 (Analyze)**: 分析影响范围，制定简明 TODO 计划。
2. **实现变更 (Implement)**: 小步修改，复用现有逻辑，**严禁**破坏现有代码风格。
3. **验证测试 (Verify)**: 
   - 桌面运行: `./gradlew lwjgl3:run` 确保无崩溃。
   - 全局构建: `./gradlew assemble` 确保编译通过。
4. **版本提交 (Commit)**: 
   - 升级版本: `./gradlew bumpPatch` (修复) 或 `bumpMinor` (新功能)。
   - 规范提交: `git commit -m "<type>: <summary>"` (精简描述，限 1-2 行)。
5. **清理交付 (Clean)**: 移除调试代码，更新文档，标记 TODO 完成。

## 2. 常用命令速查
- **运行桌面版**: `./gradlew lwjgl3:run`
- **运行安卓版**: `./gradlew android:installDebug` (安装后手动打开)
- **全局构建**: `./gradlew assemble` (全平台编译检查)
- **清理构建**: `./gradlew clean`
