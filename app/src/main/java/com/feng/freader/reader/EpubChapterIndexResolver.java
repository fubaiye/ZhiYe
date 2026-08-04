package com.feng.freader.reader;

import com.feng.freader.entity.epub.EpubTocItem;
import com.feng.freader.entity.epub.OpfData;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public final class EpubChapterIndexResolver {

    private EpubChapterIndexResolver() {
    }

    public static int resolve(List<EpubTocItem> tocItems, OpfData opfData, int tocPosition) {
        if (opfData == null || opfData.getSpine() == null || opfData.getSpine().isEmpty()) {
            return 0;
        }
        int fallback = clamp(tocPosition, opfData.getSpine().size());
        if (tocItems == null || tocPosition < 0 || tocPosition >= tocItems.size()) {
            return fallback;
        }
        EpubTocItem tocItem = tocItems.get(tocPosition);
        if (tocItem == null || tocItem.getPath() == null) {
            return fallback;
        }
        String tocPath = normalizePath(tocItem.getPath());
        for (int i = 0; i < opfData.getSpine().size(); i++) {
            String spinePath = normalizePath(opfData.getSpine().get(i));
            if (tocPath.equals(spinePath)) {
                return i;
            }
        }
        return fallback;
    }

    static String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        String normalized = decode(path.trim()).replace('\\', '/');
        int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        boolean absolute = normalized.startsWith("/");
        String[] parts = normalized.split("/");
        List<String> cleanParts = new ArrayList<>();
        for (String part : parts) {
            if (part.length() == 0 || ".".equals(part)) {
                continue;
            }
            if ("..".equals(part)) {
                if (!cleanParts.isEmpty() && !"..".equals(cleanParts.get(cleanParts.size() - 1))) {
                    cleanParts.remove(cleanParts.size() - 1);
                } else if (!absolute) {
                    cleanParts.add(part);
                }
                continue;
            }
            cleanParts.add(part);
        }
        StringBuilder builder = new StringBuilder();
        if (absolute) {
            builder.append('/');
        }
        for (int i = 0; i < cleanParts.size(); i++) {
            if (i > 0) {
                builder.append('/');
            }
            builder.append(cleanParts.get(i));
        }
        return builder.toString();
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return value;
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private static int clamp(int index, int size) {
        if (index < 0) {
            return 0;
        }
        if (index >= size) {
            return size - 1;
        }
        return index;
    }
}
