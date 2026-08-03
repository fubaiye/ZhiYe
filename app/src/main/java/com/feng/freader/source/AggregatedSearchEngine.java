package com.feng.freader.source;

import com.feng.freader.entity.data.NovelSourceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AggregatedSearchEngine {
    private final BookSourceExecutor executor = new BookSourceExecutor();

    public List<NovelSourceData> search(String keyword) {
        final String query = keyword;
        List<BookSource> sources = SourceRepository.getInstance().getEnabled();
        final List<NovelSourceData> rawResults = Collections.synchronizedList(new ArrayList<NovelSourceData>());
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(6, sources.size())));
        final CountDownLatch latch = new CountDownLatch(sources.size());
        for (final BookSource source : sources) {
            pool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        rawResults.addAll(executor.search(source, query));
                    } catch (Throwable ignored) {
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        try {
            latch.await(15, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        return sortAndDedupe(rawResults, query);
    }

    public static List<NovelSourceData> sortAndDedupe(List<NovelSourceData> rawResults, final String keyword) {
        Map<String, NovelSourceData> byKey = new LinkedHashMap<>();
        for (NovelSourceData data : rawResults) {
            if (data == null || data.getName() == null || data.getName().trim().length() == 0) {
                continue;
            }
            String key = data.getName().trim() + "|" + safe(data.getAuthor()).trim();
            if (!byKey.containsKey(key)) {
                byKey.put(key, data);
            }
        }
        List<NovelSourceData> results = new ArrayList<>(byKey.values());
        Collections.sort(results, new Comparator<NovelSourceData>() {
            @Override
            public int compare(NovelSourceData left, NovelSourceData right) {
                return score(right, keyword) - score(left, keyword);
            }
        });
        return results;
    }

    private static int score(NovelSourceData data, String keyword) {
        String name = safe(data.getName());
        String source = safe(data.getSourceName());
        int score = 0;
        if (name.equals(keyword)) {
            score += 100;
        }
        if (name.contains(keyword)) {
            score += 50;
        }
        if (source.length() > 0) {
            score += 5;
        }
        return score;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
