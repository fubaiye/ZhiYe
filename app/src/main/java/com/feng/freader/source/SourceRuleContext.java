package com.feng.freader.source;

import com.google.gson.JsonElement;

public class SourceRuleContext {
    private final BookSource source;
    private final String baseUrl;
    private final JsonElement jsonContext;

    public SourceRuleContext(BookSource source, String baseUrl, JsonElement jsonContext) {
        this.source = source;
        this.baseUrl = baseUrl == null ? "" : baseUrl;
        this.jsonContext = jsonContext;
    }

    public BookSource getSource() {
        return source;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public JsonElement getJsonContext() {
        return jsonContext;
    }
}
