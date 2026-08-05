package com.feng.freader.model;

import com.feng.freader.entity.data.DiscoveryNovelData;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DiscoverySourceProviderMergeTest {

    @Test
    public void keepsSuccessfulCategoriesWhenOneOnlineCategoryFails() {
        DiscoveryNovelData fallbackOne = group("fallback-a", "");
        DiscoveryNovelData fallbackTwo = group("fallback-b", "");
        DiscoveryNovelData onlineOne = group("online-a", "https://example.com/a.jpg");
        DiscoveryNovelData emptyOnline = group();

        List<DiscoveryNovelData> merged = DiscoverySourceProvider.mergeCategories(
                Arrays.asList(onlineOne, emptyOnline),
                Arrays.asList(fallbackOne, fallbackTwo));

        assertEquals(2, merged.size());
        assertEquals("online-a", merged.get(0).getNovelNameList().get(0));
        assertEquals("fallback-b", merged.get(1).getNovelNameList().get(0));
    }

    @Test
    public void fillsFailedRankByIndexFromFallback() {
        List<List<String>> merged = DiscoverySourceProvider.mergeRanks(
                Arrays.asList(
                        Collections.singletonList("online-a"),
                        Collections.<String>emptyList(),
                        Collections.singletonList("online-c")),
                Arrays.asList(
                        Collections.singletonList("fallback-a"),
                        Collections.singletonList("fallback-b"),
                        Collections.singletonList("fallback-c")),
                3);

        assertEquals(3, merged.size());
        assertEquals("online-a", merged.get(0).get(0));
        assertEquals("fallback-b", merged.get(1).get(0));
        assertEquals("online-c", merged.get(2).get(0));
    }

    private DiscoveryNovelData group(String name, String cover) {
        DiscoveryNovelData data = new DiscoveryNovelData();
        data.setNovelNameList(Collections.singletonList(name));
        data.setCoverUrlList(Collections.singletonList(cover));
        return data;
    }

    private DiscoveryNovelData group() {
        DiscoveryNovelData data = new DiscoveryNovelData();
        data.setNovelNameList(Collections.<String>emptyList());
        data.setCoverUrlList(Collections.<String>emptyList());
        return data;
    }
}
