package com.feng.freader.reader;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ReaderChapterTitleFormatterTest {

    @Test
    public void usesChapterNameWhenIndexIsAvailable() {
        String title = ReaderChapterTitleFormatter.titleAt(
                Arrays.asList("第一章 起始", "第二章 风起"), 1, "书名");

        assertEquals("第二章 风起", title);
    }

    @Test
    public void fallsBackToCurrentDataTitleThenBookName() {
        assertEquals("第六章 峰回路转",
                ReaderChapterTitleFormatter.firstNonEmpty("", "第六章 峰回路转", "书名"));
        assertEquals("书名",
                ReaderChapterTitleFormatter.firstNonEmpty("", "", "书名"));
    }
}
