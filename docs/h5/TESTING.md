# 测试用例与指南

## 1. 基础功能测试

| 测试项 | 操作 | 预期结果 |
| :--- | :--- | :--- |
| **游戏启动** | 打开网页点击“开始游戏” | 游戏画面加载，显示地图和角色，HUD显示初始状态 |
| **移动** | 按下 WASD 或方向键 | 角色向对应方向移动，无法穿过墙壁 |
| **触摸移动** | 在屏幕上拖动 | 角色跟随拖动方向移动 |
| **地图生成** | 刷新页面重新开始 | 地图布局发生变化，房间位置不同 |

## 2. 战斗与交互测试

| 测试项 | 操作 | 预期结果 |
| :--- | :--- | :--- |
| **攻击怪物** | 移动向怪物所在的格子 | 怪物扣血（看日志），播放攻击音效。怪物反击。 |
| **怪物死亡** | 持续攻击直到怪物HP归零 | 怪物消失，日志显示死亡信息，角色获得XP。 |
| **拾取物品** | 移动到物品格子 | 物品消失，日志显示拾取信息，播放拾取音效。 |
| **装备物品** | 拾取武器或护甲 | 角色属性（Atk/Def）在内部更新（需通过控制台或日志验证）。 |
| **升级** | 击杀足够怪物 | 日志显示“Level Up”，角色最大HP增加，HP回满。 |

## 3. 系统功能测试

| 测试项 | 操作 | 预期结果 |
| :--- | :--- | :--- |
| **技能使用** | 按下空格键 | 如果技能未冷却，播放音效，角色HP增加。如果冷却中，提示不可用。 |
| **存档** | 点击“保存游戏” | 弹出“Game Saved”提示。 |
| **读档** | 刷新页面，点击“读取存档” | 恢复之前的角色位置、状态和等级。 |
| **成就** | 达成条件（如首杀） | 日志显示“ACHIEVEMENT UNLOCKED”。 |

## 4. 自动化测试脚本 (Console)

在浏览器控制台运行以下代码进行单元测试：

```javascript
import { Dungeon } from './src/world/Dungeon.js';
import { Player } from './src/entities/Player.js';

function runTests() {
    console.group("Running Tests");

    // Test 1: Map Generation
    const dungeon = new Dungeon(50, 50);
    console.assert(dungeon.map.length === 50, "Map height should be 50");
    console.assert(dungeon.map[0].length === 50, "Map width should be 50");
    console.log("Map Gen Test Passed");

    // Test 2: Player Stats
    const player = new Player(0, 0);
    console.assert(player.stats.hp === 100, "Initial HP should be 100");
    player.stats.hp -= 10;
    console.assert(player.stats.hp === 90, "HP should decrease");
    console.log("Player Stats Test Passed");

    console.groupEnd();
}

// 注意：由于是模块化代码，直接在控制台粘贴可能需要通过 import() 动态导入
// 或者在 main.js 中暴露测试接口
```
