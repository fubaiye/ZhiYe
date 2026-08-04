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
    private static final int MAX_WORKERS = 48;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int CACHE_MAX = 40;
    private static final Map<String, CachedResults> CACHE = new LinkedHashMap<String, CachedResults>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedResults> eldest) {
            return size() > CACHE_MAX;
        }
    };

    private final BookSourceExecutor executor = new BookSourceExecutor();

    public List<NovelSourceData> search(String keyword) {
        return search(keyword, Integer.MAX_VALUE, 8);
    }

    public List<NovelSourceData> search(String keyword, int maxSources, int timeoutSeconds) {
        final String query = keyword;
        List<NovelSourceData> cached = getCached(query, maxSources);
        if (!cached.isEmpty()) {
            return cached;
        }
        List<BookSource> sources = limitSources(SourceRepository.getInstance().getEnabled(), maxSources);
        final List<NovelSourceData> rawResults = Collections.synchronizedList(new ArrayList<NovelSourceData>());
        ExecutorService pool = Executors.newFixedThreadPool(workerCount(sources.size()));
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
            latch.await(Math.max(1, timeoutSeconds), TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdownNow();
        }
        List<NovelSourceData> results = sortAndDedupe(rawResults, query);
        putCached(query, maxSources, results);
        return results;
    }

    public static List<BookSource> limitSources(List<BookSource> sources, int maxSources) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>();
        }
        int end = maxSources <= 0 ? sources.size() : Math.min(sources.size(), maxSources);
        return new ArrayList<>(sources.subList(0, end));
    }

    private static int workerCount(int sourceCount) {
        if (sourceCount <= 0) {
            return 1;
        }
        if (sourceCount <= 6) {
            return sourceCount;
        }
        return Math.min(MAX_WORKERS, Math.max(8, sourceCount / 20));
    }

    private static List<NovelSourceData> getCached(String keyword, int maxSources) {
        synchronized (CACHE) {
            CachedResults cached = CACHE.get(cacheKey(keyword, maxSources));
            if (cached == null || System.currentTimeMillis() - cached.createdAt > CACHE_TTL_MS) {
                return new ArrayList<>();
            }
            return new ArrayList<>(cached.results);
        }
    }

    private static void putCached(String keyword, int maxSources, List<NovelSourceData> results) {
        if (results == null || results.isEmpty()) {
            return;
        }
        synchronized (CACHE) {
            CACHE.put(cacheKey(keyword, maxSources), new CachedResults(results));
        }
    }

    private static String cacheKey(String keyword, int maxSources) {
        return safe(keyword).trim() + "|" + maxSources;
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

    private static class CachedResults {
        final long createdAt = System.currentTimeMillis();
        final List<NovelSourceData> results;

        CachedResults(List<NovelSourceData> results) {
            this.results = new ArrayList<>(results);
        }
    }
}
