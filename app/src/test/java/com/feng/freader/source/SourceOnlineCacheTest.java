package com.feng.freader.source;

import com.feng.freader.entity.data.CatalogData;
import com.feng.freader.entity.data.DetailedChapterData;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SourceOnlineCacheTest {

    @Test
    public void catalogCacheUsesFullSourceBookLinkAsKey() {
        SourceCatalogCache.clearForTest();
        String bookUrl = SourceBookLink.encode("source-a", "https://example.com/book/1");
        CatalogData data = new CatalogData(
                Arrays.asList("第一章", "第二章"),
                Arrays.asList(
                        SourceBookLink.encode("source-a", "https://example.com/c/1"),
                        SourceBookLink.encode("source-a", "https://example.com/c/2")));

        SourceCatalogCache.put(bookUrl, data);

        CatalogData cached = SourceCatalogCache.get(bookUrl);
        assertNotNull(cached);
        assertEquals("第二章", cached.getChapterNameList().get(1));
    }

    @Test
    public void chapterCacheKeepsOnlineChapterContent() {
        SourceChapterCache.clearForTest();
        String chapterUrl = SourceBookLink.encode("source-a", "https://example.com/c/1");
        DetailedChapterData data = new DetailedChapterData("第一章", "正文内容");

        SourceChapterCache.put(chapterUrl, data);

        DetailedChapterData cached = SourceChapterCache.get(chapterUrl);
        assertNotNull(cached);
        assertEquals("正文内容", cached.getContent());
    }
}
