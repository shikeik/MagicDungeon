# 智能任务拾取配置:
// - 目标拾取数(0则无上限): 0
// - 开始时间: 2026-02-17 10:00
// - 当前完成时间: 
// - 当前已耗时: 15min
// - 当前已完成数(开始时置0): 12


## 待拾取

- 骨骼动画: 
  - [x] 参考用户友好型库完善便捷api
  - [x] 在tests模块创建neonskel软件包, 为每一处要点编写测试, 并运行测试并通过
    - [x] 通过后在TestSelectionScreen创建演示场景, 场景结构与蒙皮场景类似, 分模式演示其效果, 可切换模式展示不同演示效果
  - 后续-分轨混合: 
    - [x] 定义 `TrackEntry` 类 (记录轨道状态, mixTime, alpha等)
    - [x] 重构 `AnimationState` 支持多轨道 (`Array<TrackEntry>`)
    - [x] 实现 `NeonTimeline` 的叠加应用逻辑 (Mix/Alpha)
    - [x] 创建 `NeonLayeredMixTest` 验证多轨道混合效果
  - 后续-IK:
    - [x] 定义 `NeonIKConstraint` 数据结构
    - [x] 实现 1-Bone (LookAt) 和 2-Bone (Analytical) IK Solver
    - [x] 创建 `NeonIKTestScreen` 验证 IK 效果

## 待检查
