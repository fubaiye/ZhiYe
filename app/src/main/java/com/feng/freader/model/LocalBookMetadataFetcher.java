package com.feng.freader.model;

import com.feng.freader.httpUrlUtil.HttpUrlRequestBuilder;
import com.feng.freader.httpUrlUtil.Request;
import com.feng.freader.httpUrlUtil.Response;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class LocalBookMetadataFetcher {

    public interface Callback {
        void onSuccess(LocalBookMetadata metadata);

        void onError(String errorMsg);
    }

    private LocalBookMetadataFetcher() {
    }

    public static void fetch(final String title, final Callback callback) {
        final String cleanTitle = LocalBookMetadata.cleanTitle(title);
        request(googleBooksUrl(cleanTitle), new Response() {
            @Override
            public void success(String response) {
                LocalBookMetadata metadata = LocalBookMetadata.fromGoogleBooksJson(response);
                if (!metadata.isEmpty()) {
                    callback.onSuccess(metadata);
                    return;
                }
                fetchFromOpenLibrary(cleanTitle, callback);
            }

            @Override
            public void error(String errorMsg) {
                fetchFromOpenLibrary(cleanTitle, callback);
            }
        });
    }

    private static void fetchFromOpenLibrary(String title, final Callback callback) {
        request(openLibraryUrl(title), new Response() {
            @Override
            public void success(String response) {
                LocalBookMetadata metadata = LocalBookMetadata.fromOpenLibraryJson(response);
                if (!metadata.isEmpty()) {
                    callback.onSuccess(metadata);
                } else {
                    callback.onError("未识别到书籍信息");
                }
            }

            @Override
            public void error(String errorMsg) {
                callback.onError(errorMsg);
            }
        });
    }

    private static void request(String url, Response response) {
        Request request = new Request.Builder()
                .setUrl(url)
                .setConnectTimeout(8000)
                .setReadTimeout(8000)
                .build();
        HttpUrlRequestBuilder.getInstance()
                .setRequest(request)
                .setResponse(response)
                .build()
                .doRequest();
    }

    private static String googleBooksUrl(String title) {
        return "https://www.googleapis.com/books/v1/volumes?q=intitle:"
                + encode(title) + "&maxResults=3&printType=books&projection=lite";
    }

    private static String openLibraryUrl(String title) {
        return "https://openlibrary.org/search.json?title=" + encode(title) + "&limit=3";
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value == null ? "" : value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }
}
