package com.goldsprite.magicdungeon2.network.lan;

public class LanNetworkEvent {
    public enum Type {
        INFO,
        ERROR,
        CHAT,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        GAME_START
    }

    private final Type type;
    private final String message;
    private final long timestamp;

    private LanNetworkEvent(Type type, String message, long timestamp) {
        this.type = type;
        this.message = message;
        this.timestamp = timestamp;
    }

    public static LanNetworkEvent info(String message) {
        return new LanNetworkEvent(Type.INFO, message, System.currentTimeMillis());
    }

    public static LanNetworkEvent error(String message) {
        return new LanNetworkEvent(Type.ERROR, message, System.currentTimeMillis());
    }

    public static LanNetworkEvent chat(String message) {
        return new LanNetworkEvent(Type.CHAT, message, System.currentTimeMillis());
    }

    public static LanNetworkEvent loginSuccess(String message) {
        return new LanNetworkEvent(Type.LOGIN_SUCCESS, message, System.currentTimeMillis());
    }

    public static LanNetworkEvent loginFailed(String message) {
        return new LanNetworkEvent(Type.LOGIN_FAILED, message, System.currentTimeMillis());
    }

    public static LanNetworkEvent gameStart(String message) {
        return new LanNetworkEvent(Type.GAME_START, message, System.currentTimeMillis());
    }

    public Type getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
