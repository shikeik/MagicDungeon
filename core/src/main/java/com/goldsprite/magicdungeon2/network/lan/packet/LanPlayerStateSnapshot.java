package com.goldsprite.magicdungeon2.network.lan.packet;

public class LanPlayerStateSnapshot {
    private int playerGuid;
    private String playerName;
    private float x;
    private float y;
    private float vx;
    private float vy;
    private String action;
    private long timestamp;
    // Phase 3/4 扩展字段：属性/血量可见
    private float hp;
    private float maxHp;
    private int level;
    private float atk;
    private float def;

    public LanPlayerStateSnapshot() {
    }

    public LanPlayerStateSnapshot(int playerGuid, String playerName, float x, float y, float vx, float vy, String action, long timestamp) {
        this.playerGuid = playerGuid;
        this.playerName = playerName;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.action = action;
        this.timestamp = timestamp;
    }

    /** 完整构造（含属性/血量） */
    public LanPlayerStateSnapshot(int playerGuid, String playerName, float x, float y,
                                  float vx, float vy, String action, long timestamp,
                                  float hp, float maxHp, int level, float atk, float def) {
        this(playerGuid, playerName, x, y, vx, vy, action, timestamp);
        this.hp = hp;
        this.maxHp = maxHp;
        this.level = level;
        this.atk = atk;
        this.def = def;
    }

    public int getPlayerGuid() {
        return playerGuid;
    }

    public void setPlayerGuid(int playerGuid) {
        this.playerGuid = playerGuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getVx() {
        return vx;
    }

    public void setVx(float vx) {
        this.vx = vx;
    }

    public float getVy() {
        return vy;
    }

    public void setVy(float vy) {
        this.vy = vy;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // ============ Phase 3/4 扩展字段 ============

    public float getHp() { return hp; }
    public void setHp(float hp) { this.hp = hp; }

    public float getMaxHp() { return maxHp; }
    public void setMaxHp(float maxHp) { this.maxHp = maxHp; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public float getAtk() { return atk; }
    public void setAtk(float atk) { this.atk = atk; }

    public float getDef() { return def; }
    public void setDef(float def) { this.def = def; }
}
