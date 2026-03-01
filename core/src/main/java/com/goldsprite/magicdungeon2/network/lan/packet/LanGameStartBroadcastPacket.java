package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.IStatus;
import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 服务器向所有客户端广播"开始游戏"信号
 * 携带地图种子，确保所有端生成相同地图
 */
public class LanGameStartBroadcastPacket extends ResponsePacket {

    private long mapSeed; // 地图种子，所有端用此种子生成相同地图

    public LanGameStartBroadcastPacket(int ownerGuid, int repCode, long mapSeed) {
        super(ownerGuid, repCode);
        this.mapSeed = mapSeed;
    }

    @Override
    public byte getCommand() {
        return LanCommands.GAME_START_BROADCAST;
    }

    public long getMapSeed() { return mapSeed; }
}
