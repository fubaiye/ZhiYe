package com.feng.freader.online;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnlineBook {
    private final String id;
    private final String sourceId;
    private final String sourceName;
    private final String title;
    private final List<String> authors = new ArrayList<>();
    private String description = "";
    private String coverUrl = "";
    private String language = "";
    private String publisher = "";
    private String publishedAt = "";
    private String detailUrl = "";
    private String licenseNote = "";
    private final List<String> subjects = new ArrayList<>();
    private final List<BookFormat> formats = new ArrayList<>();

    public OnlineBook(String id, String sourceId, String sourceName, String title) {
        this.id = id == null ? "" : id;
        this.sourceId = sourceId == null ? "" : sourceId;
        this.sourceName = sourceName == null ? "" : sourceName;
        this.title = title == null ? "" : title;
    }

    public String getId() {
        return id;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getAuthors() {
        return Collections.unmodifiableList(authors);
    }

    public void addAuthor(String author) {
        if (author != null && !author.trim().isEmpty() && !authors.contains(author.trim())) {
            authors.add(author.trim());
        }
    }

    public String getAuthorText() {
        if (authors.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String author : authors) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(author);
        }
        return builder.toString();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl == null ? "" : coverUrl;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language == null ? "" : language;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher == null ? "" : publisher;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt == null ? "" : publishedAt;
    }

    public String getDetailUrl() {
        return detailUrl;
    }

    public void setDetailUrl(String detailUrl) {
        this.detailUrl = detailUrl == null ? "" : detailUrl;
    }

    public String getLicenseNote() {
        return licenseNote;
    }

    public void setLicenseNote(String licenseNote) {
        this.licenseNote = licenseNote == null ? "" : licenseNote;
    }

    public List<String> getSubjects() {
        return Collections.unmodifiableList(subjects);
    }

    public void addSubject(String subject) {
        if (subject != null && !subject.trim().isEmpty() && !subjects.contains(subject.trim())) {
            subjects.add(subject.trim());
        }
    }

    public List<BookFormat> getFormats() {
        return Collections.unmodifiableList(formats);
    }

    public void addFormat(BookFormat format) {
        if (format == null || format.getDownloadUrl().isEmpty()) {
            return;
        }
        for (BookFormat existing : formats) {
            if (existing.getDownloadUrl().equals(format.getDownloadUrl())) {
                return;
            }
        }
        formats.add(format);
    }
}
