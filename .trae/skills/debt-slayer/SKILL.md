---
name: debt-slayer
allowed-tools: Bash, Read, Write, SearchCodebase
description: 用于“优化任务”或“技术债审查”。该技能会扫描代码库中的 TODO/FIXME 和 tech-debt.md，并强制执行重构任务。
model: claude-3-5-sonnet
user-invocable: true
---

## Phase 1: Review & Identify (审查)
1. 搜索代码库中的 `TODO` 和 `FIXME`。
2. 读取 `.trae/tech-debt.md`。
3. **强制逻辑**: 即使无高优任务，也必须选取 1-3 个低优先级条目进行优化。

## Phase 2: Execute Refactor (重构实现)
1. 执行 `Plan` 和 `Implement` 环节。
2. **禁令**: 严禁在此环节引入任何新功能逻辑，只允许纯粹的代码重构和优化。

## Phase 3: Update Debt List
1. 完成后更新 `.trae/tech-debt.md`。
2. 按照 `task-pioneer` 的规范执行本地 Commit。