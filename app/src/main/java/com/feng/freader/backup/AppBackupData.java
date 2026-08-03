package com.feng.freader.backup;

import com.feng.freader.entity.data.BookshelfNovelDbData;

import java.util.ArrayList;
import java.util.List;

public class AppBackupData {
    private int version = 1;
    private String sourcesJson = "";
    private List<BookshelfNovelDbData> bookshelf = new ArrayList<>();

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getSourcesJson() {
        return sourcesJson == null ? "" : sourcesJson;
    }

    public void setSourcesJson(String sourcesJson) {
        this.sourcesJson = sourcesJson;
    }

    public List<BookshelfNovelDbData> getBookshelf() {
        return bookshelf == null ? new ArrayList<BookshelfNovelDbData>() : bookshelf;
    }

    public void setBookshelf(List<BookshelfNovelDbData> bookshelf) {
        this.bookshelf = bookshelf;
    }
}
