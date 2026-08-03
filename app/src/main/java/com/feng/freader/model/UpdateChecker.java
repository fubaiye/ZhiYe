package com.feng.freader.model;

import com.feng.freader.httpUrlUtil.HttpUrlRequestBuilder;
import com.feng.freader.httpUrlUtil.Request;
import com.feng.freader.httpUrlUtil.Response;

public class UpdateChecker {

    public interface Callback {
        void onSuccess(UpdateInfo updateInfo);

        void onError(String errorMsg);
    }

    private UpdateChecker() {
    }

    public static void check(String releaseApiUrl, final Callback callback) {
        Request request = new Request.Builder()
                .setUrl(releaseApiUrl)
                .setConnectTimeout(10000)
                .setReadTimeout(10000)
                .build();
        HttpUrlRequestBuilder.getInstance()
                .setRequest(request)
                .setResponse(new Response() {
                    @Override
                    public void success(String response) {
                        callback.onSuccess(UpdateInfo.fromGitHubReleaseJson(response));
                    }

                    @Override
                    public void error(String errorMsg) {
                        callback.onError(errorMsg);
                    }
                })
                .build()
                .doRequest();
    }
}
