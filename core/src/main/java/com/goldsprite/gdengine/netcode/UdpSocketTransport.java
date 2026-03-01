package com.goldsprite.gdengine.netcode;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Java 原生 DatagramSocket (UDP) 的真实网络传输层实现。
 * <p>
 * 与 LocalMemoryTransport（进程内内存直传）不同，本类通过真实的操作系统 Socket 发送和接收 UDP 数据报，
 * 适用于跨进程 / 跨网络的联机场景。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>Server 端绑定端口并监听，接收来自 Client 的数据，能够广播给所有已知 Client</li>
 *   <li>Client 端向 Server 地址发送数据，同时监听 Server 的回包</li>
 *   <li>使用守护线程异步接收数据，通过 TransportReceiveCallback 回调推送给上层</li>
 *   <li>握手协议：Client 连接时发送 [0xFF,0xFF,0xFF,0xFF] 握手包，Server 据此记录 Client 地址</li>
 * </ul>
 */
public class UdpSocketTransport implements Transport {

    // 身份标识
    private final boolean isServerIdentity;

    // UDP Socket
    private DatagramSocket socket;

    // 数据接收回调
    private TransportReceiveCallback receiveCallback;

    // 接收线程
    private Thread receiveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Server 记录的所有已连接 Client 地址
    private final List<InetSocketAddress> clientAddresses = new CopyOnWriteArrayList<>();

    // Client 记录的 Server 地址
    private InetSocketAddress serverAddress;

    // 最大 UDP 数据报大小（对于游戏 Netcode，一般不超过 MTU，这里保守设为 4096）
    private static final int MAX_PACKET_SIZE = 4096;

    // 握手魔法标识
    private static final byte[] HANDSHAKE_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public UdpSocketTransport(boolean isServerIdentity) {
        this.isServerIdentity = isServerIdentity;
    }

    @Override
    public void setReceiveCallback(TransportReceiveCallback callback) {
        this.receiveCallback = callback;
    }

    @Override
    public void startServer(int port) {
        if (!isServerIdentity) return;
        try {
            socket = new DatagramSocket(port);
            running.set(true);
            startReceiveLoop();
            System.out.println("[UdpTransport] Server 已启动，监听端口: " + port);
        } catch (SocketException e) {
            throw new RuntimeException("[UdpTransport] Server 启动失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void connect(String ip, int port) {
        if (isServerIdentity) return;
        try {
            socket = new DatagramSocket(); // 随机绑定本地端口
            serverAddress = new InetSocketAddress(ip, port);
            running.set(true);
            startReceiveLoop();

            // 发送握手包，让 Server 知道我们的地址
            sendRaw(HANDSHAKE_MAGIC, serverAddress);
            System.out.println("[UdpTransport] Client 已连接到 " + ip + ":" + port);
        } catch (SocketException e) {
            throw new RuntimeException("[UdpTransport] Client 连接失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void disconnect() {
        running.set(false);
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        clientAddresses.clear();
        System.out.println("[UdpTransport] 已断开连接");
    }

    @Override
    public void sendToClient(int clientId, byte[] payload) {
        if (!isServerIdentity || clientAddresses.isEmpty()) return;
        // 根据 clientId 发送（目前简化：如果只有一个 Client，直接发）
        if (clientId >= 0 && clientId < clientAddresses.size()) {
            sendWithLengthPrefix(payload, clientAddresses.get(clientId));
        }
    }

    @Override
    public void sendToServer(byte[] payload) {
        if (isServerIdentity || serverAddress == null) return;
        sendWithLengthPrefix(payload, serverAddress);
    }

    @Override
    public void broadcast(byte[] payload) {
        if (!isServerIdentity) return;
        for (InetSocketAddress addr : clientAddresses) {
            sendWithLengthPrefix(payload, addr);
        }
    }

    @Override
    public boolean isServer() {
        return isServerIdentity;
    }

    @Override
    public boolean isClient() {
        return !isServerIdentity;
    }

    // ==================== 内部实现 ====================

    /**
     * 启动异步接收循环（守护线程）
     */
    private void startReceiveLoop() {
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[MAX_PACKET_SIZE];
            while (running.get() && !socket.isClosed()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    InetSocketAddress sender = new InetSocketAddress(packet.getAddress(), packet.getPort());
                    byte[] rawData = new byte[packet.getLength()];
                    System.arraycopy(buffer, 0, rawData, 0, packet.getLength());

                    // 解析带长度前缀的封包
                    processReceivedData(rawData, sender);

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[UdpTransport] 接收数据异常: " + e.getMessage());
                    }
                    // socket 关闭时会触发 IOException，属于正常退出
                }
            }
        }, "UdpTransport-Recv-" + (isServerIdentity ? "Server" : "Client"));
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /**
     * 处理收到的原始数据。
     * 协议格式: [4字节payload长度][payload字节]
     * 特殊: 握手包是裸的 [0xFF,0xFF,0xFF,0xFF]（无长度前缀）
     */
    private void processReceivedData(byte[] rawData, InetSocketAddress sender) {
        // 检查是否为握手包
        if (isHandshake(rawData)) {
            if (isServerIdentity) {
                // Server 收到握手包，记录 Client 地址
                if (!clientAddresses.contains(sender)) {
                    clientAddresses.add(sender);
                    System.out.println("[UdpTransport] Server 发现新客户端: " + sender);
                }
            }
            // 握手包不传递给上层
            return;
        }

        // 解析长度前缀封包: [4字节length][payload]
        if (rawData.length < 4) return;

        ByteBuffer bb = ByteBuffer.wrap(rawData, 0, 4);
        int payloadLen = bb.getInt();

        if (payloadLen <= 0 || payloadLen + 4 > rawData.length) {
            System.err.println("[UdpTransport] 封包长度异常: declared=" + payloadLen + ", actual=" + (rawData.length - 4));
            return;
        }

        byte[] payload = new byte[payloadLen];
        System.arraycopy(rawData, 4, payload, 0, payloadLen);

        // 通过回调推送给上层
        if (receiveCallback != null) {
            receiveCallback.onReceiveData(payload);
        }
    }

    /**
     * 发送带长度前缀的封包: [4字节payload长度][payload字节]
     */
    private void sendWithLengthPrefix(byte[] payload, InetSocketAddress target) {
        byte[] packet = new byte[4 + payload.length];
        ByteBuffer bb = ByteBuffer.wrap(packet);
        bb.putInt(payload.length);
        bb.put(payload);
        sendRaw(packet, target);
    }

    /**
     * 底层发送原始字节
     */
    private void sendRaw(byte[] data, InetSocketAddress target) {
        if (socket == null || socket.isClosed()) return;
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, target.getAddress(), target.getPort());
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[UdpTransport] 发送失败: " + e.getMessage());
        }
    }

    /**
     * 判断是否为握手包 [0xFF,0xFF,0xFF,0xFF]
     */
    private boolean isHandshake(byte[] data) {
        if (data.length != HANDSHAKE_MAGIC.length) return false;
        for (int i = 0; i < HANDSHAKE_MAGIC.length; i++) {
            if (data[i] != HANDSHAKE_MAGIC[i]) return false;
        }
        return true;
    }
}
