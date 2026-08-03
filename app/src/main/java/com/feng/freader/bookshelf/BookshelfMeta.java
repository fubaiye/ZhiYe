package com.feng.freader.bookshelf;

public class BookshelfMeta {
    private String novelUrl = "";
    private String category = "";
    private boolean pinned;
    private boolean favorite;
    private long recentAt;

    public String getNovelUrl() {
        return novelUrl == null ? "" : novelUrl;
    }

    public void setNovelUrl(String novelUrl) {
        this.novelUrl = novelUrl;
    }

    public String getCategory() {
        return category == null ? "" : category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public long getRecentAt() {
        return recentAt;
    }

    public void setRecentAt(long recentAt) {
        this.recentAt = recentAt;
    }
}
