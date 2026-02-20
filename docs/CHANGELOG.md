# 更新日志 (Changelog)

本项目的所有显著更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本控制 (Semantic Versioning)](https://semver.org/lang/zh-CN/)。

## [未发布(最新)] (Unreleased)

---

## [0.2.0] - 2026-02-21

### 新增 (Added)

- 核心数据模型系统 — 6 个源文件 + 4 个测试文件，共 72 个单元测试
  - `StatType`: 6 大属性枚举 (HP/MP/ATK/DEF/ASP/MOV)，含 `valuePerPoint` 与 `isAllocatable`
  - `StatData`: 属性容器与统一计算流水线，支持装备/百分比/突破加成
  - `StatCalculator`: 等级点数公式 (`fixedPointsPerStat`, `totalFreePoints`, `totalPoints`)
  - `CombatEngine`: 物理/魔法/穿透伤害引擎，穿透衰减 0.7，最低阈值 0.1
  - `GrowthCalculator`: 经验-等级公式 (`100×1.2^(L-1)`)，返回 `long`，全链路溢出保护
  - `DeathPenalty`: 死亡惩罚系统 (经验 20%/金币 50%/装备 2-5 件)，自由点循环均匀扣除
- 文档: 重构系统设计案 `2_0_系统设计案.md`，以实际代码为权威同步

### 修复 (Fixed)

- `StatData.addFreePoints` 增加余额校验与不可加点属性拒绝，返回 `boolean`
- `StatData.validate()` 校验自由点分配不超限
- `CombatEngine` 穿透衰减伤害低于 0.1 时归零（首目标豁免）
- `GrowthCalculator.xpForNextLevel` 从 `int` 改为 `long`，防止高等级 (>95) 溢出
- `GrowthCalculator` 全链路溢出保护 (`totalXpForLevel`/`levelFromXp`/`xpProgress`)

### 变更 (Changed)

- 属性与伤害系统 `int` 全面改为 `float`
- `CombatEngine.calcMagicDamage` 改由调用方传入 MDEF（不内部计算 DEF/2）
- ASP/MOV 统一为乘法管线: `min((base+equip)×(1+pct), 3.0) + uncapped`
- 死亡惩罚自由点扣除改为循环均匀分配算法（每轮对有余额属性均匀扣减）
- `StatData` 点数公式全部委托 `StatCalculator`，消除重复
- `GrowthCalculator.totalXpForLevel` 累加优化为局部变量，避免逐级调用

---

## [0.1.2] - 2026-02-20

### 新增 (Added)

- 新增 `magicdungeon2.BuildConfig` 独立包名配置
- 新增 `globalFileNameAndContentReplace.sh` 全局文件名+内容替换脚本

### 修复 (Fixed)

### 变更 (Changed)

- 项目重命名: MagicDungeon → MagicDungeon2
- 版本重置: 0.9.0 → 0.1.2
- 包名更新: com.goldsprite.magicdungeon → com.goldsprite.magicdungeon2
- 同步更新所有构建配置、运行配置、文档、脚本中的项目名引用
