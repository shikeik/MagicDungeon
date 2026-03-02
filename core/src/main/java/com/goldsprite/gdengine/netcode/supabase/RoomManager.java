package com.goldsprite.gdengine.netcode.supabase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Timer;

import java.util.List;

public class RoomManager {
    
    private final SupabaseClient client;
    private String currentRoomId;
    private Timer.Task heartbeatTask;
    private final int HEARTBEAT_INTERVAL_SECONDS = 15;
    
    public interface CreateRoomCallback {
        void onSuccess(String roomId);
        void onError(Throwable t);
    }
    
    public interface FetchRoomsCallback {
        void onSuccess(List<RoomModel> rooms);
        void onError(Throwable t);
    }

    public RoomManager() {
        this.client = new SupabaseClient(SupabaseConfig.URL, SupabaseConfig.PUBLISHABLE_KEY);
    }

    public RoomManager(String supabaseUrl, String publishableKey) {
        this.client = new SupabaseClient(supabaseUrl, publishableKey);
    }

    /**
     * 房主：创建一个房间并注册到大厅
     */
    public void createRoom(String roomName, int maxPlayers, int currentPort, final CreateRoomCallback callback) {
        // 先获取公网 IP
        PublicIPResolver.resolvePublicIP(new PublicIPResolver.ResolveCallback() {
            @Override
            public void onSuccess(String ip) {
                Gdx.app.log("RoomManager", "获取公网IP成功: " + ip + "，准备注册房间...");
                RoomModel room = new RoomModel(roomName, ip, currentPort, 1, maxPlayers);
                
                client.createRoom(room, new SupabaseClient.RequestCallback<RoomModel>() {
                    @Override
                    public void onSuccess(RoomModel result) {
                        currentRoomId = result.getId();
                        Gdx.app.log("RoomManager", "房间注册成功 ID: " + currentRoomId);
                        startHeartbeat();
                        if (callback != null) callback.onSuccess(currentRoomId);
                    }

                    @Override
                    public void onError(Throwable t) {
                        if (callback != null) callback.onError(t);
                    }
                });
            }

            @Override
            public void onError(Throwable t) {
                if (callback != null) callback.onError(new RuntimeException("无法获取公网IP, 建房失败", t));
            }
        });
    }

    /**
     * 客户端：拉取大厅房间列表
     */
    public void fetchRooms(final FetchRoomsCallback callback) {
        client.fetchRooms(new SupabaseClient.RequestCallback<List<RoomModel>>() {
            @Override
            public void onSuccess(List<RoomModel> result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Throwable t) {
                if (callback != null) callback.onError(t);
            }
        });
    }

    /**
     * 房主：关闭并销毁房间
     */
    public void destroyRoom() {
        stopHeartbeat();
        if (currentRoomId != null) {
            client.deleteRoom(currentRoomId, new SupabaseClient.RequestCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Gdx.app.log("RoomManager", "房间销毁成功: " + currentRoomId);
                    currentRoomId = null;
                }

                @Override
                public void onError(Throwable t) {
                    Gdx.app.error("RoomManager", "房间销毁失败", t);
                }
            });
        }
    }

    private void startHeartbeat() {
        if (heartbeatTask != null) return;
        
        heartbeatTask = Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                if (currentRoomId != null) {
                    client.updateHeartbeat(currentRoomId, new SupabaseClient.RequestCallback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Gdx.app.debug("RoomManager", "心跳更新成功");
                        }

                        @Override
                        public void onError(Throwable t) {
                            Gdx.app.error("RoomManager", "心跳更新失败", t);
                        }
                    });
                }
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS);
        Gdx.app.log("RoomManager", "心跳定时器已启动，间隔: " + HEARTBEAT_INTERVAL_SECONDS + " 秒");
    }

    private void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel();
            heartbeatTask = null;
            Gdx.app.log("RoomManager", "心跳定时器已停止");
        }
    }
}
