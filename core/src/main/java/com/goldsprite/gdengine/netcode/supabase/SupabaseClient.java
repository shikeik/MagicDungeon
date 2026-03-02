package com.goldsprite.gdengine.netcode.supabase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;

import java.util.ArrayList;
import java.util.List;

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
                        // Post returns an array with the newly created row
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
}
