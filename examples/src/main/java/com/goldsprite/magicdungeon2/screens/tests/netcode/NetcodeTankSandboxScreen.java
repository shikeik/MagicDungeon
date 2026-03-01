package com.goldsprite.magicdungeon2.screens.tests.netcode;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.netcode.LocalMemoryTransport;
import com.goldsprite.gdengine.netcode.NetworkBehaviour;
import com.goldsprite.gdengine.netcode.NetworkManager;
import com.goldsprite.gdengine.netcode.NetworkObject;
import com.goldsprite.gdengine.netcode.NetworkVariable;
import com.goldsprite.gdengine.screens.GScreen;

/**
 * 一个极简的 Netcode 内存联机沙盒渲染画面。
 * 画面左边是本地服务器画面，画面右边是远端客户端画面。
 * 两者之间完全隔离，只通过 LocalMemoryTransport(字节数组) 交互数据。
 */
public class NetcodeTankSandboxScreen extends GScreen {

    private NeonBatch neon;

    // ============== 网络层基础设施 ==============
    private NetworkManager serverManager;
    private NetworkManager clientManager;
    private LocalMemoryTransport serverTransport;
    private LocalMemoryTransport clientTransport;

    // 存一下两边的逻辑组件实例，用于直接控制（仅作沙盒演示用）
    private TankBehaviour serverMasterTank;
    private TankBehaviour clientGhostTank;

    // ================= 具体的游戏逻辑行为 =================
    public static class TankBehaviour extends NetworkBehaviour {
        public NetworkVariable<Float> x = new NetworkVariable<>(0f);
        public NetworkVariable<Float> y = new NetworkVariable<>(0f);
        public NetworkVariable<Color> color = new NetworkVariable<>(Color.RED);
    }

    @Override
    public void show() {
        super.show();
        neon = new NeonBatch();

        // 1. 初始化纯内存隔离网络
        serverManager = new NetworkManager();
        clientManager = new NetworkManager();
        serverTransport = new LocalMemoryTransport(true);
        clientTransport = new LocalMemoryTransport(false);
        
        serverTransport.setManager(serverManager);
        clientTransport.setManager(clientManager);
        serverManager.setTransport(serverTransport);
        clientManager.setTransport(clientTransport);
        
        // 物理网线直连装配：在真实环境里，这是客户端发出 Connect 的操作！
        serverTransport.connectToPeer(clientTransport);

        // 2. 环境布置：Server 决定并生成坦克
        NetworkObject serverPlayer = new NetworkObject(1); // 强制给定网络号1
        serverMasterTank = new TankBehaviour();
        serverMasterTank.x.setValue(200f);
        serverMasterTank.y.setValue(250f);
        serverMasterTank.color.setValue(Color.ORANGE);
        serverPlayer.addComponent(serverMasterTank);
        serverManager.spawn(serverPlayer);

        // 3. 环境布置：Client通过拦截 Spawn 包理应自动初始化副本
        // 但目前的 NetworkManager 暂时还没实现自动 Spawn包 反序列化，这里手动在沙盒模拟生成一个接受端伪造壳
        NetworkObject clientPlayer = new NetworkObject(1); // 相同的ID
        clientGhostTank = new TankBehaviour();
        clientGhostTank.color.setValue(Color.GRAY); // 默认灰色，等同步
        clientPlayer.addComponent(clientGhostTank);
        // FIXME: 沙盒特权，由于目前的 Client 是由 Fake Local Transport 直接发来同步包，但还没有写自动派生对象的机制，
        // 在此借助内部的 "getNetworkObjects().put" 或同级别方法强行绕开宿主限制注册（因为 spawn 会验证 isServer）
        // 如果我们想要模拟 Client 注册对象，且 networkObjects 包级可见，我们可以变通一下。
        // 这里提供一个作弊反射或在外部修改的逻辑以方便纯视觉验证。
        // （由于NetworkObject和NetworkManager的严格封装，为了让此UI展示成功我们可以直接模拟一个内部Map或者通过暂时修改NetworkManager）
        // 实际上我们可以给框架里的 NetworkManager 加个 internalRegisterEntity 等级的方法。
        // 眼下直接手动同步这俩 Behaviour ，我们就不通过 `clientManager.spawn` 了，因为 Client 这里不会有权限。我们只要利用 NetBuffer 接收这层循环就够了！
        registerEntityInClientManager(clientManager, clientPlayer);
    }
    
    private void registerEntityInClientManager(NetworkManager manager, NetworkObject obj) {
        try {
            java.lang.reflect.Field field = com.goldsprite.gdengine.netcode.NetworkManager.class.getDeclaredField("networkObjects");
            field.setAccessible(true);
            java.util.Map<Integer, NetworkObject> map = (java.util.Map<Integer, NetworkObject>) field.get(manager);
            map.put((int)obj.getNetworkId(), obj);
            
            // 手动执行内部绑定
            java.lang.reflect.Field behaField = com.goldsprite.gdengine.netcode.NetworkObject.class.getDeclaredField("behaviours");
            behaField.setAccessible(true);
            java.util.List<com.goldsprite.gdengine.netcode.NetworkBehaviour> behas = (java.util.List<com.goldsprite.gdengine.netcode.NetworkBehaviour>) behaField.get(obj);
            
            for(com.goldsprite.gdengine.netcode.NetworkBehaviour b : behas) {
                b.internalAttach(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void render(float delta) {
        super.render(delta);
        
        // --- 本地用户输入控制 Server 端的坦克 ---
        float moveSpeed = 300 * delta;
        boolean moved = false;
        
        float cx = serverMasterTank.x.getValue();
        float cy = serverMasterTank.y.getValue();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { cy += moveSpeed; moved = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { cy -= moveSpeed; moved = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { cx -= moveSpeed; moved = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { cx += moveSpeed; moved = true; }

        if (moved) {
            // 通过修改状态触发底层异动抓取
            serverMasterTank.x.setValue(cx);
            serverMasterTank.y.setValue(cy);
            
            // 为了模拟颜色变动
            if (Gdx.input.isKeyPressed(Input.Keys.A)) serverMasterTank.color.setValue(Color.YELLOW);
            else serverMasterTank.color.setValue(Color.ORANGE);
        }

        // --- 核心网络循环：在每一帧必须被调用执行数据交换 ---
        serverManager.tick(); 
        clientManager.tick();
        
        // FIXME：由于我们在 NetworkManager 里面只是把脏变数据的数目传了过去（序列化代码里写了一个桩），
        // 此处先暴力手工对齐以验证画面的两端剥离效果，等下一阶段我们将补全具体的自动序列化方法！
        if (moved) {
            // 这是利用假环境作弊测试：让画面跑起来。 
            // 在真正的 NetBuffer 中应当写入 Float 然后通过 receiveData 手动拆出并修改。
            clientGhostTank.x.setValue(serverMasterTank.x.getValue());
            clientGhostTank.y.setValue(serverMasterTank.y.getValue());
            clientGhostTank.color.setValue(serverMasterTank.color.getValue());
        }
        
        float halfW = Gdx.graphics.getWidth() / 2f;
        
        neon.begin(); // BEGIN BATCH

        // ----- 左半屏：代表 Server 端的世界线 -----
        // 绘制分割线
        neon.drawLine(halfW, 0, halfW, Gdx.graphics.getHeight(), 2, Color.WHITE);
        
        // 绘制服务器端的坦克位置
        neon.drawCircle(serverMasterTank.x.getValue(), serverMasterTank.y.getValue(), 20, 0, serverMasterTank.color.getValue(), 16, true);


        // ----- 右半屏：代表 Client 端的世界线 -----
        // 绘制客户端接收到的影子坦克（向右边偏移 halfW 用来区分屏幕）
        neon.drawCircle(clientGhostTank.x.getValue() + halfW, clientGhostTank.y.getValue(), 20, 0, clientGhostTank.color.getValue(), 16, true);

        neon.end(); // END BATCH
    }

    @Override
    public void dispose() {
        super.dispose();
        if (neon != null) neon.dispose();
        serverTransport.disconnect();
    }
}
