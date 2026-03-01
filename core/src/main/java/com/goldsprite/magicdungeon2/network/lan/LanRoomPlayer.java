package com.goldsprite.magicdungeon2.network.lan;

public class LanRoomPlayer {
    private final int guid;
    private final String name;
    private final float x;
    private final float y;
    private final float vx;
    private final float vy;
    private final String action;
    private final long timestamp;
    // Phase 3/4 扩展字段
    private final float hp;
    private final float maxHp;
    private final int level;
    private final float atk;
    private final float def;

    public LanRoomPlayer(int guid, String name, float x, float y, float vx, float vy, String action, long timestamp) {
        this(guid, name, x, y, vx, vy, action, timestamp, 0, 0, 0, 0, 0);
    }

    public LanRoomPlayer(int guid, String name, float x, float y, float vx, float vy,
                          String action, long timestamp,
                          float hp, float maxHp, int level, float atk, float def) {
        this.guid = guid;
        this.name = name;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.action = action;
        this.timestamp = timestamp;
        this.hp = hp;
        this.maxHp = maxHp;
        this.level = level;
        this.atk = atk;
        this.def = def;
    }

    public int getGuid() {
        return guid;
    }

    public String getName() {
        return name;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getVx() {
        return vx;
    }

    public float getVy() {
        return vy;
    }

    public String getAction() {
        return action;
    }

    public long getTimestamp() {
        return timestamp;
    }

    // ============ Phase 3/4 扩展 ============

    public float getHp() { return hp; }
    public float getMaxHp() { return maxHp; }
    public int getLevel() { return level; }
    public float getAtk() { return atk; }
    public float getDef() { return def; }
}
