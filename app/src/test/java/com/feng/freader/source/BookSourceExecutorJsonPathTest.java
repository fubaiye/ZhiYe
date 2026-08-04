package com.feng.freader.source;

import com.feng.freader.entity.data.NovelSourceData;

import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class BookSourceExecutorJsonPathTest {

    @Test
    public void parsesEveryJsonPathSearchItem() {
        BookSource source = new BookSource();
        source.setId("json");
        source.setName("JSON源");
        Map<String, String> variables = new HashMap<>();
        variables.put("host", "https://example.com");
        source.setVariables(variables);

        BookSource.SourceRules rules = new BookSource.SourceRules();
        rules.setList("jsonpath:$.data.list[*]");
        rules.setName("jsonpath:name");
        rules.setAuthor("jsonpath:author");
        rules.setUrl("jsonpath:url");
        rules.setCover("jsonpath:cover");
        source.setSearchRules(rules);

        String body = "{"
                + "\"data\":{\"list\":["
                + "{\"name\":\"第一本\",\"author\":\"甲\",\"url\":\"/book/1\",\"cover\":\"/1.jpg\"},"
                + "{\"name\":\"第二本\",\"author\":\"乙\",\"url\":\"https://cdn.example.com/book/2\",\"cover\":\"/2.jpg\"}"
                + "]}"
                + "}";

        List<NovelSourceData> results = new BookSourceExecutor().parseSearchPage(source, body);

        assertEquals(2, results.size());
        assertEquals("第一本", results.get(0).getName());
        assertEquals("https://example.com/book/1", results.get(0).getUrl());
        assertEquals("第二本", results.get(1).getName());
        assertEquals("https://cdn.example.com/book/2", results.get(1).getUrl());
    }
}
