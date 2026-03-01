package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

public class LanPlayerSyncRequestPacket extends Packet {
    private float x;
    private float y;
    private float vx;
    private float vy;
    private String action;
    private long timestamp;
    // Phase 3/4: 扩展属性
    private float hp;
    private float maxHp;
    private int level;
    private float atk;
    private float def;

    public LanPlayerSyncRequestPacket(int ownerGuid, float x, float y, float vx, float vy,
                                      String action, long timestamp,
                                      float hp, float maxHp, int level, float atk, float def) {
        super(ownerGuid);
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

    @Override
    public byte getCommand() {
        return LanCommands.PLAYER_SYNC_REQUEST;
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

    // Phase 3/4 扩展
    public float getHp() { return hp; }
    public float getMaxHp() { return maxHp; }
    public int getLevel() { return level; }
    public float getAtk() { return atk; }
    public float getDef() { return def; }
}
