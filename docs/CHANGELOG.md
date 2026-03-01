# 更新日志 (Changelog)

本项目的所有显著更改都将记录在此文件中�?
格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)�?并且本项目遵�?[语义化版本控�?(Semantic Versioning)](https://semver.org/lang/zh-CN/)�?
## [未发�?最�?] (Unreleased)

---

## [0.4.0] - 2026-02-22

### 新增 (Added)

- **虚拟摇杆增强**
  - 菱形方向指示器，进入扇区时点亮对应方�?  - 可配�?`stickHalfAngle` 扇区半角（默�?45°，全覆盖�?  - 扩展矩形触摸区域，提升移动端操控体验
  - VirtualJoystick �?Touchpad 重写为自定义 Widget
- **核心系统接入**（`SimpleGameScreen` 对接核心数据模型�?  - 等级系统: 击杀获取经验、自动升级、升级自动分配属性点
  - 死亡惩罚系统: 经验损失 20%，死亡覆盖层 UI 显示详情
  - 重生系统: R 键重生，地图/敌人重置，保留玩家进�?  - 视口统一: 世界相机 ExtendViewport(400,400) + UI 分离
  - MP 系统: 自然回复 + 魔法攻击消耗，MP 不足时短冷却
- **三阶段架构重�?*（`SimpleGameScreen` 871 �?�?345 行，减少 60%�?  - `GameEntity`: �?Entity 内部类提取为独立顶级�?  - `DamagePopup`: 伤害飘字数据�?  - `EnemyDefs`: 敌人/玩家属性配置工�?  - `GameConfig`: 20+ 命名常量（替代魔法数字）
  - `CombatHelper`: 统一战斗扫描/伤害/击杀，CombatListener 回调接口
  - `GrowthHelper`: 经验/升级/死亡惩罚/重生逻辑，GrowthListener 回调接口
  - `EnemyAI`: 敌人 AI 更新循环（冷�?追击/徘徊/碰撞检测）
  - `GameRenderer`: 渲染器，通过 GameState 只读接口与逻辑解�?- **输入系统重构**
  - `InputManager`: 统一键盘 WASD + 手柄 + 虚拟摇杆轴输�?  - atan2 角度扇区检测替代简单阈值判定，修复方向偏移 Bug
  - 消除 `isPressed(MOVE_*)` �?`getAxis()` 数据/显示不一致问�?- **其他**
  - `DLog` 控制台输出支�?ANSI 颜色
  - 统一日志输出�?`DLog`
  - 纹理重绘与第二转场效�?  - 无等待渐变转�?(`playOverlayFade`)

### 修复 (Fixed)

- 修复摇杆四向判定偏移（菱形分�?�?atan2 角度检测）
- 修复 `isPressed(MOVE_*)` 内部 `isAxisMappedAction` 简单阈值拦截导致方向不一�?- 修复 `stickHalfAngle` �?Screen �?VirtualJoystick 之间不同�?- 修正主类名称以匹配新包结�?
### 变更 (Changed)

- `SimpleGameScreen` 全面重构: God Object 拆分�?8 个职责单一的类
- `Entity` 标记�?`@Deprecated`，由 `GameEntity` 替代
- 移动输入统一�?`getAxis()` + atan2 路径，移�?`isPressed(MOVE_*)` 用于移动
- `VirtualJoystick` 从继�?Touchpad 改为自定�?Widget

---

## [0.3.0] - 2026-02-21

### 新增 (Added)

- **JSON 数据驱动绘制系统** �?完全替代硬编码纹理生成器
  - `AIDrawPlan` / `AIDrawCommand`: JSON 绘制计划与命令数据结�?  - `AIDrawMethodRegistry`: 反射扫描 NeonBatch 方法，MethodHandle 动态调�?  - `AIDrawExecutor`: JSON 文本 �?TextureRegion 执行引擎，支�?FBO 导出
- **JSON 纹理管理器与资源工具**
  - `TextureManager`: JSON 驱动纹理管理器，自动扫描 `ai_draw_cmds/`，按需生成并缓�?  - `AssetUtils`: assets.txt 索引扫描器，按目录前缀批量获取资源路径
  - `TextureExporter`: FBO �?PNG 纹理导出工具
- **20 �?JSON 绘制计划** (数据驱动所有游戏纹�?
  - 角色: player
  - 怪物: slime, skeleton, bat, wolf, orc, boss_dragon
  - 图块: wall, floor, door, stairs_down, stairs_up, torch, pillar
  - 物品: health_potion, mana_potion, sword, shield, coin, chest
- **纹理预览测试界面**
  - `TexturePreviewScreen`: 网格预览所�?JSON 绘制纹理
  - `TestSelectionScreen` 新增入口注册

### 变更 (Changed)

- 移除所有硬编码纹理生成�?(NeonSpriteGenerator / NeonItemGenerator / NeonTileGenerator)
- 纹理生成全面转向 JSON 绘制指令驱动，支持热更新

---

## [0.2.0] - 2026-02-21

### 新增 (Added)

- 核心数据模型系统 �?6 个源文件 + 4 个测试文件，�?72 个单元测�?  - `StatType`: 6 大属性枚�?(HP/MP/ATK/DEF/ASP/MOV)，含 `valuePerPoint` �?`isAllocatable`
  - `StatData`: 属性容器与统一计算流水线，支持装备/百分�?突破加成
  - `StatCalculator`: 等级点数公式 (`fixedPointsPerStat`, `totalFreePoints`, `totalPoints`)
  - `CombatEngine`: 物理/魔法/穿透伤害引擎，穿透衰�?0.7，最低阈�?0.1
  - `GrowthCalculator`: 经验-等级公式 (`100×1.2^(L-1)`)，返�?`long`，全链路溢出保护
  - `DeathPenalty`: 死亡惩罚系统 (经验 20%/金币 50%/装备 2-5 �?，自由点循环均匀扣除
- 文档: 重构系统设计�?`2_0_系统设计�?md`，以实际代码为权威同�?
### 修复 (Fixed)

- `StatData.addFreePoints` 增加余额校验与不可加点属性拒绝，返回 `boolean`
- `StatData.validate()` 校验自由点分配不超限
- `CombatEngine` 穿透衰减伤害低�?0.1 时归零（首目标豁免）
- `GrowthCalculator.xpForNextLevel` �?`int` 改为 `long`，防止高等级 (>95) 溢出
- `GrowthCalculator` 全链路溢出保�?(`totalXpForLevel`/`levelFromXp`/`xpProgress`)

### 变更 (Changed)

- 属性与伤害系统 `int` 全面改为 `float`
- `CombatEngine.calcMagicDamage` 改由调用方传�?MDEF（不内部计算 DEF/2�?- ASP/MOV 统一为乘法管�? `min((base+equip)×(1+pct), 3.0) + uncapped`
- 死亡惩罚自由点扣除改为循环均匀分配算法（每轮对有余额属性均匀扣减�?- `StatData` 点数公式全部委托 `StatCalculator`，消除重�?- `GrowthCalculator.totalXpForLevel` 累加优化为局部变量，避免逐级调用

---

## [0.1.2] - 2026-02-20

### 新增 (Added)

- 新增 `magicdungeon2.BuildConfig` 独立包名配置
- 新增 `globalFileNameAndContentReplace.sh` 全局文件�?内容替换脚本

### 修复 (Fixed)

### 变更 (Changed)

- 项目重命�? MagicDungeon �?MagicDungeon2
- 版本重置: 0.9.0 �?0.1.2
- 包名更新: com.goldsprite.magicdungeon �?com.goldsprite.magicdungeon2
- 同步更新所有构建配置、运行配置、文档、脚本中的项目名引用
