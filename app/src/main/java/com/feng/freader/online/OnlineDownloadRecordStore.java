package com.feng.freader.online;

import java.util.ArrayList;
import java.util.List;

public class OnlineDownloadRecordStore {
    private final List<OnlineDownloadRecord> records = new ArrayList<>();

    public synchronized void add(OnlineDownloadRecord record) {
        if (record != null && !exists(record.getSourceId(), record.getOriginalDownloadUrl())) {
            records.add(record);
        }
    }

    public synchronized boolean exists(String sourceId, String originalDownloadUrl) {
        for (OnlineDownloadRecord record : records) {
            if (record.getSourceId().equals(sourceId)
                    && record.getOriginalDownloadUrl().equals(originalDownloadUrl)) {
                return true;
            }
        }
        return false;
    }
}
