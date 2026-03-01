package com.goldsprite.magicdungeon2.network.lan.packet;

import java.util.List;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 房主定期广播所有敌人状态给全体客户端
 */
public class LanEnemySyncBroadcastPacket extends ResponsePacket {
    private List<EnemyStateSnapshot> enemies;

    public LanEnemySyncBroadcastPacket(int ownerGuid, int repCode, List<EnemyStateSnapshot> enemies) {
        super(ownerGuid, repCode);
        this.enemies = enemies;
    }

    @Override
    public byte getCommand() {
        return LanCommands.ENEMY_SYNC_BROADCAST;
    }

    public List<EnemyStateSnapshot> getEnemies() {
        return enemies;
    }
}
