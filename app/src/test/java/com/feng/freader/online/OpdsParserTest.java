package com.feng.freader.online;

import org.junit.Test;

import static org.junit.Assert.*;

public class OpdsParserTest {
    private static final String FEED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<feed xmlns=\"http://www.w3.org/2005/Atom\" xmlns:opds=\"http://opds-spec.org/2010/catalog\">" +
            "<title>Catalog</title>" +
            "<link rel=\"next\" href=\"/page/2.xml\" />" +
            "<link rel=\"search\" type=\"application/opensearchdescription+xml\" href=\"/search.xml\" />" +
            "<entry>" +
            "<title>Alice &amp; Public Domain</title>" +
            "<id>urn:book:1</id>" +
            "<author><name>Lewis Carroll</name></author>" +
            "<summary>Open book</summary>" +
            "<dc:language xmlns:dc=\"http://purl.org/dc/terms/\">en</dc:language>" +
            "<category term=\"fiction\" />" +
            "<link rel=\"http://opds-spec.org/image/thumbnail\" href=\"covers/alice.jpg\" />" +
            "<link rel=\"http://opds-spec.org/acquisition\" type=\"application/epub+zip\" href=\"books/alice.epub\" />" +
            "<link rel=\"http://opds-spec.org/acquisition\" type=\"application/pdf\" href=\"books/alice.pdf\" />" +
            "<link rel=\"alternate\" href=\"/books/alice\" />" +
            "</entry>" +
            "<entry><title>No Author</title><id>urn:book:2</id></entry>" +
            "</feed>";

    @Test
    public void parsesNamespacedOpdsEntriesAndRelativeLinks() throws Exception {
        OpdsParser parser = new OpdsParser();

        BookPage page = parser.parse("https://example.org/catalog/index.xml", FEED, "source", "Source");

        assertEquals(2, page.getItems().size());
        assertTrue(page.hasMore());
        assertEquals("https://example.org/page/2.xml", page.getNextUrl());
        OnlineBook first = page.getItems().get(0);
        assertEquals("Alice & Public Domain", first.getTitle());
        assertEquals("Lewis Carroll", first.getAuthorText());
        assertEquals("https://example.org/catalog/covers/alice.jpg", first.getCoverUrl());
        assertEquals("en", first.getLanguage());
        assertEquals("fiction", first.getSubjects().get(0));
        assertEquals("https://example.org/catalog/books/alice.epub", first.getFormats().get(0).getDownloadUrl());
        assertEquals(BookFormat.TYPE_EPUB, first.getFormats().get(0).getType());
        assertEquals(BookFormat.TYPE_PDF, first.getFormats().get(1).getType());
    }

    @Test
    public void handlesMissingCoverAndAuthor() throws Exception {
        BookPage page = new OpdsParser().parse("https://example.org/feed.xml", FEED, "source", "Source");

        OnlineBook second = page.getItems().get(1);

        assertEquals("", second.getCoverUrl());
        assertEquals("", second.getAuthorText());
        assertTrue(second.getFormats().isEmpty());
    }

    @Test
    public void rejectsHtmlInsteadOfXml() {
        try {
            new OpdsParser().parse("https://example.org/feed.xml", "<html>down</html>", "source", "Source");
            fail("Expected invalid feed");
        } catch (Exception expected) {
            assertTrue(expected.getMessage().contains("OPDS"));
        }
    }
}
