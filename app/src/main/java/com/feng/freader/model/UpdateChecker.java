package com.feng.freader.model;

import com.feng.freader.constant.Constant;
import com.feng.freader.httpUrlUtil.HttpUrlRequestBuilder;
import com.feng.freader.httpUrlUtil.Request;
import com.feng.freader.httpUrlUtil.Response;

public class UpdateChecker {
    private static final int TIMEOUT_MS = 6000;

    public interface Callback {
        void onSuccess(UpdateInfo updateInfo);

        void onError(String errorMsg);
    }

    private interface Parser {
        UpdateInfo parse(String response);
    }

    private static class UpdateSource {
        final String url;
        final Parser parser;

        UpdateSource(String url, Parser parser) {
            this.url = url;
            this.parser = parser;
        }
    }

    private UpdateChecker() {
    }

    public static void check(String releaseApiUrl, final Callback callback) {
        UpdateSource[] sources = new UpdateSource[]{
                new UpdateSource(releaseApiUrl, new Parser() {
                    @Override
                    public UpdateInfo parse(String response) {
                        return UpdateInfo.fromGitHubReleaseJson(response);
                    }
                }),
                new UpdateSource(Constant.UPDATE_RELEASE_ATOM_URL, new Parser() {
                    @Override
                    public UpdateInfo parse(String response) {
                        return UpdateInfo.fromGitHubReleaseAtom(response);
                    }
                }),
                new UpdateSource(Constant.UPDATE_RELEASE_PAGE_URL, new Parser() {
                    @Override
                    public UpdateInfo parse(String response) {
                        return UpdateInfo.fromGitHubReleasePage(response);
                    }
                })
        };
        checkSource(sources, 0, callback, "");
    }

    private static void checkSource(final UpdateSource[] sources, final int index,
                                    final Callback callback, final String lastError) {
        if (callback == null) {
            return;
        }
        if (index >= sources.length) {
            callback.onError(lastError);
            return;
        }
        final UpdateSource source = sources[index];
        Request request = new Request.Builder()
                .setUrl(source.url)
                .setConnectTimeout(TIMEOUT_MS)
                .setReadTimeout(TIMEOUT_MS)
                .build();
        HttpUrlRequestBuilder.getInstance()
                .setRequest(request)
                .setResponse(new Response() {
                    @Override
                    public void success(String response) {
                        UpdateInfo info = source.parser.parse(response);
                        if (info.isValid()) {
                            callback.onSuccess(info);
                            return;
                        }
                        checkSource(sources, index + 1, callback, "invalid update info from " + source.url);
                    }

                    @Override
                    public void error(String errorMsg) {
                        checkSource(sources, index + 1, callback, errorMsg);
                    }
                })
                .build()
                .doRequest();
    }
}
