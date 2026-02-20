---
alwaysApply: false
description: 定义 MagicDungeon2 项目进行次要版本更新 (x.x -> x.x+1) 的完整生命周期。
---
# Minor 版本更新流程指南 (Minor Version Update Guide)

> 本文档定义了 MagicDungeon2 项目进行次要版本更新 (x.x -> x.x+1) 的完整生命周期。
> 适用于引入新功能、重大重构或视觉升级的迭代。

## 1. 定义与触发条件
- **Minor Version (次要版本)**: 包含向下兼容的功能性新增。
- **Trigger (触发)**: 当积累了足够的特性，或完成了一个完整的 Milestone (里程碑) 时。

## 2. 流程概览
1. **[P] 规划与主题定义 (Planning & Theme)**
2. **[D] 开发与迭代 (Development)**
3. **[S] 稳定与交付 (Stabilization & Delivery)**

---

## 3. 详细步骤

### [P] 规划与主题定义
> 在开始编码前，必须明确本版本的核心目标。

1.  **审查现状**:
    - 检查 `Tasks.md` 中的积压任务。
    - 检查 `tech-debt.md` 中的高优先级债务。
2.  **定义主题 (Define Theme)**:
    - 为下一个版本确立一个核心主题 (例如: "视觉升级", "战斗系统重构", "新地牢区域")。
    - **[关键步骤] 撰写版本设计案**:
        - 在 `.trae/documents/designs/` 下创建文档 (如 `35_Neon生成与艺术化升级计划.md`)。
        - 明确目标、技术方案、里程碑。
3.  **更新规划**:
    - 在 `Tasks.md` 中创建新的版本分区 (e.g., `## Version 0.2.0 - Neon Upgrade`)。
    - 将设计案中的里程碑拆解为具体 Task 填入。

### [D] 开发与迭代
> 遵循标准的开发流程 (参见 `develop-flow.md`)。

1.  **拾取任务**: 按照规划顺序执行任务。
2.  **持续记录**: 
    - 每次完成重要功能或修复，**立即**更新 `CHANGELOG.md` 的 `[未发布] (Unreleased)` 区域。
    - 格式: `- 修复了 NeonGenTestScreen 的闪退问题 (#Fix)`.

### [S] 稳定与交付
> 当所有里程碑任务完成时进入此阶段。

1.  **全量回归测试**:
    - 运行所有测试屏幕 (`TestSelectionScreen`)。
    - 验证核心玩法流程。
2.  **[关键步骤] 撰写 CHANGELOG**:
    - 将 `CHANGELOG.md` 中的 `[未发布]` 标题修改为当前版本号和日期 (e.g., `## [0.2.0] - 2026-02-17`)。
    - 创建新的空 `## [未发布]` 区域在顶部。
3.  **推进版本 (Release)**:
    > 完整交付标准与操作流程请务必遵循 **[交付与版本环节 (Deliver)](flow-step-deliver.md)**。

    **核心步骤摘要**:
    - **版本推进**: 运行 `gradle bumpMinor` 自动升级 Minor 版本。
    - **封版提交**: 遵循提交规范 commit 所有变更。
    - **打标签**: `git tag x.x.0`。
    - **任务清理**: 归档 `Tasks.md` 中已完成的任务。

## 4. 常用检查清单 (Checklist)

- [ ] 版本主题是否明确? 设计文档是否已创建?
- [ ] 所有 P0/P1 技术债是否已解决?
- [ ] `CHANGELOG.md` 是否包含了本版本所有变更?
- [ ] `build.gradle` 版本号是否已更新?
- [ ] 所有测试屏幕是否通过?
