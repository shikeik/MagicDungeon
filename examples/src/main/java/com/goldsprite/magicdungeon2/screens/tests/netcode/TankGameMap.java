package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.netcode.MapCollisionUtils;

/**
 * 坦克对战地图数据与碰撞逻辑。
 * <p>
 * 职责：墙体生成、边界约束、AABB碰撞检测。
 * 纯数据+数学计算，不依赖图形API，可直接单元测试。
 */
public class TankGameMap {

    private final float mapWidth;
    private final float mapHeight;
    private final List<float[]> walls = new ArrayList<>();
    private long mapSeed;

    public TankGameMap(float mapWidth, float mapHeight) {
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
    }

    // ══════════════ 墙体生成 ══════════════

    /**
     * 根据种子生成随机墙体布局。
     * 双端使用相同种子可得到一致的墙体布局。
     *
     * @param seed 随机种子（Server 时间戳 / 房间名哈希）
     */
    public void generateWalls(long seed) {
        this.mapSeed = seed;
        walls.clear();
        float[][] generated = MapCollisionUtils.generateWalls(seed, mapWidth, mapHeight);
        for (float[] w : generated) {
            walls.add(w);
        }
    }

    // ══════════════ 边界约束 ══════════════

    /**
     * 将坐标限制在地图边界内（Clamp）。
     *
     * @param x        当前X坐标
     * @param y        当前Y坐标
     * @param halfSize 实体半径（如坦克30x30的一半=15px）
     * @return 修正后的坐标（复用传入的 Vector2 或新建）
     */
    public Vector2 clampToBoundary(float x, float y, float halfSize) {
        float cx = Math.max(halfSize, Math.min(mapWidth - halfSize, x));
        float cy = Math.max(halfSize, Math.min(mapHeight - halfSize, y));
        return new Vector2(cx, cy);
    }

    // ══════════════ 墙体碰撞 ══════════════

    /**
     * 将实体推出所有墙体（AABB 碰撞 + 最小穿透推回）。
     * 实体视为 halfSize*2 × halfSize*2 的正方形。
     *
     * @param x        当前X坐标
     * @param y        当前Y坐标
     * @param halfSize 实体半径
     * @return 修正后的坐标
     */
    public Vector2 pushOutOfWalls(float x, float y, float halfSize) {
        float tx = x, ty = y;
        float tLeft = tx - halfSize, tBottom = ty - halfSize;
        float tRight = tx + halfSize, tTop = ty + halfSize;

        for (float[] w : walls) {
            float wLeft = w[0], wBottom = w[1];
            float wRight = w[0] + w[2], wTop = w[1] + w[3];

            // AABB 重叠检测
            if (tLeft < wRight && tRight > wLeft && tBottom < wTop && tTop > wBottom) {
                // 计算四个方向的穿透深度，取最小值推回
                float pushLeft = tRight - wLeft;
                float pushRight = wRight - tLeft;
                float pushDown = tTop - wBottom;
                float pushUp = wTop - tBottom;

                float minPush = Math.min(Math.min(pushLeft, pushRight), Math.min(pushDown, pushUp));
                if (minPush == pushLeft) {
                    tx -= pushLeft;
                } else if (minPush == pushRight) {
                    tx += pushRight;
                } else if (minPush == pushDown) {
                    ty -= pushDown;
                } else {
                    ty += pushUp;
                }
                // 更新局部变量以正确处理多墙体碰撞
                tLeft = tx - halfSize; tBottom = ty - halfSize;
                tRight = tx + halfSize; tTop = ty + halfSize;
            }
        }
        return new Vector2(tx, ty);
    }

    /**
     * 检测子弹是否命中任何墙体。
     *
     * @param bx         子弹中心X
     * @param by         子弹中心Y
     * @param bulletSize 子弹边长（默认8）
     * @return true 表示命中墙体
     */
    public boolean bulletHitsWall(float bx, float by, float bulletSize) {
        float half = bulletSize / 2f;
        for (float[] w : walls) {
            if (MapCollisionUtils.rectOverlap(
                bx - half, by - half, bulletSize, bulletSize,
                w[0], w[1], w[2], w[3])) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检测坐标是否越出地图边界。
     *
     * @param x X坐标
     * @param y Y坐标
     * @return true 表示越界
     */
    public boolean isOutOfBounds(float x, float y) {
        return x < 0 || x > mapWidth || y < 0 || y > mapHeight;
    }

    // ══════════════ Getter ══════════════

    public float getMapWidth() {
        return mapWidth;
    }

    public float getMapHeight() {
        return mapHeight;
    }

    public long getMapSeed() {
        return mapSeed;
    }

    /** 返回不可变的墙体列表（外部只读） */
    public List<float[]> getWalls() {
        return Collections.unmodifiableList(walls);
    }

    public void clear() {
        walls.clear();
        mapSeed = 0;
    }
}
