package com.feng.freader.source;

import com.feng.freader.entity.data.CatalogData;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BookSourceExecutorCatalogRulesTest {

    @Test
    public void parsesCssCatalogItems() throws Exception {
        BookSource source = source("css:.chapter", "css:a", "css:a@href");
        CatalogData catalog = catalog(source,
                "<div class='chapter'><a href='/c/1'>第一章</a></div>"
                        + "<div class='chapter'><a href='/c/2'>第二章</a></div>");

        assertCatalog(catalog);
    }

    @Test
    public void parsesJsonPathCatalogItems() throws Exception {
        BookSource source = source("jsonpath:$.chapters[*]",
                "jsonpath:$.name", "jsonpath:$.url");
        CatalogData catalog = catalog(source,
                "{\"chapters\":[{\"name\":\"第一章\",\"url\":\"/c/1\"},"
                        + "{\"name\":\"第二章\",\"url\":\"/c/2\"}]}");

        assertCatalog(catalog);
    }

    @Test
    public void parsesXPathCatalogItemsAndHrefAttributes() throws Exception {
        BookSource source = source("xpath://ul[@id='toc']/li",
                "xpath:.//a/text()", "xpath:.//a/@href");
        CatalogData catalog = catalog(source,
                "<html><body><ul id='toc'>"
                        + "<li><a href='/c/1'>第一章</a></li>"
                        + "<li><a href='/c/2'>第二章</a></li>"
                        + "</ul></body></html>");

        assertCatalog(catalog);
    }

    private CatalogData catalog(BookSource source, String catalogBody) throws Exception {
        return new BookSourceExecutor(new FakeHttpClient(catalogBody))
                .catalog(source, SourceBookLink.encode(source.getId(), "https://example.com/book/1"));
    }

    private BookSource source(String list, String name, String url) {
        BookSource source = new BookSource();
        source.setId("legado_test");
        source.getVariables().put("host", "https://example.com");
        BookSource.SourceRules catalog = new BookSource.SourceRules();
        catalog.setList(list);
        catalog.setName(name);
        catalog.setUrl(url);
        source.setCatalogRules(catalog);
        return source;
    }

    private void assertCatalog(CatalogData catalog) {
        assertEquals(2, catalog.getChapterNameList().size());
        assertEquals("第一章", catalog.getChapterNameList().get(0));
        assertEquals("第二章", catalog.getChapterNameList().get(1));
        assertTrue(SourceBookLink.isSourceLink(catalog.getChapterUrlList().get(0)));
        assertEquals("https://example.com/c/1",
                SourceBookLink.originalUrl(catalog.getChapterUrlList().get(0)));
    }

    private static class FakeHttpClient extends SourceHttpClient {
        private final String body;

        FakeHttpClient(String body) {
            this.body = body;
        }

        @Override
        public String executeUrl(BookSource source, String url) throws IOException {
            return body;
        }
    }
}
