package com.feng.freader.bookshelf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BookshelfMetaTest {
    @Test
    public void emptyCategoryIsSafe() {
        BookshelfMeta meta = new BookshelfMeta();

        assertEquals("", meta.getCategory());
    }
}
