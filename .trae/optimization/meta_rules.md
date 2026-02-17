## 优先级标准定义 (Priority Standards)

| 级别 | 定义 (Definition) | 判定标准 (Criteria) | 响应时效 (Response) | 代码示例 (Example) |
| :--- | :--- | :--- | :--- | :--- |
| **P0** | **阻断性 (Critical)** | 系统崩溃、无法启动、核心数据丢失、无法构建、严重安全漏洞。 | **立即修复**，阻断后续开发。 | `throw new RuntimeException("Game Crash");` 导致启动失败。 |
| **P1** | **严重 (High)** | 核心功能受损、严重内存泄漏、严重性能掉帧(<30FPS)、主要逻辑错误。 | **当前版本/迭代必须修复**。 | `texture = new Texture(...)` 但从未调用 `dispose()`。 |
| **P2** | **重要 (Medium)** | 代码结构混乱(难以维护)、硬编码、次要功能缺陷、缺乏错误处理。 | **列入计划**，后续迭代修复。 | `if (id == 1001) { ... }` (Magic Number/硬编码)。 |
| **P3** | **优化 (Low)** | 代码风格问题、注释缺失、命名不规范、微小UI/UX瑕疵、建议性重构。 | **Backlog**，有空闲时修复。 | `int a = 1;` (命名不规范) 或 缺少 JavaDoc。 |

### 代码示例详解

#### P1 - 资源泄漏 (Resource Leak)
**Bad Case:**
```java
public void create() {
    batch = new SpriteBatch(); // 创建了 native 资源
}
// 缺失 dispose() 方法，导致显存泄漏
```

**Good Case:**
```java
@Override
public void dispose() {
    if (batch != null) batch.dispose(); // 显式释放
}
```

#### P2 - 硬编码 (Hardcoding)
**Bad Case:**
```java
// 硬编码路径和数值
Texture t = new Texture("spines/wolf.png"); 
player.setHealth(100);
```

**Good Case:**
```java
// 使用常量或配置
public static final String WOLF_PATH = "spines/wolf.png";
// 或通过 AssetManager 加载
player.setHealth(GameConfig.DEFAULT_HP);
```

---