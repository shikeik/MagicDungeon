package com.goldsprite.magicdungeon2.network.lan.packet;

import goldsprite.myUdpNetty.codec.packets.ResponsePacket;

/**
 * 房主向全体客户端广播伤害判定结果
 * 用于各端同步显示伤害飘字和更新敌人血量
 */
public class LanDamageResultBroadcastPacket extends ResponsePacket {
    private int enemyId;        // 被攻击的敌人ID
    private float damage;       // 造成的伤害
    private float remainHp;     // 敌人剩余HP
    private boolean killed;     // 是否被击杀
    private int attackerGuid;   // 攻击者的玩家GUID
    private int xpReward;       // 击杀经验奖励（仅killed=true时有效）

    public LanDamageResultBroadcastPacket(int ownerGuid, int repCode,
                                          int enemyId, float damage, float remainHp,
                                          boolean killed, int attackerGuid, int xpReward) {
        super(ownerGuid, repCode);
        this.enemyId = enemyId;
        this.damage = damage;
        this.remainHp = remainHp;
        this.killed = killed;
        this.attackerGuid = attackerGuid;
        this.xpReward = xpReward;
    }

    @Override
    public byte getCommand() {
        return LanCommands.DAMAGE_RESULT_BROADCAST;
    }

    public int getEnemyId() { return enemyId; }
    public float getDamage() { return damage; }
    public float getRemainHp() { return remainHp; }
    public boolean isKilled() { return killed; }
    public int getAttackerGuid() { return attackerGuid; }
    public int getXpReward() { return xpReward; }
}
