## User
jpackage+EnigmaVirtualBox可以直接gradle编写完成一键式发布脚本吗

## Gemini
**可以，完全可以。**

你可以通过 Gradle 的 `Exec` 任务（调用命令行工具）来串联整个流程。核心思路是将过程分为三步：

1.  **构建 (Build):** 生成 Jar 包。
2.  **打包 (jpackage):** 使用 JDK 自带的 `jpackage` 生成**应用程序镜像 (App Image)**（即包含 JVM 和程序的文件夹，而不是最终的安装包）。
3.  **单文件化 (Enigma):** 调用 Enigma Virtual Box 的命令行工具 (`enigmavbconsole.exe`)，将上一步生成的文件夹打包成一个独立的 `.exe` 文件。

## 当前实现方案 (已集成到项目中)

已修改 `gradle/jpackage.gradle` 和根目录 `build.gradle`，实现了以下一键命令：

```bash
gradlew oneClickRelease
```

### 首次使用配置指南 (关键步骤)

由于 Enigma Virtual Box 需要一个模板文件 (`template.evb`) 来定义哪些文件需要打包，你需要**手动执行一次**以下步骤来生成这个模板：

1.  **生成应用程序镜像**:
    运行以下命令，生成基础的文件结构：
    ```bash
    gradlew :lwjgl3:packageApp
    ```
    成功后，你会看到 `outputs/dist/MagicDungeon2` 文件夹。

2.  **配置 Enigma Virtual Box**:
    打开 Enigma Virtual Box (GUI界面)，进行以下设置：
    *   **Enter Input File Name**: 选择 `outputs/dist/MagicDungeon2/MagicDungeon2.exe` (这是 jpackage 生成的主程序)。
    *   **Enter Output File Name**: 设置为你想要的发布路径，例如 `release/MagicDungeon2.exe`。
    *   **添加虚拟文件**:
        *   在下方的 "Files" 列表中，将整个 `outputs/dist/MagicDungeon2` 文件夹拖进去。
        *   **重要**: 确保目录结构正确。通常你应该把 `outputs/dist/MagicDungeon2` 目录下的所有内容（app, runtime, MagicDungeon2.exe等）都包含在内。
        *   建议设置：将 `outputs/dist/MagicDungeon2` 文件夹拖入后，选择 "Default Folder" (默认文件夹)。
    *   **压缩设置 (可选)**: 点击 "Compression"，启用压缩以减小体积。

3.  **保存模板**:
    *   点击菜单栏 `File` -> `Save Project As...`。
    *   保存为 `template.evb`，**必须放在项目根目录下** (`e:\WorkSpaces\Libgdx_WSpace\Projs\MagicDungeon2\template.evb`)。
    *   **注意**: 建议在保存前，将文件路径修改为相对路径（如果 Enigma 支持），或者确保你的项目路径不会变动。

4.  **验证一键发布**:
    现在你可以运行完整的一键发布命令了：
    ```bash
    gradlew oneClickRelease
    ```
    如果一切正常，它会自动执行：Build -> Jar -> jpackage -> Enigma -> 生成单文件 Exe。

### Gradle 脚本说明

脚本位于 `gradle/jpackage.gradle`，主要逻辑如下：

*   **packageApp**: 调用 `jpackage` 生成带 JRE 的文件夹。
*   **packageSingleExe**: 检查 `enigmavbconsole.exe` 和 `template.evb` 是否存在。如果存在，则调用 Enigma 命令行工具进行打包。

### 常见问题 Troubleshooting

1.  **找不到 Enigma Virtual Box**:
    脚本默认查找 `C:/Program Files/Enigma Virtual Box/enigmavbconsole.exe` 等常见路径。如果你的安装路径不同，请在 `local.properties` 中添加：
    ```properties
    enigmavb.path=E:\\WorkApps\\Enigma Virtual Box\\enigmavbconsole.exe
    ```

2.  **template.evb 路径问题**:
    如果 `jpackage` 输出的文件结构发生变化（例如 jar 包名字变了），你可能需要重新更新 `template.evb`。为了避免这种情况，脚本中固定了输出目录名为 `MagicDungeon2`，但 jar 包名带有版本号。
    *   **建议**: 在 Enigma 中添加文件时，如果可能，尽量使用通配符或者确保版本号变动不影响结构。或者每次发版前检查一下 evb。
