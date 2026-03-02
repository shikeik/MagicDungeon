package com.goldsprite.gdengine.netcode.supabase;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;

/**
 * 获取公网 IP 工具类
 */
public class PublicIPResolver {
    
    private static final String API_URL = "https://checkip.amazonaws.com";
    
    public interface ResolveCallback {
        void onSuccess(String ip);
        void onError(Throwable t);
    }
    
    /**
     * 异步获取当前机器的公网 IPv4 地址
     */
    public static void resolvePublicIP(final ResolveCallback callback) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();
        Net.HttpRequest httpRequest = requestBuilder.newRequest()
            .method(Net.HttpMethods.GET)
            .url(API_URL)
            .timeout(5000)
            .build();
            
        Gdx.net.sendHttpRequest(httpRequest, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                int statusCode = httpResponse.getStatus().getStatusCode();
                if (statusCode == 200) {
                    String ip = httpResponse.getResultAsString().trim();
                    if (callback != null) {
                        callback.onSuccess(ip);
                    }
                } else {
                    if (callback != null) {
                        callback.onError(new RuntimeException("获取公网IP失败，HTTP状态码: " + statusCode));
                    }
                }
            }

            @Override
            public void failed(Throwable t) {
                if (callback != null) {
                    callback.onError(t);
                }
            }
            
            @Override
            public void cancelled() {
                if (callback != null) {
                    callback.onError(new RuntimeException("获取公网IP请求被取消"));
                }
            }
        });
    }
}