package com.feng.freader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StatusBarInsetPolicyTest {

    @Test
    public void topPaddingKeepsOriginalPaddingAndAddsStatusBarHeight() {
        assertEquals(54, StatusBarInsetPolicy.topPadding(18, 36));
    }

    @Test
    public void topPaddingIgnoresInvalidStatusBarHeight() {
        assertEquals(18, StatusBarInsetPolicy.topPadding(18, -1));
        assertEquals(18, StatusBarInsetPolicy.topPadding(18, 0));
    }
}
