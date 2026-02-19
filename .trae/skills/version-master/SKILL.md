---
name: version-master
allowed-tools: Bash, Read, Write
description: 用于处理 Minor (x.x -> x.x+1) 版本更新。包含设计案撰写、Changelog 归档、版本号推进和打标签。
model: claude-3-5-sonnet
user-invocable: true
---

## Phase 1: Planning
1. 检查 `Tasks.md` 的积压情况。
2. 定义版本主题，在 `.trae/minor_themes/` 下创建设计文档。

## Phase 2: Stabilization
1. 运行 `TestSelectionScreen` 进行全量回归测试。
2. 将 `CHANGELOG.md` 中的 `[未发布]` 标题修改为正式版本号和日期。

## Phase 3: Release Operation
1. 运行 `gradle bumpMinor` 自动升级版本号。
2. 执行封版提交：`git commit -m "chore(release): version x.x.0"`。
3. 执行 `git tag x.x.0`。
4. 归档 `Tasks.md`。