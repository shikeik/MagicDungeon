#!/bin/bash

# ================= 配置区域 =================
# 目标文件后缀（内容替换时使用）
FILE_PATTERN="*.*"

# 查找的关键词 (区分大小写)
SEARCH_STR="com.goldsprite.magicdungeon"

# 替换后的关键词
REPLACE_STR="com.goldsprite.magicdungeon2"

# 排除路径/文件的正则表达式，apply to both filename/content lookup
# 类似 collectProjCode.sh 中使用的模式，可设置为空以不排除任何内容
EXCLUDE_PATTERN="/\.git|\.png$|\.jpg$|build|bin|\.zip$|\.apk$"

# 模式: name=仅改文件名，content=仅改文件内容，both=同时处理
MODE="both"          # 可选: name, content, both

# 是否空跑模式 (true: 只打印不执行; false: 实际执行)
# 建议先运行一次预览，确认无误后再改为 false
DRY_RUN=false
# ===========================================

echo "========================================"
echo "配置信息:"
echo "查找: [$SEARCH_STR]"
echo "替换: [$REPLACE_STR]"
echo "模式: $MODE"
echo "排除: $EXCLUDE_PATTERN"
echo "空跑: $DRY_RUN"
echo "========================================"

# 获取脚本自身名称，以便在查找时排除自己
script_name=$(basename "$0")

# 只有在处理文件名模式时才执行下面的重命名逻辑
if [ "$MODE" = "name" ] || [ "$MODE" = "both" ]; then

# 使用 -depth 确保先处理子文件/子目录，再处理父目录
# 排除脚本自身路径
# find 输出的路径可能包含空格，使用 while read -r 处理
find . -depth -not -path "./$script_name" -name "*${SEARCH_STR}*" | grep -vE "$EXCLUDE_PATTERN" | while read -r file_path; do
    # 获取目录路径和文件名
    dir_name=$(dirname "$file_path")
    base_name=$(basename "$file_path")

    # 执行替换 (使用 BASH 字符串替换 ${var//pattern/replacement})
    # 注意: 这里只替换文件名部分
    new_base_name="${base_name//$SEARCH_STR/$REPLACE_STR}"

    # 防止无变化 (find 已经筛选过，但为了双重保险)
    if [ "$base_name" != "$new_base_name" ]; then
        new_full_path="$dir_name/$new_base_name"

        if [ "$DRY_RUN" = true ]; then
            echo "[预览] mv \"$file_path\" -> \"$new_full_path\""
        else
            # 检查目标是否已存在，避免覆盖
            if [ -e "$new_full_path" ]; then
                echo "[跳过] 目标已存在: $new_full_path"
            else
                mv "$file_path" "$new_full_path"
                echo "[已重命名] $file_path -> $new_full_path"
            fi
        fi
    fi
done

echo "========================================"
if [ "$DRY_RUN" = true ]; then
    echo "完成预览。请将脚本中的 DRY_RUN=true 改为 false 以执行实际操作。"
else
    echo "✅ 批量重命名完成！"
fi

echo ""
fi   # end of name/both mode

# 如果需要处理内容模式，执行下面逻辑
if [ "$MODE" = "content" ] || [ "$MODE" = "both" ]; then
echo "✅ 开始批量处理文件内容！"

# 查找当前目录下所有的指定文件，排除脚本自身，并应用排除模式
find . -name "$FILE_PATTERN" -type f -not -path "./$script_name" | grep -vE "$EXCLUDE_PATTERN" | while read file; do
# 先检测是否包含关键词
    if grep -q "$SEARCH_STR" "$file"; then
        if [ "$DRY_RUN" = true ]; then
            echo "[预览已处理] $file"
        else
            sed -i "s/$SEARCH_STR/$REPLACE_STR/g" "$file"
            echo "[已处理] $file"
        fi
    else
        echo "[跳过] $file"
    fi

done

echo "✅ 所有 $FILE_PATTERN 文件内容替换已处理完毕！"
fi