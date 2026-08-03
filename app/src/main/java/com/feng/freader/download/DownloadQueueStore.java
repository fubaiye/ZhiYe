package com.feng.freader.download;

import android.content.Context;
import android.content.SharedPreferences;

import com.feng.freader.app.App;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DownloadQueueStore {
    private static final String PREF = "download_queue";
    private static final String KEY_QUEUE = "queue";
    private static DownloadQueueStore instance;
    private final SharedPreferences preferences;

    private DownloadQueueStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static DownloadQueueStore getInstance() {
        if (instance == null) {
            instance = new DownloadQueueStore(App.getContext());
        }
        return instance;
    }

    public synchronized void upsert(DownloadRequest request) {
        List<DownloadRequest> all = getAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(request.getId())) {
                all.set(i, request);
                save(all);
                return;
            }
        }
        all.add(request);
        save(all);
    }

    public synchronized List<DownloadRequest> getAll() {
        String json = preferences.getString(KEY_QUEUE, "[]");
        try {
            Type type = new TypeToken<List<DownloadRequest>>() {
            }.getType();
            List<DownloadRequest> result = new Gson().fromJson(json, type);
            return result == null ? new ArrayList<DownloadRequest>() : result;
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }

    public synchronized DownloadRequest find(String id) {
        for (DownloadRequest request : getAll()) {
            if (request.getId().equals(id)) {
                return request;
            }
        }
        return null;
    }

    private void save(List<DownloadRequest> all) {
        preferences.edit().putString(KEY_QUEUE, new Gson().toJson(all)).apply();
    }
}
