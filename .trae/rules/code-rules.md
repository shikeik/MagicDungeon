---
alwaysApply: true
description: 项目核心代码规范
---

# 项目代码规范 (Code Rules)

1. **中文优先**: 注释、文档、日志、报错均强制使用**中文**。
2. **命名规范**: 类名 PascalCase，方法/变量 camelCase，常量 UPPER_SNAKE_CASE；**严禁**使用全限定名，必须 Import。
3. **LibGDX**: 资源对象 (Texture/Batch等) 必须手动 `dispose()`；坐标原点在左下角；日志用 `Gdx.app.log`。
4. **代码质量**: 优先复用逻辑 (DRY)，保持简洁；文件 UTF-8 编码。
