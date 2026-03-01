package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.netcode.ClientRpc;
import com.goldsprite.gdengine.netcode.NetworkBehaviour;
import com.goldsprite.gdengine.netcode.NetworkVariable;

/**
 * 坦克沙盒共用的网络行为组件。
 * 单进程双屏沙盒和跨进程 UDP 沙盒共用此类，避免重复定义。
 */
public class TankBehaviour extends NetworkBehaviour {
    public NetworkVariable<Float> x = new NetworkVariable<>(0f);
    public NetworkVariable<Float> y = new NetworkVariable<>(0f);
    public NetworkVariable<Float> rot = new NetworkVariable<>(90f);
    public NetworkVariable<Color> color = new NetworkVariable<>(Color.RED);
    public NetworkVariable<Integer> hp = new NetworkVariable<>(4);
    public NetworkVariable<Boolean> isDead = new NetworkVariable<>(false);
    public NetworkVariable<Float> respawnTimer = new NetworkVariable<>(0f);

    /** 客户端本地子弹列表（由 ClientRpc 触发生成） */
    public transient List<Bullet> localBullets = new ArrayList<>();

    /**
     * Server -> Client: 通知客户端生成子弹，客户端独立模拟运动
     */
    @ClientRpc
    public void rpcSpawnBullet(float bx, float by, float bvx, float bvy, int ownerId) {
        Bullet b = new Bullet();
        b.x = bx; b.y = by;
        b.vx = bvx; b.vy = bvy;
        b.ownerId = ownerId;
        b.color = color.getValue();
        localBullets.add(b);
    }
}
