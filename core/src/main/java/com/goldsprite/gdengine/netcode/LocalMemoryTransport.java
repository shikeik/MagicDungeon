package com.goldsprite.gdengine.netcode;

/**
 * 为了 TDD 而生的假冒网络环境 (MockTransport)。
 * 它在内存中直接持有“另一端”的 Transport 引用，用来模拟本地进程中的网络即时发送。
 */
public class LocalMemoryTransport implements Transport {
    
    // 网络对方端点的引用
    private LocalMemoryTransport connectedPeer;
    
    // 自认身份
    private boolean isServerIdentity;
    
    // 用于对接上级的网络管理器
    private NetworkManager manager;
    
    // 调试统计用
    public int bytesSent = 0;
    public int messagesReceived = 0;

    public void setManager(NetworkManager manager) {
        this.manager = manager;
    }

    public void connectToPeer(LocalMemoryTransport peer) {
        this.connectedPeer = peer;
        peer.connectedPeer = this;
    }

    public LocalMemoryTransport(boolean isServerIdentity) {
        this.isServerIdentity = isServerIdentity;
    }

    @Override
    public void startServer(int port) {
        // Mock 环境，不需要真实端口
    }

    @Override
    public void connect(String ip, int port) {
        // Mock 环境，通过 connectToPeer 手动接线
    }

    @Override
    public void disconnect() {
        if (connectedPeer != null) {
            connectedPeer.connectedPeer = null;
            this.connectedPeer = null;
        }
    }

    @Override
    public void sendToClient(int clientId, byte[] payload) {
        if (isServerIdentity && connectedPeer != null) {
            bytesSent += payload.length;
            // 真实物理网络中，这会扔进底层并在另一端触发解析回调。
            // 这里我们直接用代码强行塞给对面的模拟接收总线。
            connectedPeer.receiveData(payload);
        }
    }

    @Override
    public void sendToServer(byte[] payload) {
        if (!isServerIdentity && connectedPeer != null) {
            bytesSent += payload.length;
            connectedPeer.receiveData(payload);
        }
    }

    @Override
    public void broadcast(byte[] payload) {
        if (isServerIdentity && connectedPeer != null) {
            bytesSent += payload.length;
            connectedPeer.receiveData(payload);
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

    /**
     * 模拟收到网络封包时的底层回调
     */
    public void receiveData(byte[] payload) {
        messagesReceived++;
        if (manager != null) {
            manager.onReceiveData(payload);
        }
    }
}
