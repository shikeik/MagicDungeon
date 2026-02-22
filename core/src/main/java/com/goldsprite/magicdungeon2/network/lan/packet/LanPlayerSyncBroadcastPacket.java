package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

public class LanPlayerSyncBroadcastPacket extends ResponsePacket {
    private LanPlayerStateSnapshot state;

    public LanPlayerSyncBroadcastPacket(int ownerGuid, int repCode, LanPlayerStateSnapshot state) {
        super(ownerGuid, repCode);
        this.state = state;
    }

    @Override
    public byte getCommand() {
        return LanCommands.PLAYER_SYNC_BROADCAST;
    }

    public LanPlayerStateSnapshot getState() {
        return state;
    }
}
