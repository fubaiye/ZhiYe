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

    public SourceHttpClient() {
    }

    SourceHttpClient(OkHttpClient client) {
        this.client = client;
    }

    public String execute(BookSource source, String keyword, int page) throws IOException {
        String url = SourceTemplate.render(source.getSearchUrl(), source, keyword, page);
        Request.Builder builder = createRequestBuilder(source, url, keyword, page);
        if ("POST".equalsIgnoreCase(source.getSearchMethod())) {
            String body = SourceTemplate.render(source.getSearchBody(), source, keyword, page);
            builder.post(RequestBody.create(FORM_TYPE, body));
        } else {
            builder.get();
        }
        return executeRequest(builder.build());
    }

    public String executeUrl(BookSource source, String url) throws IOException {
        Request.Builder builder = createRequestBuilder(source, url, "", 1);
        builder.get();
        return executeRequest(builder.build());
    }

    private Request.Builder createRequestBuilder(BookSource source, String url,
                                                 String keyword, int page) {
        Request.Builder builder = new Request.Builder().url(url);
        for (Map.Entry<String, String> entry : source.getHeaders().entrySet()) {
            builder.header(entry.getKey(), SourceTemplate.render(entry.getValue(), source, keyword, page));
        }
        if (!source.getCookies().isEmpty()) {
            builder.header("Cookie", cookieHeader(source, keyword, page));
        }
        return builder;
    }

    private String executeRequest(Request request) throws IOException {
        Response response = client().newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code()
                        + ", Method=" + request.method()
                        + ", URL=" + request.url());
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
