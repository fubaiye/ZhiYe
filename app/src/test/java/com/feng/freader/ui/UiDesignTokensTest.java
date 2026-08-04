package com.feng.freader.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UiDesignTokensTest {

    @Test
    public void exposesModernReaderPalette() {
        assertEquals("#F6F7F9", UiDesignTokens.BACKGROUND);
        assertEquals("#4F7CF7", UiDesignTokens.PRIMARY);
        assertEquals("#222222", UiDesignTokens.TEXT_PRIMARY);
    }

    @Test
    public void usesAppleBooksLikeShapeAndMotion() {
        assertEquals(18, UiDesignTokens.COVER_RADIUS_DP);
        assertEquals(24, UiDesignTokens.SEARCH_RADIUS_DP);
        assertEquals(200, UiDesignTokens.PAGE_TRANSITION_MS);
        assertEquals(0.97f, UiDesignTokens.CARD_PRESSED_SCALE, 0.001f);
    }
}
