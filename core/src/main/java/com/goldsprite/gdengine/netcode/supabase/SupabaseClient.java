package com.goldsprite.gdengine.netcode.supabase;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

public class SupabaseClient {

    private final String SUPABASE_URL;
    private final String SUPABASE_ANON_KEY;
    private final Json json;

    public interface RequestCallback<T> {
        void onSuccess(T result);

        void onError(Throwable t);
    }

    public SupabaseClient(String url, String anonKey) {
        this.SUPABASE_URL = url;
        this.SUPABASE_ANON_KEY = anonKey;
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        // Ignore unknown fields to prevent crashes if Supabase adds fields we don't care about
        this.json.setIgnoreUnknownFields(true);
    }

    private void addHeaders(Net.HttpRequest httpRequest) {
        httpRequest.setHeader("apikey", SUPABASE_ANON_KEY);
        httpRequest.setHeader("Authorization", "Bearer " + SUPABASE_ANON_KEY);
        httpRequest.setHeader("Content-Type", "application/json");
        httpRequest.setHeader("Prefer", "return=representation");
    }

    /**
     * Create a new room
     */
    public void createRoom(RoomModel room, final RequestCallback<RoomModel> callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.POST)
                .url(SUPABASE_URL + "/rest/v1/rooms")
                .content(json.toJson(room))
                .timeout(5000)
                .build();

        addHeaders(httpRequest);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    try {
                        String resultString = httpResponse.getResultAsString();
                        @SuppressWarnings("unchecked")
                        ArrayList<RoomModel> list = json.fromJson(ArrayList.class, RoomModel.class, resultString);
                        if (list != null && !list.isEmpty()) {
                            if (callback != null) callback.onSuccess(list.get(0));
                        } else {
                            if (callback != null) callback.onError(new RuntimeException("Empty response array"));
                        }
                    } catch (Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("HTTP Status: " + statusCode + ", " + httpResponse.getResultAsString()));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                if (callback != null) callback.onError(t);
            }

            @Override
            public void cancelled() {
                if (callback != null) callback.onError(new RuntimeException("Request cancelled"));
            }
        });
    }

    /**
     * Fetch alive rooms
     */
    public void fetchRooms(final RequestCallback<List<RoomModel>> callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        // Query param: select=*, and filter last_ping >= (now - 30 seconds)
        // Note: The exact filter syntax depends on how your PostgreSQL defines the timezone, 
        // a simple way without 'now()' math on DB side is sorting and filtering all locally, or using simple age filters
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.GET)
                .url(SUPABASE_URL + "/rest/v1/rooms?select=*&order=created_at.desc")
                .timeout(5000)
                .build();

        addHeaders(httpRequest);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    try {
                        String resultString = httpResponse.getResultAsString();
                        @SuppressWarnings("unchecked")
                        ArrayList<RoomModel> list = json.fromJson(ArrayList.class, RoomModel.class, resultString);
                        if (callback != null) {
                            // You could do local filtering of dead ping limits here
                            callback.onSuccess(list == null ? new ArrayList<RoomModel>() : list);
                        }
                    } catch (Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("HTTP Status: " + statusCode + ", " + httpResponse.getResultAsString()));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                if (callback != null) callback.onError(t);
            }

            @Override
            public void cancelled() {
                if (callback != null) callback.onError(new RuntimeException("Request cancelled"));
            }
        });
    }

    /**
     * Delete a room
     */
    public void deleteRoom(String roomId, final RequestCallback<Void> callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.DELETE)
                .url(SUPABASE_URL + "/rest/v1/rooms?id=eq." + roomId)
                .timeout(5000)
                .build();

        addHeaders(httpRequest);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("HTTP Status: " + statusCode + ", " + httpResponse.getResultAsString()));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                if (callback != null) callback.onError(t);
            }

            @Override
            public void cancelled() {
                if (callback != null) callback.onError(new RuntimeException("Request cancelled"));
            }
        });
    }

    /**
     * Update heartbeat
     */
    public void updateHeartbeat(String roomId, final RequestCallback<Void> callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        
        // Supabase REST PATCH request
        // {"last_ping": "now()"} might need to be resolved correctly depending on Postgres extensions, 
        // often we send the ISO string of current time from Java
        String timeIso = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .format(new java.util.Date());
                
        String payload = "{\"last_ping\":\"" + timeIso + "\"}";
        
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
                .method(Net.HttpMethods.PATCH)
                .url(SUPABASE_URL + "/rest/v1/rooms?id=eq." + roomId)
                .content(payload)
                .timeout(5000)
                .build();

        addHeaders(httpRequest);

        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    if (callback != null) callback.onSuccess(null);
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("HTTP Status: " + statusCode + ", " + httpResponse.getResultAsString()));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                if (callback != null) callback.onError(t);
            }

            @Override
            public void cancelled() {
                if (callback != null) callback.onError(new RuntimeException("Request cancelled"));
            }
        });
    }
}
