package com.feng.freader.source;

import com.feng.freader.entity.data.NovelSourceData;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class AggregatedSearchEngineLimitTest {

    @Test
    public void limitsSourcesForDiscoverySampling() {
        List<BookSource> sources = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            BookSource source = new BookSource();
            source.setId("s" + i);
            source.setName("源" + i);
            sources.add(source);
        }

        List<BookSource> limited = AggregatedSearchEngine.limitSources(sources, 2);

        assertEquals(2, limited.size());
        assertEquals("s0", limited.get(0).getId());
        assertEquals("s1", limited.get(1).getId());
    }

    @Test
    public void sortsAndDedupeKeepsSourceBadgeData() {
        List<NovelSourceData> raw = new ArrayList<>();
        NovelSourceData first = new NovelSourceData("雪中悍刀行", "烽火戏诸侯", "", "u1", "");
        first.setSourceName("源A");
        NovelSourceData duplicate = new NovelSourceData("雪中悍刀行", "烽火戏诸侯", "", "u2", "");
        duplicate.setSourceName("源B");
        raw.add(duplicate);
        raw.add(first);

        List<NovelSourceData> results = AggregatedSearchEngine.sortAndDedupe(raw, "雪中");

        assertEquals(1, results.size());
        assertEquals("源B", results.get(0).getSourceName());
    }
}
