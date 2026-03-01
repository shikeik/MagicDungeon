package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

/**
 * 客户端向服务器发送攻击请求
 * 服务器将请求转交给房主处理（房主权威判定）
 */
public class LanAttackRequestPacket extends Packet {
    private String attackType;  // "physical" 或 "magic"
    private float x, y;        // 攻击者当前网格坐标
    private int dx, dy;        // 攻击方向
    private float atk;         // 攻击者的ATK属性值

    public LanAttackRequestPacket(int ownerGuid, String attackType,
                                  float x, float y, int dx, int dy, float atk) {
        super(ownerGuid);
        this.attackType = attackType;
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.atk = atk;
    }

    @Override
    public byte getCommand() {
        return LanCommands.ATTACK_REQUEST;
    }

    public String getAttackType() { return attackType; }
    public float getX() { return x; }
    public float getY() { return y; }
    public int getDx() { return dx; }
    public int getDy() { return dy; }
    public float getAtk() { return atk; }
}
