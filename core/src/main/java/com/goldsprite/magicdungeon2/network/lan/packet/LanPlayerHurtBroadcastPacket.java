package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 房主向全体客户端广播"玩家受到敌人攻击"
 * 用于远程玩家同步扣血（房主权威判定后通知目标客户端）
 */
public class LanPlayerHurtBroadcastPacket extends ResponsePacket {
    private int targetGuid;      // 被攻击的玩家GUID
    private float damage;        // 造成的伤害
    private float remainHp;      // 玩家剩余HP
    private int attackerEnemyId; // 攻击者的敌人ID

    public LanPlayerHurtBroadcastPacket(int ownerGuid, int repCode,
                                        int targetGuid, float damage,
                                        float remainHp, int attackerEnemyId) {
        super(ownerGuid, repCode);
        this.targetGuid = targetGuid;
        this.damage = damage;
        this.remainHp = remainHp;
        this.attackerEnemyId = attackerEnemyId;
    }

    @Override
    public byte getCommand() {
        return LanCommands.PLAYER_HURT_BROADCAST;
    }

    public int getTargetGuid() { return targetGuid; }
    public float getDamage() { return damage; }
    public float getRemainHp() { return remainHp; }
    public int getAttackerEnemyId() { return attackerEnemyId; }
}
