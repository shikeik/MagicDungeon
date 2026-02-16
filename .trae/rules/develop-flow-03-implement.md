---
alwaysApply: false
description: 阶段3：实现与验证规则
---
# 第三阶段：实现与验证 (Do & Check)

1.  **实现变更**:
    - **小步修改**: 避免一次性修改过多文件。
    - **复用逻辑**: 优先使用现有工具类和模式。
    - **保持风格**: 代码风格需与现有代码库一致。
2.  **验证测试**:
    - **运行检查**: 执行 `./gradlew lwjgl3:run` 确保程序运行无崩溃。
    - **构建检查**: 执行 `./gradlew assemble` 确保编译完全通过。
3.  **自动化可视化测试**:
    - **场景**: 当涉及复杂 UI 交互、长流程操作或无法编写常规单元测试时，必须使用 `HumanSimulatorTest` 进行模拟验证。
    - **原理**: 该测试模拟人类输入（按键、点击、拖拽），在真实游戏环境中执行。
    - **位置**: `examples/src/main/java/com/goldsprite/magicdungeon/testing/HumanSimulatorTest.java`
    - **扩展**: 根据需求向 `HumanSimulatorTest` 添加新的测试步骤（如输入作弊码、穿戴装备等）。
