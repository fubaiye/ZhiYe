package com.feng.freader.source;

import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceHttpClientMethodTest {

    @Test
    public void postSourceUsesPostOnlyForSearch() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("{}"));
        server.start();
        try {
            BookSource source = postSource(server.url("/search").toString());

            new SourceHttpClient(new OkHttpClient()).execute(source, "sword", 1);

            RecordedRequest request = server.takeRequest();
            assertEquals("POST", request.getMethod());
            assertTrue(request.getBody().readUtf8().contains("keyword=sword"));
        } finally {
            server.shutdown();
        }
    }

    @Test
    public void postSourceUsesGetForDetailCatalogAndContentUrls() throws Exception {
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody("<html></html>"));
        server.start();
        try {
            BookSource source = postSource(server.url("/search").toString());

            new SourceHttpClient(new OkHttpClient()).executeUrl(source, server.url("/book/1").toString());

            RecordedRequest request = server.takeRequest();
            assertEquals("GET", request.getMethod());
            assertEquals(0, request.getBodySize());
        } finally {
            server.shutdown();
        }
    }

    private BookSource postSource(String searchUrl) {
        BookSource source = new BookSource();
        source.setSearchUrl(searchUrl);
        source.setSearchMethod("POST");
        source.setSearchBody("keyword={{keyword}}&page={{page}}");
        source.getHeaders().put("User-Agent", "UnitTest {{keyword}}");
        source.getCookies().put("token", "{{page}}");
        return source;
    }
}
