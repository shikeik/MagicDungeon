package com.goldsprite.magicdungeon.model;

/**
 * 存档元数据 (meta.json)
 */
public class SaveData {
    public String saveName;
    public long createTime;
    public long lastPlayedTime;
    public String currentPlayerName;
    public String currentAreaId;
    public int currentFloor;

    public SaveData() {
    }

    public SaveData(String saveName, String playerName) {
        this.saveName = saveName;
        this.currentPlayerName = playerName;
        this.createTime = System.currentTimeMillis();
        this.lastPlayedTime = this.createTime;
        this.currentAreaId = "camp"; // Default start area
        this.currentFloor = 1;
    }
}
