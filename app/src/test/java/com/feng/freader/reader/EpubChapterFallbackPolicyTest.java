package com.feng.freader.reader;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class EpubChapterFallbackPolicyTest {

    @Test
    public void advancesFromCoverToFirstReadableChapter() {
        int next = EpubChapterFallbackPolicy.nextCandidate(
                Arrays.asList("cover.xhtml", "chapter1.xhtml", "chapter2.xhtml"), 0);

        assertEquals(1, next);
    }

    @Test
    public void stopsWhenNoLaterChapterExists() {
        int next = EpubChapterFallbackPolicy.nextCandidate(
                Collections.singletonList("cover.xhtml"), 0);

        assertEquals(-1, next);
    }
}
