package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

import java.util.List;

public class LanRoomPlayersResponsePacket extends ResponsePacket {
    private List<LanPlayerStateSnapshot> players;

    public LanRoomPlayersResponsePacket(int ownerGuid, int repCode, List<LanPlayerStateSnapshot> players) {
        super(ownerGuid, repCode);
        this.players = players;
    }

    @Override
    public byte getCommand() {
        return LanCommands.ROOM_PLAYERS_RESPONSE;
    }

    public List<LanPlayerStateSnapshot> getPlayers() {
        return players;
    }
}
