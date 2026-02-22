package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

public class LanPlayerSyncRequestPacket extends Packet {
    private float x;
    private float y;
    private float vx;
    private float vy;
    private String action;
    private long timestamp;

    public LanPlayerSyncRequestPacket(int ownerGuid, float x, float y, float vx, float vy, String action, long timestamp) {
        super(ownerGuid);
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.action = action;
        this.timestamp = timestamp;
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
}
