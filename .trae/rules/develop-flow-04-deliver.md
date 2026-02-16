---
alwaysApply: false
description: 阶段4：交付与版本规则
---
# 第四阶段：交付与版本 (Deliver)

1.  **版本决策**:
    - **内容性更新**: 必须执行发版。
        - **常规修复/优化**: 执行 `gradle bumpPatch`。
        - **次版本特性**: 执行 `gradle bumpMinor` (需遵循次版本流程: 确定主题 -> 开发 -> 提版 -> 下个主题 -> 提交)。
    - **纯杂项/文档**: 可跳过发版。

2.  **规范提交** (必须):
    - **格式**: `type(scope): summary` (必须中文描述, 参照 `.git/hooks/commit-msg` 规范)
        - `feat`: 新功能
        - `fix`: 修复
        - `docs`: 文档
        - `style`: 格式
        - `refactor`: 重构
        - `perf`: 性能
        - `test`: 测试
        - `chore`: 杂务
        - `assets`: 资源
    - **操作**:
        1. 编写 `commit_msg.txt` (确保简短高效)。
        2. 执行 `git commit -F commit_msg.txt`。
    - **禁止 Push**: 仅本地提交。
