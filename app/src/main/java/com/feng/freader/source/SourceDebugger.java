package com.feng.freader.source;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Cookie;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SourceDebugger {
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public SourceDebugResult run(String url, String cssRule, String xpathRule, String jsonPathRule)
            throws IOException {
        long start = System.currentTimeMillis();
        SourceDebugResult result = new SourceDebugResult();
        result.setUrl(url);
        Response response = client.newCall(new Request.Builder().url(url).get().build()).execute();
        try {
            result.setHeaders(response.headers().toString());
            result.setCookies(cookieText(Cookie.parseAll(HttpUrl.parse(url), response.headers())));
            String body = response.body() == null ? "" : response.body().string();
            result.setHtml(body);
            evaluateRules(body, cssRule, xpathRule, jsonPathRule, result);
        } finally {
            response.close();
            result.setExecutionTimeMs(System.currentTimeMillis() - start);
        }
        return result;
    }

    private void evaluateRules(String body, String cssRule, String xpathRule, String jsonPathRule,
                               SourceDebugResult result) {
        try {
            result.setCssResult(RuleEvaluator.eval(body, "css:" + safe(cssRule)));
        } catch (Throwable t) {
            result.getErrors().add("CSS " + t.getMessage());
        }
        try {
            result.setXpathResult(RuleEvaluator.eval(body, "xpath:" + safe(xpathRule)));
        } catch (Throwable t) {
            result.getErrors().add("XPath " + t.getMessage());
        }
        try {
            result.setJsonPathResult(RuleEvaluator.eval(body, "jsonpath:" + safe(jsonPathRule)));
        } catch (Throwable t) {
            result.getErrors().add("JSONPath " + t.getMessage());
        }
    }

    private String cookieText(List<Cookie> cookies) {
        StringBuilder builder = new StringBuilder();
        for (Cookie cookie : cookies) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(cookie.name()).append('=').append(cookie.value());
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
