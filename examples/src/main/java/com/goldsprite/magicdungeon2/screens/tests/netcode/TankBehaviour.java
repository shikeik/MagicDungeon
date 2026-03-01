package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.netcode.ClientRpc;
import com.goldsprite.gdengine.netcode.NetworkBehaviour;
import com.goldsprite.gdengine.netcode.NetworkVariable;
import com.goldsprite.gdengine.netcode.ServerRpc;

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

    /** Server 端暂存的移动方向（由 rpcMoveInput 写入，由 Server update 消费） */
    public transient float pendingMoveX = 0f;
    public transient float pendingMoveY = 0f;

    // ==================== ServerRpc: Client → Server 上报输入 ====================

    /**
     * Client → Server: 上报移动输入（归一化方向）。
     * Server 收到后仅存储方向，实际位移由 Server update 统一驱动。
     * @param dx 水平方向 (-1/0/1)
     * @param dy 垂直方向 (-1/0/1)
     */
    @ServerRpc
    public void rpcMoveInput(float dx, float dy) {
        pendingMoveX = dx;
        pendingMoveY = dy;
    }

    /**
     * Client → Server: 上报开火请求。
     * Server 收到后执行子弹生成逻辑。
     */
    @ServerRpc
    public void rpcFireInput() {
        // Server 端会处理开火，具体由沙盒屏幕的 update 逻辑调用
        // 此处仅作标记，实际开火逻辑在沙盒层判断并调用 TankSandboxUtils.spawnBullet
        pendingFire = true;
    }

    /** Server 端标记：此坦克是否有待处理的开火请求 */
    public transient boolean pendingFire = false;

    /**
     * Server -> Client: 通知客户端生成子弹，客户端独立模拟运动
     */
    @ClientRpc
    public void rpcSpawnBullet(float bx, float by, float bvx, float bvy, int ownerId, int bulletId) {
        Bullet b = new Bullet();
        b.bulletId = bulletId;
        b.x = bx; b.y = by;
        b.vx = bvx; b.vy = bvy;
        b.ownerId = ownerId;
        b.color = color.getValue();
        localBullets.add(b);
    }

    /**
     * Server -> Client: 通知客户端销毁指定子弹（命中时）
     */
    @ClientRpc
    public void rpcDestroyBullet(int bulletId) {
        localBullets.removeIf(b -> b.bulletId == bulletId);
    }
}
