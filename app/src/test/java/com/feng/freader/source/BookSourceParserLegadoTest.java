package com.feng.freader.source;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class BookSourceParserLegadoTest {

    @Test
    public void convertsLegadoBookSourceJson() {
        String json = "[{"
                + "\"bookSourceName\":\"测试书源\","
                + "\"bookSourceUrl\":\"https://example.com\","
                + "\"enabled\":true,"
                + "\"searchUrl\":\"https://example.com/search/{{key}}/{{page}}\","
                + "\"ruleSearch\":{"
                + "\"bookList\":\".book\","
                + "\"name\":\".title@text\","
                + "\"author\":\".author@text\","
                + "\"bookUrl\":\"a@href\","
                + "\"coverUrl\":\"img@src\","
                + "\"intro\":\".intro@text\""
                + "}"
                + "}]";

        List<BookSource> sources = BookSourceParser.parseList(json);

        assertEquals(1, sources.size());
        BookSource source = sources.get(0);
        assertEquals("测试书源", source.getName());
        assertEquals("https://example.com/search/{{keyword}}/{{page}}", source.getSearchUrl());
        assertEquals("css:.book", source.getSearchRules().getList());
        assertEquals("css:.title", source.getSearchRules().getName());
        assertEquals("css:a@href", source.getSearchRules().getUrl());
        assertFalse(source.isEnabled());
    }
}
