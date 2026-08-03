package com.feng.freader.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LocalBookMetadata {
    private final String title;
    private final String coverUrl;

    public LocalBookMetadata(String title, String coverUrl) {
        this.title = emptyIfNull(title).trim();
        this.coverUrl = normalizeCoverUrl(coverUrl);
    }

    public static String cleanTitle(String fileName) {
        String title = emptyIfNull(fileName).trim();
        int queryIndex = title.indexOf('?');
        if (queryIndex >= 0) {
            title = title.substring(0, queryIndex);
        }
        int slashIndex = Math.max(title.lastIndexOf('/'), title.lastIndexOf('\\'));
        if (slashIndex >= 0) {
            title = title.substring(slashIndex + 1);
        }
        int dotIndex = title.lastIndexOf('.');
        if (dotIndex > 0) {
            title = title.substring(0, dotIndex);
        }
        title = title.replaceAll("^[0-9]{6,}[_\\-\\s]*", "");
        title = title.replaceAll("(?i)[_\\-\\s]*(精校版|校对版|完结|全集|全本|下载|txt|epub)$", "");
        title = title.replace('_', ' ').replace('-', ' ').trim();
        title = title.replaceAll("\\s+", " ");
        return title.isEmpty() ? emptyIfNull(fileName) : title;
    }

    public static LocalBookMetadata fromGoogleBooksJson(String json) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray items = getArray(root, "items");
            if (items == null) {
                return empty();
            }
            for (JsonElement element : items) {
                JsonObject volumeInfo = getObject(element.getAsJsonObject(), "volumeInfo");
                if (volumeInfo == null) {
                    continue;
                }
                String title = getString(volumeInfo, "title");
                JsonObject imageLinks = getObject(volumeInfo, "imageLinks");
                String cover = "";
                if (imageLinks != null) {
                    cover = firstNonEmpty(getString(imageLinks, "thumbnail"),
                            getString(imageLinks, "smallThumbnail"));
                }
                if (!title.isEmpty() || !cover.isEmpty()) {
                    return new LocalBookMetadata(title, cover);
                }
            }
        } catch (Throwable ignored) {
        }
        return empty();
    }

    public static LocalBookMetadata fromOpenLibraryJson(String json) {
        try {
            JsonObject root = new JsonParser().parse(json).getAsJsonObject();
            JsonArray docs = getArray(root, "docs");
            if (docs == null) {
                return empty();
            }
            for (JsonElement element : docs) {
                JsonObject doc = element.getAsJsonObject();
                String title = getString(doc, "title");
                String cover = "";
                if (doc.has("cover_i") && !doc.get("cover_i").isJsonNull()) {
                    cover = "https://covers.openlibrary.org/b/id/"
                            + doc.get("cover_i").getAsString() + "-L.jpg";
                }
                if (!title.isEmpty() || !cover.isEmpty()) {
                    return new LocalBookMetadata(title, cover);
                }
            }
        } catch (Throwable ignored) {
        }
        return empty();
    }

    public static LocalBookMetadata empty() {
        return new LocalBookMetadata("", "");
    }

    public boolean isEmpty() {
        return title.isEmpty() && coverUrl.isEmpty();
    }

    public String mergeTitle(String fallbackTitle) {
        return title.isEmpty() ? cleanTitle(fallbackTitle) : title;
    }

    public String mergeCover(String fallbackCover) {
        return coverUrl.isEmpty() ? emptyIfNull(fallbackCover) : coverUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private static JsonArray getArray(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }
        return object.getAsJsonArray(key);
    }

    private static String getString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return "";
        }
        return object.get(key).getAsString();
    }

    private static String firstNonEmpty(String first, String second) {
        return emptyIfNull(first).isEmpty() ? emptyIfNull(second) : first;
    }

    private static String normalizeCoverUrl(String coverUrl) {
        String url = emptyIfNull(coverUrl).trim();
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
