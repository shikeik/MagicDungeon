# 智能任务拾取配置:
// - 目标拾取数(0则无上限): 4
// - 开始时间: 2026-02-17 10:15
// - 当前完成时间: 
// - 当前已耗时: 0min
// - 当前已完成数(开始时置0): 0


## 待拾取
- [Core] 分轨混合: 定义 `TrackEntry` 类 (记录轨道状态, mixTime, alpha等)
- [Core] 分轨混合: 重构 `AnimationState` 支持多轨道 (`Array<TrackEntry>`)
- [Core] 分轨混合: 实现 `NeonTimeline` 的叠加应用逻辑 (Mix/Alpha)
- [Test] 分轨混合: 创建 `NeonLayeredMixTest` 验证多轨道混合效果

## 待检查
