package com.feng.freader.source;

import com.google.gson.JsonElement;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceTemplateEvaluator {
    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{([\\s\\S]*?)}}");
    private static final Pattern BASE_URL_MATCH =
            Pattern.compile("baseUrl\\.match\\(/(.+?)/\\)\\[(\\d+)]");

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

        BookSource source = context.getSource();
        if (source != null && source.getVariables().containsKey(expression)) {
            return source.getVariables().get(expression);
        }

        throw new IOException("暂不支持的模板表达式：" + expression);
    }
}
