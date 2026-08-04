package com.feng.freader.source;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BookSourceParser {
    private static final Gson GSON = new Gson();

    private BookSourceParser() {
    }

    public static List<BookSource> parseList(String json) {
        List<BookSource> converted = parseCompatibleList(json);
        if (!converted.isEmpty()) {
            return converted;
        }
        try {
            Type type = new TypeToken<List<BookSource>>() {
            }.getType();
            List<BookSource> sources = GSON.fromJson(json, type);
            return sources == null ? new ArrayList<BookSource>() : sources;
        } catch (Throwable ignored) {
            List<BookSource> one = new ArrayList<>();
            BookSource source = parseOne(json);
            if (source != null && !source.getName().isEmpty()) {
                one.add(source);
            }
            return one;
        }
    }

    public static BookSource parseOne(String json) {
        try {
            JsonElement element = new JsonParser().parse(json);
            BookSource source = parseElement(element);
            if (source != null) {
                return source;
            }
        } catch (Throwable ignored) {
        }
        try {
            return GSON.fromJson(json, BookSource.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String toJson(List<BookSource> sources) {
        return GSON.toJson(sources);
    }

    private static List<BookSource> parseCompatibleList(String json) {
        List<BookSource> sources = new ArrayList<>();
        try {
            JsonElement root = new JsonParser().parse(json);
            if (root == null || root.isJsonNull()) {
                return sources;
            }
            if (root.isJsonArray()) {
                JsonArray array = root.getAsJsonArray();
                for (JsonElement element : array) {
                    BookSource source = parseElement(element);
                    if (source != null && source.getName().length() > 0) {
                        sources.add(source);
                    }
                }
            } else {
                BookSource source = parseElement(root);
                if (source != null && source.getName().length() > 0) {
                    sources.add(source);
                }
            }
        } catch (Throwable ignored) {
        }
        return sources;
    }

    private static BookSource parseElement(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return null;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("bookSourceName") || object.has("bookSourceUrl") || object.has("ruleSearch")) {
            return fromLegado(object);
        }
        BookSource source = GSON.fromJson(object, BookSource.class);
        normalizeInternalSource(source);
        return source;
    }

    private static BookSource fromLegado(JsonObject object) {
        BookSource source = new BookSource();
        String name = string(object, "bookSourceName");
        String baseUrl = string(object, "bookSourceUrl");
        source.setName(name);
        source.setId("legado_" + Integer.toHexString((name + "|" + baseUrl).hashCode()));
        source.setEnabled(false);
        source.setSearchUrl(normalizeSearchUrl(string(object, "searchUrl"), baseUrl));
        source.setSearchMethod(detectMethod(string(object, "searchUrl")));
        source.setSearchBody(extractBody(string(object, "searchUrl")));
        source.setHeaders(readStringMap(object, "headers", "header"));
        source.setCookies(readStringMap(object, "cookies", "cookie"));

        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("host", baseUrl);
        variables.put("source.key", baseUrl);
        variables.put("bookSourceUrl", baseUrl);
        source.setVariables(variables);

        BookSource.Pagination pagination = new BookSource.Pagination();
        pagination.setStart(1);
        pagination.setMax(1);
        source.setPagination(pagination);

        source.setSearchRules(fromLegadoRules(object.get("ruleSearch"), true));
        source.setDetailRules(fromLegadoRules(object.get("ruleBookInfo"), false));
        source.setCatalogRules(fromLegadoRules(object.get("ruleToc"), false));
        source.setContentRules(fromLegadoRules(object.get("ruleContent"), false));
        return source;
    }

    private static void normalizeInternalSource(BookSource source) {
        if (source == null) {
            return;
        }
        if (source.getId().length() == 0 && source.getName().length() > 0) {
            source.setId("source_" + Integer.toHexString(source.getName().hashCode()));
        }
    }

    private static BookSource.SourceRules fromLegadoRules(JsonElement element, boolean search) {
        BookSource.SourceRules rules = new BookSource.SourceRules();
        if (element == null || !element.isJsonObject()) {
            return rules;
        }
        JsonObject object = element.getAsJsonObject();
        String list = normalizeRule(string(object, search ? "bookList" : "chapterList"), false);
        boolean jsonContext = list.startsWith("jsonpath:");
        rules.setList(list);
        rules.setName(normalizeRule(first(object, "name", "bookName", "chapterName", "title"), jsonContext));
        rules.setAuthor(normalizeRule(first(object, "author", "bookAuthor"), jsonContext));
        rules.setIntro(normalizeRule(first(object, "intro", "desc", "description"), jsonContext));
        rules.setUrl(normalizeRule(first(object, "bookUrl", "tocUrl", "chapterUrl", "url"), jsonContext));
        rules.setCover(normalizeRule(first(object, "coverUrl", "cover"), jsonContext));
        rules.setContent(normalizeRule(first(object, "content", "body"), jsonContext));
        rules.setNextPage(normalizeRule(first(object, "nextPage", "nextContentUrl"), jsonContext));
        rules.setJavaScript(jsRule(first(object, "name", "content")));
        return rules;
    }

    private static String normalizeSearchUrl(String raw, String baseUrl) {
        String value = firstAlternative(raw).trim();
        if (value.startsWith("@js:")) {
            return "javascript:" + value.substring(4);
        }
        int comma = value.indexOf(',');
        if (comma > 0 && value.substring(comma).toLowerCase(Locale.US).contains("method")) {
            value = value.substring(0, comma);
        }
        value = value.replace("{{key}}", "{{keyword}}")
                .replace("{{source.key}}", "{{host}}")
                .replace("${key}", "${keyword}")
                .replace("${source.key}", "${host}");
        if (value.startsWith("/") && baseUrl.length() > 0) {
            return joinUrl(baseUrl, value);
        }
        return value;
    }

    private static String detectMethod(String raw) {
        String lower = raw == null ? "" : raw.toLowerCase(Locale.US);
        if (lower.contains("\"method\"") && lower.contains("post")) {
            return "POST";
        }
        return "GET";
    }

    private static String extractBody(String raw) {
        if (raw == null || raw.length() == 0) {
            return "";
        }
        int comma = raw.indexOf(',');
        if (comma < 0 || !raw.substring(comma).toLowerCase(Locale.US).contains("body")) {
            return "";
        }
        try {
            JsonObject object = new JsonParser().parse(raw.substring(comma + 1)).getAsJsonObject();
            return string(object, "body").replace("{{key}}", "{{keyword}}");
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String normalizeRule(String raw, boolean jsonContext) {
        String value = firstAlternative(raw).trim();
        if (value.length() == 0) {
            return "";
        }
        if (value.startsWith("@js:")) {
            return "javascript:" + value.substring(4);
        }
        int transform = value.indexOf("##");
        if (transform >= 0) {
            value = value.substring(0, transform);
        }
        int put = value.indexOf("@put:");
        if (put >= 0) {
            value = value.substring(0, put);
        }
        value = value.replace("@text", "").replace("@Text", "").trim();
        if (value.startsWith("css:") || value.startsWith("xpath:") || value.startsWith("jsonpath:")
                || value.startsWith("javascript:")) {
            return value;
        }
        if (jsonContext || isJsonPathLike(value)) {
            return "jsonpath:" + normalizeJsonPath(value);
        }
        if (value.startsWith("//") || value.startsWith("(//")) {
            return "xpath:" + value;
        }
        return "css:" + value;
    }

    private static boolean isJsonPathLike(String value) {
        return value.startsWith("$.") || value.startsWith("$..") || value.contains("[*]")
                || value.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)*(\\[[0-9]+\\])?");
    }

    private static String normalizeJsonPath(String value) {
        if (value.startsWith("$")) {
            return value;
        }
        if (value.startsWith(".")) {
            return "$" + value;
        }
        return value;
    }

    private static String firstAlternative(String value) {
        if (value == null) {
            return "";
        }
        String[] lines = value.split("\\r?\\n");
        String first = lines.length == 0 ? value : lines[0];
        int index = first.indexOf("||");
        return index >= 0 ? first.substring(0, index) : first;
    }

    private static String jsRule(String raw) {
        if (raw == null) {
            return "";
        }
        int index = raw.indexOf("@js:");
        return index >= 0 ? raw.substring(index + 4) : "";
    }

    private static Map<String, String> readStringMap(JsonObject object, String primary, String fallback) {
        Map<String, String> map = new LinkedHashMap<>();
        JsonElement element = object.has(primary) ? object.get(primary) : object.get(fallback);
        if (element == null || element.isJsonNull()) {
            return map;
        }
        try {
            if (element.isJsonObject()) {
                for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
                return map;
            }
            String text = element.getAsString();
            if (text.trim().startsWith("{")) {
                JsonObject headerObject = new JsonParser().parse(text).getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : headerObject.entrySet()) {
                    map.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Throwable ignored) {
        }
        return map;
    }

    private static String first(JsonObject object, String... keys) {
        for (String key : keys) {
            String value = string(object, key);
            if (value.length() > 0) {
                return value;
            }
        }
        return "";
    }

    private static String string(JsonObject object, String key) {
        try {
            JsonElement element = object.get(key);
            return element == null || element.isJsonNull() ? "" : element.getAsString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String joinUrl(String host, String path) {
        if (host.endsWith("/") && path.startsWith("/")) {
            return host.substring(0, host.length() - 1) + path;
        }
        if (!host.endsWith("/") && !path.startsWith("/")) {
            return host + "/" + path;
        }
        return host + path;
    }
}
