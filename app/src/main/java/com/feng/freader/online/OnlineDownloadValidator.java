package com.feng.freader.online;

public final class OnlineDownloadValidator {
    private OnlineDownloadValidator() {
    }

    public static boolean isAllowed(String url, String mimeType) {
        if (url == null || mimeType == null) {
            return false;
        }
        String lowerUrl = url.toLowerCase();
        String mime = mimeType.toLowerCase();
        if (!lowerUrl.startsWith("https://") && !lowerUrl.startsWith("http://")) {
            return false;
        }
        if (mime.contains("epub") || mime.contains("pdf") || mime.startsWith("text/plain")
                || mime.contains("html") || mime.contains("mobipocket")) {
            return true;
        }
        return lowerUrl.endsWith(".epub") || lowerUrl.endsWith(".pdf") || lowerUrl.endsWith(".txt")
                || lowerUrl.endsWith(".html") || lowerUrl.endsWith(".htm") || lowerUrl.endsWith(".mobi")
                || lowerUrl.endsWith(".azw3");
    }

    public static boolean isReaderSupported(String type) {
        return BookFormat.TYPE_EPUB.equals(type) || BookFormat.TYPE_TXT.equals(type)
                || BookFormat.TYPE_HTML.equals(type);
    }
}
