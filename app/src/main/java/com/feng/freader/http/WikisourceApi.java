package com.feng.freader.http;

import com.feng.freader.entity.data.DetailedChapterData;
import com.feng.freader.entity.data.NovelSourceData;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class WikisourceApi {
    private static final String API_BASE = "https://zh.wikisource.org/w/api.php";
    private static final String PAGE_BASE = "https://zh.wikisource.org/wiki/";
    private static final String SOURCE_PREFIX = "freader-wikisource://page/";

    private WikisourceApi() {
    }

    public static String buildSearchUrl(String query) {
        return API_BASE + "?action=query&list=search&format=json&utf8=1&srlimit=20&srsearch="
                + encode(query);
    }

    public static String buildPageTextUrl(String title) {
        return API_BASE + "?action=parse&prop=text&format=json&utf8=1&page="
                + encode(title);
    }

    public static String toSourceUrl(String title) {
        return SOURCE_PREFIX + encode(title);
    }

    public static boolean isWikisourceUrl(String url) {
        return url != null && url.startsWith(SOURCE_PREFIX);
    }

    public static boolean isWikisourcePageTextUrl(String url) {
        return url != null
                && url.startsWith(API_BASE)
                && url.contains("action=parse")
                && url.contains("prop=text");
    }

    public static String titleFromSourceUrl(String url) {
        if (!isWikisourceUrl(url)) {
            return url;
        }
        return decode(url.substring(SOURCE_PREFIX.length()));
    }

    public static String toBrowserUrl(String sourceUrl) {
        return PAGE_BASE + encode(titleFromSourceUrl(sourceUrl)).replace("+", "%20");
    }

    public static List<NovelSourceData> parseSearchResults(String json) throws Exception {
        List<NovelSourceData> results = new ArrayList<>();
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        JsonObject query = getObject(root, "query");
        if (query == null) {
            return results;
        }
        JsonArray search = getArray(query, "search");
        if (search == null) {
            return results;
        }
        for (JsonElement element : search) {
            JsonObject item = element.getAsJsonObject();
            String title = getString(item, "title");
            if (title == null || title.trim().isEmpty()) {
                continue;
            }
            String snippet = cleanSnippet(getString(item, "snippet"));
            if (snippet.isEmpty()) {
                snippet = "来自维基文库的开放文本。";
            }
            results.add(new NovelSourceData(title, "维基文库", snippet, toSourceUrl(title), ""));
        }
        return results;
    }

    public static DetailedChapterData parsePageText(String json) throws Exception {
        JsonObject root = new JsonParser().parse(json).getAsJsonObject();
        JsonObject parse = getObject(root, "parse");
        if (parse == null) {
            return null;
        }
        String title = getString(parse, "title");
        if (title == null || title.trim().isEmpty()) {
            title = "维基文库";
        }
        JsonObject text = getObject(parse, "text");
        if (text == null) {
            return null;
        }
        String html = getString(text, "*");
        String content = htmlToText(html);
        if (content.trim().isEmpty()) {
            return null;
        }
        return new DetailedChapterData(title, content);
    }

    public static String cleanSnippet(String html) {
        return Jsoup.parse(html == null ? "" : html).text().trim();
    }

    private static String htmlToText(String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        document.select("style,script,sup.reference,.mw-editsection,.noprint,.metadata").remove();
        Elements blocks = document.select("h1,h2,h3,h4,p,li,dd");
        StringBuilder builder = new StringBuilder();
        for (Element block : blocks) {
            String text = block.text().trim();
            if (text.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append("    ").append(text);
        }
        if (builder.length() == 0) {
            builder.append(document.text().trim());
        }
        return builder.toString();
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }
        return object.getAsJsonArray(key);
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
