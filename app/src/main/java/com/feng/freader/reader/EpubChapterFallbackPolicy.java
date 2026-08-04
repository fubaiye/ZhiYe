package com.feng.freader.reader;

import java.util.List;

public final class EpubChapterFallbackPolicy {

    private EpubChapterFallbackPolicy() {
    }

    public static int nextCandidate(List<String> spine, int currentIndex) {
        if (spine == null || currentIndex < 0) {
            return -1;
        }
        int next = currentIndex + 1;
        return next < spine.size() ? next : -1;
    }
}
