# 世界地图模块: World_MapModule (Terrain & Generation)

## 1. 模块职责
负责游戏地图的生成、地形数据管理、寻路算法支持以及迷雾系统的计算。

## 2. 地图数据结构

### 2.1 网格单元 (Grid Cell)
每个坐标点 `(x, y)` 包含：
*   **地形类型 (TerrainType)**: 
    *   `Wall`: 不可视、不可通过。
    *   `Floor`: 可视、可通过。
    *   `Door`: 可视、需钥匙交互开启。
    *   `Stairs`: 下一层入口。
    *   `Trap`: 触发伤害。
*   **动态层 (Dynamic Layer)**: 
    *   存放 `EntityID` (玩家/怪物/巢穴/物品)。
*   **探索状态 (Exploration)**:
    *   `Unseen`: 未探索 (黑色)。
    *   `Fog`: 已探索但当前不可见 (灰色)。
    *   `Visible`: 当前可见 (亮色)。

### 2.2 地图生成 (Generation)
*   **算法**: 采用 `Cellular Automata` (细胞自动机) 生成洞穴状地形，或 `BSP` (二叉空间分割) 生成房间走廊结构。
*   **尺寸**: 固定大尺寸 `200x200` 或更大。
*   **特性**: 
    *   **连通性保障**: 必须保证所有可达区域连通 (Flood Fill check)。
    *   **资源分布**: 均匀分布怪物巢穴和宝箱，避免过于集中。

## 3. 核心算法

### 3.1 寻路 (Pathfinding)
*   **A* 算法 (A-Star)**: 用于怪物追踪玩家。
*   **优化**: 
    *   针对静态地图预计算部分路径 (若是固定地图)。
    *   动态回避: 怪物之间互相视为障碍物 (Soft Collision) 或允许重叠但有惩罚。

### 3.2 视野计算 (FOV - Field of View)
*   **算法**: `Recursive Shadowcasting` (递归阴影投射) 或 `Raycasting`。
*   **范围**: 玩家视野半径 (如 8 格)。
*   **更新频率**: 仅在玩家移动时更新。

## 4. 接口设计 (API Draft)

```java
interface IMapManager {
    // 地图生成
    void generateNewLevel(int floorIndex, long seed);
    
    // 查询
    TerrainType getTerrain(int x, int y);
    boolean isBlocked(int x, int y);
    boolean isOpaque(int x, int y); // 阻挡视线
    
    // 寻路
    List<Position> findPath(Position start, Position end);
    
    // 视野
    void updatePlayerFOV(Position playerPos, int radius);
    boolean isVisible(int x, int y);
}
```
