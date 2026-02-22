package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.codecInterfaces.Packet;

public class LanRoomPlayersRequestPacket extends Packet {
    public LanRoomPlayersRequestPacket(int ownerGuid) {
        super(ownerGuid);
    }

    @Override
    public byte getCommand() {
        return LanCommands.ROOM_PLAYERS_REQUEST;
    }
}
