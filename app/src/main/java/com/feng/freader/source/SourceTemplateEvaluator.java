package com.feng.freader.source;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceTemplateEvaluator {
    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{([\\s\\S]*?)}}");
    private static final Pattern BASE_URL_MATCH =
            Pattern.compile("baseUrl\\.match\\(/(.+?)/\\)\\[(\\d+)]");
    private static final Pattern JAVA_HELPER =
            Pattern.compile("java\\.(encodeURI|base64)\\((.*?)\\)");
    private static final char[] BASE64 =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    private SourceTemplateEvaluator() {
    }

    public static String render(String template, SourceRuleContext context) throws IOException {
        if (template == null) {
            return "";
        }
        SourceRuleContext safeContext = context == null
                ? new SourceRuleContext(null, "", null)
                : context;
        Matcher matcher = TEMPLATE.matcher(template);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String expression = matcher.group(1).trim();
            matcher.appendReplacement(output,
                    Matcher.quoteReplacement(evaluate(expression, safeContext)));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    private static String evaluate(String expression, SourceRuleContext context) throws IOException {
        if (expression.startsWith("$")) {
            JsonElement json = context.getJsonContext();
            if (json == null) {
                throw new IOException("模板缺少 JSON 上下文：" + expression);
            }
            List<JsonElement> values = RuleEvaluator.selectJsonElements(json, expression);
            if (values.isEmpty()) {
                throw new IOException("JSON 模板未匹配：" + expression);
            }
            JsonElement value = values.get(0);
            return value == null || value.isJsonNull()
                    ? ""
                    : (value.isJsonPrimitive() ? value.getAsString() : value.toString());
        }

        Matcher baseUrlMatcher = BASE_URL_MATCH.matcher(expression);
        if (baseUrlMatcher.matches()) {
            String regex = baseUrlMatcher.group(1).replace("\\/", "/");
            int groupIndex = Integer.parseInt(baseUrlMatcher.group(2));
            Matcher urlMatcher = Pattern.compile(regex).matcher(context.getBaseUrl());
            if (!urlMatcher.find() || groupIndex > urlMatcher.groupCount()) {
                throw new IOException("baseUrl.match 未匹配：" + expression
                        + "，baseUrl=" + context.getBaseUrl());
            }
            return urlMatcher.group(groupIndex);
        }

        Matcher javaHelperMatcher = JAVA_HELPER.matcher(expression);
        if (javaHelperMatcher.matches()) {
            String helper = javaHelperMatcher.group(1);
            String argument = resolveValue(javaHelperMatcher.group(2).trim(), context);
            if ("encodeURI".equals(helper)) {
                return encodeUri(argument);
            }
            if ("base64".equals(helper)) {
                return base64(argument);
            }
        }

        if ("baseUrl".equals(expression)) {
            return context.getBaseUrl();
        }
        if ("host".equals(expression)) {
            return host(context.getBaseUrl());
        }

        String resolved = resolveVariable(expression, context);
        if (resolved != null) {
            return resolved;
        }

        throw new IOException("暂不支持的模板表达式：" + expression);
    }

    private static String resolveValue(String expression, SourceRuleContext context) throws IOException {
        if (expression == null || expression.length() == 0) {
            return "";
        }
        if ((expression.startsWith("'") && expression.endsWith("'"))
                || (expression.startsWith("\"") && expression.endsWith("\""))) {
            return expression.substring(1, expression.length() - 1);
        }
        String resolved = resolveVariable(expression, context);
        if (resolved != null) {
            return resolved;
        }
        if ("baseUrl".equals(expression)) {
            return context.getBaseUrl();
        }
        if ("host".equals(expression)) {
            return host(context.getBaseUrl());
        }
        if (expression.startsWith("$")) {
            return evaluate(expression, context);
        }
        return expression;
    }

    private static String resolveVariable(String expression, SourceRuleContext context) {
        BookSource source = context.getSource();
        if (source != null && source.getVariables().containsKey(expression)) {
            return source.getVariables().get(expression);
        }
        return null;
    }

    private static String host(String baseUrl) {
        if (baseUrl == null || baseUrl.length() == 0) {
            return "";
        }
        try {
            URI uri = URI.create(baseUrl);
            return uri.getHost() == null ? "" : uri.getHost();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String encodeUri(String value) throws IOException {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8")
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new IOException(e);
        }
    }

    private static String base64(String value) {
        byte[] input;
        try {
            input = (value == null ? "" : value).getBytes("UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            input = new byte[0];
        }
        StringBuilder output = new StringBuilder((input.length + 2) / 3 * 4);
        for (int i = 0; i < input.length; i += 3) {
            int b0 = input[i] & 0xff;
            int b1 = i + 1 < input.length ? input[i + 1] & 0xff : 0;
            int b2 = i + 2 < input.length ? input[i + 2] & 0xff : 0;
            output.append(BASE64[b0 >>> 2]);
            output.append(BASE64[((b0 & 0x03) << 4) | (b1 >>> 4)]);
            output.append(i + 1 < input.length ? BASE64[((b1 & 0x0f) << 2) | (b2 >>> 6)] : '=');
            output.append(i + 2 < input.length ? BASE64[b2 & 0x3f] : '=');
        }
        return output.toString();
    }
}
