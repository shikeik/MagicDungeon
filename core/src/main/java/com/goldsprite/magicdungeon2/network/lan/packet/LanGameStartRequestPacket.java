package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

/**
 * 房主请求服务器广播"开始游戏"信号
 */
public class LanGameStartRequestPacket extends Packet {

    public LanGameStartRequestPacket(int ownerGuid) {
        super(ownerGuid);
    }

    @Override
    public byte getCommand() {
        return LanCommands.GAME_START_REQUEST;
    }
}
