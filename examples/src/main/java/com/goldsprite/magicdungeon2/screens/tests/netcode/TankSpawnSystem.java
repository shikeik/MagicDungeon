package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;

/**
 * 坦克出生系统 —— 负责坦克的出生位置计算、颜色分配、生成及复活。
 * <p>
 * 纯逻辑方法（getSpawnPosition / getSpawnColor / configureTank / respawnTank）
 * 不依赖网络层，可独立进行单元测试。
 * <p>
 * spawnTankForOwner() 整合了 NetworkManager 的 SpawnWithPrefab 调用，
 * 适用于 Server 端在线运行。
 */
public class TankSpawnSystem {

    // ── 可配的出生颜色列表 ──
    public static final Color[] SPAWN_COLORS = {
        Color.ORANGE, Color.CYAN, Color.LIME, Color.MAGENTA, Color.GOLD, Color.SKY
    };

    /** 每行出生点数量 */
    private static final int SPAWN_COLUMNS = 4;
    /** 出生点起始偏移 */
    private static final float SPAWN_OFFSET = 150f;
    /** 出生点间距 */
    private static final float SPAWN_SPACING = 150f;

    // ── 纯逻辑方法（可独立测试）──

    /**
     * 根据序号计算出生位置。
     * 按 4 列网格排列：index 0→(150,150), 1→(300,150), ..., 4→(150,300)。
     *
     * @param index 出生序号（从 0 开始）
     * @return 出生坐标
     */
    public static Vector2 getSpawnPosition(int index) {
        float x = SPAWN_OFFSET + (index % SPAWN_COLUMNS) * SPAWN_SPACING;
        float y = SPAWN_OFFSET + (index / SPAWN_COLUMNS) * SPAWN_SPACING;
        return new Vector2(x, y);
    }

    /**
     * 根据序号获取出生颜色（循环取色）。
     *
     * @param index 出生序号
     * @return 该坦克的颜色
     */
    public static Color getSpawnColor(int index) {
        return SPAWN_COLORS[index % SPAWN_COLORS.length];
    }

    /**
     * 配置坦克的出生属性（位置、颜色、名称）。
     * 纯数据操作，不涉及网络 RPC。
     *
     * @param tank           目标坦克
     * @param index          出生序号（决定位置和颜色）
     * @param ownerClientId  所属客户端 ID（-1 = Host）
     */
    public static void configureTank(TankBehaviour tank, int index, int ownerClientId) {
        Vector2 pos = getSpawnPosition(index);
        tank.x.setValue(pos.x);
        tank.y.setValue(pos.y);
        tank.color.setValue(getSpawnColor(index));
        if (ownerClientId == -1) {
            tank.playerName.setValue("Host");
        } else {
            tank.playerName.setValue("Player#" + ownerClientId);
        }
    }

    /**
     * 通过 NetworkManager 生成并配置一辆新坦克。
     * Server 端调用：Spawn 网络对象 → 配置出生属性 → 加入 clientTanks 映射。
     *
     * @param manager        网络管理器（用于 Spawn）
     * @param ownerClientId  所属客户端 ID（-1 = Host）
     * @param clientTanks    当前坦克映射（用于计算序号 + 注册）
     * @return 新创建的 TankBehaviour
     */
    public static TankBehaviour spawnTankForOwner(
        NetworkManager manager, int ownerClientId, Map<Integer, TankBehaviour> clientTanks) {
        NetworkObject obj = manager.spawnWithPrefab(TankSandboxUtils.TANK_PREFAB_ID, ownerClientId);
        TankBehaviour tank = (TankBehaviour) obj.getBehaviours().get(0);

        int index = clientTanks.size();
        configureTank(tank, index, ownerClientId);
        clientTanks.put(ownerClientId, tank);
        return tank;
    }

    /**
     * 复活一辆已死亡的坦克：重置 HP、isDead，并回到对应出生点。
     *
     * @param tank           目标坦克
     * @param ownerClientId  所属客户端 ID
     * @param clientTanks    当前坦克映射（用于查找序号）
     */
    public static void respawnTank(
        TankBehaviour tank, int ownerClientId, Map<Integer, TankBehaviour> clientTanks) {
        tank.isDead.setValue(false);
        tank.hp.setValue(4);
        // 查找该 owner 在映射中的序号
        int index = new ArrayList<>(clientTanks.keySet()).indexOf(ownerClientId);
        if (index < 0) index = 0;
        Vector2 pos = getSpawnPosition(index);
        tank.x.setValue(pos.x);
        tank.y.setValue(pos.y);
        tank.color.setValue(getSpawnColor(index));
    }
}
