#!/bin/bash

# ================= 配置区域 =================
# 查找的关键词 (区分大小写)
SEARCH_STR="MagicDungeon"

# 替换后的关键词
REPLACE_STR="MagicDungeon"

# 是否空跑模式 (true: 只打印不执行; false: 实际执行)
# 建议先运行一次预览，确认无误后再改为 false
DRY_RUN=false
# ===========================================

echo "========================================"
echo "配置信息:"
echo "查找: [$SEARCH_STR]"
echo "替换: [$REPLACE_STR]"
echo "空跑: $DRY_RUN"
echo "========================================"

# 使用 -depth 确保先处理子文件/子目录，再处理父目录
# 排除 .git 目录
# find 输出的路径可能包含空格，使用 while read -r 处理
find . -depth -not -path '*/.git/*' -name "*$SEARCH_STR*" | while read -r file_path; do
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
