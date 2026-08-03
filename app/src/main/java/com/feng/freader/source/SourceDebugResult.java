package com.feng.freader.source;

import java.util.ArrayList;
import java.util.List;

public class SourceDebugResult {
    private String url = "";
    private String html = "";
    private String headers = "";
    private String cookies = "";
    private String cssResult = "";
    private String xpathResult = "";
    private String jsonPathResult = "";
    private long executionTimeMs;
    private final List<String> errors = new ArrayList<>();

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = safe(url);
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = safe(html);
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = safe(headers);
    }

    public String getCookies() {
        return cookies;
    }

    public void setCookies(String cookies) {
        this.cookies = safe(cookies);
    }

    public String getCssResult() {
        return cssResult;
    }

    public void setCssResult(String cssResult) {
        this.cssResult = safe(cssResult);
    }

    public String getXpathResult() {
        return xpathResult;
    }

    public void setXpathResult(String xpathResult) {
        this.xpathResult = safe(xpathResult);
    }

    public String getJsonPathResult() {
        return jsonPathResult;
    }

    public void setJsonPathResult(String jsonPathResult) {
        this.jsonPathResult = safe(jsonPathResult);
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public List<String> getErrors() {
        return errors;
    }

    public String toDisplayText() {
        StringBuilder builder = new StringBuilder();
        builder.append("URL\n").append(url).append("\n\n");
        builder.append("Execution Time\n").append(executionTimeMs).append(" ms\n\n");
        builder.append("Headers\n").append(headers).append("\n\n");
        builder.append("Cookies\n").append(cookies).append("\n\n");
        builder.append("CSS Result\n").append(cssResult).append("\n\n");
        builder.append("XPath Result\n").append(xpathResult).append("\n\n");
        builder.append("JSONPath Result\n").append(jsonPathResult).append("\n\n");
        if (!errors.isEmpty()) {
            builder.append("Errors\n").append(errors).append("\n\n");
        }
        builder.append("HTML/JSON\n").append(limit(html, 8000));
        return builder.toString();
    }

    private static String limit(String value, int max) {
        String text = safe(value);
        return text.length() <= max ? text : text.substring(0, max) + "\n...";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
