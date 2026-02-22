package com.goldsprite.magicdungeon2.network.lan;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;

import com.goldsprite.magicdungeon2.network.lan.packet.LanCommands;
import com.goldsprite.magicdungeon2.network.lan.packet.LanGameStartBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanGameStartRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerStateSnapshot;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerSyncBroadcastPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanPlayerSyncRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanRoomPlayersRequestPacket;
import com.goldsprite.magicdungeon2.network.lan.packet.LanRoomPlayersResponsePacket;

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
    private volatile long syncIntervalMs = 50L; // 默认 20Hz (1000ms / 50ms)

    private static volatile boolean protocolRegistered = false;

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
            if (state == null) return;
            if (state.getPlayerGuid() == localGuid) return;
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
            // 检测特殊命令前缀
            if (CMD_GAME_START.equals(msg)) {
                eventQueue.offer(LanNetworkEvent.gameStart("房主已开始游戏！"));
            } else {
                eventQueue.offer(LanNetworkEvent.chat(msg));
            }
        });

        // 保留自定义包订阅作为备用（如果框架支持的话也会触发）
        handler.addSubscriber(LanGameStartBroadcastPacket.class, packet -> {
            eventQueue.offer(LanNetworkEvent.gameStart("房主已开始游戏！"));
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
        return true;
    }

    /** 服务器收到房主的"开始游戏"请求，向所有客户端广播 */
    private void onGameStartRequest(LanGameStartRequestPacket packet) {
        if (server == null) return;
        server.clients.forEach((targetGuid, ignored) -> {
            LanGameStartBroadcastPacket rep = new LanGameStartBroadcastPacket(targetGuid, IStatus.RETURN_SUCCESS);
            server.sendPacket(rep);
        });
    }

    private void onPlayerSyncRequest(LanPlayerSyncRequestPacket packet) {
        if (server == null) return;
        int ownerGuid = packet.getOwnerGuid();
        ClientInfoStatus info = server.clients.get(ownerGuid);
        if (info == null) return;

        LanPlayerStateSnapshot state = new LanPlayerStateSnapshot(
            ownerGuid,
            info.name,
            packet.getX(),
            packet.getY(),
            packet.getVx(),
            packet.getVy(),
            packet.getAction(),
            packet.getTimestamp()
        );
        playerStates.put(ownerGuid, state);

        server.clients.forEach((targetGuid, ignored) -> {
            LanPlayerSyncBroadcastPacket rep = new LanPlayerSyncBroadcastPacket(targetGuid, IStatus.RETURN_SUCCESS, state);
            server.sendPacket(rep);
        });
    }

    private void onRoomPlayersRequest(LanRoomPlayersRequestPacket packet) {
        if (server == null) return;
        int responseOwner = packet.getOwnerGuid();
        List<LanPlayerStateSnapshot> snapshots = new ArrayList<>();
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
    }

    public void sendLocalState(float x, float y, float vx, float vy, String action) {
        if (!connected || client == null || localGuid < 0) return;
        long now = System.currentTimeMillis();
        if (now - lastSyncMillis < syncIntervalMs) return;
        lastSyncMillis = now;

        LanPlayerSyncRequestPacket packet = new LanPlayerSyncRequestPacket(localGuid, x, y, vx, vy, action, now);
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

    /** 房主调用：通知所有客户端"开始游戏"（通过广播聊天通道发送命令） */
    public void broadcastGameStart() {
        if (!connected || client == null || localGuid < 0) return;
        // 复用已验证可靠的 BroadcastRequest 通道，用特殊前缀标识命令
        client.sendPacket(new BroadcastRequestPacket(localGuid, CMD_GAME_START));
    }

    /** 游戏开始命令前缀 */
    public static final String CMD_GAME_START = "$CMD:GAME_START";

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
            state.getTimestamp()
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
