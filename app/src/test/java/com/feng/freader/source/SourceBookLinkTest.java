package com.feng.freader.source;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SourceBookLinkTest {

    @Test
    public void encodesSourceIdWithOriginalUrlForBookshelfPersistence() {
        String link = SourceBookLink.encode("legado_abc", "https://example.com/book/1?name=凡人修仙传");

        assertTrue(SourceBookLink.isSourceLink(link));
        assertEquals("legado_abc", SourceBookLink.sourceId(link));
        assertEquals("https://example.com/book/1?name=凡人修仙传", SourceBookLink.originalUrl(link));
    }

    @Test
    public void leavesNormalUrlsUntouched() {
        String url = "https://zh.wikisource.org/wiki/狂人日记";

        assertFalse(SourceBookLink.isSourceLink(url));
        assertEquals("", SourceBookLink.sourceId(url));
        assertEquals(url, SourceBookLink.originalUrl(url));
    }
}
