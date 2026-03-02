package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.Map;

import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.NetworkVariable;

/**
 * 坦克游戏核心逻辑 —— Server 端和 Client 端通用的 tick 驱动。
 * <p>
 * Sandbox（纯内存场景）和 Online（真 UDP 联机场景）共用此类，
 * 避免维护两套重复逻辑。所有游戏规则和物理行为只在此处实现一次。
 * <p>
 * 使用方式:
 * <ol>
 *   <li>外部读取输入 → 写入 {@link TankBehaviour#pendingMoveX}/{@code pendingMoveY}
 *       和 {@link TankBehaviour#pendingFire}</li>
 *   <li>Server 每帧调用 {@link #serverTick(float, Map)} 驱动所有坦克逻辑</li>
 *   <li>Client 每帧调用 {@link #clientReconcileTick(float, NetworkManager)} 驱动插值</li>
 *   <li>Client 每帧调用 {@link #clientBulletTick(float, NetworkManager)} 驱动子弹渲染同步</li>
 * </ol>
 */
public class TankGameLogic {

    private final BulletSystem bulletSystem;
    private final TankGameMap gameMap;

    /** 移动速度（像素/秒） */
    public static final float MOVE_SPEED = 200f;
    /** 坦克碰撞半径（30×30 的一半） */
    public static final float TANK_HALF_SIZE = 15f;

    public TankGameLogic(BulletSystem bulletSystem, TankGameMap gameMap) {
        this.bulletSystem = bulletSystem;
        this.gameMap = gameMap;
    }

    // ══════════════ Server 端统一驱动 ══════════════

    /**
     * Server 端每帧总入口：依次处理坦克移动/开火/死亡 → 子弹物理 → 碰撞修正。
     * <p>
     * 外部只需在调用前把输入写入 {@code pendingMoveX/Y} 和 {@code pendingFire}，
     * 统一由此方法消费和执行。
     *
     * @param delta 帧间隔（秒）
     * @param tanks 所有坦克映射（ownerClientId → TankBehaviour）
     */
    public void serverTick(float delta, Map<Integer, TankBehaviour> tanks) {
        serverTickTanks(delta, tanks);
        serverTickBullets(delta, tanks);
        serverTickCollision(tanks);
    }

    /**
     * 处理所有坦克的移动、开火请求、死亡倒计时和复活。
     * <p>
     * 输入约定：
     * <ul>
     *   <li>{@code pendingMoveX/Y}: 归一化方向 (−1~1)，代表当前摇杆状态（持续生效，不清零）</li>
     *   <li>{@code pendingFire}: true 表示有开火请求，本方法消费后自动复位</li>
     * </ul>
     */
    private void serverTickTanks(float delta, Map<Integer, TankBehaviour> tanks) {
        float speed = MOVE_SPEED * delta;

        for (Map.Entry<Integer, TankBehaviour> entry : tanks.entrySet()) {
            TankBehaviour tank = entry.getValue();
            int ownerId = entry.getKey();

            if (!tank.isDead.getValue()) {
                // ── 消费移动输入（持续状态，不清零，发送端负责发0,0停止）──
                float mx = tank.pendingMoveX;
                float my = tank.pendingMoveY;
                if (mx != 0 || my != 0) {
                    tank.x.setValue(tank.x.getValue() + mx * speed);
                    tank.y.setValue(tank.y.getValue() + my * speed);
                    tank.rot.setValue(new Vector2(mx, my).angleDeg());
                }

                // ── 消费开火请求 ──
                if (tank.pendingFire) {
                    bulletSystem.spawnBullet(tank, ownerId);
                    tank.pendingFire = false;
                }
            } else {
                // ── 死亡倒计时 → 复活 ──
                float timer = tank.respawnTimer.getValue() - delta;
                tank.respawnTimer.setValue(Math.max(0, timer));
                if (timer <= 0) {
                    TankSpawnSystem.respawnTank(tank, ownerId, tanks);
                }
            }
        }
    }

    /**
     * Server 端子弹物理更新 + 坦克碰撞检测。
     */
    private void serverTickBullets(float delta, Map<Integer, TankBehaviour> tanks) {
        bulletSystem.updateServerBullets(delta, gameMap, tanks);
    }

    /**
     * Server 端坦克边界约束 + 墙体推出。
     * 所有坦克位置在此统一修正，保证不超出地图、不嵌入墙体。
     */
    private void serverTickCollision(Map<Integer, TankBehaviour> tanks) {
        for (TankBehaviour tank : tanks.values()) {
            if (tank.isDead.getValue()) continue;

            // 边界限制
            Vector2 clamped = gameMap.clampToBoundary(
                tank.x.getValue(), tank.y.getValue(), TANK_HALF_SIZE);
            if (clamped.x != tank.x.getValue()) tank.x.setValue(clamped.x);
            if (clamped.y != tank.y.getValue()) tank.y.setValue(clamped.y);

            // 推出墙体（无墙时原样返回，不影响性能）
            Vector2 pushed = gameMap.pushOutOfWalls(
                tank.x.getValue(), tank.y.getValue(), TANK_HALF_SIZE);
            tank.x.setValue(pushed.x);
            tank.y.setValue(pushed.y);
        }
    }

    // ══════════════ Client 端统一驱动 ══════════════

    /**
     * Client 端: 驱动所有网络变量的平滑调和插值。
     * <p>
     * 当 {@link NetworkVariable#enableSmooth} 处于开启状态时，
     * {@code deserialize} 仅写入 {@code serverAuthoritativeValue}，
     * 必须由此方法每帧驱动 {@code reconcileTick} 才能让 {@code value} 跟上。
     */
    public static void clientReconcileTick(float delta, NetworkManager manager) {
        for (NetworkObject obj : manager.getAllNetworkObjects()) {
            for (NetworkVariable<?> var : obj.getNetworkVariables()) {
                var.reconcileTick(delta);
            }
        }
    }

    /**
     * Client 端: 从所有 NetworkObject 收集并模拟客户端子弹。
     * 包含 {@code drainPendingBulletEvents} + 运动 + 越界/墙体移除。
     */
    public void clientBulletTick(float delta, NetworkManager manager) {
        bulletSystem.updateClientBullets(delta, gameMap, manager.getAllNetworkObjects());
    }

    // ══════════════ 通用工具 ══════════════

    /**
     * 将输入方向归一化，避免斜向移动速度比直线更快。
     * Sandbox 和 Online 共用。
     */
    public static Vector2 normalizeDir(float dx, float dy) {
        if (dx == 0f && dy == 0f) {
            return new Vector2(0f, 0f);
        }
        return new Vector2(dx, dy).nor();
    }

    // ══════════════ Getter ══════════════

    public BulletSystem getBulletSystem() { return bulletSystem; }
    public TankGameMap getGameMap() { return gameMap; }
}
