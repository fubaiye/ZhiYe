package com.feng.freader.reader;

public class ReaderRecord {
    public static final String BOOKMARK = "bookmark";
    public static final String NOTE = "note";
    public static final String HIGHLIGHT = "highlight";

    private String type = "";
    private String bookUrl = "";
    private String bookName = "";
    private String text = "";
    private String progress = "";
    private int chapterIndex;
    private int position;
    private int secondPosition;
    private long createdAt;

    public ReaderRecord() {
    }

    public ReaderRecord(String type, String bookUrl, String bookName, String text,
                        String progress, int chapterIndex, int position, int secondPosition,
                        long createdAt) {
        this.type = type;
        this.bookUrl = bookUrl;
        this.bookName = bookName;
        this.text = text;
        this.progress = progress;
        this.chapterIndex = chapterIndex;
        this.position = position;
        this.secondPosition = secondPosition;
        this.createdAt = createdAt;
    }

    public String getType() {
        return safe(type);
    }

    public String getBookUrl() {
        return safe(bookUrl);
    }

    public String getBookName() {
        return safe(bookName);
    }

    public String getText() {
        return safe(text);
    }

    public String getProgress() {
        return safe(progress);
    }

    public int getChapterIndex() {
        return chapterIndex;
    }

    public int getPosition() {
        return position;
    }

    public int getSecondPosition() {
        return secondPosition;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public String formatPosition() {
        return "第" + (chapterIndex + 1) + "章 " + getProgress();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
