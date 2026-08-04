package com.feng.freader.http;

import android.content.Context;

import com.feng.freader.app.App;

import java.io.File;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;

public class NetworkClientFactory {
    private static volatile OkHttpClient client;

    private NetworkClientFactory() {
    }

    public static OkHttpClient shared() {
        if (client == null) {
            synchronized (NetworkClientFactory.class) {
                if (client == null) {
                    Context context = App.getContext();
                    File cacheDir = new File(context.getCacheDir(), "http_cache");
                    Cache cache = new Cache(cacheDir, 32L * 1024L * 1024L);
                    client = new OkHttpClient.Builder()
                            .cache(cache)
                            .connectTimeout(4, TimeUnit.SECONDS)
                            .readTimeout(8, TimeUnit.SECONDS)
                            .writeTimeout(8, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return client;
    }
}
