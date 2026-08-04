package com.feng.freader.source;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;

public class SourceTemplate {
    private SourceTemplate() {
    }

    public static String render(String template, BookSource source, String keyword, int page) {
        String result = template == null ? "" : template;
        result = result.replace("{{keyword}}", encode(keyword));
        result = result.replace("${keyword}", encode(keyword));
        result = result.replace("{{key}}", encode(keyword));
        result = result.replace("${key}", encode(keyword));
        result = result.replace("{{keyword.raw}}", value(keyword));
        result = result.replace("${keyword.raw}", value(keyword));
        result = result.replace("{{key.raw}}", value(keyword));
        result = result.replace("${key.raw}", value(keyword));
        result = result.replace("{{page}}", String.valueOf(page));
        result = result.replace("${page}", String.valueOf(page));
        for (Map.Entry<String, String> entry : source.getVariables().entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", value(entry.getValue()));
            result = result.replace("${" + entry.getKey() + "}", value(entry.getValue()));
        }
        return result;
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value(value), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value(value);
        }
    }

    private static String value(String value) {
        return value == null ? "" : value;
    }
}
