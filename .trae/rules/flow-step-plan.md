---
alwaysApply: false
description: 分析与计划环节
---
# 分析与计划 (Plan)

> **目标**: 确定实现路径，避免盲目编码。

1.  **语义搜索**:
    - 使用 `SearchCodebase` 搜索相关代码。
    - 理解现有逻辑和依赖关系。

2.  **制定计划**:
    - 在对话中列出 implementation plan (Todo List)。
    - 涉及文件修改、新类创建、关键逻辑点。

3.  **用户确认** (可选):
    - 当用户指定`(自动)`等意图时, 禁止任何用户确认操作, 所有操作使用全自动执行
    - 否则, 
    - 若方案存在不确定性，使用 `AskUserQuestion`。
