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
 * 鍩轰簬 Java 鍘熺敓 DatagramSocket (UDP) 鐨勭湡瀹炵綉缁滀紶杈撳眰瀹炵幇銆?
 * <p>
 * 涓?LocalMemoryTransport锛堣繘绋嬪唴鍐呭瓨鐩翠紶锛変笉鍚岋紝鏈被閫氳繃鐪熷疄鐨勬搷浣滅郴缁?Socket 鍙戦€佸拰鎺ユ敹 UDP 鏁版嵁鎶ワ紝
 * 閫傜敤浜庤法杩涚▼ / 璺ㄧ綉缁滅殑鑱旀満鍦烘櫙銆?
 * <p>
 * 璁捐瑕佺偣锛?
 * <ul>
 *   <li>Server 绔粦瀹氱鍙ｅ苟鐩戝惉锛屾帴鏀舵潵鑷?Client 鐨勬暟鎹紝鑳藉骞挎挱缁欐墍鏈夊凡鐭?Client</li>
 *   <li>Client 绔悜 Server 鍦板潃鍙戦€佹暟鎹紝鍚屾椂鐩戝惉 Server 鐨勫洖鍖?/li>
 *   <li>浣跨敤瀹堟姢绾跨▼寮傛鎺ユ敹鏁版嵁锛岄€氳繃 TransportReceiveCallback 鍥炶皟鎺ㄩ€佺粰涓婂眰</li>
 *   <li>鎻℃墜鍗忚锛欳lient 杩炴帴鏃跺彂閫?[0xFF,0xFF,0xFF,0xFF] 鎻℃墜鍖咃紝Server 鎹璁板綍 Client 鍦板潃</li>
 * </ul>
 */
public class UdpSocketTransport implements Transport {

    // 韬唤鏍囪瘑
    private final boolean isServerIdentity;

    // UDP Socket
    private DatagramSocket socket;

    // 鏁版嵁鎺ユ敹鍥炶皟
    private TransportReceiveCallback receiveCallback;

    // 鎺ユ敹绾跨▼
    private Thread receiveThread;
    private final AtomicBoolean running = new AtomicBoolean(false);

    // Server 璁板綍鐨勬墍鏈夊凡杩炴帴 Client 鍦板潃
    private final List<InetSocketAddress> clientAddresses = new CopyOnWriteArrayList<>();

    // Client 璁板綍鐨?Server 鍦板潃
    private InetSocketAddress serverAddress;

    // 鏈€澶?UDP 鏁版嵁鎶ュぇ灏忥紙瀵逛簬娓告垙 Netcode锛屼竴鑸笉瓒呰繃 MTU锛岃繖閲屼繚瀹堣涓?4096锛?
    private static final int MAX_PACKET_SIZE = 4096;

    // 鎻℃墜榄旀硶鏍囪瘑
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
            System.out.println("[UdpTransport] Server 宸插惎鍔紝鐩戝惉绔彛: " + port);
        } catch (SocketException e) {
            throw new RuntimeException("[UdpTransport] Server 鍚姩澶辫触: " + e.getMessage(), e);
        }
    }

    @Override
    public void connect(String ip, int port) {
        if (isServerIdentity) return;
        try {
            socket = new DatagramSocket(); // 闅忔満缁戝畾鏈湴绔彛
            serverAddress = new InetSocketAddress(ip, port);
            running.set(true);
            startReceiveLoop();

            // 鍙戦€佹彙鎵嬪寘锛岃 Server 鐭ラ亾鎴戜滑鐨勫湴鍧€
            sendRaw(HANDSHAKE_MAGIC, serverAddress);
            System.out.println("[UdpTransport] Client 宸茶繛鎺ュ埌 " + ip + ":" + port);
        } catch (SocketException e) {
            throw new RuntimeException("[UdpTransport] Client 杩炴帴澶辫触: " + e.getMessage(), e);
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
        System.out.println("[UdpTransport] 宸叉柇寮€杩炴帴");
    }

    @Override
    public void sendToClient(int clientId, byte[] payload) {
        if (!isServerIdentity || clientAddresses.isEmpty()) return;
        // 鏍规嵁 clientId 鍙戦€侊紙鐩墠绠€鍖栵細濡傛灉鍙湁涓€涓?Client锛岀洿鎺ュ彂锛?
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

    // ==================== 鍐呴儴瀹炵幇 ====================

    /**
     * 鍚姩寮傛鎺ユ敹寰幆锛堝畧鎶ょ嚎绋嬶級
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

                    // 瑙ｆ瀽甯﹂暱搴﹀墠缂€鐨勫皝鍖?
                    processReceivedData(rawData, sender);

                } catch (IOException e) {
                    if (running.get()) {
                        System.err.println("[UdpTransport] 鎺ユ敹鏁版嵁寮傚父: " + e.getMessage());
                    }
                    // socket 鍏抽棴鏃朵細瑙﹀彂 IOException锛屽睘浜庢甯搁€€鍑?
                }
            }
        }, "UdpTransport-Recv-" + (isServerIdentity ? "Server" : "Client"));
        receiveThread.setDaemon(true);
        receiveThread.start();
    }

    /**
     * 澶勭悊鏀跺埌鐨勫師濮嬫暟鎹€?
     * 鍗忚鏍煎紡: [4瀛楄妭payload闀垮害][payload瀛楄妭]
     * 鐗规畩: 鎻℃墜鍖呮槸瑁哥殑 [0xFF,0xFF,0xFF,0xFF]锛堟棤闀垮害鍓嶇紑锛?
     */
    private void processReceivedData(byte[] rawData, InetSocketAddress sender) {
        // 妫€鏌ユ槸鍚︿负鎻℃墜鍖?
        if (isHandshake(rawData)) {
            if (isServerIdentity) {
                // Server 鏀跺埌鎻℃墜鍖咃紝璁板綍 Client 鍦板潃
                if (!clientAddresses.contains(sender)) {
                    clientAddresses.add(sender);
                    System.out.println("[UdpTransport] Server 鍙戠幇鏂板鎴风: " + sender);
                }
            }
            // 鎻℃墜鍖呬笉浼犻€掔粰涓婂眰
            return;
        }

        // 瑙ｆ瀽闀垮害鍓嶇紑灏佸寘: [4瀛楄妭length][payload]
        if (rawData.length < 4) return;

        ByteBuffer bb = ByteBuffer.wrap(rawData, 0, 4);
        int payloadLen = bb.getInt();

        if (payloadLen <= 0 || payloadLen + 4 > rawData.length) {
            System.err.println("[UdpTransport] 灏佸寘闀垮害寮傚父: declared=" + payloadLen + ", actual=" + (rawData.length - 4));
            return;
        }

        byte[] payload = new byte[payloadLen];
        System.arraycopy(rawData, 4, payload, 0, payloadLen);

        // 閫氳繃鍥炶皟鎺ㄩ€佺粰涓婂眰
        if (receiveCallback != null) {
            receiveCallback.onReceiveData(payload);
        }
    }

    /**
     * 鍙戦€佸甫闀垮害鍓嶇紑鐨勫皝鍖? [4瀛楄妭payload闀垮害][payload瀛楄妭]
     */
    private void sendWithLengthPrefix(byte[] payload, InetSocketAddress target) {
        byte[] packet = new byte[4 + payload.length];
        ByteBuffer bb = ByteBuffer.wrap(packet);
        bb.putInt(payload.length);
        bb.put(payload);
        sendRaw(packet, target);
    }

    /**
     * 搴曞眰鍙戦€佸師濮嬪瓧鑺?
     */
    private void sendRaw(byte[] data, InetSocketAddress target) {
        if (socket == null || socket.isClosed()) return;
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length, target.getAddress(), target.getPort());
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("[UdpTransport] 鍙戦€佸け璐? " + e.getMessage());
        }
    }

    /**
     * 鍒ゆ柇鏄惁涓烘彙鎵嬪寘 [0xFF,0xFF,0xFF,0xFF]
     */
    private boolean isHandshake(byte[] data) {
        if (data.length != HANDSHAKE_MAGIC.length) return false;
        for (int i = 0; i < HANDSHAKE_MAGIC.length; i++) {
            if (data[i] != HANDSHAKE_MAGIC[i]) return false;
        }
        return true;
    }
}

