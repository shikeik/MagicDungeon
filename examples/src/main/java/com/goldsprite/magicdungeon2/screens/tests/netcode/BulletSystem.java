package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.netcode.NetworkObject;

/**
 * 子弹管理系统。
 * <p>
 * 职责：
 * <ul>
 *   <li>Server 端：子弹物理运动、墙体碰撞、坦克碰撞检测</li>
 *   <li>Client 端：本地子弹模拟（从 NetworkObject 收集 + 物理运动）</li>
 * </ul>
 * 不依赖图形API，可独立单元测试。
 */
public class BulletSystem {

    // ── Server 端子弹 ──
    private final List<Bullet> serverBullets = new ArrayList<>();
    private int nextBulletId = 1;

    // ── Client 端子弹 ──
    private final List<Bullet> clientBullets = new ArrayList<>();

    // ══════════════ Server 端 ══════════════

    /**
     * 纯数据层创建子弹（不发送 RPC，可在无网络环境下测试）。
     *
     * @param tank    射击坦克
     * @param ownerId 坦克所属 clientId
     * @return 创建的子弹
     */
    public Bullet createBullet(TankBehaviour tank, int ownerId) {
        Bullet b = new Bullet();
        b.bulletId = nextBulletId++;
        float rad = tank.rot.getValue() * MathUtils.degreesToRadians;
        b.x = tank.x.getValue() + MathUtils.cos(rad) * 20;
        b.y = tank.y.getValue() + MathUtils.sin(rad) * 20;
        b.vx = MathUtils.cos(rad) * 400;
        b.vy = MathUtils.sin(rad) * 400;
        b.ownerId = ownerId;
        b.color = tank.color.getValue();
        serverBullets.add(b);
        return b;
    }

    /**
     * Server 端: 为坦克生成子弹并通过 ClientRpc 广播。
     * 需要 TankBehaviour 已绑定到 NetworkManager。
     *
     * @param tank    射击坦克
     * @param ownerId 坦克所属 clientId
     * @return 生成的子弹
     */
    public Bullet spawnBullet(TankBehaviour tank, int ownerId) {
        Bullet b = createBullet(tank, ownerId);
        // 通过 ClientRpc 广播给所有客户端
        tank.sendClientRpc("rpcSpawnBullet", b.x, b.y, b.vx, b.vy, ownerId, b.bulletId);
        return b;
    }

    /**
     * Server 端: 更新所有子弹物理 + 碰撞检测。
     *
     * @param delta 帧时间
     * @param map   地图碰撞数据
     * @param tanks 所有坦克映射 (ownerClientId → TankBehaviour)
     * @return 命中事件列表
     */
    public List<BulletHitEvent> updateServerBullets(float delta, TankGameMap map,
                                                     Map<Integer, TankBehaviour> tanks) {
        List<BulletHitEvent> hitEvents = new ArrayList<>();

        Iterator<Bullet> iter = serverBullets.iterator();
        while (iter.hasNext()) {
            Bullet b = iter.next();
            b.x += b.vx * delta;
            b.y += b.vy * delta;

            // 越界移除
            if (map.isOutOfBounds(b.x, b.y)) {
                iter.remove();
                continue;
            }

            // 墙体碰撞
            if (map.bulletHitsWall(b.x, b.y, 8f)) {
                iter.remove();
                continue;
            }

            // 坦克碰撞检测
            boolean hit = false;
            for (Map.Entry<Integer, TankBehaviour> entry : tanks.entrySet()) {
                int tankOwnerId = entry.getKey();
                TankBehaviour tank = entry.getValue();
                if (b.ownerId == tankOwnerId) continue; // 不打自己
                if (tank.isDead.getValue()) continue;

                double dist = Math.hypot(b.x - tank.x.getValue(), b.y - tank.y.getValue());
                if (dist < 20) {
                    TankSandboxUtils.hitTank(tank);
                    // 通知客户端销毁子弹（仅当绑定了 Manager 时生效）
                    TankBehaviour shooter = tanks.get(b.ownerId);
                    if (shooter != null && shooter.getManager() != null) {
                        shooter.sendClientRpc("rpcDestroyBullet", b.bulletId);
                    }
                    hitEvents.add(new BulletHitEvent(b.bulletId, tankOwnerId, b.ownerId));
                    hit = true;
                    break;
                }
            }
            if (hit) {
                iter.remove();
            }
        }
        return hitEvents;
    }

    // ══════════════ Client 端 ══════════════

    /**
     * Client 端: 从所有 NetworkObject 收集子弹并模拟物理运动。
     *
     * @param delta          帧时间
     * @param map            地图碰撞数据
     * @param networkObjects 所有网络对象（从 manager 获取）
     */
    public void updateClientBullets(float delta, TankGameMap map,
                                     Iterable<NetworkObject> networkObjects) {
        clientBullets.clear();

        for (NetworkObject obj : networkObjects) {
            if (obj.getBehaviours().isEmpty()) continue;
            TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);

            // 主线程统一消费 IO 线程缓冲的子弹生成/销毁事件（线程安全）
            tank.drainPendingBulletEvents();

            Iterator<Bullet> iter = tank.localBullets.iterator();
            while (iter.hasNext()) {
                Bullet b = iter.next();
                b.x += b.vx * delta;
                b.y += b.vy * delta;

                // 越界移除
                if (map.isOutOfBounds(b.x, b.y)) {
                    iter.remove();
                    continue;
                }

                // 墙体碰撞（视觉一致性）
                if (map.bulletHitsWall(b.x, b.y, 8f)) {
                    iter.remove();
                    continue;
                }

                clientBullets.add(b);
            }
        }
    }

    // ══════════════ Getter / 工具 ══════════════

    /** 获取 Server 端子弹列表（只读） */
    public List<Bullet> getServerBullets() {
        return Collections.unmodifiableList(serverBullets);
    }

    /** 获取 Server 端子弹列表（可写，供直接操作） */
    public List<Bullet> getServerBulletsMutable() {
        return serverBullets;
    }

    /** 获取 Client 端子弹列表（只读） */
    public List<Bullet> getClientBullets() {
        return Collections.unmodifiableList(clientBullets);
    }

    /** 获取当前子弹自增 ID */
    public int getNextBulletId() {
        return nextBulletId;
    }

    /** 清空所有子弹状态 */
    public void clear() {
        serverBullets.clear();
        clientBullets.clear();
        nextBulletId = 1;
    }

    // ══════════════ 子弹命中事件 ══════════════

    /**
     * 子弹命中事件数据。
     */
    public static class BulletHitEvent {
        public final int bulletId;
        public final int hitTankOwnerId;
        public final int shooterOwnerId;

        public BulletHitEvent(int bulletId, int hitTankOwnerId, int shooterOwnerId) {
            this.bulletId = bulletId;
            this.hitTankOwnerId = hitTankOwnerId;
            this.shooterOwnerId = shooterOwnerId;
        }
    }
}
