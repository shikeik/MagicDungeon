package com.goldsprite.magicdungeon2.network.lan.packet;

public interface LanCommands {
    byte PLAYER_SYNC_REQUEST = 41;
    byte PLAYER_SYNC_BROADCAST = 42;
    byte ROOM_PLAYERS_REQUEST = 43;
    byte ROOM_PLAYERS_RESPONSE = 44;
    byte GAME_START_REQUEST = 45;
    byte GAME_START_BROADCAST = 46;
}
