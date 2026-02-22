package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.IStatus;
import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 服务器向所有客户端广播"开始游戏"信号
 */
public class LanGameStartBroadcastPacket extends ResponsePacket {

    public LanGameStartBroadcastPacket(int ownerGuid, int repCode) {
        super(ownerGuid, repCode);
    }

    @Override
    public byte getCommand() {
        return LanCommands.GAME_START_BROADCAST;
    }
}
