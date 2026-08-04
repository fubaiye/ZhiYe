package com.feng.freader.source;

import java.io.IOException;
import java.util.Map;
import com.feng.freader.http.NetworkClientFactory;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SourceHttpClient {
    private static final MediaType FORM_TYPE =
            MediaType.parse("application/x-www-form-urlencoded; charset=utf-8");
    private OkHttpClient client;

    public String execute(BookSource source, String keyword, int page) throws IOException {
        String url = SourceTemplate.render(source.getSearchUrl(), source, keyword, page);
        return execute(source, url, keyword, page);
    }

    public String executeUrl(BookSource source, String url) throws IOException {
        return execute(source, url, "", 1);
    }

    private String execute(BookSource source, String url, String keyword, int page) throws IOException {
        Request.Builder builder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : source.getHeaders().entrySet()) {
            builder.header(entry.getKey(), SourceTemplate.render(entry.getValue(), source, keyword, page));
        }
        if (!source.getCookies().isEmpty()) {
            builder.header("Cookie", cookieHeader(source, keyword, page));
        }
        if ("POST".equalsIgnoreCase(source.getSearchMethod())) {
            String body = SourceTemplate.render(source.getSearchBody(), source, keyword, page);
            builder.post(RequestBody.create(FORM_TYPE, body));
        } else {
            builder.get();
        }
        Response response = client().newCall(builder.build()).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            return response.body() == null ? "" : response.body().string();
        } finally {
            response.close();
        }
    }

    private OkHttpClient client() {
        if (client == null) {
            client = NetworkClientFactory.shared();
        }
        return client;
    }

    private String cookieHeader(BookSource source, String keyword, int page) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : source.getCookies().entrySet()) {
            if (builder.length() > 0) {
                builder.append("; ");
            }
            builder.append(entry.getKey())
                    .append('=')
                    .append(SourceTemplate.render(entry.getValue(), source, keyword, page));
        }
        return builder.toString();
    }
}
