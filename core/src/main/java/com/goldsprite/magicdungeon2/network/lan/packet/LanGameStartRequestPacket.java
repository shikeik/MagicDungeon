package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

/**
 * 房主请求服务器广播"开始游戏"信号
 * 携带地图种子，服务器转发给所有客户端
 */
public class LanGameStartRequestPacket extends Packet {

    private long mapSeed; // 地图种子

    public LanGameStartRequestPacket(int ownerGuid, long mapSeed) {
        super(ownerGuid);
        this.mapSeed = mapSeed;
    }

    @Override
    public byte getCommand() {
        return LanCommands.GAME_START_REQUEST;
    }

    public long getMapSeed() { return mapSeed; }
}
