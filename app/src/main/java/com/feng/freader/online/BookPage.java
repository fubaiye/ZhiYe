package com.feng.freader.online;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BookPage {
    private final List<OnlineBook> items;
    private final int page;
    private final boolean hasMore;
    private final String nextUrl;

    public BookPage(List<OnlineBook> items, int page, boolean hasMore, String nextUrl) {
        this.items = items == null ? new ArrayList<OnlineBook>() : new ArrayList<>(items);
        this.page = page;
        this.hasMore = hasMore;
        this.nextUrl = nextUrl == null ? "" : nextUrl;
    }

    public static BookPage empty() {
        return new BookPage(Collections.<OnlineBook>emptyList(), 1, false, "");
    }

    public List<OnlineBook> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getPage() {
        return page;
    }

    public boolean hasMore() {
        return hasMore;
    }

    public String getNextUrl() {
        return nextUrl;
    }
}
