package com.feng.freader.http;

import com.feng.freader.entity.data.DetailedChapterData;
import com.feng.freader.entity.data.NovelSourceData;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WikisourceApiTest {

    @Test
    public void sourceUrlRoundTripsTitle() {
        String sourceUrl = WikisourceApi.toSourceUrl("狂人日記");

        assertTrue(WikisourceApi.isWikisourceUrl(sourceUrl));
        assertEquals("狂人日記", WikisourceApi.titleFromSourceUrl(sourceUrl));
    }

    @Test
    public void searchResponseConvertsToNovelSources() throws Exception {
        String json = "{\"query\":{\"search\":[{\"title\":\"狂人日記\",\"snippet\":\"鲁迅 <span class=\\\"searchmatch\\\">作品</span>\"}]}}";

        List<NovelSourceData> results = WikisourceApi.parseSearchResults(json);

        assertEquals(1, results.size());
        assertEquals("狂人日記", results.get(0).getName());
        assertFalse(results.get(0).getIntroduce().contains("<span"));
    }

    @Test
    public void parsePageResponseConvertsHtmlToReadableText() throws Exception {
        String json = "{\"parse\":{\"title\":\"狂人日記\",\"text\":{\"*\":\"<div class=\\\"mw-parser-output\\\"><p>第一段。</p><p>第二段。</p></div>\"}}}";

        DetailedChapterData data = WikisourceApi.parsePageText(json);

        assertEquals("狂人日記", data.getName());
        assertTrue(data.getContent().contains("第一段。"));
        assertTrue(data.getContent().contains("第二段。"));
    }
}
