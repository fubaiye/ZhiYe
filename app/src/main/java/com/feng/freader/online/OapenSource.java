package com.feng.freader.online;

import java.net.URLEncoder;

public class OapenSource extends AbstractOnlineBookSource {
    private static final String BASE = "https://library.oapen.org";
    private final OnlineHttpClient httpClient = new OnlineHttpClient();

    public OapenSource() {
        super("oapen", "OAPEN", SourceType.API, BASE,
                "开放获取学术图书，来自 OAPEN；仅提供公开访问下载", true);
    }

    @Override
    public BookPage getHome() throws Exception {
        return search("open access", 1);
    }

    @Override
    public BookPage search(String keyword, int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        String url = BASE + "/discover?query=" + URLEncoder.encode(keyword.trim(), "UTF-8");
        OnlineBook book = new OnlineBook("oapen-search-" + keyword.trim(), getId(), getName(),
                "OAPEN: " + keyword.trim());
        book.setDescription("OAPEN 提供开放获取学术图书。点击详情在来源站点查看出版社、年份、语言、学科和许可。");
        book.setDetailUrl(url);
        book.setLanguage("multi");
        book.setLicenseNote(getLicenseNote());
        book.addFormat(new BookFormat(BookFormat.TYPE_HTML, "text/html", url, 0));
        java.util.List<OnlineBook> books = new java.util.ArrayList<>();
        books.add(book);
        return new BookPage(books, Math.max(1, page), false, "");
    }

    @Override
    public OnlineBook getBookDetail(String bookId) throws Exception {
        return search(bookId.replace("oapen-search-", ""), 1).getItems().get(0);
    }
}
