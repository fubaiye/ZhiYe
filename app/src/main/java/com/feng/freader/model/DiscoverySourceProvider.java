package com.feng.freader.model;

import android.os.Handler;
import android.os.Looper;

import com.feng.freader.entity.data.DiscoveryNovelData;
import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.source.AggregatedSearchEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoverySourceProvider {
    private static final int FULL_SOURCE_LIMIT = Integer.MAX_VALUE;
    private static final int DISCOVERY_TIMEOUT_SECONDS = 8;
    private final AggregatedSearchEngine searchEngine = new AggregatedSearchEngine();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Callback<T> {
        void onResult(T data);
    }

    public void loadRanks(final List<String> keywords, final List<List<String>> fallback,
                          final Callback<List<List<String>>> callback) {
        post(callback, fallback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<List<String>> ranks = new ArrayList<>();
                for (String keyword : keywords) {
                    List<String> names = names(searchEngine.search(keyword, FULL_SOURCE_LIMIT,
                            DISCOVERY_TIMEOUT_SECONDS), 3);
                    if (!names.isEmpty()) {
                        ranks.add(names);
                    }
                }
                if (!ranks.isEmpty()) {
                    post(callback, ranks);
                }
            }
        }).start();
    }

    public void loadCategories(final List<String> keywords, final List<DiscoveryNovelData> fallback,
                               final Callback<List<DiscoveryNovelData>> callback) {
        post(callback, fallback);
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<DiscoveryNovelData> groups = new ArrayList<>();
                for (String keyword : keywords) {
                    DiscoveryNovelData group = group(searchEngine.search(keyword, FULL_SOURCE_LIMIT,
                            DISCOVERY_TIMEOUT_SECONDS), 6);
                    if (!group.getNovelNameList().isEmpty()) {
                        groups.add(group);
                    }
                }
                if (!groups.isEmpty()) {
                    post(callback, mergeCategories(groups, fallback));
                }
            }
        }).start();
    }

    public static List<String> maleRankKeywords() {
        return Arrays.asList("玄幻", "都市", "修仙", "历史", "悬疑");
    }

    public static List<String> femaleRankKeywords() {
        return Arrays.asList("言情", "古代言情", "现代言情");
    }

    public static List<String> maleCategoryKeywords() {
        return Arrays.asList("玄幻", "都市", "武侠");
    }

    public static List<String> femaleCategoryKeywords() {
        return Arrays.asList("言情", "青春", "穿越");
    }

    public static List<String> pressCategoryKeywords() {
        return Arrays.asList("文学", "经典", "历史", "传记");
    }

    private DiscoveryNovelData group(List<NovelSourceData> results, int count) {
        DiscoveryNovelData data = new DiscoveryNovelData();
        List<String> names = new ArrayList<>();
        List<String> covers = new ArrayList<>();
        int limit = Math.min(count, results.size());
        for (int i = 0; i < limit; i++) {
            NovelSourceData item = results.get(i);
            names.add(item.getName());
            covers.add(item.getCover());
        }
        data.setNovelNameList(names);
        data.setCoverUrlList(covers);
        return data;
    }

    private List<String> names(List<NovelSourceData> results, int count) {
        List<String> names = new ArrayList<>();
        int limit = Math.min(count, results.size());
        for (int i = 0; i < limit; i++) {
            names.add(results.get(i).getName());
        }
        return names;
    }

    private <T> void post(final Callback<T> callback, final T data) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onResult(data);
            }
        });
    }

    public static List<DiscoveryNovelData> mergeCategories(
            List<DiscoveryNovelData> online,
            List<DiscoveryNovelData> fallback) {
        List<DiscoveryNovelData> merged = new ArrayList<>();
        int count = Math.max(size(online), size(fallback));
        for (int i = 0; i < count; i++) {
            DiscoveryNovelData onlineGroup = i < size(online) ? online.get(i) : null;
            DiscoveryNovelData fallbackGroup = i < size(fallback) ? fallback.get(i) : null;
            if (hasNames(onlineGroup)) {
                merged.add(fillMissingCovers(onlineGroup, fallbackGroup));
            } else if (fallbackGroup != null) {
                merged.add(fallbackGroup);
            }
        }
        return merged;
    }

    private static DiscoveryNovelData fillMissingCovers(
            DiscoveryNovelData onlineGroup,
            DiscoveryNovelData fallbackGroup) {
        List<String> names = onlineGroup.getNovelNameList();
        List<String> covers = new ArrayList<>();
        List<String> onlineCovers = onlineGroup.getCoverUrlList();
        List<String> fallbackCovers = fallbackGroup == null
                ? new ArrayList<String>()
                : fallbackGroup.getCoverUrlList();
        for (int i = 0; i < names.size(); i++) {
            String cover = i < size(onlineCovers) ? onlineCovers.get(i) : "";
            if ((cover == null || cover.trim().length() == 0) && i < size(fallbackCovers)) {
                cover = fallbackCovers.get(i);
            }
            covers.add(cover == null ? "" : cover);
        }
        DiscoveryNovelData data = new DiscoveryNovelData();
        data.setNovelNameList(new ArrayList<>(names));
        data.setCoverUrlList(covers);
        return data;
    }

    private static boolean hasNames(DiscoveryNovelData data) {
        return data != null && data.getNovelNameList() != null && !data.getNovelNameList().isEmpty();
    }

    private static int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
