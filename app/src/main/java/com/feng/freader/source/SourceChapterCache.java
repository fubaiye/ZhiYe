package com.feng.freader.source;

import com.feng.freader.entity.data.DetailedChapterData;

import java.util.LinkedHashMap;
import java.util.Map;

public class SourceChapterCache {
    private static final int MAX_SIZE = 128;
    private static final Map<String, DetailedChapterData> CACHE =
            new LinkedHashMap<String, DetailedChapterData>(MAX_SIZE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, DetailedChapterData> eldest) {
                    return size() > MAX_SIZE;
                }
            };

    private SourceChapterCache() {
    }

    public static synchronized void put(String chapterUrl, DetailedChapterData data) {
        if (isBlank(chapterUrl) || data == null || isBlank(data.getContent())) {
            return;
        }
        CACHE.put(key(chapterUrl), data);
    }

    public static synchronized DetailedChapterData get(String chapterUrl) {
        if (isBlank(chapterUrl)) {
            return null;
        }
        return CACHE.get(key(chapterUrl));
    }

    static synchronized void clearForTest() {
        CACHE.clear();
    }

    private static String key(String chapterUrl) {
        return SourceBookLink.isSourceLink(chapterUrl)
                ? chapterUrl
                : SourceBookLink.originalUrl(chapterUrl);
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
