package com.feng.freader.export;

public enum ExportFormat {
    TXT(".txt", 1),
    EPUB(".epub", 2);

    private final String suffix;
    private final int bookshelfType;

    ExportFormat(String suffix, int bookshelfType) {
        this.suffix = suffix;
        this.bookshelfType = bookshelfType;
    }

    public String getSuffix() {
        return suffix;
    }

    public int getBookshelfType() {
        return bookshelfType;
    }
}
