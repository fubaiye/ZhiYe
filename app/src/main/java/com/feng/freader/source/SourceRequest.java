package com.feng.freader.source;

import java.util.LinkedHashMap;
import java.util.Map;

public class SourceRequest {
    private String url = "";
    private String method = "GET";
    private String body = "";
    private String contentType = "";
    private final Map<String, String> headers = new LinkedHashMap<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url == null ? "" : url;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method == null ? "GET" : method.trim().toUpperCase();
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body == null ? "" : body;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType == null ? "" : contentType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }
}
