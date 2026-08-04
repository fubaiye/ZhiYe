package com.feng.freader.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderDisplayPolicyTest {

    @Test
    public void fontSizeCanGrowBeyondOldLimitAndIsClamped() {
        assertEquals(84f, ReaderDisplayPolicy.increaseFontSize(82f), 0.001f);
        assertEquals(96f, ReaderDisplayPolicy.increaseFontSize(96f), 0.001f);
        assertEquals(96f, ReaderDisplayPolicy.clampFontSize(120f), 0.001f);
    }

    @Test
    public void rowSpaceCanGrowBeyondOldLimitAndIsClamped() {
        assertEquals(52f, ReaderDisplayPolicy.increaseRowSpace(48f), 0.001f);
        assertEquals(72f, ReaderDisplayPolicy.increaseRowSpace(72f), 0.001f);
        assertEquals(72f, ReaderDisplayPolicy.clampRowSpace(99f), 0.001f);
    }

    @Test
    public void labelsShowConcreteValues() {
        assertEquals("字号 64sp", ReaderDisplayPolicy.fontSizeLabel(64f));
        assertEquals("行距 32px", ReaderDisplayPolicy.rowSpaceLabel(32f));
    }

    @Test
    public void fontIndexFallsBackToSystem() {
        assertEquals(0, ReaderDisplayPolicy.normalizeFontIndex(-1));
        assertEquals(0, ReaderDisplayPolicy.normalizeFontIndex(99));
        assertEquals(2, ReaderDisplayPolicy.normalizeFontIndex(2));
    }
}
