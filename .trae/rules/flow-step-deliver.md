---
alwaysApply: false
description: 交付与版本环节
---
# 交付与版本 (Deliver)

> **目标**: 规范化提交变更。

1.  **版本号推进**:
    - **内容性更新** (feat/fix/perf): 视情况执行 `gradle bumpPatch` 或 `gradle bumpMinor`。
    - **纯杂项/文档** (docs/chore): 可跳过版本号更新。

2.  **规范提交**:
    - **格式**: `type(scope): summary` (必须中文)。
        - `feat`, `fix`, `docs`, `style`, `refactor`, `perf`, `test`, `chore`.
    - **操作**:
        1. 编写 `commit_msg.txt`。
        2. 执行 `git commit -F commit_msg.txt`。
    - **禁止 Push**: 仅本地提交。
