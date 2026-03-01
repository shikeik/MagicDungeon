package com.goldsprite.magicdungeon2.network.lan.packet;

public interface LanCommands {
    byte PLAYER_SYNC_REQUEST = 41;
    byte PLAYER_SYNC_BROADCAST = 42;
    byte ROOM_PLAYERS_REQUEST = 43;
    byte ROOM_PLAYERS_RESPONSE = 44;
    byte GAME_START_REQUEST = 45;
    byte GAME_START_BROADCAST = 46;

    // ============ Phase 2+: 敌人同步 / 合作战斗 / 换层 ============
    byte ENEMY_SYNC_BROADCAST = 47;     // 房主 → 全体：敌人状态广播
    byte ATTACK_REQUEST = 48;           // 客户端 → 服务器：攻击请求
    byte DAMAGE_RESULT_BROADCAST = 49;  // 房主 → 全体：伤害判定结果
    byte PLAYER_HURT_BROADCAST = 50;    // 房主 → 全体：玩家被敌人攻击
    byte FLOOR_CHANGE_BROADCAST = 51;   // 房主 → 全体：换层信号
}
