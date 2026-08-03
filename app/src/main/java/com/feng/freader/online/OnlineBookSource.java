package com.feng.freader.online;

public interface OnlineBookSource {
    String getId();

    String getName();

    SourceType getType();

    String getBaseUrl();

    String getLicenseNote();

    boolean isEnabled();

    void setEnabled(boolean enabled);

    BookPage getHome() throws Exception;

    BookPage search(String keyword, int page) throws Exception;

    OnlineBook getBookDetail(String bookId) throws Exception;
}
