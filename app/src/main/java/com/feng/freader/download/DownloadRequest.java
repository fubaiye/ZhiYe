package com.feng.freader.download;

import com.google.gson.Gson;

import java.util.UUID;

public class DownloadRequest {
    private String id;
    private String name;
    private String url;
    private String cover;
    private DownloadState state = DownloadState.QUEUED;
    private int retryCount;
    private int progress;
    private String outputPath;
    private String error;

    public DownloadRequest(String id, String name, String url, String cover) {
        this.id = id == null || id.length() == 0 ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.url = url;
        this.cover = cover;
    }

    public static DownloadRequest fromJson(String json) {
        DownloadRequest request = new Gson().fromJson(json, DownloadRequest.class);
        return request == null ? new DownloadRequest("", "", "", "") : request;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public boolean isTerminal() {
        return state == DownloadState.COMPLETED
                || state == DownloadState.FAILED
                || state == DownloadState.CANCELED;
    }

    public String getId() {
        return id == null ? "" : id;
    }

    public String getName() {
        return name == null ? "" : name;
    }

    public String getUrl() {
        return url == null ? "" : url;
    }

    public String getCover() {
        return cover == null ? "" : cover;
    }

    public DownloadState getState() {
        return state == null ? DownloadState.QUEUED : state;
    }

    public void setState(DownloadState state) {
        this.state = state;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getOutputPath() {
        return outputPath == null ? "" : outputPath;
    }

    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getError() {
        return error == null ? "" : error;
    }

    public void setError(String error) {
        this.error = error;
    }
}
