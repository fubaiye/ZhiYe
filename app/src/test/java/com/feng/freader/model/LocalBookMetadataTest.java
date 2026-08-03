package com.feng.freader.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class LocalBookMetadataTest {

    @Test
    public void cleansTimestampPrefixAndExtension() {
        assertEquals("大苍守夜人",
                LocalBookMetadata.cleanTitle("1785726502864_大苍守夜人.txt"));
    }

    @Test
    public void cleansCommonDownloaderSuffixes() {
        assertEquals("大苍守夜人",
                LocalBookMetadata.cleanTitle("1785726502864_大苍守夜人_精校版.txt"));
    }

    @Test
    public void parsesGoogleBooksMetadata() {
        String json = "{\"items\":[{\"volumeInfo\":{\"title\":\"大苍守夜人\","
                + "\"imageLinks\":{\"thumbnail\":\"http://books.google.com/cover.jpg\"}}}]}";

        LocalBookMetadata metadata = LocalBookMetadata.fromGoogleBooksJson(json);

        assertEquals("大苍守夜人", metadata.getTitle());
        assertEquals("https://books.google.com/cover.jpg", metadata.getCoverUrl());
        assertFalse(metadata.isEmpty());
    }

    @Test
    public void parsesOpenLibraryMetadata() {
        String json = "{\"docs\":[{\"title\":\"大苍守夜人\",\"cover_i\":123456}]}";

        LocalBookMetadata metadata = LocalBookMetadata.fromOpenLibraryJson(json);

        assertEquals("大苍守夜人", metadata.getTitle());
        assertEquals("https://covers.openlibrary.org/b/id/123456-L.jpg", metadata.getCoverUrl());
    }
}
