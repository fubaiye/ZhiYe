package com.feng.freader.source;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BookSourceParserTest {

    @Test
    public void parsesBookSourceJsonWithHeadersCookiesPostAndPagination() {
        String json = "{"
                + "\"id\":\"demo\","
                + "\"name\":\"示例源\","
                + "\"enabled\":true,"
                + "\"searchUrl\":\"https://example.com/search?q={{keyword}}&page={{page}}\","
                + "\"searchMethod\":\"POST\","
                + "\"searchBody\":\"kw={{keyword}}\","
                + "\"headers\":{\"User-Agent\":\"ZhiYe\"},"
                + "\"cookies\":{\"token\":\"abc\"},"
                + "\"variables\":{\"host\":\"https://example.com\"},"
                + "\"pagination\":{\"start\":1,\"max\":3},"
                + "\"searchRules\":{\"list\":\"css:.book\",\"name\":\"css:.name\",\"url\":\"css:a@href\"}"
                + "}";

        List<BookSource> sources = BookSourceParser.parseList("[" + json + "]");
        BookSource source = sources.get(0);

        assertEquals("demo", source.getId());
        assertEquals("示例源", source.getName());
        assertTrue(source.isEnabled());
        assertEquals("POST", source.getSearchMethod());
        assertEquals("ZhiYe", source.getHeaders().get("User-Agent"));
        assertEquals("abc", source.getCookies().get("token"));
        assertEquals(1, source.getPagination().getStart());
        assertEquals(3, source.getPagination().getMax());
    }

    @Test
    public void expandsVariablesAndPage() {
        BookSource source = new BookSource();
        source.setSearchUrl("https://{{host}}/s/{{keyword}}/{{page}}");
        Map<String, String> variables = new HashMap<>();
        variables.put("host", "example.com");
        source.setVariables(variables);

        String url = SourceTemplate.render(source.getSearchUrl(), source, "雪中", 2);

        assertEquals("https://example.com/s/%E9%9B%AA%E4%B8%AD/2", url);
    }
}
