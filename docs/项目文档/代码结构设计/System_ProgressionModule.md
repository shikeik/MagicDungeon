# 系统流程模块: System_ProgressionModule (Game Loop & State)

## 1. 模块职责
负责管理游戏的整体流程，包括 游戏开始、层级推进、存档/读档、死亡结算 (Game Over) 以及全局进度持久化。

## 2. 游戏流程 (Game Loop)

### 2.1 状态机 (GameState)
*   `MainMenu`: 主菜单。
*   `Playing`: 游戏进行中 (核心循环)。
*   `Paused`: 暂停 (打开背包/菜单)。
*   `GameOver`: 死亡结算界面。
*   `Victory`: 通关结算。

### 2.2 层级推进 (Level Progression)
*   **进入下一层 (Next Floor)**:
    *   保存当前层状态 (怪物存活、掉落物)。
    *   生成/加载下一层数据。
    *   **难度曲线**: 
        *   怪物等级 = `FloorIndex * x`。
        *   掉落品质权重提升。

### 2.3 死亡惩罚 (Death Penalty)
*   **复活 (Respawn)**: 玩家在当前层入口或上一个存档点复活。
*   **损失**: 
    *   失去 20% 当前经验值。
    *   若经验不足扣除，则 **掉级 (Level Down)**，属性相应扣除。
    *   金币减少 10%。
*   **永久死亡 (Hardcore Mode)**: 可选模式，死亡即存档删除。

## 3. 持久化 (Save/Load)

### 3.1 存档结构 (JSON/Binary)
```json
{
  "player": {
    "level": 5,
    "exp": 1200,
    "stats": {...},
    "inventory": [...]
  },
  "world": {
    "currentFloor": 3,
    "seed": 123456,
    "floors": {
      "1": { "cleared": true, ... },
      "2": { "nests": [...], ... }
    }
  },
  "meta": {
    "playTime": 3600,
    "timestamp": 1678888888
  }
}
```

## 4. 接口设计 (API Draft)

```java
interface IGameManager {
    // 流程控制
    void startGame();
    void pauseGame();
    void resumeGame();
    void endGame(boolean isVictory);
    
    // 场景切换
    void loadScene(String sceneName);
    void enterNextFloor();
    
    // 存档管理
    void saveGame(int slotId);
    void loadGame(int slotId);
    boolean hasSave(int slotId);
    
    // 全局配置
    GameSettings getSettings(); // 音量、难度等
}
```
