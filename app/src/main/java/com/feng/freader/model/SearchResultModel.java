package com.feng.freader.model;

import android.os.Handler;
import android.os.Looper;

import com.feng.freader.constant.Constant;
import com.feng.freader.constract.ISearchResultContract;
import com.feng.freader.entity.bean.NovelsSourceBean;
import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.http.UrlObtainer;
import com.feng.freader.http.WikisourceApi;
import com.feng.freader.httpUrlUtil.HttpUrlRequestBuilder;
import com.feng.freader.httpUrlUtil.Request;
import com.feng.freader.httpUrlUtil.Response;
import com.feng.freader.source.AggregatedSearchEngine;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class SearchResultModel implements ISearchResultContract.Model {

    private final ISearchResultContract.Presenter mPresenter;
    private final Gson mGson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public SearchResultModel(ISearchResultContract.Presenter mPresenter) {
        this.mPresenter = mPresenter;
    }

    @Override
    public void getNovelsSource(final String novelName) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int[] lastProgressCount = new int[]{0};
                List<NovelSourceData> aggregatedResults =
                        new AggregatedSearchEngine().search(novelName, Integer.MAX_VALUE, 10,
                                new AggregatedSearchEngine.ProgressListener() {
                                    @Override
                                    public void onProgress(List<NovelSourceData> results) {
                                        if (results.size() <= lastProgressCount[0]) {
                                            return;
                                        }
                                        lastProgressCount[0] = results.size();
                                        postSuccess(results);
                                    }
                                });
                if (!aggregatedResults.isEmpty()) {
                    if (aggregatedResults.size() > lastProgressCount[0]) {
                        postSuccess(aggregatedResults);
                    }
                    return;
                }
                searchLegacy(novelName);
            }
        }).start();
    }

    private void searchLegacy(final String novelName) {
        List<NovelSourceData> localResults = DiscoveryFallbackProvider.searchSources(novelName);
        if (!localResults.isEmpty()) {
            postSuccess(localResults);
            return;
        }

        Request request = new Request.Builder()
                .setUrl(UrlObtainer.getNovelsSource(novelName))
                .build();
        HttpUrlRequestBuilder.getInstance()
                .setRequest(request)
                .setResponse(new Response() {
                    @Override
                    public void success(String response) {
                        try {
                            List<NovelSourceData> wikisourceResults =
                                    WikisourceApi.parseSearchResults(response);
                            if (!wikisourceResults.isEmpty()) {
                                postSuccess(wikisourceResults);
                                return;
                            }
                            NovelsSourceBean novelsSourceBean = mGson.fromJson(response,
                                    NovelsSourceBean.class);
                            if (novelsSourceBean == null || novelsSourceBean.getCode() != 0) {
                                postError(Constant.NOT_FOUND_NOVELS);
                                return;
                            }
                            List<NovelSourceData> novelSourceDataList = new ArrayList<>();
                            List<NovelsSourceBean.ListBean> list = novelsSourceBean.getList();
                            for (int i = 0; i < list.size(); i++) {
                                NovelsSourceBean.ListBean curr = list.get(i);
                                NovelSourceData data = new NovelSourceData(curr.getName(),
                                        curr.getAuthor(), curr.getIntroduce(),
                                        curr.getUrl(), curr.getCover());
                                data.setSourceName("默认源");
                                novelSourceDataList.add(data);
                            }
                            postSuccess(novelSourceDataList);
                        } catch (Throwable t) {
                            postError(Constant.NOT_FOUND_NOVELS);
                        }
                    }

                    @Override
                    public void error(String errorMsg) {
                        List<NovelSourceData> fallbackResults =
                                DiscoveryFallbackProvider.searchSources(novelName);
                        if (!fallbackResults.isEmpty()) {
                            postSuccess(fallbackResults);
                        } else {
                            postError(errorMsg);
                        }
                    }
                })
                .build()
                .doRequest();
    }

    private void postSuccess(final List<NovelSourceData> results) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mPresenter.getNovelsSourceSuccess(results);
            }
        });
    }

    private void postError(final String error) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                mPresenter.getNovelsSourceError(error);
            }
        });
    }
}
