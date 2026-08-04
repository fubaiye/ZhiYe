package com.feng.freader.model;

import com.feng.freader.entity.data.ANNovelData;
import com.feng.freader.entity.data.DiscoveryNovelData;
import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.http.WikisourceApi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DiscoveryFallbackProvider {
    private static final String AUTHOR = "维基文库";
    private static final String INTRO = "公开版权文本，可通过维基文库搜索阅读。";

    private static final List<String> MODERN = Arrays.asList(
            "狂人日记", "阿Q正传", "孔乙己", "故乡", "药", "祝福");
    private static final List<String> CLASSICS = Arrays.asList(
            "桃花源记", "岳阳楼记", "醉翁亭记", "出师表", "兰亭集序", "滕王阁序");
    private static final List<String> ESSAYS = Arrays.asList(
            "少年中国说", "论语", "孟子", "道德经", "孙子兵法", "诗经");
    private static final List<String> STORIES = Arrays.asList(
            "聊斋志异", "儒林外史", "西游记", "三国演义", "水浒传", "红楼梦");

    private DiscoveryFallbackProvider() {
    }

    public static List<List<String>> maleRanks() {
        List<List<String>> ranks = new ArrayList<>();
        ranks.add(Arrays.asList("狂人日记", "阿Q正传", "孔乙己"));
        ranks.add(Arrays.asList("桃花源记", "岳阳楼记", "醉翁亭记"));
        ranks.add(Arrays.asList("孙子兵法", "三国演义", "水浒传"));
        ranks.add(Arrays.asList("少年中国说", "论语", "孟子"));
        ranks.add(Arrays.asList("聊斋志异", "儒林外史", "西游记"));
        return ranks;
    }

    public static List<List<String>> femaleRanks() {
        List<List<String>> ranks = new ArrayList<>();
        ranks.add(Arrays.asList("祝福", "故乡", "药"));
        ranks.add(Arrays.asList("诗经", "兰亭集序", "滕王阁序"));
        ranks.add(Arrays.asList("红楼梦", "聊斋志异", "儒林外史"));
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
        List<String> all = allTitles();
        List<ANNovelData> page = new ArrayList<>();
        int end = Math.min(all.size(), start + num);
        for (int i = Math.max(0, start); i < end; i++) {
            page.add(new ANNovelData(all.get(i), AUTHOR, INTRO, ""));
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
                NovelSourceData data = new NovelSourceData(title, AUTHOR, INTRO,
                        WikisourceApi.toSourceUrl(title), "");
                data.setSourceName(AUTHOR);
                results.add(data);
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
                .replace("樓", "楼")
                .replace("閣", "阁")
                .replace("語", "语")
                .replace("經", "经")
                .replace("詩", "诗")
                .replace("齋", "斋")
                .replace("異", "异")
                .replace("遊", "游")
                .replace("國", "国")
                .replace("藥", "药")
                .replace("陽", "阳")
                .replace("蘭", "兰")
                .replace("夢", "梦")
                .replace("顛", "颠");
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
