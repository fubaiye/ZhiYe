package com.feng.freader.source;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class SourceBookLink {
    private static final String PREFIX = "freader-source://book?";
    private static final String UTF_8 = "UTF-8";

    private SourceBookLink() {
    }

    public static String encode(String sourceId, String originalUrl) {
        if (sourceId == null || sourceId.trim().length() == 0
                || originalUrl == null || originalUrl.trim().length() == 0) {
            return originalUrl == null ? "" : originalUrl;
        }
        return PREFIX + "source=" + encodePart(sourceId.trim())
                + "&url=" + encodePart(originalUrl.trim());
    }

    public static boolean isSourceLink(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    public static String sourceId(String value) {
        return param(value, "source");
    }

    public static String originalUrl(String value) {
        if (!isSourceLink(value)) {
            return value == null ? "" : value;
        }
        return param(value, "url");
    }

    private static String param(String value, String key) {
        if (!isSourceLink(value)) {
            return "";
        }
        String query = value.substring(PREFIX.length());
        String[] parts = query.split("&");
        for (String part : parts) {
            int index = part.indexOf('=');
            if (index <= 0) {
                continue;
            }
            if (key.equals(part.substring(0, index))) {
                return decodePart(part.substring(index + 1));
            }
        }
        return "";
    }

    private static String encodePart(String value) {
        try {
            return URLEncoder.encode(value, UTF_8);
        } catch (UnsupportedEncodingException ignored) {
            return value;
        }
    }

    private static String decodePart(String value) {
        try {
            return URLDecoder.decode(value, UTF_8);
        } catch (UnsupportedEncodingException ignored) {
            return value;
        }
    }
}
