package com.feng.freader.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;

public class WikisourceOnlineSource extends AbstractOnlineBookSource {
    private static final String API = "https://zh.wikisource.org/w/api.php";
    private final OnlineHttpClient httpClient = new OnlineHttpClient();

    public WikisourceOnlineSource() {
        super("wikisource_zh", "Wikisource / 维基文库", SourceType.MEDIAWIKI,
                "https://zh.wikisource.org", "中文维基文库开放内容，按页面许可开放访问", true);
    }

    @Override
    public BookPage getHome() throws Exception {
        return search("小说", 1);
    }

    @Override
    public BookPage search(String keyword, int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        String url = API + "?action=query&list=search&format=json&srlimit=20&sroffset="
                + ((Math.max(1, page) - 1) * 20)
                + "&srsearch=" + URLEncoder.encode(keyword.trim(), "UTF-8");
        JsonObject root = new JsonParser().parse(httpClient.get(url)).getAsJsonObject();
        JsonArray results = root.getAsJsonObject("query").getAsJsonArray("search");
        java.util.List<OnlineBook> books = new java.util.ArrayList<>();
        for (JsonElement element : results) {
            JsonObject result = element.getAsJsonObject();
            String title = value(result, "title");
            OnlineBook book = new OnlineBook(title, getId(), getName(), title);
            book.setDescription(stripHtml(value(result, "snippet")));
            String pageUrl = getBaseUrl() + "/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8");
            book.setDetailUrl(pageUrl);
            book.setLanguage("zh");
            book.setLicenseNote(getLicenseNote());
            book.addFormat(new BookFormat(BookFormat.TYPE_HTML, "text/html", pageUrl, 0));
            books.add(book);
        }
        return new BookPage(books, Math.max(1, page), results.size() == 20, "");
    }

    @Override
    public OnlineBook getBookDetail(String bookId) throws Exception {
        String title = bookId;
        int wiki = title.indexOf("/wiki/");
        if (wiki >= 0) {
            title = java.net.URLDecoder.decode(title.substring(wiki + 6), "UTF-8").replace('_', ' ');
        }
        String url = API + "?action=parse&format=json&prop=text|categories&page="
                + URLEncoder.encode(title, "UTF-8");
        JsonObject root = new JsonParser().parse(httpClient.get(url)).getAsJsonObject();
        JsonObject parse = root.getAsJsonObject("parse");
        OnlineBook book = new OnlineBook(title, getId(), getName(), value(parse, "title"));
        JsonObject text = parse.getAsJsonObject("text");
        book.setDescription(stripHtml(value(text, "*")));
        book.setDetailUrl(getBaseUrl() + "/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8"));
        book.setLanguage("zh");
        book.setLicenseNote(getLicenseNote());
        book.addFormat(new BookFormat(BookFormat.TYPE_HTML, "text/html", book.getDetailUrl(), 0));
        return book;
    }

    private static String stripHtml(String value) {
        return value == null ? "" : value.replaceAll("<[^>]+>", "").replace("&quot;", "\"").trim();
    }

    private static String value(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }
}
