#!/bin/bash

# 配置区域
SEARCH_DIR="core/src" # 默认搜索目录
# 排除 import 和 package 语句，查找全限定名模式
# 模式解释：
# 排除 import|package 开头的行
# 匹配类似 java.util.List 的全限定名
# [a-z0-9_]+(\.[a-z0-9_]+)+\.[A-Z][a-zA-Z0-9_]*

echo "开始扫描全限定名使用..."

find "$SEARCH_DIR" -name "*.java" -type f | while read -r file; do
    # 获取文件内容
    content=$(cat "$file")
    
    # 使用 grep 查找全限定名
    # 正则表达式修改：去掉 \b，简化匹配
    fqcns=$(grep -vE "^\s*(import|package)\s+" "$file" | grep -oE "[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+\.[A-Z][a-zA-Z0-9_]*" | sort | uniq)
    
    if [ -z "$fqcns" ]; then
        continue
    fi
    
    echo "处理文件: $file"
    
    # 逐个处理，使用换行符分隔
    echo "$fqcns" | while read -r fqcn; do
        if [ -z "$fqcn" ]; then continue; fi
        
        # echo "  - 处理 FQCN: '$fqcn'"
        
        # 获取简单名 (Simple Name) 和包名 (Package Name)
        simple_name=${fqcn##*.}
        package_name=${fqcn%.*}
        
        # echo "    Simple: '$simple_name', Package: '$package_name'"
        
        # 检查是否是 java.lang 包（不需要 import）
        if [ "$package_name" == "java.lang" ]; then
            # java.lang 包下的类不需要显式导入，直接替换
            # 使用 sed 的 b 命令跳过 import/package 行
            sed -i "/^\s*import/b; /^\s*package/b; s/$fqcn/$simple_name/g" "$file"
            echo "  [java.lang] $fqcn -> $simple_name"
            continue
        fi
        
        # 检查是否在同一个包（不需要 import）
        current_package=$(grep "^package " "$file" | awk '{print $2}' | tr -d ';')
        if [ "$package_name" == "$current_package" ]; then
            # 同包，直接替换，不需要 import
            sed -i "/^\s*import/b; /^\s*package/b; s/$fqcn/$simple_name/g" "$file"
            echo "  [同包] $fqcn -> $simple_name"
            continue
        fi
        
        # 检查是否已有 import (包括 import simple_name; 和 import package.*;)
        has_import=$(grep -E "^import\s+($fqcn|${package_name}\.\*)\s*;" "$file")
        
        # 检查是否有同名类冲突 (import 了其他包的同名类)
        # 例如 import java.awt.List; 而代码中用了 java.util.List
        conflict_import=$(grep -E "^import\s+.*\.$simple_name\s*;" "$file" | grep -v "$fqcn")
        
        if [ -n "$conflict_import" ]; then
            echo "  [跳过] $fqcn (存在同名类冲突: $conflict_import)"
            continue
        fi
        
        if [ -n "$has_import" ]; then
            # 已有 import，直接替换
            sed -i "/^\s*import/b; /^\s*package/b; s/$fqcn/$simple_name/g" "$file"
            echo "  [已有导入] $fqcn -> $simple_name"
        else
            # 没有 import，添加 import 并替换
            # 找到最后一个 import 语句的位置，或者 package 语句的位置
            
            # 1. 查找最后一个 import 行号
            last_import_line=$(grep -n "^import " "$file" | tail -n 1 | cut -d: -f1)
            
            if [ -n "$last_import_line" ]; then
                # 在最后一个 import 后插入
                sed -i "${last_import_line}a import $fqcn;" "$file"
            else
                # 没有 import，查找 package 行号
                package_line=$(grep -n "^package " "$file" | tail -n 1 | cut -d: -f1)
                if [ -n "$package_line" ]; then
                    sed -i "${package_line}a \nimport $fqcn;" "$file"
                else
                    # 既没有 import 也没有 package (默认包)，直接在第一行插入
                    sed -i "1i import $fqcn;" "$file"
                fi
            fi
            
            # 替换，排除 import/package 行
            sed -i "/^\s*import/b; /^\s*package/b; s/$fqcn/$simple_name/g" "$file"
            echo "  [新增导入] $fqcn -> $simple_name"
        fi
    done
done

echo "处理完毕！"
