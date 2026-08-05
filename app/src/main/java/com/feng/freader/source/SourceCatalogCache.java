package com.feng.freader.source;

import com.feng.freader.entity.data.CatalogData;

import java.util.LinkedHashMap;
import java.util.Map;

public class SourceCatalogCache {
    private static final int MAX_SIZE = 32;
    private static final Map<String, CatalogData> CACHE =
            new LinkedHashMap<String, CatalogData>(MAX_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CatalogData> eldest) {
                    return size() > MAX_SIZE;
                }
            };

    private SourceCatalogCache() {
    }

    public static synchronized void put(String bookUrl, CatalogData data) {
        if (isBlank(bookUrl) || data == null
                || data.getChapterUrlList() == null
                || data.getChapterUrlList().isEmpty()) {
            return;
        }
        CACHE.put(key(bookUrl), data);
    }

    public static synchronized CatalogData get(String bookUrl) {
        if (isBlank(bookUrl)) {
            return null;
        }
        return CACHE.get(key(bookUrl));
    }

    static synchronized void clearForTest() {
        CACHE.clear();
    }

    private static String key(String bookUrl) {
        return SourceBookLink.isSourceLink(bookUrl)
                ? bookUrl
                : SourceBookLink.originalUrl(bookUrl);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
