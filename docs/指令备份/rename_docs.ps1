$path = "e:\WorkSpaces\Libgdx_WSpace\Projs\MagicDungeon2\.trae\documents"
if (Test-Path $path) {
    # 获取所有文件，排除脚本自身
    $files = Get-ChildItem $path | Where-Object { $_.Name -ne "rename_docs.ps1" } | Sort-Object CreationTime

    # 第一阶段：重命名为临时名称，去除现有编号，避免冲突
    foreach ($file in $files) {
        if ($file.Name -match '^\d+_') {
            $cleanName = $file.Name -replace '^\d+_', ''
            $tempName = "TEMP_RENAME_" + [Guid]::NewGuid().ToString() + "_" + $cleanName
            Rename-Item -LiteralPath $file.FullName -NewName $tempName
        }
    }

    # 第二阶段：按创建时间排序并应用新编号
    $files = Get-ChildItem $path | Where-Object { $_.Name -ne "rename_docs.ps1" } | Sort-Object CreationTime
    $i = 1
    foreach ($file in $files) {
        # 还原原始名称（去掉临时前缀或旧编号）
        # 使用更精确的正则匹配 GUID，避免 \w 匹配中文导致贪婪吞噬
        $originalName = $file.Name -replace '^TEMP_RENAME_[a-fA-F0-9-]+_', '' -replace '^\d+_', ''
        
        $newName = "{0}_{1}" -f $i, $originalName
        $newPath = Join-Path $path $newName
        
        if ($file.Name -ne $newName) {
            Rename-Item -LiteralPath $file.FullName -NewName $newName
            Write-Host "Renamed $($file.Name) to $newName"
        }
        $i++
    }
} else {
    Write-Error "Path not found: $path"
}
