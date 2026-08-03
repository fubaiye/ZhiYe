package com.feng.freader.online;

public class OnlineDownloadRecord {
    private final String sourceId;
    private final String sourceBookId;
    private final String sourceName;
    private final String originalDownloadUrl;
    private final String localFilePath;
    private final String fileHash;
    private final long downloadedAt;

    public OnlineDownloadRecord(String sourceId, String sourceBookId, String sourceName,
                                String originalDownloadUrl, String localFilePath, String fileHash) {
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceBookId = sourceBookId == null ? "" : sourceBookId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.originalDownloadUrl = originalDownloadUrl == null ? "" : originalDownloadUrl;
        this.localFilePath = localFilePath == null ? "" : localFilePath;
        this.fileHash = fileHash == null ? "" : fileHash;
        this.downloadedAt = System.currentTimeMillis();
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getOriginalDownloadUrl() {
        return originalDownloadUrl;
    }
}
