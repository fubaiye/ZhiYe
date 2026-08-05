package com.feng.freader.util;

import java.util.Locale;

public final class UpdateDownloadProgressFormatter {
    private static final long KB = 1024L;
    private static final long MB = KB * 1024L;

    private UpdateDownloadProgressFormatter() {
    }

    public static String format(long downloadedBytes, long totalBytes) {
        long safeDownloaded = Math.max(0L, downloadedBytes);
        if (totalBytes > 0L) {
            int percent = (int) Math.min(100L, safeDownloaded * 100L / totalBytes);
            return "已下载 " + formatBytes(safeDownloaded)
                    + " / " + formatBytes(totalBytes)
                    + "（" + percent + "%）";
        }
        return "已下载 " + formatBytes(safeDownloaded);
    }

    private static String formatBytes(long bytes) {
        if (bytes >= MB) {
            return String.format(Locale.US, "%.1f MB", bytes / (double) MB);
        }
        return String.format(Locale.US, "%.1f KB", bytes / (double) KB);
    }
}
