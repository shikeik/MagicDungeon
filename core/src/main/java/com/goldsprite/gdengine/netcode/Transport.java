package com.goldsprite.gdengine.netcode;

public interface Transport {
    void startServer(int port);
    void connect(String ip, int port);
    void disconnect();
    void sendToClient(int clientId, byte[] payload);
    void sendToServer(byte[] payload);
    void broadcast(byte[] payload);
    
    // 返回是否是服务器端
    boolean isServer();
    // 返回是否是客户端
    boolean isClient();
}
