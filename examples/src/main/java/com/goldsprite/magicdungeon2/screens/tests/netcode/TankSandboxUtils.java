package com.goldsprite.magicdungeon2.screens.tests.netcode;

import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.NetworkPrefabFactory;

/**
 * 坦克沙盒共用的渲染与工厂工具类。
 * 单进程双屏沙盒和跨进程 UDP 沙盒共用，避免重复绘制代码。
 */
public class TankSandboxUtils {

    /** 坦克预制体 ID（双端必须一致） */
    public static final int TANK_PREFAB_ID = 1;

    /** 创建坦克预制体工厂 */
    public static NetworkPrefabFactory createTankFactory() {
        return () -> {
            NetworkObject obj = new NetworkObject();
            obj.addComponent(new TankBehaviour());
            return obj;
        };
    }

    /** 在 Server 端生成一颗子弹并通过 ClientRpc 广播给客户端 */
    public static void spawnBullet(TankBehaviour tank, int ownerId, List<Bullet> serverBullets) {
        Bullet b = new Bullet();
        float rad = tank.rot.getValue() * MathUtils.degreesToRadians;
        b.x = tank.x.getValue() + MathUtils.cos(rad) * 20;
        b.y = tank.y.getValue() + MathUtils.sin(rad) * 20;
        b.vx = MathUtils.cos(rad) * 400;
        b.vy = MathUtils.sin(rad) * 400;
        b.ownerId = ownerId;
        b.color = tank.color.getValue();
        serverBullets.add(b);
        tank.sendClientRpc("rpcSpawnBullet", b.x, b.y, b.vx, b.vy, ownerId);
    }

    /** Server 端扣血逻辑 */
    public static void hitTank(TankBehaviour tank) {
        int hp = tank.hp.getValue() - 1;
        tank.hp.setValue(hp);
        if (hp <= 0) {
            tank.isDead.setValue(true);
            tank.respawnTimer.setValue(3f);
            tank.color.setValue(Color.GRAY);
        }
    }

    /** 绘制坦克（车体 + 炮管 + HP 槽） */
    public static void drawTank(NeonBatch neon, BitmapFont font, TankBehaviour t, float offsetX) {
        float cx = t.x.getValue() + offsetX;
        float cy = t.y.getValue();
        float rot = t.rot.getValue();

        if (t.isDead.getValue()) {
            font.setColor(Color.WHITE);
            font.draw(neon, String.format("Respawn: %.1f s", t.respawnTimer.getValue()), cx - 40, cy);
            return;
        }

        // 车体
        neon.drawRect(cx - 15, cy - 15, 30, 30, rot, 0, t.color.getValue(), true);

        // 炮管
        float rad = rot * MathUtils.degreesToRadians;
        float dirX = MathUtils.cos(rad);
        float dirY = MathUtils.sin(rad);
        float barrelLen = 25f;
        float bcx = cx + dirX * (barrelLen / 2f + 5);
        float bcy = cy + dirY * (barrelLen / 2f + 5);
        neon.drawRect(bcx - barrelLen / 2f, bcy - 4f, barrelLen, 8f, rot, 0, Color.WHITE, true);

        // HP 槽
        int hp = t.hp.getValue();
        for (int i = 0; i < 4; i++) {
            Color hpColor = i < hp ? Color.GREEN : Color.RED;
            neon.drawRect(cx - 15 + i * 8, cy - 25, 6, 6, 0, 0, hpColor, true);
        }
    }

    /** 绘制子弹列表 */
    public static void drawBullets(NeonBatch neon, List<Bullet> bullets, float offsetX) {
        for (Bullet b : bullets) {
            neon.drawRect(b.x + offsetX - 4, b.y - 4, 8, 8, 0, 0, b.color, true);
        }
    }
}
