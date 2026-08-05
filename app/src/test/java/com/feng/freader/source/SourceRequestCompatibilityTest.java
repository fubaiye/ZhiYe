package com.feng.freader.source;

import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.entity.data.CatalogData;
import com.feng.freader.entity.data.DetailedChapterData;

import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceRequestCompatibilityTest {

    @Test
    public void executeUrlHonorsPostJsonOptionsAndRequestHeaders() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"ok\":true}"));
        server.start();
        try {
            String rawUrl = server.url("/chapter").toString()
                    + ",{\"method\":\"POST\",\"body\":{\"id\":\"42\"},"
                    + "\"headers\":{\"X-Source\":\"unit\"}}";

            BookSource source = new BookSource();
            source.getHeaders().put("User-Agent", "Source-UA");

            String body = new SourceHttpClient(new OkHttpClient()).executeUrl(
                    source,
                    rawUrl,
                    new SourceRuleContext(source, rawUrl, null));

            RecordedRequest request = server.takeRequest();
            assertEquals("{\"ok\":true}", body);
            assertEquals("POST", request.getMethod());
            assertEquals("{\"id\":\"42\"}", request.getBody().readUtf8());
            assertEquals("unit", request.getHeader("X-Source"));
            assertTrue(request.getHeader("Content-Type").startsWith("application/json"));
            assertEquals("/chapter", request.getPath());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void requestParserSplitsOptionsAfterUrl() throws Exception {
        SourceRequest request = SourceRequestParser.parse(
                "https://example.com/api,{\"method\":\"POST\",\"body\":\"a,b\"}",
                new SourceRuleContext(null, "https://example.com/book", null));

        assertEquals("https://example.com/api", request.getUrl());
        assertEquals("POST", request.getMethod());
        assertEquals("a,b", request.getBody());
    }

    @Test
    public void templateReadsJsonFieldAndBaseUrlMatchGroup() throws Exception {
        com.google.gson.JsonElement item = new com.google.gson.JsonParser()
                .parse("{\"serialID\":\"10086\"}");

        String result = SourceTemplateEvaluator.render(
                "https://api.example.com/read?book={{baseUrl.match(/bookId=(\\d+)/)[1]}}"
                        + "&serial={{$.serialID}}",
                new SourceRuleContext(null, "https://m.example.com/detail?bookId=7788", item));

        assertEquals("https://api.example.com/read?book=7788&serial=10086", result);
    }

    @Test
    public void jsonCatalogRendersChapterUrlTemplateFromItemAndDetailUrl() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{\"data\":{\"chapters\":["
                + "{\"name\":\"Chapter One\",\"serialID\":\"1\"},"
                + "{\"name\":\"Chapter Two\",\"serialID\":\"2\"}"
                + "]}}"));
        server.start();
        try {
            BookSource source = sourceWithIdAndHost("jsonCatalog", server.url("/").toString());
            source.getCatalogRules().setList("jsonpath:$.data.chapters[*]");
            source.getCatalogRules().setName("jsonpath:$.name");
            source.getCatalogRules().setUrl("template:"
                    + server.url("/content").toString()
                    + "?book={{baseUrl.match(/bookId=(\\d+)/)[1]}}&serial={{$.serialID}}");

            CatalogData data = new BookSourceExecutor(new SourceHttpClient(new OkHttpClient()))
                    .catalog(source, SourceBookLink.encode(source.getId(),
                            server.url("/toc?bookId=7788").toString()));

            assertEquals(2, data.getChapterNameList().size());
            assertEquals("Chapter One", data.getChapterNameList().get(0));
            assertEquals(server.url("/content").toString() + "?book=7788&serial=1",
                    SourceBookLink.originalUrl(data.getChapterUrlList().get(0)));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void contentExtractsJsonTemplateTextFromPostResponse() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(
                "{\"data\":{\"Content\":[{\"Content\":\"<p>Line One</p><p>Line Two</p>\"}]}}"));
        server.start();
        try {
            BookSource source = sourceWithIdAndHost("jsonContent", server.url("/").toString());
            source.getContentRules().setContent("template:<article>{{$.data.Content[0].Content}}</article>");

            String rawUrl = server.url("/chapter").toString()
                    + ",{\"method\":\"POST\",\"body\":{\"id\":\"42\"}}";
            DetailedChapterData data = new BookSourceExecutor(new SourceHttpClient(new OkHttpClient()))
                    .content(source, SourceBookLink.encode(source.getId(), rawUrl));

            RecordedRequest request = server.takeRequest();
            assertEquals("POST", request.getMethod());
            assertEquals("Line One Line Two", data.getContent());
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void sourceBookLinkPreservesRequestOptionsForBookshelfRecovery() {
        String rawUrl = "https://example.com/chapter,{\"method\":\"POST\",\"body\":{\"id\":\"42\"}}";
        String encoded = SourceBookLink.encode("legado_source", rawUrl);
        BookshelfNovelDbData book = new BookshelfNovelDbData(encoded, "Book", "", 0, 0, 1);

        assertTrue(SourceBookLink.isSourceLink(book.getNovelUrl()));
        assertEquals("legado_source", SourceBookLink.sourceId(book.getNovelUrl()));
        assertEquals(rawUrl, SourceBookLink.originalUrl(book.getNovelUrl()));
    }

    @Test
    public void legadoChapterUrlTemplateIsNotNormalizedAsJsonPath() {
        String json = "{"
                + "\"bookSourceName\":\"Unit\","
                + "\"bookSourceUrl\":\"https://example.com\","
                + "\"ruleToc\":{"
                + "\"chapterList\":\"$.data.list[*]\","
                + "\"chapterName\":\"name\","
                + "\"chapterUrl\":\"https://example.com/read,{{$.serialID}}\""
                + "}"
                + "}";

        BookSource source = BookSourceParser.parseOne(json);

        assertEquals("jsonpath:$.data.list[*]", source.getCatalogRules().getList());
        assertEquals("template:https://example.com/read,{{$.serialID}}",
                source.getCatalogRules().getUrl());
    }

    private BookSource sourceWithIdAndHost(String id, String host) {
        BookSource source = new BookSource();
        source.setId(id);
        java.util.Map<String, String> variables = new java.util.LinkedHashMap<>();
        variables.put("host", host);
        source.setVariables(variables);
        return source;
    }
}
