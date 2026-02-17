---
alwaysApply: false
description: 技术债优化流入口
---
# 技术债优化流 (Optimization Flow)

> **适用场景**: 代码审查、性能优化、重构、清理技术债。

## 1. 流程图

**1. 审查与识别** (Review)
  ↓
**2. 分析与计划** (Plan) -> **3. 实现与验证** (Implement) -> **4. 交付与版本** (Deliver) -> **5. 清理** (Cleanup)
  ↓
**[判断]** 仍有高优债务 (P0/P1)?
  ├─ [是] -> (回到 1. 审查)
  └─ [否] -> **结束**

## 2. 环节索引
1.  **[Review]**: [审查与识别](flow-step-review.md)
2.  **[Plan]**: [分析与计划](flow-step-plan.md)
3.  **[Implement]**: [实现与验证](flow-step-implement.md) (严禁引入新功能)
4.  **[Deliver]**: [交付与版本](flow-step-deliver.md)
5.  **[Cleanup]**: [清理](flow-step-cleanup.md)

## 3. 循环决策
*   **仍有高优先级债务 (P0/P1)**:
    *   **自动循环**: 立即读取 `flow-step-review.md`。
*   **无高优债务**:
    *   **结束**: 汇报优化成果。
