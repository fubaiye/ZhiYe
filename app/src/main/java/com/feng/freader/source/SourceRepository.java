package com.feng.freader.source;

import android.content.Context;
import android.content.SharedPreferences;

import com.feng.freader.app.App;

import java.util.ArrayList;
import java.util.List;

public class SourceRepository {
    private static final String PREF = "book_sources";
    private static final String KEY_JSON = "json";
    private static SourceRepository instance;

    private final SharedPreferences preferences;

    private SourceRepository(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static SourceRepository getInstance() {
        if (instance == null) {
            instance = new SourceRepository(App.getContext());
        }
        return instance;
    }

    public List<BookSource> getAll() {
        String json = preferences.getString(KEY_JSON, "");
        List<BookSource> sources = json == null || json.length() == 0
                ? new ArrayList<BookSource>()
                : BookSourceParser.parseList(json);
        if (sources.isEmpty()) {
            sources.addAll(defaultSources());
            save(sources);
        }
        return sources;
    }

    public List<BookSource> getEnabled() {
        List<BookSource> enabled = new ArrayList<>();
        for (BookSource source : getAll()) {
            if (source.isEnabled()) {
                enabled.add(source);
            }
        }
        return enabled;
    }

    public void save(List<BookSource> sources) {
        preferences.edit().putString(KEY_JSON, BookSourceParser.toJson(sources)).apply();
    }

    public String exportJson() {
        return BookSourceParser.toJson(getAll());
    }

    public int importJson(String json) {
        List<BookSource> incoming = BookSourceParser.parseList(json);
        if (incoming.isEmpty()) {
            return 0;
        }
        List<BookSource> all = getAll();
        for (BookSource source : incoming) {
            upsert(all, source);
        }
        save(all);
        return incoming.size();
    }

    public void saveSource(BookSource source) {
        List<BookSource> all = getAll();
        upsert(all, source);
        save(all);
    }

    public void setEnabled(String id, boolean enabled) {
        List<BookSource> all = getAll();
        for (BookSource source : all) {
            if (source.getId().equals(id)) {
                source.setEnabled(enabled);
                break;
            }
        }
        save(all);
    }

    public void delete(String id) {
        List<BookSource> all = getAll();
        for (int i = all.size() - 1; i >= 0; i--) {
            if (all.get(i).getId().equals(id)) {
                all.remove(i);
            }
        }
        save(all);
    }

    private void upsert(List<BookSource> all, BookSource source) {
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(source.getId())) {
                all.set(i, source);
                return;
            }
        }
        all.add(source);
    }

    private List<BookSource> defaultSources() {
        String json = "[{"
                + "\"id\":\"wikisource\","
                + "\"name\":\"维基文库\","
                + "\"enabled\":true,"
                + "\"searchUrl\":\"https://zh.wikisource.org/w/api.php?action=query&list=search&format=json&srsearch={{keyword}}&srlimit=20\","
                + "\"searchMethod\":\"GET\","
                + "\"headers\":{\"User-Agent\":\"ZhiYe/1.3\"},"
                + "\"variables\":{\"host\":\"https://zh.wikisource.org\"},"
                + "\"pagination\":{\"start\":1,\"max\":1},"
                + "\"searchRules\":{"
                + "\"list\":\"css:.mw-search-result\","
                + "\"name\":\"css:.mw-search-result-heading a\","
                + "\"url\":\"css:.mw-search-result-heading a@href\","
                + "\"intro\":\"css:.searchresult\""
                + "}"
                + "}]";
        return BookSourceParser.parseList(json);
    }
}
