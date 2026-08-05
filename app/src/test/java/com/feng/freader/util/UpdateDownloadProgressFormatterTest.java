package com.feng.freader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UpdateDownloadProgressFormatterTest {

    @Test
    public void formatsKnownDownloadSizeWithPercent() {
        String message = UpdateDownloadProgressFormatter.format(5L * 1024L * 1024L,
                20L * 1024L * 1024L);

        assertEquals("已下载 5.0 MB / 20.0 MB（25%）", message);
    }

    @Test
    public void formatsUnknownDownloadSizeWithoutPercent() {
        String message = UpdateDownloadProgressFormatter.format(1536L * 1024L, -1L);

        assertEquals("已下载 1.5 MB", message);
    }

    @Test
    public void capsPercentAtOneHundred() {
        String message = UpdateDownloadProgressFormatter.format(30L * 1024L * 1024L,
                20L * 1024L * 1024L);

        assertEquals("已下载 30.0 MB / 20.0 MB（100%）", message);
    }
}
