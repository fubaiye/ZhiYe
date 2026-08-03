package com.feng.freader.entity.data;

/**
 * @author Feng Zhaohao
 * Created on 2019/11/9
 */
public class NovelSourceData {
    private String name;
    private String author;
    private String introduce;
    private String url;
    private String cover;
    private String sourceId;
    private String sourceName;

    public NovelSourceData(String name, String author,
                           String introduce, String url, String cover) {
        this.name = name;
        this.author = author;
        this.introduce = introduce;
        this.url = url;
        this.cover = cover;
    }

    public String getName() {
        return empty(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAuthor() {
        return empty(author);
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getIntroduce() {
        return empty(introduce);
    }

    public void setIntroduce(String introduce) {
        this.introduce = introduce;
    }

    public String getUrl() {
        return empty(url);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCover() {
        return empty(cover);
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getSourceId() {
        return empty(sourceId);
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public String getSourceName() {
        return empty(sourceName);
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    @Override
    public String toString() {
        return "NovelSourceData{" +
                "name='" + name + '\'' +
                ", author='" + author + '\'' +
                ", introduce='" + introduce + '\'' +
                ", url='" + url + '\'' +
                ", cover='" + cover + '\'' +
                ", sourceId='" + sourceId + '\'' +
                ", sourceName='" + sourceName + '\'' +
                '}';
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
