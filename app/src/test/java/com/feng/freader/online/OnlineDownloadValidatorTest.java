package com.feng.freader.online;

import org.junit.Test;

import static org.junit.Assert.*;

public class OnlineDownloadValidatorTest {
    @Test
    public void acceptsOpenEpubAndPdfDownloads() {
        assertTrue(OnlineDownloadValidator.isAllowed("https://example.org/book.epub", "application/epub+zip"));
        assertTrue(OnlineDownloadValidator.isAllowed("https://example.org/book.pdf", "application/pdf"));
    }

    @Test
    public void rejectsUnsafeOrUnknownDownloads() {
        assertFalse(OnlineDownloadValidator.isAllowed("javascript:alert(1)", "application/epub+zip"));
        assertFalse(OnlineDownloadValidator.isAllowed("http://example.org/book.exe", "application/octet-stream"));
        assertFalse(OnlineDownloadValidator.isAllowed("https://example.org/book", ""));
    }

    @Test
    public void detectsDuplicateBySourceAndUrl() {
        OnlineDownloadRecordStore store = new OnlineDownloadRecordStore();
        store.add(new OnlineDownloadRecord("gutenberg", "1", "Project Gutenberg",
                "https://example.org/book.epub", "/tmp/book.epub", "hash"));

        assertTrue(store.exists("gutenberg", "https://example.org/book.epub"));
        assertFalse(store.exists("standardebooks", "https://example.org/book.epub"));
    }
}
