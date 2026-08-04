package com.feng.freader.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EpubTitleStylePolicyTest {

    @Test
    public void keepsChapterTitleTheSameSizeAsBodyText() {
        assertEquals(42f, EpubTitleStylePolicy.titleTextSize(42f), 0.001f);
    }

    @Test
    public void keepsChapterTitleLineHeightAlignedWithBodyText() {
        assertEquals(54f, EpubTitleStylePolicy.titleLineStep(42f, 12f), 0.001f);
    }
}
