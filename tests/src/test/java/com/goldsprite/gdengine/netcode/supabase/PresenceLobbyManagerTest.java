package com.goldsprite.gdengine.netcode.supabase;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.badlogic.gdx.backends.headless.HeadlessNet;
import com.goldsprite.GdxTestRunner;

/**
 * Supabase Realtime Presence 连通性测试
 * <p>
 * 测试 PresenceLobbyManager 的核心流程:
 * 1. WebSocket 连接 → Supabase Realtime
 * 2. 加入 Phoenix Channel
 * 3. Track Presence (发布房间)
 * 4. 接收 presence_state 同步
 * 5. Untrack + 断开连接
 * <p>
 * 注意: 此测试需要网络连通到 Supabase 服务器。
 * 如果 SupabaseConfig 中的凭证无效，测试将跳过。
 */
@RunWith(GdxTestRunner.class)
public class PresenceLobbyManagerTest {

    private PresenceLobbyManager manager;

    @BeforeClass
    public static void setup() {
        if (Gdx.net == null) {
            Gdx.net = new HeadlessNet(new HeadlessApplicationConfiguration());
        }
    }

    @After
    public void tearDown() {
        if (manager != null) {
            manager.disconnect();
            manager = null;
        }
    }

    /**
     * 测试1: WebSocket 连接并加入频道
     */
    @Test
    public void testConnectAndJoinChannel() throws InterruptedException {
        if (SupabaseConfig.REALTIME_URL.contains("your-project-id")) {
            System.out.println("跳过测试: 未配置真实 Supabase 凭证");
            return;
        }

        CountDownLatch joinedLatch = new CountDownLatch(1);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        manager = new PresenceLobbyManager();
        manager.connect(
            rooms -> {
                // sync 回调 (可能在 join 之前或之后触发)
                System.out.println("收到房间同步, 数量: " + rooms.size());
            },
            new PresenceLobbyManager.OnStatusListener() {
                @Override
                public void onConnected() {
                    System.out.println("[测试] WebSocket 已连接");
                }

                @Override
                public void onJoined() {
                    System.out.println("[测试] 已加入频道");
                    joinedLatch.countDown();
                }

                @Override
                public void onError(String message) {
                    System.err.println("[测试] 错误: " + message);
                    errorRef.set(message);
                    joinedLatch.countDown();
                }

                @Override
                public void onDisconnected(String reason) {
                    System.out.println("[测试] 断开: " + reason);
                }
            }
        );

        boolean completed = joinedLatch.await(10, TimeUnit.SECONDS);
        assertNull("不应有错误: " + errorRef.get(), errorRef.get());
        assertTrue("加入频道超时 (10秒)", completed);
        assertTrue("应该已加入频道", manager.isReady());
    }

    /**
     * 测试2: 发布房间并通过 Presence 接收同步
     */
    @Test
    public void testPublishRoomAndReceiveSync() throws InterruptedException {
        if (SupabaseConfig.REALTIME_URL.contains("your-project-id")) {
            System.out.println("跳过测试: 未配置真实 Supabase 凭证");
            return;
        }

        CountDownLatch joinedLatch = new CountDownLatch(1);
        CountDownLatch syncLatch = new CountDownLatch(1);
        AtomicReference<List<PresenceRoomInfo>> syncedRooms = new AtomicReference<>(null);
        AtomicReference<String> errorRef = new AtomicReference<>(null);

        manager = new PresenceLobbyManager();
        manager.connect(
            rooms -> {
                System.out.println("收到房间同步, 数量: " + rooms.size());
                // 当收到包含我们自己房间的同步时
                for (PresenceRoomInfo room : rooms) {
                    if ("测试房间_Presence".equals(room.roomName)) {
                        syncedRooms.set(rooms);
                        syncLatch.countDown();
                        break;
                    }
                }
            },
            new PresenceLobbyManager.OnStatusListener() {
                @Override
                public void onConnected() {}

                @Override
                public void onJoined() {
                    joinedLatch.countDown();
                }

                @Override
                public void onError(String message) {
                    errorRef.set(message);
                    joinedLatch.countDown();
                    syncLatch.countDown();
                }

                @Override
                public void onDisconnected(String reason) {}
            }
        );

        // 等待加入频道
        assertTrue("加入频道超时", joinedLatch.await(10, TimeUnit.SECONDS));
        assertNull("加入时不应有错误", errorRef.get());

        // 发布房间
        PresenceRoomInfo info = new PresenceRoomInfo("测试房间_Presence", "127.0.0.1", 20001, 1, 6);
        manager.publishRoom(info);

        // 等待同步回调收到我们的房间
        boolean synced = syncLatch.await(10, TimeUnit.SECONDS);
        assertTrue("应该收到包含我们房间的同步", synced);

        List<PresenceRoomInfo> rooms = syncedRooms.get();
        assertNotNull("同步列表不应为空", rooms);
        System.out.println("同步到 " + rooms.size() + " 个房间");

        // 验证房间信息
        boolean found = false;
        for (PresenceRoomInfo room : rooms) {
            if ("测试房间_Presence".equals(room.roomName)) {
                assertEquals("127.0.0.1", room.hostIp);
                assertEquals(20001, room.hostPort);
                assertEquals(1, room.currentPlayers);
                assertEquals(6, room.maxPlayers);
                assertEquals("waiting", room.status);
                found = true;
                break;
            }
        }
        assertTrue("同步列表中应包含我们发布的房间", found);

        // 取消发布
        manager.unpublishRoom();
        System.out.println("[测试] 已取消发布房间，测试通过");
    }
}
