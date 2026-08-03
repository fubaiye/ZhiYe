package com.feng.freader.online;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URLEncoder;

public class InternetArchiveSource extends AbstractOnlineBookSource {
    private static final String BASE = "https://archive.org";
    private final OnlineHttpClient httpClient = new OnlineHttpClient();

    public InternetArchiveSource() {
        super("internetarchive", "Internet Archive", SourceType.API, BASE,
                "开放获取或公开下载资源，来自 Internet Archive；不绕过借阅、登录或 DRM", true);
    }

    @Override
    public BookPage getHome() throws Exception {
        return search("public domain texts", 1);
    }

    @Override
    public BookPage search(String keyword, int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        String query = "mediatype:texts AND (" + keyword.trim() + ")";
        String url = BASE + "/advancedsearch.php?q=" + URLEncoder.encode(query, "UTF-8")
                + "&fl[]=identifier&fl[]=title&fl[]=creator&fl[]=language&fl[]=date&fl[]=description"
                + "&fl[]=subject&rows=20&page=" + Math.max(1, page) + "&output=json";
        JsonObject root = new JsonParser().parse(httpClient.get(url)).getAsJsonObject();
        JsonArray docs = root.getAsJsonObject("response").getAsJsonArray("docs");
        java.util.List<OnlineBook> books = new java.util.ArrayList<>();
        for (JsonElement element : docs) {
            JsonObject doc = element.getAsJsonObject();
            String id = value(doc, "identifier");
            String title = value(doc, "title");
            OnlineBook book = new OnlineBook(id, getId(), getName(), title.isEmpty() ? id : title);
            book.addAuthor(value(doc, "creator"));
            book.setLanguage(value(doc, "language"));
            book.setPublishedAt(value(doc, "date"));
            book.setDescription(value(doc, "description"));
            book.setDetailUrl(BASE + "/details/" + id);
            book.setCoverUrl(BASE + "/services/img/" + id);
            book.setLicenseNote(getLicenseNote());
            addKnownArchiveFormats(book, id);
            books.add(book);
        }
        return new BookPage(books, Math.max(1, page), books.size() == 20, "");
    }

    @Override
    public OnlineBook getBookDetail(String bookId) throws Exception {
        String id = bookId;
        if (id.startsWith(BASE + "/details/")) {
            id = id.substring((BASE + "/details/").length());
        }
        String url = BASE + "/metadata/" + URLEncoder.encode(id, "UTF-8");
        JsonObject root = new JsonParser().parse(httpClient.get(url)).getAsJsonObject();
        JsonObject meta = root.getAsJsonObject("metadata");
        OnlineBook book = new OnlineBook(id, getId(), getName(), value(meta, "title"));
        book.addAuthor(value(meta, "creator"));
        book.setDescription(value(meta, "description"));
        book.setLanguage(value(meta, "language"));
        book.setPublishedAt(value(meta, "date"));
        book.setPublisher(value(meta, "publisher"));
        book.setDetailUrl(BASE + "/details/" + id);
        book.setCoverUrl(BASE + "/services/img/" + id);
        book.setLicenseNote(getLicenseNote());
        JsonArray files = root.getAsJsonArray("files");
        if (files != null) {
            for (JsonElement item : files) {
                JsonObject file = item.getAsJsonObject();
                String name = value(file, "name");
                String format = value(file, "format").toLowerCase();
                String mime = value(file, "source");
                String download = BASE + "/download/" + id + "/" + name;
                if (format.contains("epub") || name.toLowerCase().endsWith(".epub")) {
                    book.addFormat(new BookFormat(BookFormat.TYPE_EPUB, "application/epub+zip", download, size(file)));
                } else if (format.contains("pdf") || name.toLowerCase().endsWith(".pdf")) {
                    book.addFormat(new BookFormat(BookFormat.TYPE_PDF, "application/pdf", download, size(file)));
                } else if (format.contains("text") || name.toLowerCase().endsWith(".txt")) {
                    book.addFormat(new BookFormat(BookFormat.TYPE_TXT, "text/plain", download, size(file)));
                }
            }
        }
        return book;
    }

    private void addKnownArchiveFormats(OnlineBook book, String id) {
        book.addFormat(new BookFormat(BookFormat.TYPE_EPUB, "application/epub+zip",
                BASE + "/download/" + id + "/" + id + ".epub", 0));
        book.addFormat(new BookFormat(BookFormat.TYPE_PDF, "application/pdf",
                BASE + "/download/" + id + "/" + id + ".pdf", 0));
    }

    private static long size(JsonObject object) {
        try {
            return Long.parseLong(value(object, "size"));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static String value(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        JsonElement value = object.get(key);
        if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            return array.size() == 0 ? "" : array.get(0).getAsString();
        }
        return value.getAsString();
    }
}
