package com.feng.freader.online;

import com.feng.freader.http.NetworkClientFactory;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

public class OnlineHttpClient {
    public String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "ZhiYe/1.4 (self-hosted reader; public-domain sources)")
                .header("Accept", "application/atom+xml, application/json, text/xml, */*")
                .build();
        Response response = NetworkClientFactory.shared().newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP " + response.code());
        }
        String contentType = response.header("Content-Type", "");
        String body = response.body() == null ? "" : response.body().string();
        if (contentType.contains("text/html") && body.trim().toLowerCase().startsWith("<html")) {
            throw new IOException("Source returned HTML error page");
        }
        return body;
    }
}
