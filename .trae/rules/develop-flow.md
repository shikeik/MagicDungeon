---
alwaysApply: true
---
# AI 自动开发流程

> **核心原则**: 严格遵循"环节推进"机制，完成一个环节后，**必须**重新回到此文件查看下一环节的行动指导。

## 核心循环流程图

```mermaid
flowchart TD
    Start([开始]) --> Pick[1. 智能拾取 & 启动]
    
    subgraph Execution [执行循环]
        Pick --> CheckComplex{任务过大?}
        CheckComplex -- 是 --> Decompose[1.5 任务拆解]
        Decompose --> Pick
        CheckComplex -- 否 --> Analyze[2. 需求分析 & 计划]
        Analyze --> Implement[3. 代码实现]
        Implement --> Verify[4. 验证测试]
        Verify --> Release[5. 版本 & 提交]
        Release --> Cleanup[6. 清理 & 交付]
    end
    
    Cleanup --> LoopCheck{任务达标 或 无任务?}
    LoopCheck -- 未结束 --> Pick
    LoopCheck -- 已结束 --> Summary[收尾统计]
    Summary --> End([结束])
```

## 1. 详细执行环节索引

*   [第一阶段：任务启动 (Pick)](develop-flow-01-pick.md)
*   [阶段 1.5：任务拆解 (Decompose - 可选)](develop-flow-01-1-decompose.md)
*   [第二阶段：分析与计划 (Plan)](develop-flow-02-plan.md)
*   [第三阶段：实现与验证 (Do & Check)](develop-flow-03-implement.md)
*   [第四阶段：交付与版本 (Deliver)](develop-flow-04-deliver.md)
*   [第五阶段：清理与循环 (Loop)](develop-flow-05-loop.md)

## 2. 常用命令速查

*   [常用命令速查表](develop-flow-99-commands.md)
