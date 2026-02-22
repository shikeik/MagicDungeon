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

    public LanRoomPlayer(int guid, String name, float x, float y, float vx, float vy, String action, long timestamp) {
        this.guid = guid;
        this.name = name;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.action = action;
        this.timestamp = timestamp;
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
}
