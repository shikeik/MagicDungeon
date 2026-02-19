---
alwaysApply: false
description: 功能开发流入口
---
# 功能开发流 (Feature Flow)

> **适用场景**: 实现新功能、修复 Bug、常规迭代。

## 1. 流程图

**1. 任务启动** (Pick)
  ↓
**[判断]** 任务是否过大?
  ├─ [是] -> **1.5 任务拆解** (Decompose) -> (回到 1. 启动)
  └─ [否] -> **2. 分析与计划** (Plan) -> **3. 实现与验证** (Implement) -> **4. 交付与版本** (Deliver) -> **5. 清理** (Cleanup)
       ↓
**[判断]** 任务达标或无任务?
  ├─ [否] -> (回到 1. 启动)
  └─ [是] -> **收尾统计** -> **结束**

## 2. 环节索引
1.  **[Pick]**: [任务启动](flow-step-pick.md)
2.  **[Decompose]**: [任务拆解](flow-step-decompose.md) (可选)
3.  **[Plan]**: [分析与计划](flow-step-plan.md)
4.  **[Implement]**: [实现与验证](flow-step-implement.md)
5.  **[Deliver]**: [交付与版本](flow-step-deliver.md)
6.  **[Cleanup]**: [清理](flow-step-cleanup.md)

## 3. 循环决策
*   **未达标 (已完成 < 目标数) 且 有剩余任务**:
    *   **自动循环**: 立即读取 `flow-step-pick.md`。
    *   **严禁停止**: 不要询问用户。
*   **已达标 或 无剩余任务**:
    *   **结束**: 汇报成果，重置计数器。
