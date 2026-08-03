package com.feng.freader.online;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.URLEncoder;

public class StandardEbooksSource extends AbstractOnlineBookSource {
    private static final String BASE = "https://standardebooks.org";
    private final OnlineHttpClient httpClient = new OnlineHttpClient();

    public StandardEbooksSource() {
        super("standardebooks", "Standard Ebooks", SourceType.API, BASE,
                "高质量公版开放 EPUB，来自 Standard Ebooks；公开页面内容采用 CC0 公共领域奉献", true);
    }

    @Override
    public BookPage getHome() throws Exception {
        return parseList(BASE + "/ebooks?sort=newest", 1);
    }

    @Override
    public BookPage search(String keyword, int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        String url = BASE + "/ebooks?query=" + URLEncoder.encode(keyword.trim(), "UTF-8")
                + "&page=" + Math.max(1, page);
        return parseList(url, Math.max(1, page));
    }

    @Override
    public OnlineBook getBookDetail(String bookId) throws Exception {
        String url = bookId.startsWith("http") ? bookId : BASE + bookId;
        Document document = Jsoup.parse(httpClient.get(url), url);
        String title = text(document.selectFirst("article.ebook h1[property=schema:name]"));
        OnlineBook book = new OnlineBook(url, getId(), getName(), title);
        Elements authors = document.select("[property=schema:author] [property=schema:name]");
        for (Element author : authors) {
            book.addAuthor(author.text());
        }
        Element desc = document.selectFirst("meta[property=schema:description]");
        if (desc != null) {
            book.setDescription(desc.attr("content"));
        } else {
            book.setDescription(text(document.selectFirst("section#description p")));
        }
        Element image = document.selectFirst("meta[property=og:image], section#read-free meta[property=schema:image]");
        if (image != null) {
            book.setCoverUrl(image.attr("content"));
        }
        Element lang = document.selectFirst("meta[property=schema:inLanguage]");
        if (lang != null) {
            book.setLanguage(lang.attr("content"));
        }
        Element date = document.selectFirst("meta[property=schema:datePublished]");
        if (date != null) {
            book.setPublishedAt(date.attr("content"));
        }
        book.setPublisher("Standard Ebooks");
        book.setDetailUrl(url);
        book.setLicenseNote(getLicenseNote());
        for (Element tag : document.select("ul.tags a")) {
            book.addSubject(tag.text());
        }
        for (Element link : document.select("section#download a[href]")) {
            String href = link.absUrl("href");
            String type = BookFormat.fromMimeAndUrl("", href);
            if (BookFormat.TYPE_EPUB.equals(type) || BookFormat.TYPE_AZW3.equals(type)
                    || BookFormat.TYPE_MOBI.equals(type)) {
                String mime = BookFormat.TYPE_EPUB.equals(type)
                        ? "application/epub+zip"
                        : "application/x-mobipocket-ebook";
                book.addFormat(new BookFormat(type, mime, href, 0));
            }
        }
        return book;
    }

    private BookPage parseList(String url, int page) throws Exception {
        Document document = Jsoup.parse(httpClient.get(url), url);
        java.util.List<OnlineBook> books = new java.util.ArrayList<>();
        for (Element item : document.select("ol.ebooks-list li[typeof=schema:Book]")) {
            Element titleLink = item.selectFirst("a[property=schema:url]");
            Element titleSpan = item.selectFirst("[property=schema:name]");
            if (titleLink == null || titleSpan == null) {
                continue;
            }
            String detail = titleLink.absUrl("href");
            OnlineBook book = new OnlineBook(detail, getId(), getName(), titleSpan.text());
            Element author = item.selectFirst(".author [property=schema:name]");
            if (author != null) {
                book.addAuthor(author.text());
            }
            Element cover = item.selectFirst("img[property=schema:image], img");
            if (cover != null) {
                book.setCoverUrl(cover.absUrl("src"));
            }
            book.setDetailUrl(detail);
            book.setLanguage("en");
            book.setLicenseNote(getLicenseNote());
            book.addFormat(new BookFormat(BookFormat.TYPE_EPUB, "application/epub+zip",
                    detail + "/downloads/" + slug(detail) + ".epub", 0));
            books.add(book);
        }
        boolean hasMore = document.selectFirst("nav.pagination a[href]:contains(Next)") != null;
        return new BookPage(books, page, hasMore, "");
    }

    private static String slug(String detailUrl) {
        int idx = detailUrl.lastIndexOf("/ebooks/");
        String path = idx >= 0 ? detailUrl.substring(idx + 8) : detailUrl;
        return path.replace('/', '_');
    }

    private static String text(Element element) {
        return element == null ? "" : element.text();
    }
}
