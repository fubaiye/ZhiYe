package com.feng.freader.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderCurlColorPolicyTest {

    @Test
    public void keepsNightCurlBackgroundConsistentWithPage() {
        assertEquals(0xFF010101, ReaderCurlColorPolicy.backBackgroundFor(0xFF010101));
    }

    @Test
    public void keepsGreenThemeCurlBackgroundConsistentWithPage() {
        assertEquals(0xFFCCE9CD, ReaderCurlColorPolicy.backBackgroundFor(0xFFCCE9CD));
    }

    @Test
    public void makesCurlBackTextUseCurrentTextColorWithLowAlpha() {
        assertEquals(0x33D3D2CD, ReaderCurlColorPolicy.backTextFor(0xFFD3D2CD));
    }
}
