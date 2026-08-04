package com.feng.freader.reader;

import java.util.List;

public class ReaderChapterTitleFormatter {

    private ReaderChapterTitleFormatter() {
    }

    public static String titleAt(List<String> chapterNames, int chapterIndex, String fallback) {
        if (chapterNames != null && chapterIndex >= 0 && chapterIndex < chapterNames.size()) {
            String value = clean(chapterNames.get(chapterIndex));
            if (value.length() > 0) {
                return value;
            }
        }
        return clean(fallback);
    }

    public static String firstNonEmpty(String primary, String secondary, String fallback) {
        String value = clean(primary);
        if (value.length() > 0) {
            return value;
        }
        value = clean(secondary);
        if (value.length() > 0) {
            return value;
        }
        return clean(fallback);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
