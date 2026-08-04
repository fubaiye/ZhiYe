package com.feng.freader.source;

import com.feng.freader.entity.data.CatalogData;
import com.feng.freader.entity.data.DetailedChapterData;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BookSourceExecutorCatalogContentTest {

    @Test
    public void parsesCatalogAndKeepsSourceLinkOnChapterUrls() throws Exception {
        BookSource source = source();
        BookSourceExecutor executor = new BookSourceExecutor(new FakeHttpClient()
                .when("https://example.com/book/1", "<html><body><a class='toc' href='/toc/1'>目录</a></body></html>")
                .when("https://example.com/toc/1", "<div class='chapter'><a href='/chapter/1'>第一章</a></div>"));

        CatalogData catalog = executor.catalog(source, "https://example.com/book/1");

        assertEquals(1, catalog.getChapterNameList().size());
        assertEquals("第一章", catalog.getChapterNameList().get(0));
        assertTrue(SourceBookLink.isSourceLink(catalog.getChapterUrlList().get(0)));
        assertEquals("https://example.com/chapter/1",
                SourceBookLink.originalUrl(catalog.getChapterUrlList().get(0)));
    }

    @Test
    public void parsesChapterContentFromSourceRules() throws Exception {
        BookSource source = source();
        BookSourceExecutor executor = new BookSourceExecutor(new FakeHttpClient()
                .when("https://example.com/chapter/1",
                        "<h1>第一章</h1><div id='content'><p>天地玄黄。</p><p>宇宙洪荒。</p></div>"));

        DetailedChapterData data = executor.content(source, "https://example.com/chapter/1");

        assertEquals("第一章", data.getName());
        assertEquals("天地玄黄。\n宇宙洪荒。", data.getContent().trim());
    }

    private BookSource source() {
        BookSource source = new BookSource();
        source.setId("legado_test");
        source.setName("测试书源");
        source.getVariables().put("host", "https://example.com");
        BookSource.SourceRules detail = new BookSource.SourceRules();
        detail.setUrl("css:.toc@href");
        source.setDetailRules(detail);
        BookSource.SourceRules catalog = new BookSource.SourceRules();
        catalog.setList("css:.chapter");
        catalog.setName("css:a");
        catalog.setUrl("css:a@href");
        source.setCatalogRules(catalog);
        BookSource.SourceRules content = new BookSource.SourceRules();
        content.setName("css:h1");
        content.setContent("css:#content p");
        source.setContentRules(content);
        return source;
    }

    private static class FakeHttpClient extends SourceHttpClient {
        private final java.util.Map<String, String> responses = new java.util.LinkedHashMap<>();

        FakeHttpClient when(String url, String body) {
            responses.put(url, body);
            return this;
        }

        @Override
        public String executeUrl(BookSource source, String url) throws IOException {
            String response = responses.get(url);
            if (response == null) {
                throw new IOException("missing " + url);
            }
            return response;
        }
    }
}
