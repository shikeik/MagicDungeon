package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 房主向全体客户端广播"换层"信号
 * 携带新层的地图种子，所有客户端用此种子重建地图
 */
public class LanFloorChangeBroadcastPacket extends ResponsePacket {
    private long newSeed;   // 新一层的地图种子
    private int floor;      // 层数编号

    public LanFloorChangeBroadcastPacket(int ownerGuid, int repCode,
                                         long newSeed, int floor) {
        super(ownerGuid, repCode);
        this.newSeed = newSeed;
        this.floor = floor;
    }

    @Override
    public byte getCommand() {
        return LanCommands.FLOOR_CHANGE_BROADCAST;
    }

    public long getNewSeed() { return newSeed; }
    public int getFloor() { return floor; }
}
