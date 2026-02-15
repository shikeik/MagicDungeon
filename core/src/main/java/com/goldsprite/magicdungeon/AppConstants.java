package com.goldsprite.magicdungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/**
 * 全局常量定义
 */
public class AppConstants {
    /**
     * 统一存储根路径 (Storage Root)
     * 使用项目名称作为根目录名，避免直接硬编码字符串
     */
    public static final String STORAGE_ROOT = BuildConfig.PROJECT_NAME + "/";

    /**
     * 获取本地存储文件句柄
     * @param path 相对路径
     * @return FileHandle
     */
    public static FileHandle getLocalFile(String path) {
        return Gdx.files.local(STORAGE_ROOT + path);
    }
}
