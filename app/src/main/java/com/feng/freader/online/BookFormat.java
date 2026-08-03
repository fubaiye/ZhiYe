package com.feng.freader.online;

public class BookFormat {
    public static final String TYPE_EPUB = "epub";
    public static final String TYPE_PDF = "pdf";
    public static final String TYPE_MOBI = "mobi";
    public static final String TYPE_AZW3 = "azw3";
    public static final String TYPE_TXT = "txt";
    public static final String TYPE_HTML = "html";
    public static final String TYPE_OTHER = "other";

    private final String type;
    private final String mimeType;
    private final String downloadUrl;
    private final long fileSize;

    public BookFormat(String type, String mimeType, String downloadUrl, long fileSize) {
        this.type = type == null ? TYPE_OTHER : type;
        this.mimeType = mimeType == null ? "" : mimeType;
        this.downloadUrl = downloadUrl == null ? "" : downloadUrl;
        this.fileSize = fileSize;
    }

    public String getType() {
        return type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public static String fromMimeAndUrl(String mimeType, String url) {
        String mime = mimeType == null ? "" : mimeType.toLowerCase();
        String lowerUrl = url == null ? "" : url.toLowerCase();
        if (mime.contains("epub") || lowerUrl.contains(".epub")) {
            return TYPE_EPUB;
        }
        if (mime.contains("pdf") || lowerUrl.contains(".pdf")) {
            return TYPE_PDF;
        }
        if (mime.contains("mobipocket") || lowerUrl.contains(".mobi")) {
            return TYPE_MOBI;
        }
        if (lowerUrl.contains(".azw3")) {
            return TYPE_AZW3;
        }
        if (mime.startsWith("text/plain") || lowerUrl.contains(".txt")) {
            return TYPE_TXT;
        }
        if (mime.contains("html") || lowerUrl.contains(".html") || lowerUrl.contains(".htm")) {
            return TYPE_HTML;
        }
        return TYPE_OTHER;
    }
}
