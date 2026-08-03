package com.feng.freader.source;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BookSourceParser {
    private static final Gson GSON = new Gson();

    private BookSourceParser() {
    }

    public static List<BookSource> parseList(String json) {
        try {
            Type type = new TypeToken<List<BookSource>>() {
            }.getType();
            List<BookSource> sources = GSON.fromJson(json, type);
            return sources == null ? new ArrayList<BookSource>() : sources;
        } catch (Throwable ignored) {
            List<BookSource> one = new ArrayList<>();
            BookSource source = parseOne(json);
            if (source != null && !source.getName().isEmpty()) {
                one.add(source);
            }
            return one;
        }
    }

    public static BookSource parseOne(String json) {
        try {
            return GSON.fromJson(json, BookSource.class);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static String toJson(List<BookSource> sources) {
        return GSON.toJson(sources);
    }
}
