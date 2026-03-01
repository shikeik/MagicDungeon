package com.goldsprite.magicdungeon2.network.lan;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.goldsprite.magicdungeon2.network.lan.packet.EnemyStateSnapshot;
import com.goldsprite.magicdungeon2.network.lan.packet.LanAttackRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanCommands;
import com.goldsprite.magicdungeon2.network.lan.packet.LanDamageResultBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanEnemySyncBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanFloorChangeBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanGameStartBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanGameStartRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerHurtBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerStateSnapshot;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerSyncBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerSyncRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanRoomPlayersRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanRoomPlayersResponsePacket;

import com.goldsprite.gdengine.log.DLog;

import goldsprite.myUdpNetty.codec.PacketCodeC;
import goldsprite.myUdpNetty.codec.codecInterfaces.IStatus;
import goldsprite.myUdpNetty.codec.packets.BroadcastRequestPacket;
import goldsprite.myUdpNetty.codec.packets.BroadcastResponsePacket;
import goldsprite.myUdpNetty.codec.packets.LoginRequestPacket;
import goldsprite.myUdpNetty.codec.packets.LoginResponsePacket;
import goldsprite.myUdpNetty.handlers.PacketsHandler;
import goldsprite.myUdpNetty.other.ClientInfoStatus;
import goldsprite.myUdpNetty.starter.Client;
import goldsprite.myUdpNetty.starter.Server;

public class LanMultiplayerService {
    public enum Mode {
        NONE,
        HOST,
        CLIENT
    }

    private volatile Mode mode = Mode.NONE;
    private volatile String localName = "";
    private volatile int localGuid = -1;
    private volatile boolean connected = false;

    private volatile Server server;
    private volatile Client client;

    private final ConcurrentLinkedQueue<LanNetworkEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Integer, LanRoomPlayer> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, LanPlayerStateSnapshot> playerStates = new ConcurrentHashMap<>();
    private volatile long lastSyncMillis = 0L;
    private volatile long syncIntervalMs = 16L; // 默认 60Hz (1000ms / 16ms ≈ 62.5Hz)
    // 敌人状态广播节流（房主用）
    private volatile long lastEnemyBroadcastMillis = 0L;
    private volatile long enemyBroadcastIntervalMs = 16L; // 敌人状态广播间隔（20Hz，不需要像玩家同步那么频繁）

    // Phase 1: 共享地图种子
    private volatile long pendingMapSeed = 0L;

    // Phase 2: 攻击请求队列（服务器收到客户端攻击请求后放入，房主游戏循环处理）
    private final ConcurrentLinkedQueue<LanAttackRequestPacket> pendingAttackRequests = new ConcurrentLinkedQueue<>();
    // Phase 2: 伤害结果队列（客户端收到广播后放入，游戏循环处理弹出飘字）
    private final ConcurrentLinkedQueue<LanDamageResultBroadcastPacket> pendingDamageResults = new ConcurrentLinkedQueue<>();
    // Phase 2: 玩家受伤队列（客户端收到广播后放入，游戏循环处理扣血）
    private final ConcurrentLinkedQueue<LanPlayerHurtBroadcastPacket> pendingPlayerHurts = new ConcurrentLinkedQueue<>();
    // Phase 2: 最新敌人状态缓存（客户端接收房主广播的敌人状态）
    private volatile List<EnemyStateSnapshot> latestEnemyStates = new ArrayList<>();

    private static volatile boolean protocolRegistered = false;
    private static final String LAN_TAG = "LAN";

    public synchronized void startHost(String playerName, int serverPort) {
        stop();
        localName = playerName;
        ensureLanProtocolRegistered();

        String localIp = resolveLocalIp();
        server = new Server();
        server.startAsync(new InetSocketAddress("0.0.0.0", serverPort), new InetSocketAddress(localIp, serverPort), false);
        
        if (!registerServerSubscribers()) {
            stop();
            return;
        }

        startClientInternal(playerName, localIp, serverPort);
        mode = Mode.HOST;

        eventQueue.offer(LanNetworkEvent.info("房主已启动: " + localIp + ":" + serverPort));
    }

    public synchronized void join(String playerName, String hostIp, int hostPort) {
        stop();
        localName = playerName;
        ensureLanProtocolRegistered();
        startClientInternal(playerName, hostIp, hostPort);
        mode = Mode.CLIENT;
        eventQueue.offer(LanNetworkEvent.info("正在加入房间: " + hostIp + ":" + hostPort));
    }

    private void startClientInternal(String playerName, String hostIp, int hostPort) {
        int localPort = randomUdpPort();
        client = new Client();
        client.start(new InetSocketAddress("0.0.0.0", localPort), new InetSocketAddress(hostIp, hostPort), false);

        registerPacketSubscribers(client);

        LoginRequestPacket login = new LoginRequestPacket(-1, playerName, "lan");
        client.sendPacket(login, LoginResponsePacket.class, rep -> {
            if (IStatus.isSuccessStatus(rep)) {
                localGuid = rep.getOwnerGuid();
                client.setOwnerGuid(localGuid);
                connected = true;
                eventQueue.offer(LanNetworkEvent.loginSuccess("登录成功: guid=" + localGuid));
                requestRoomPlayers();
            } else {
                connected = false;
                eventQueue.offer(LanNetworkEvent.loginFailed("登录失败: " + IStatus.getStatusMsg(rep)));
            }
        });
    }

    private void registerPacketSubscribers(Client c) {
        PacketsHandler handler = c.getPacketsHandler();
        if (handler == null) return;

        handler.addSubscriber(LanPlayerSyncBroadcastPacket.class, packet -> {
            LanPlayerStateSnapshot state = packet.getState();
            if (state == null) {
                DLog.logT(LAN_TAG, "收到SyncBroadcast但state=null");
                return;
            }
            if (state.getPlayerGuid() == localGuid) return;
            DLog.infoT(LAN_TAG, "收到同步: guid=%d pos=(%.1f,%.1f) vis=(%.1f,%.1f) hp=%.0f lv=%d",
                state.getPlayerGuid(), state.getX(), state.getY(),
                state.getVx(), state.getVy(), state.getHp(), state.getLevel());
            players.put(state.getPlayerGuid(), toLanRoomPlayer(state));
        });

        handler.addSubscriber(LanRoomPlayersResponsePacket.class, packet -> {
            if (packet.getPlayers() == null) return;
            for (LanPlayerStateSnapshot state : packet.getPlayers()) {
                if (state == null || state.getPlayerGuid() == localGuid) continue;
                players.put(state.getPlayerGuid(), toLanRoomPlayer(state));
            }
            eventQueue.offer(LanNetworkEvent.info("房间成员刷新: " + packet.getPlayers().size()));
        });

        handler.addSubscriber(BroadcastResponsePacket.class, packet -> {
            String msg = packet.getMessage();
            eventQueue.offer(LanNetworkEvent.chat(msg));
        });

        // 专用包：服务器广播"开始游戏"信号（携带地图种子）
        handler.addSubscriber(LanGameStartBroadcastPacket.class, packet -> {
            pendingMapSeed = packet.getMapSeed();
            eventQueue.offer(LanNetworkEvent.gameStartWithSeed("房主已开始游戏！", packet.getMapSeed()));
        });

        // Phase 2: 敌人状态广播（客户端接收）
        handler.addSubscriber(LanEnemySyncBroadcastPacket.class, packet -> {
            if (mode == Mode.HOST) return; // 房主自己是权威端，忽略
            if (packet.getEnemies() != null) {
                latestEnemyStates = packet.getEnemies();
            }
        });

        // Phase 2: 伤害结果广播
        handler.addSubscriber(LanDamageResultBroadcastPacket.class, packet -> {
            pendingDamageResults.offer(packet);
        });

        // Phase 2: 玩家受伤广播
        handler.addSubscriber(LanPlayerHurtBroadcastPacket.class, packet -> {
            pendingPlayerHurts.offer(packet);
        });

        // Phase 5: 换层广播
        handler.addSubscriber(LanFloorChangeBroadcastPacket.class, packet -> {
            if (mode == Mode.HOST) return; // 房主自己处理换层
            eventQueue.offer(LanNetworkEvent.floorChange(
                "进入第" + packet.getFloor() + "层！",
                packet.getNewSeed(), packet.getFloor()));
        });
    }

    private boolean registerServerSubscribers() {
        if (server == null) return false;
        PacketsHandler handler = null;
        for (int i = 0; i < 30; i++) { // 最多等待 3 秒
            handler = server.getPacketsHandler();
            if (handler != null) break;
            sleepSilently(100);
        }
        if (handler == null) {
            eventQueue.offer(LanNetworkEvent.error("房主处理器初始化超时"));
            return false;
        }

        handler.addSubscriber(LanPlayerSyncRequestPacket.class, this::onPlayerSyncRequest);
        handler.addSubscriber(LanRoomPlayersRequestPacket.class, this::onRoomPlayersRequest);
        handler.addSubscriber(LanGameStartRequestPacket.class, this::onGameStartRequest);
        // Phase 2: 服务器接收客户端攻击请求，放入队列交由房主游戏逻辑处理
        handler.addSubscriber(LanAttackRequestPacket.class, this::onAttackRequest);
        return true;
    }

    /** 服务器收到房主的"开始游戏"请求，向所有客户端广播（含地图种子） */
    private void onGameStartRequest(LanGameStartRequestPacket packet) {
        if (server == null) return;
        long seed = packet.getMapSeed();
        try {
            server.clients.forEach((targetGuid, ignored) -> {
                LanGameStartBroadcastPacket rep = new LanGameStartBroadcastPacket(targetGuid, IStatus.RETURN_SUCCESS, seed);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "onGameStartRequest广播异常: %s", e.getMessage());
        }
    }

    /** 服务器收到客户端攻击请求，放入待处理队列（房主游戏线程消费） */
    private void onAttackRequest(LanAttackRequestPacket packet) {
        pendingAttackRequests.offer(packet);
    }

    private void onPlayerSyncRequest(LanPlayerSyncRequestPacket packet) {
        if (server == null) return;
        int ownerGuid = packet.getOwnerGuid();
        ClientInfoStatus info = server.clients.get(ownerGuid);
        if (info == null) {
            DLog.logT(LAN_TAG, "onPlayerSyncRequest: 找不到client info, guid=%d", ownerGuid);
            return;
        }

        LanPlayerStateSnapshot state = new LanPlayerStateSnapshot(
            ownerGuid,
            info.name,
            packet.getX(),
            packet.getY(),
            packet.getVx(),
            packet.getVy(),
            packet.getAction(),
            packet.getTimestamp(),
            packet.getHp(),
            packet.getMaxHp(),
            packet.getLevel(),
            packet.getAtk(),
            packet.getDef()
        );
        playerStates.put(ownerGuid, state);

        try {
            server.clients.forEach((targetGuid, ignored) -> {
                LanPlayerSyncBroadcastPacket rep = new LanPlayerSyncBroadcastPacket(targetGuid, IStatus.RETURN_SUCCESS, state);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "onPlayerSyncRequest广播异常: %s", e.getMessage());
        }
    }

    private void onRoomPlayersRequest(LanRoomPlayersRequestPacket packet) {
        if (server == null) return;
        int responseOwner = packet.getOwnerGuid();
        List<LanPlayerStateSnapshot> snapshots = new ArrayList<>();
        try {
            server.clients.forEach((guid, info) -> {
                LanPlayerStateSnapshot state = playerStates.get(guid);
                if (state == null) {
                    state = new LanPlayerStateSnapshot(guid, info.name, 0f, 0f, 0f, 0f, "idle", System.currentTimeMillis());
                    playerStates.put(guid, state);
                }
                state.setPlayerName(info.name);
                snapshots.add(state);
            });

            LanRoomPlayersResponsePacket rep = new LanRoomPlayersResponsePacket(responseOwner, IStatus.RETURN_SUCCESS, snapshots);
            server.sendPacket(rep);
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "onRoomPlayersRequest异常: %s", e.getMessage());
        }
    }

    public void sendLocalState(float x, float y, float vx, float vy, String action,
                               float hp, float maxHp, int level, float atk, float def) {
        if (!connected || client == null || localGuid < 0) {
            DLog.infoT(LAN_TAG, "sendLocalState跳过: connected=%s client=%s guid=%d",
                connected, client == null ? "null" : "ok", localGuid);
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastSyncMillis < syncIntervalMs) return;
        lastSyncMillis = now;

        DLog.infoT(LAN_TAG, "发送同步: pos=(%.1f,%.1f) vis=(%.1f,%.1f) hp=%.0f lv=%d",
            x, y, vx, vy, hp, level);

        LanPlayerSyncRequestPacket packet = new LanPlayerSyncRequestPacket(
            localGuid, x, y, vx, vy, action, now, hp, maxHp, level, atk, def);
        client.sendPacket(packet);
    }

    public void setSyncIntervalMs(long ms) {
        this.syncIntervalMs = ms;
    }

    public long getSyncIntervalMs() {
        return syncIntervalMs;
    }

    public void requestRoomPlayers() {
        if (!connected || client == null || localGuid < 0) return;
        requestRoomPlayersInternal();
    }

    private void requestRoomPlayersInternal() {
        LanRoomPlayersRequestPacket packet = new LanRoomPlayersRequestPacket(localGuid);
        client.sendPacket(packet);
    }

    /** 房主调用：通知所有客户端"开始游戏"（通过专用包类型发送，携带地图种子） */
    public void broadcastGameStart(long mapSeed) {
        if (!connected || client == null || localGuid < 0) return;
        this.pendingMapSeed = mapSeed;
        client.sendPacket(new LanGameStartRequestPacket(localGuid, mapSeed));
    }

    public void sendChat(String msg) {
        if (!connected || client == null || localGuid < 0) return;
        String safeMsg = msg == null ? "" : msg.trim();
        if (safeMsg.isEmpty()) return;
        client.sendPacket(new BroadcastRequestPacket(localGuid, safeMsg));
    }

    public synchronized void stop() {
        connected = false;
        mode = Mode.NONE;
        localGuid = -1;
        players.clear();
        playerStates.clear();

        if (client != null) {
            client.stop();
            client = null;
        }
        if (server != null) {
            server.stop();
            server = null;
        }

        eventQueue.offer(LanNetworkEvent.info("联机会话已停止"));
    }

    // ============ Phase 2+: 房主权威广播方法（直接通过 server 广播） ============

    /** 房主广播所有敌人状态给全体客户端（带节流，避免每帧都广播） */
    public void broadcastEnemyStates(List<EnemyStateSnapshot> states) {
        if (server == null || !connected) return;
        long now = System.currentTimeMillis();
        if (now - lastEnemyBroadcastMillis < enemyBroadcastIntervalMs) return;
        lastEnemyBroadcastMillis = now;
        try {
            server.clients.forEach((targetGuid, ignored) -> {
                // 不发给自己（房主已有本地数据）
                if (targetGuid == localGuid) return;
                LanEnemySyncBroadcastPacket rep = new LanEnemySyncBroadcastPacket(
                    targetGuid, IStatus.RETURN_SUCCESS, states);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "broadcastEnemyStates异常: %s", e.getMessage());
        }
    }

    /** 房主广播伤害判定结果给全体客户端 */
    public void broadcastDamageResult(int enemyId, float damage, float remainHp,
                                       boolean killed, int attackerGuid, int xpReward) {
        if (server == null || !connected) return;
        try {
            server.clients.forEach((targetGuid, ignored) -> {
                LanDamageResultBroadcastPacket rep = new LanDamageResultBroadcastPacket(
                    targetGuid, IStatus.RETURN_SUCCESS,
                    enemyId, damage, remainHp, killed, attackerGuid, xpReward);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "broadcastDamageResult异常: %s", e.getMessage());
        }
    }

    /** 房主广播"玩家被敌人攻击"给全体客户端 */
    public void broadcastPlayerHurt(int targetGuid, float damage, float remainHp, int attackerEnemyId) {
        if (server == null || !connected) return;
        try {
            server.clients.forEach((tGuid, ignored) -> {
                LanPlayerHurtBroadcastPacket rep = new LanPlayerHurtBroadcastPacket(
                    tGuid, IStatus.RETURN_SUCCESS,
                    targetGuid, damage, remainHp, attackerEnemyId);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "broadcastPlayerHurt异常: %s", e.getMessage());
        }
    }

    /** 房主广播换层信号给全体客户端 */
    public void broadcastFloorChange(long newSeed, int floor) {
        if (server == null || !connected) return;
        try {
            server.clients.forEach((targetGuid, ignored) -> {
                LanFloorChangeBroadcastPacket rep = new LanFloorChangeBroadcastPacket(
                    targetGuid, IStatus.RETURN_SUCCESS, newSeed, floor);
                server.sendPacket(rep);
            });
        } catch (Exception e) {
            DLog.logT(LAN_TAG, "broadcastFloorChange异常: %s", e.getMessage());
        }
    }

    /** 客户端发送攻击请求给服务器（由房主处理） */
    public void sendAttackRequest(String attackType, float x, float y, int dx, int dy, float atk) {
        if (!connected || client == null || localGuid < 0) return;
        LanAttackRequestPacket packet = new LanAttackRequestPacket(localGuid, attackType, x, y, dx, dy, atk);
        client.sendPacket(packet);
    }

    // ============ Phase 2+: 队列读取方法（游戏线程消费） ============

    /** 房主读取待处理的攻击请求 */
    public List<LanAttackRequestPacket> drainAttackRequests() {
        List<LanAttackRequestPacket> list = new ArrayList<>();
        LanAttackRequestPacket req;
        while ((req = pendingAttackRequests.poll()) != null) list.add(req);
        return list;
    }

    /** 客户端读取待处理的伤害结果 */
    public List<LanDamageResultBroadcastPacket> drainDamageResults() {
        List<LanDamageResultBroadcastPacket> list = new ArrayList<>();
        LanDamageResultBroadcastPacket pkt;
        while ((pkt = pendingDamageResults.poll()) != null) list.add(pkt);
        return list;
    }

    /** 客户端读取待处理的玩家受伤通知 */
    public List<LanPlayerHurtBroadcastPacket> drainPlayerHurts() {
        List<LanPlayerHurtBroadcastPacket> list = new ArrayList<>();
        LanPlayerHurtBroadcastPacket pkt;
        while ((pkt = pendingPlayerHurts.poll()) != null) list.add(pkt);
        return list;
    }

    /** 客户端获取最新的敌人状态快照列表 */
    public List<EnemyStateSnapshot> getLatestEnemyStates() {
        return latestEnemyStates;
    }

    /** 获取待消费的地图种子（GAME_START 时设置） */
    public long getPendingMapSeed() {
        return pendingMapSeed;
    }

    public List<LanNetworkEvent> drainEvents() {
        List<LanNetworkEvent> list = new ArrayList<>();
        LanNetworkEvent event;
        while ((event = eventQueue.poll()) != null) {
            list.add(event);
        }
        return list;
    }

    public List<LanRoomPlayer> getRemotePlayers() {
        List<LanRoomPlayer> list = new ArrayList<>(players.values());
        list.sort(Comparator.comparingInt(LanRoomPlayer::getGuid));
        return list;
    }

    public int getRemotePlayerCount() {
        return players.size();
    }

    public boolean isConnected() {
        return connected;
    }

    public Mode getMode() {
        return mode;
    }

    public String getLocalName() {
        return localName;
    }

    public int getLocalGuid() {
        return localGuid;
    }

    private LanRoomPlayer toLanRoomPlayer(LanPlayerStateSnapshot state) {
        return new LanRoomPlayer(
            state.getPlayerGuid(),
            state.getPlayerName(),
            state.getX(),
            state.getY(),
            state.getVx(),
            state.getVy(),
            state.getAction(),
            state.getTimestamp(),
            state.getHp(),
            state.getMaxHp(),
            state.getLevel(),
            state.getAtk(),
            state.getDef()
        );
    }

    private static synchronized void ensureLanProtocolRegistered() {
        if (protocolRegistered) return;
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.PLAYER_SYNC_REQUEST, LanPlayerSyncRequestPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.PLAYER_SYNC_BROADCAST, LanPlayerSyncBroadcastPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.ROOM_PLAYERS_REQUEST, LanRoomPlayersRequestPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.ROOM_PLAYERS_RESPONSE, LanRoomPlayersResponsePacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.GAME_START_REQUEST, LanGameStartRequestPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.GAME_START_BROADCAST, LanGameStartBroadcastPacket.class);
        // Phase 2+: 敌人同步 / 合作战斗 / 换层
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.ENEMY_SYNC_BROADCAST, LanEnemySyncBroadcastPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.ATTACK_REQUEST, LanAttackRequestPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.DAMAGE_RESULT_BROADCAST, LanDamageResultBroadcastPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.PLAYER_HURT_BROADCAST, LanPlayerHurtBroadcastPacket.class);
        PacketCodeC.INSTANCE.registerPacketType(LanCommands.FLOOR_CHANGE_BROADCAST, LanFloorChangeBroadcastPacket.class);
        protocolRegistered = true;
    }

    private static String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private static int randomUdpPort() {
        return ThreadLocalRandom.current().nextInt(30000, 45000);
    }

    private static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
