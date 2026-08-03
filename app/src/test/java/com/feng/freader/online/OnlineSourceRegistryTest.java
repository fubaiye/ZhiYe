package com.feng.freader.online;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class OnlineSourceRegistryTest {
    @Test
    public void exposesOnlyLegalBuiltInSources() {
        OnlineSourceRegistry registry = OnlineSourceRegistry.createDefault();

        List<OnlineBookSource> sources = registry.getSources();

        assertEquals(5, sources.size());
        assertNotNull(registry.find("gutenberg"));
        assertNotNull(registry.find("standardebooks"));
        assertNotNull(registry.find("internetarchive"));
        assertNotNull(registry.find("wikisource_zh"));
        assertNotNull(registry.find("oapen"));
        for (OnlineBookSource source : sources) {
            assertTrue(source.getLicenseNote().contains("开放") || source.getLicenseNote().contains("公版"));
        }
    }

    @Test
    public void disabledSourceIsSkippedByAggregator() throws Exception {
        FakeSource enabled = new FakeSource("enabled", true);
        FakeSource disabled = new FakeSource("disabled", false);
        OnlineSearchAggregator aggregator = new OnlineSearchAggregator();

        BookPage page = aggregator.search(java.util.Arrays.<OnlineBookSource>asList(enabled, disabled), "鲁迅", 1);

        assertEquals(1, page.getItems().size());
        assertEquals(1, enabled.calls);
        assertEquals(0, disabled.calls);
    }

    @Test
    public void emptyKeywordDoesNotCallNetwork() throws Exception {
        FakeSource enabled = new FakeSource("enabled", true);
        OnlineSearchAggregator aggregator = new OnlineSearchAggregator();

        BookPage page = aggregator.search(java.util.Arrays.<OnlineBookSource>asList(enabled), "   ", 1);

        assertTrue(page.getItems().isEmpty());
        assertEquals(0, enabled.calls);
    }

    private static class FakeSource extends AbstractOnlineBookSource {
        int calls;

        FakeSource(String id, boolean enabled) {
            super(id, id, SourceType.OPDS, "https://example.org", "公版开放资源", enabled);
        }

        @Override
        public BookPage getHome() {
            return BookPage.empty();
        }

        @Override
        public BookPage search(String keyword, int page) {
            calls++;
            OnlineBook book = new OnlineBook("1", getId(), getName(), "Book");
            return new BookPage(java.util.Collections.singletonList(book), 1, false, "");
        }

        @Override
        public OnlineBook getBookDetail(String bookId) {
            return null;
        }
    }
}
