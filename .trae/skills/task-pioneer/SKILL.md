---
name: task-pioneer
allowed-tools: Bash, Read, Write, SearchCodebase
description: 用于“拾取任务”、“开发新功能”或“修复Bug”。该技能会自动从 Tasks.md 提取任务并引导完整的开发生命周期（Pick-Plan-Implement-Deliver-Cleanup）。
model: claude-3-5-sonnet
argument-hint: [task-id-or-name]
user-invocable: true
---

## Pre-operation Checks
1. 确认当前 Git 工作区无未提交更改。
2. 确认 `Tasks.md` 文件存在。

## Phase 1: Pick & Decompose (任务拾取与拆解)
**目标**: 明确当前执行的任务。
1. 如果无参数，从 `Tasks.md` 智能拾取优先级最高的任务（Bug > Core > Opt）。
2. 在 `Tasks.md` 顶部记录开始时间 `(开始: HH:mm)`（北京时间）。
3. **判断**: 若任务预计超过 30 分钟或逻辑模糊，执行拆解，将子任务写入 `Tasks.md`。

## Phase 2: Plan (分析与计划)
**目标**: 确定实现路径。
1. 使用 `SearchCodebase` 理解相关逻辑。
2. 在对话中列出 Implementation Plan (Todo List)。
3. **注意**: 若指令含“(自动)”，禁止询问用户，直接开始实现。

## Phase 3: Implement & Verify (实现与验证)
**目标**: 编写代码并确保质量。
1. 遵循代码规范。
2. 运行验证：执行 `./gradlew lwjgl3:run` 和 `./gradlew assemble`。

## Phase 4: Deliver & Cleanup (交付与清理)
**目标**: 规范化提交。
1. 更新 `CHANGELOG.md`。
2. 编写 `commit-msg.txt`，执行 `git commit -F commit-msg.txt`（禁止 Push）, 保留`commit-msg.txt`文件。
3. 标记 `Tasks.md` 任务为完成，移动到 Done 区域。