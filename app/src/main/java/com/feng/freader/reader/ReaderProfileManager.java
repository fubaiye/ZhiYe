package com.feng.freader.reader;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ReaderProfileManager {
    private static final String PREF = "reader_profile";
    private static final String KEY_RECORDS = "records";
    private static final String KEY_TOTAL_MS_PREFIX = "total_ms_";
    private static final int MAX_RECORDS = 500;

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public ReaderProfileManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public void addRecord(ReaderRecord record) {
        List<ReaderRecord> records = getRecords();
        records.add(0, record);
        while (records.size() > MAX_RECORDS) {
            records.remove(records.size() - 1);
        }
        preferences.edit().putString(KEY_RECORDS, gson.toJson(records)).apply();
    }

    public List<ReaderRecord> getRecords() {
        try {
            Type type = new TypeToken<List<ReaderRecord>>() {
            }.getType();
            List<ReaderRecord> records = gson.fromJson(preferences.getString(KEY_RECORDS, "[]"), type);
            return records == null ? new ArrayList<ReaderRecord>() : records;
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }

    public void addReadingTime(String bookUrl, long durationMs) {
        if (durationMs <= 0) {
            return;
        }
        String key = KEY_TOTAL_MS_PREFIX + bookUrl;
        long total = preferences.getLong(key, 0L) + durationMs;
        preferences.edit().putLong(key, total).apply();
    }

    public long getReadingTime(String bookUrl) {
        return preferences.getLong(KEY_TOTAL_MS_PREFIX + bookUrl, 0L);
    }

    public String buildSummary(String bookUrl) {
        int bookmark = 0;
        int note = 0;
        int highlight = 0;
        for (ReaderRecord record : getRecords()) {
            if (!record.getBookUrl().equals(bookUrl)) {
                continue;
            }
            if (ReaderRecord.BOOKMARK.equals(record.getType())) {
                bookmark++;
            } else if (ReaderRecord.NOTE.equals(record.getType())) {
                note++;
            } else if (ReaderRecord.HIGHLIGHT.equals(record.getType())) {
                highlight++;
            }
        }
        long minutes = getReadingTime(bookUrl) / 60000L;
        return "阅读统计\n"
                + "累计阅读：" + minutes + " 分钟\n"
                + "书签：" + bookmark + "\n"
                + "笔记：" + note + "\n"
                + "高亮：" + highlight;
    }
}
