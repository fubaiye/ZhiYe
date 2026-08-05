package com.feng.freader.source;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Map;

public final class SourceRequestParser {
    private SourceRequestParser() {
    }

    public static SourceRequest parse(String rawValue, SourceRuleContext context) throws IOException {
        if (rawValue == null || rawValue.trim().length() == 0) {
            throw new IOException("章节请求地址为空");
        }
        String rendered = SourceTemplateEvaluator.render(rawValue.trim(), context);
        SourceRequest request = new SourceRequest();
        int separator = findOptionsSeparator(rendered);
        if (separator < 0) {
            request.setUrl(rendered);
            request.setMethod("GET");
            return request;
        }

        request.setUrl(rendered.substring(0, separator).trim());
        String optionText = rendered.substring(separator + 1).trim();
        try {
            JsonObject options = new JsonParser().parse(optionText).getAsJsonObject();
            if (options.has("method")) {
                request.setMethod(options.get("method").getAsString());
            }
            if (options.has("body")) {
                JsonElement body = options.get("body");
                request.setBody(body.isJsonPrimitive() ? body.getAsString() : body.toString());
            }
            if (options.has("headers") && options.get("headers").isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry
                        : options.getAsJsonObject("headers").entrySet()) {
                    request.getHeaders().put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            if (options.has("contentType")) {
                request.setContentType(options.get("contentType").getAsString());
            }
        } catch (Throwable throwable) {
            throw new IOException("章节请求配置解析失败：" + optionText, throwable);
        }
        return request;
    }

    static int findOptionsSeparator(String value) {
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < value.length() - 1; i++) {
            char ch = value.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (ch == '\\') {
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                continue;
            }
            if (!inString && ch == ',' && value.charAt(i + 1) == '{') {
                return i;
            }
        }
        return -1;
    }
}
