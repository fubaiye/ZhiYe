package com.feng.freader.source;

import com.feng.freader.entity.data.NovelSourceData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class AggregatedSearchEngine {
    private static final int MAX_WORKERS = 12;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;
    private static final int CACHE_MAX = 40;
    private static final long PROGRESS_INTERVAL_MS = 450L;
    private static final Map<String, CachedResults> CACHE = new LinkedHashMap<String, CachedResults>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CachedResults> eldest) {
            return size() > CACHE_MAX;
        }
    };

    private final BookSourceExecutor executor = new BookSourceExecutor();

    public interface ProgressListener {
        void onProgress(List<NovelSourceData> results);
    }

    public List<NovelSourceData> search(String keyword) {
        return search(keyword, Integer.MAX_VALUE, 10);
    }

    public List<NovelSourceData> search(String keyword, int maxSources, int timeoutSeconds) {
        return search(keyword, maxSources, timeoutSeconds, null);
    }

    public List<NovelSourceData> search(String keyword, int maxSources, int timeoutSeconds,
                                        ProgressListener listener) {
        final String query = keyword;
        List<NovelSourceData> cached = getCached(query, maxSources);
        if (!cached.isEmpty()) {
            notifyProgress(listener, cached);
            return cached;
        }
        List<BookSource> sources = limitSources(SourceRepository.getInstance().getEnabled(), maxSources);
        final List<NovelSourceData> rawResults = Collections.synchronizedList(new ArrayList<NovelSourceData>());
        ExecutorService pool = Executors.newFixedThreadPool(workerCount(sources.size()));
        CompletionService<List<NovelSourceData>> completionService =
                new ExecutorCompletionService<>(pool);
        for (final BookSource source : sources) {
            completionService.submit(new Callable<List<NovelSourceData>>() {
                @Override
                public List<NovelSourceData> call() {
                    try {
                        return executor.search(source, query);
                    } catch (Throwable ignored) {
                    }
                    return new ArrayList<>();
                }
            });
        }
        int completed = 0;
        int lastProgressSize = 0;
        long lastProgressAt = 0L;
        long deadlineAt = System.currentTimeMillis() + Math.max(1, timeoutSeconds) * 1000L;
        try {
            while (completed < sources.size()) {
                long remainMs = deadlineAt - System.currentTimeMillis();
                if (remainMs <= 0) {
                    break;
                }
                Future<List<NovelSourceData>> future =
                        completionService.poll(Math.min(PROGRESS_INTERVAL_MS, remainMs),
                                TimeUnit.MILLISECONDS);
                if (future == null) {
                    continue;
                }
                completed++;
                List<NovelSourceData> sourceResults = future.get();
                if (sourceResults != null && !sourceResults.isEmpty()) {
                    rawResults.addAll(sourceResults);
                    List<NovelSourceData> progress = sortAndDedupe(rawResults, query);
                    long now = System.currentTimeMillis();
                    if (progress.size() > lastProgressSize
                            || now - lastProgressAt >= PROGRESS_INTERVAL_MS) {
                        lastProgressSize = progress.size();
                        lastProgressAt = now;
                        notifyProgress(listener, progress);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable ignored) {
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
        return Math.min(MAX_WORKERS, Math.max(6, sourceCount / 30));
    }

    private static void notifyProgress(ProgressListener listener, List<NovelSourceData> results) {
        if (listener == null || results == null || results.isEmpty()) {
            return;
        }
        listener.onProgress(new ArrayList<>(results));
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
