package com.feng.freader.online;

import java.net.URLEncoder;

public class OpdsBookSource extends AbstractOnlineBookSource {
    private final String homeUrl;
    private final String searchTemplate;
    private final OnlineHttpClient httpClient;
    private final OpdsParser parser = new OpdsParser();

    public OpdsBookSource(String id, String name, String baseUrl, String homeUrl,
                          String searchTemplate, String licenseNote) {
        this(id, name, baseUrl, homeUrl, searchTemplate, licenseNote, new OnlineHttpClient());
    }

    OpdsBookSource(String id, String name, String baseUrl, String homeUrl,
                   String searchTemplate, String licenseNote, OnlineHttpClient httpClient) {
        super(id, name, SourceType.OPDS, baseUrl, licenseNote, true);
        this.homeUrl = homeUrl;
        this.searchTemplate = searchTemplate == null ? "" : searchTemplate;
        this.httpClient = httpClient;
    }

    @Override
    public BookPage getHome() throws Exception {
        String xml = httpClient.get(homeUrl);
        return parser.parse(homeUrl, xml, getId(), getName());
    }

    @Override
    public BookPage search(String keyword, int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        if (searchTemplate.isEmpty()) {
            return BookPage.empty();
        }
        String url = searchTemplate.replace("{searchTerms}",
                URLEncoder.encode(keyword.trim(), "UTF-8"))
                .replace("{page}", String.valueOf(Math.max(1, page)));
        String xml = httpClient.get(url);
        return parser.parse(url, xml, getId(), getName());
    }

    @Override
    public OnlineBook getBookDetail(String bookId) throws Exception {
        BookPage page = getHome();
        for (OnlineBook book : page.getItems()) {
            if (book.getId().equals(bookId) || book.getDetailUrl().equals(bookId)) {
                return book;
            }
        }
        return null;
    }
}
