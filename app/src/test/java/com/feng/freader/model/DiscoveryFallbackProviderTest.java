package com.feng.freader.model;

import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.http.WikisourceApi;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DiscoveryFallbackProviderTest {

    @Test
    public void searchFindsTraditionalTitleWithSimplifiedQuery() {
        List<NovelSourceData> results = DiscoveryFallbackProvider.searchSources("狂人日记");

        assertFalse(results.isEmpty());
        assertEquals("狂人日記", results.get(0).getName());
        assertTrue(WikisourceApi.isWikisourceUrl(results.get(0).getUrl()));
    }

    @Test
    public void searchReturnsEmptyForUnknownQuery() {
        assertTrue(DiscoveryFallbackProvider.searchSources("不存在的书名").isEmpty());
    }
}
