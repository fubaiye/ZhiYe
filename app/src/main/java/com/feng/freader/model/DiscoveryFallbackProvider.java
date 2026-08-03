package com.feng.freader.model;

import com.feng.freader.entity.data.ANNovelData;
import com.feng.freader.entity.data.DiscoveryNovelData;
import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.http.WikisourceApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoveryFallbackProvider {
    private static final String INTRO = "公开版权文本，可通过维基文库搜索阅读。";

    private static final List<String> MODERN = Arrays.asList(
            "狂人日記", "阿Q正傳", "孔乙己", "故鄉", "藥", "祝福");
    private static final List<String> CLASSICS = Arrays.asList(
            "桃花源記", "岳陽樓記", "醉翁亭記", "出師表", "蘭亭集序", "滕王閣序");
    private static final List<String> ESSAYS = Arrays.asList(
            "少年中國說", "論語", "孟子", "道德經", "孫子兵法", "詩經");
    private static final List<String> STORIES = Arrays.asList(
            "聊齋志異", "儒林外史", "西遊記", "三國演義", "水滸傳", "紅樓夢");

    private DiscoveryFallbackProvider() {
    }

    public static List<List<String>> maleRanks() {
        List<List<String>> ranks = new ArrayList<>();
        ranks.add(Arrays.asList("狂人日記", "阿Q正傳", "孔乙己"));
        ranks.add(Arrays.asList("桃花源記", "岳陽樓記", "醉翁亭記"));
        ranks.add(Arrays.asList("孫子兵法", "三國演義", "水滸傳"));
        ranks.add(Arrays.asList("少年中國說", "論語", "孟子"));
        ranks.add(Arrays.asList("聊齋志異", "儒林外史", "西遊記"));
        return ranks;
    }

    public static List<List<String>> femaleRanks() {
        List<List<String>> ranks = new ArrayList<>();
        ranks.add(Arrays.asList("祝福", "故鄉", "藥"));
        ranks.add(Arrays.asList("詩經", "蘭亭集序", "滕王閣序"));
        ranks.add(Arrays.asList("紅樓夢", "聊齋志異", "儒林外史"));
        return ranks;
    }

    public static List<DiscoveryNovelData> maleCategories() {
        return discoveryGroups(Arrays.asList(MODERN, CLASSICS, ESSAYS));
    }

    public static List<DiscoveryNovelData> femaleCategories() {
        return discoveryGroups(Arrays.asList(MODERN, STORIES, CLASSICS));
    }

    public static List<DiscoveryNovelData> pressCategories() {
        return discoveryGroups(Arrays.asList(MODERN, CLASSICS, ESSAYS, STORIES));
    }

    public static List<ANNovelData> page(int start, int num) {
        List<String> all = new ArrayList<>();
        all.addAll(MODERN);
        all.addAll(CLASSICS);
        all.addAll(ESSAYS);
        all.addAll(STORIES);
        List<ANNovelData> page = new ArrayList<>();
        int end = Math.min(all.size(), start + num);
        for (int i = Math.max(0, start); i < end; i++) {
            page.add(new ANNovelData(all.get(i), "维基文库", INTRO, ""));
        }
        return page;
    }

    public static List<NovelSourceData> searchSources(String query) {
        List<NovelSourceData> results = new ArrayList<>();
        String normalizedQuery = normalizeChinese(query);
        if (normalizedQuery.isEmpty()) {
            return results;
        }
        for (String title : allTitles()) {
            String normalizedTitle = normalizeChinese(title);
            if (normalizedTitle.contains(normalizedQuery)
                    || normalizedQuery.contains(normalizedTitle)) {
                results.add(new NovelSourceData(title, "维基文库", INTRO,
                        WikisourceApi.toSourceUrl(title), ""));
            }
        }
        return results;
    }

    public static int totalCount() {
        return MODERN.size() + CLASSICS.size() + ESSAYS.size() + STORIES.size();
    }

    private static List<String> allTitles() {
        List<String> all = new ArrayList<>();
        all.addAll(MODERN);
        all.addAll(CLASSICS);
        all.addAll(ESSAYS);
        all.addAll(STORIES);
        return all;
    }

    private static String normalizeChinese(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .replace("記", "记")
                .replace("傳", "传")
                .replace("鄉", "乡")
                .replace("藥", "药")
                .replace("陽", "阳")
                .replace("樓", "楼")
                .replace("蘭", "兰")
                .replace("閣", "阁")
                .replace("國", "国")
                .replace("說", "说")
                .replace("論", "论")
                .replace("經", "经")
                .replace("孫", "孙")
                .replace("詩", "诗")
                .replace("齋", "斋")
                .replace("異", "异")
                .replace("遊", "游")
                .replace("義", "义")
                .replace("滸", "浒")
                .replace("紅", "红")
                .replace("夢", "梦");
    }

    private static List<DiscoveryNovelData> discoveryGroups(List<List<String>> groups) {
        List<DiscoveryNovelData> dataList = new ArrayList<>();
        for (List<String> group : groups) {
            DiscoveryNovelData data = new DiscoveryNovelData();
            data.setNovelNameList(new ArrayList<>(group));
            data.setCoverUrlList(emptyCovers(group.size()));
            dataList.add(data);
        }
        return dataList;
    }

    private static List<String> emptyCovers(int count) {
        List<String> covers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            covers.add("");
        }
        return covers;
    }
}
