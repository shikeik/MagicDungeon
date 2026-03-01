package com.goldsprite.magicdungeon2.network.lan;

public class LanNetworkEvent {
    public enum Type {
        INFO,
        ERROR,
        CHAT,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        GAME_START,
        FLOOR_CHANGE   // 换层信号（携带新种子和层数）
    }

    private final Type type;
    private final String message;
    private final long timestamp;
    private long mapSeed;   // GAME_START / FLOOR_CHANGE 时携带的种子
    private int floor;      // FLOOR_CHANGE 时携带的层数

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

    /** 创建带有地图种子的开始游戏事件 */
    public static LanNetworkEvent gameStartWithSeed(String message, long mapSeed) {
        LanNetworkEvent e = new LanNetworkEvent(Type.GAME_START, message, System.currentTimeMillis());
        e.mapSeed = mapSeed;
        return e;
    }

    /** 创建换层事件 */
    public static LanNetworkEvent floorChange(String message, long newSeed, int floor) {
        LanNetworkEvent e = new LanNetworkEvent(Type.FLOOR_CHANGE, message, System.currentTimeMillis());
        e.mapSeed = newSeed;
        e.floor = floor;
        return e;
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

    public long getMapSeed() { return mapSeed; }
    public int getFloor() { return floor; }
}
