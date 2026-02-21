# 更新日志 (Changelog)

本项目的所有显著更改都将记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，
并且本项目遵循 [语义化版本控制 (Semantic Versioning)](https://semver.org/lang/zh-CN/)。

## [未发布(最新)] (Unreleased)

### 新增 (Added)

- **简易地牢游戏场景** (`SimpleGameScreen`)
  - 9×9 网格地图，半即时制冷却驱动格子移动 + 近战战斗
  - `Entity` 冷却系统: `moveTimer`/`moveDelay`，按住方向键自动重复移动
  - `visualX/Y` 平滑插值 + `bumpX/Y` 攻击碰撞动画
  - 4 种敌人 (slime/skeleton/bat/wolf)，各有独立冷却和属性
  - 敌人独立AI: 仇恨范围追踪、冷却独立更新，支持多人联机扩展
  - 伤害飘字、血条、冷却条、HUD 显示击杀数和游戏时间、楼梯重置
  - `ExampleSelectScreen` "开始游戏" 入口对接
- **AI 绘制回放编辑器** (`AIDrawReplayScreen`)
  - JSON 绘制计划逐步回放，可视化绘制过程
  - 时间线控制: 进度条、步进 (|< < > >|)、播放/暂停、倍速 (1/2/4/8x)
  - 文件选择器 + 命令信息面板
  - `TestSelectionScreen` 新增 "AI绘制回放编辑器" 入口
- **人类模拟可视化自动测试** (`HumanSimulatorTest`)
  - 基于 `AutoTestManager` 时间线任务队列，模拟玩家按住方向键移动、打怪
  - 7 阶段测试流程: 初始验证 → 击杀 slime → 击杀 wolf → 战斗状态检查 → 清扫 → 最终断言
  - 使用 `simulatePress` + `addWait` + `simulateRelease` 模拟半即时制持续按键
  - `DebugLaunchConfig` AUTO_TEST 模式直接启动
- **基础配置与工具类**
  - `ThemeConfig`: 主题颜色配置
  - `Constants`: 全局常量
  - `NeonGenerator`: NeonBatch 绘制辅助工具

### 修复 (Fixed)

- 修复 `SimpleGameScreen` LibGDX Array 嵌套迭代器异常 (`#iterator() cannot be used nested`)
  - 所有 `Array<Entity>` / `Array<DamagePopup>` 的 foreach 改为索引循环
- 移除未使用的 `Vector2` 导入

### 变更 (Changed)

- `SimpleGameScreen` 从回合制改为半即时制冷却驱动系统
  - `isPressed()` 持续按键替代 `isJustPressed()` 单次触发
  - 敌人独立 `moveTimer` 冷却，不再等待玩家回合
  - HUD 从 "回合数" 改为 "击杀数 + 游戏时间"
- `SimpleGameScreen.Entity` / `DamagePopup` 改为 `public static class`，添加公共 getter
- `HumanSimulatorTest` 适配半即时制按住+释放模式
- `GameAutoTests.run()` 启用 `HumanSimulatorTest`

---

## [0.3.0] - 2026-02-21

### 新增 (Added)

- **JSON 数据驱动绘制系统** — 完全替代硬编码纹理生成器
  - `AIDrawPlan` / `AIDrawCommand`: JSON 绘制计划与命令数据结构
  - `AIDrawMethodRegistry`: 反射扫描 NeonBatch 方法，MethodHandle 动态调用
  - `AIDrawExecutor`: JSON 文本 → TextureRegion 执行引擎，支持 FBO 导出
- **JSON 纹理管理器与资源工具**
  - `TextureManager`: JSON 驱动纹理管理器，自动扫描 `ai_draw_cmds/`，按需生成并缓存
  - `AssetUtils`: assets.txt 索引扫描器，按目录前缀批量获取资源路径
  - `TextureExporter`: FBO → PNG 纹理导出工具
- **20 个 JSON 绘制计划** (数据驱动所有游戏纹理)
  - 角色: player
  - 怪物: slime, skeleton, bat, wolf, orc, boss_dragon
  - 图块: wall, floor, door, stairs_down, stairs_up, torch, pillar
  - 物品: health_potion, mana_potion, sword, shield, coin, chest
- **纹理预览测试界面**
  - `TexturePreviewScreen`: 网格预览所有 JSON 绘制纹理
  - `TestSelectionScreen` 新增入口注册

### 变更 (Changed)

- 移除所有硬编码纹理生成器 (NeonSpriteGenerator / NeonItemGenerator / NeonTileGenerator)
- 纹理生成全面转向 JSON 绘制指令驱动，支持热更新

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
