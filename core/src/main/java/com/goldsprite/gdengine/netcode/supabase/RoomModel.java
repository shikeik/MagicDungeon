package com.goldsprite.gdengine.netcode.supabase;

public class RoomModel {
    private String id;
    private String room_name;
    private String host_ip;
    private int host_port;
    private int player_count;
    private int max_players;
    private String last_ping;
    private String created_at;

    public RoomModel() {
    }

    public RoomModel(String room_name, String host_ip, int host_port, int player_count, int max_players) {
        this.room_name = room_name;
        this.host_ip = host_ip;
        this.host_port = host_port;
        this.player_count = player_count;
        this.max_players = max_players;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRoom_name() {
        return room_name;
    }

    public void setRoom_name(String room_name) {
        this.room_name = room_name;
    }

    public String getHost_ip() {
        return host_ip;
    }

    public void setHost_ip(String host_ip) {
        this.host_ip = host_ip;
    }

    public int getHost_port() {
        return host_port;
    }

    public void setHost_port(int host_port) {
        this.host_port = host_port;
    }

    public int getPlayer_count() {
        return player_count;
    }

    public void setPlayer_count(int player_count) {
        this.player_count = player_count;
    }

    public int getMax_players() {
        return max_players;
    }

    public void setMax_players(int max_players) {
        this.max_players = max_players;
    }

    public String getLast_ping() {
        return last_ping;
    }

    public void setLast_ping(String last_ping) {
        this.last_ping = last_ping;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }
}
