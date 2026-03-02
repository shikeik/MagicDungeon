package com.goldsprite.gdengine.netcode.supabase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.goldsprite.GdxTestRunner;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessNet;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

@RunWith(GdxTestRunner.class)
public class SupabaseClientTest {

    // 替换为你的真实 Supabase 测试环境配置
    // 或者使用 Mock / 本地测试服务器
    // 这个由于涉及真实 API 操作可能会无法跑通直到真配了对应的URL，仅作为TDD案例
    private static final String TEST_URL = "https://your-project-id.supabase.co";
    private static final String TEST_KEY = "your-anon-key";

    @BeforeClass
    public static void setup() {
        if (Gdx.net == null) {
            Gdx.net = new HeadlessNet(new HeadlessApplicationConfiguration());
        }
    }

    @Test
    public void testCreateRoom() throws InterruptedException {
        // [INFO] 如果你没有配置好真实的 Supabase URL，这个测试会报 DNS 解析错误，
        // 在正式使用前需要确保这里是填入你自己在 supabase 建立的合法 URL 和 Anon-KEY
        if (TEST_URL.contains("your-project-id")) {
            System.out.println("跳过 testCreateRoom 测试，因为未配置真实 Supabase 凭证");
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        SupabaseClient client = new SupabaseClient(TEST_URL, TEST_KEY);

        RoomModel room = new RoomModel("测试房间_TDD", "127.0.0.1", 20000, 1, 6);

        client.createRoom(room, new SupabaseClient.RequestCallback<RoomModel>() {
            @Override
            public void onSuccess(RoomModel result) {
                System.out.println("成功创建房间, ID: " + result.getId());
                assertNotNull("房间 ID 不应该为空", result.getId());
                assertEquals("测试房间_TDD", result.getRoom_name());
                latch.countDown();
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                latch.countDown();
                fail("不应抛出异常: " + t.getMessage());
            }
        });

        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue("建房请求超时", completed);
    }
}
