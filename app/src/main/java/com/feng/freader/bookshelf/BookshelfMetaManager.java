package com.feng.freader.bookshelf;

import android.content.Context;
import android.content.SharedPreferences;

import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookshelfMetaManager {
    public static final int SORT_DEFAULT = 0;
    public static final int SORT_RECENT = 1;
    public static final int SORT_FAVORITE = 2;
    public static final int SORT_PROGRESS = 3;

    private static final String PREF = "bookshelf_meta";
    private static final String KEY_META = "meta";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();

    public BookshelfMetaManager(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public BookshelfMeta get(String novelUrl) {
        BookshelfMeta meta = load().get(novelUrl);
        if (meta == null) {
            meta = new BookshelfMeta();
            meta.setNovelUrl(novelUrl);
        }
        return meta;
    }

    public void markRecent(String novelUrl) {
        BookshelfMeta meta = get(novelUrl);
        meta.setRecentAt(System.currentTimeMillis());
        saveMeta(meta);
    }

    public void togglePinned(String novelUrl) {
        BookshelfMeta meta = get(novelUrl);
        meta.setPinned(!meta.isPinned());
        saveMeta(meta);
    }

    public void toggleFavorite(String novelUrl) {
        BookshelfMeta meta = get(novelUrl);
        meta.setFavorite(!meta.isFavorite());
        saveMeta(meta);
    }

    public void setCategory(String novelUrl, String category) {
        BookshelfMeta meta = get(novelUrl);
        meta.setCategory(category == null ? "" : category.trim());
        saveMeta(meta);
    }

    public List<BookshelfNovelDbData> sort(List<BookshelfNovelDbData> data, final int mode) {
        List<BookshelfNovelDbData> result = new ArrayList<>(data);
        final Map<String, BookshelfMeta> metas = load();
        Collections.sort(result, new Comparator<BookshelfNovelDbData>() {
            @Override
            public int compare(BookshelfNovelDbData left, BookshelfNovelDbData right) {
                BookshelfMeta l = meta(metas, left.getNovelUrl());
                BookshelfMeta r = meta(metas, right.getNovelUrl());
                int pinned = Boolean.compare(r.isPinned(), l.isPinned());
                if (pinned != 0) {
                    return pinned;
                }
                if (mode == SORT_FAVORITE) {
                    int favorite = Boolean.compare(r.isFavorite(), l.isFavorite());
                    if (favorite != 0) {
                        return favorite;
                    }
                }
                if (mode == SORT_PROGRESS) {
                    return Integer.compare(progress(right), progress(left));
                }
                return Long.compare(r.getRecentAt(), l.getRecentAt());
            }
        });
        return result;
    }

    public String labelFor(BookshelfNovelDbData book) {
        BookshelfMeta meta = get(book.getNovelUrl());
        StringBuilder builder = new StringBuilder();
        if (meta.isPinned()) {
            builder.append("置顶 ");
        }
        if (meta.isFavorite()) {
            builder.append("收藏 ");
        }
        if (!meta.getCategory().isEmpty()) {
            builder.append("[").append(meta.getCategory()).append("] ");
        }
        builder.append(book.getName());
        return builder.toString();
    }

    private void saveMeta(BookshelfMeta meta) {
        Map<String, BookshelfMeta> map = load();
        map.put(meta.getNovelUrl(), meta);
        preferences.edit().putString(KEY_META, gson.toJson(map)).apply();
    }

    private Map<String, BookshelfMeta> load() {
        try {
            Type type = new TypeToken<Map<String, BookshelfMeta>>() {
            }.getType();
            Map<String, BookshelfMeta> map = gson.fromJson(preferences.getString(KEY_META, "{}"), type);
            return map == null ? new LinkedHashMap<String, BookshelfMeta>() : map;
        } catch (Throwable ignored) {
            return new LinkedHashMap<>();
        }
    }

    private BookshelfMeta meta(Map<String, BookshelfMeta> metas, String novelUrl) {
        BookshelfMeta meta = metas.get(novelUrl);
        if (meta == null) {
            meta = new BookshelfMeta();
            meta.setNovelUrl(novelUrl);
        }
        return meta;
    }

    private int progress(BookshelfNovelDbData data) {
        return data.getChapterIndex() * 100000 + data.getPosition() + data.getSecondPosition();
    }
}
