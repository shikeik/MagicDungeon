---
alwaysApply: false
description: 审查与识别环节
---
# 审查与识别 (Review & Identify)

> **目标**: 发现代码中的问题和改进点。

1.  **扫描**:
    - 搜索 `TODO`, `FIXME` 标记。
    - 检查 `.trae/tech-debt.md` 清单。
    - 审查核心模块 (Dispose, DRY, Magic Numbers)。

2.  **记录**:
    - 将新发现的问题写入 `.trae/tech-debt.md`。
    - 标记优先级 (P0/P1/P2)。

3.  **选择 (强制自动执行)**:
    - 优先选取 1-3 个高优先级 (P0/P1) 条目。
    - **若无高优条目，则必须选取 1-3 个中低优先级 (P2/P3) 条目**。
    - **严禁**在此环节因“无高优任务”而停止流程。
