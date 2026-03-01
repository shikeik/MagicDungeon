package com.goldsprite.magicdungeon2.network.lan.packet;

/**
 * 敌人状态快照 — 用于网络同步
 * 房主定期广播所有敌人的状态给客户端
 */
public class EnemyStateSnapshot {
    private int enemyId;        // 敌人唯一标识（房主分配）
    private String enemyType;   // 敌人类型名（用于客户端选择贴图）
    private int x, y;           // 网格坐标
    private float visualX, visualY; // 像素坐标（用于插值渲染）
    private float hp, maxHp;    // 生命值
    private boolean alive;      // 是否存活
    private String action;      // 动作状态（idle/walk/attack/hurt/die）
    private long timestamp;

    public EnemyStateSnapshot() {}

    public EnemyStateSnapshot(int enemyId, String enemyType, int x, int y,
                              float visualX, float visualY, float hp, float maxHp,
                              boolean alive, String action, long timestamp) {
        this.enemyId = enemyId;
        this.enemyType = enemyType;
        this.x = x;
        this.y = y;
        this.visualX = visualX;
        this.visualY = visualY;
        this.hp = hp;
        this.maxHp = maxHp;
        this.alive = alive;
        this.action = action;
        this.timestamp = timestamp;
    }

    // ============ Getters & Setters ============

    public int getEnemyId() { return enemyId; }
    public void setEnemyId(int enemyId) { this.enemyId = enemyId; }

    public String getEnemyType() { return enemyType; }
    public void setEnemyType(String enemyType) { this.enemyType = enemyType; }

    public int getX() { return x; }
    public void setX(int x) { this.x = x; }

    public int getY() { return y; }
    public void setY(int y) { this.y = y; }

    public float getVisualX() { return visualX; }
    public void setVisualX(float visualX) { this.visualX = visualX; }

    public float getVisualY() { return visualY; }
    public void setVisualY(float visualY) { this.visualY = visualY; }

    public float getHp() { return hp; }
    public void setHp(float hp) { this.hp = hp; }

    public float getMaxHp() { return maxHp; }
    public void setMaxHp(float maxHp) { this.maxHp = maxHp; }

    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
