# Dungeon Explorer

基于 HTML5 Canvas 的地牢探险游戏。

## 功能特性

*   **随机地牢生成**: 每次游戏都会生成不同的地图布局。
*   **战斗系统**: 与多种怪物进行回合制战斗（实际上是即时制的，但有冷却时间）。
*   **物品系统**: 收集武器、护甲和药水。
*   **角色成长**: 升级系统，增加属性。
*   **技能系统**: 简单的技能树（目前包含治疗技能）。
*   **成就系统**: 追踪并解锁成就。
*   **存档/读档**: 使用 LocalStorage 保存进度。
*   **移动端适配**: 支持触摸控制。
*   **音效**: 使用 Web Audio API 生成音效。

## 运行方式

由于使用了 ES6 Modules，需要通过 HTTP 服务器运行。

1.  安装依赖（如果需要开发工具）：
    ```bash
    npm install
    ```
    (本项目暂无外部依赖，可直接运行)

2.  启动本地服务器：
    可以使用 VS Code 的 Live Server 插件，或者 Python:
    ```bash
    python -m http.server 8000
    ```
    或者 Node.js http-server:
    ```bash
    npx http-server .
    ```

3.  在浏览器打开 `http://localhost:8000`

## 操作说明

*   **WASD / 方向键**: 移动
*   **Space**: 使用技能 (Heal)
*   **鼠标/触摸**: 点击按钮进行交互

## 开发文档

### 目录结构

*   `src/core`: 核心游戏逻辑 (Game, Input)
*   `src/entities`: 实体类 (Player, Monster, Item)
*   `src/systems`: 子系统 (Renderer, Audio, Save, Achievement, Skill)
*   `src/world`: 地图生成与管理 (Dungeon, MapGenerator, Tile)
*   `src/utils`: 工具与常量

### 核心类

*   `Game`: 游戏主入口，管理主循环和状态。
*   `Dungeon`: 管理地图数据。
*   `MapGenerator`: 生成随机地图算法。
*   `Renderer`: 负责 Canvas 渲染。

## 性能优化

*   **视锥剔除**: 仅渲染摄像机范围内的 Tile。
*   **对象池**: (计划中) 减少垃圾回收。
*   **Canvas 优化**: 使用 `imageSmoothingEnabled = false` 保持像素风格且提高性能。
