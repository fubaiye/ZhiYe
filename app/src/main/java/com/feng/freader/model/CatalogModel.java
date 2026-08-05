package com.feng.freader.model;

import android.util.Log;
import android.os.Handler;
import android.os.Looper;

import com.feng.freader.constant.Constant;
import com.feng.freader.constract.ICatalogContract;
import com.feng.freader.entity.bean.CatalogBean;
import com.feng.freader.entity.bean.CategoryNovelsBean;
import com.feng.freader.entity.data.ANNovelData;
import com.feng.freader.entity.data.CatalogData;
import com.feng.freader.http.WikisourceApi;
import com.feng.freader.http.OkhttpBuilder;
import com.feng.freader.http.OkhttpCall;
import com.feng.freader.http.OkhttpUtil;
import com.feng.freader.source.BookSource;
import com.feng.freader.source.BookSourceExecutor;
import com.feng.freader.source.SourceBookLink;
import com.feng.freader.source.SourceRepository;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Feng Zhaohao
 * Created on 2019/11/17
 */
public class CatalogModel implements ICatalogContract.Model {

    private ICatalogContract.Presenter mPresenter;
    private Gson mGson = new Gson();

    public CatalogModel(ICatalogContract.Presenter mPresenter) {
        this.mPresenter = mPresenter;
    }

    @Override
    public void getCatalogData(String url) {
        Log.d("fzh", "getCatalogData: url = " + url);
        if (SourceBookLink.isSourceLink(url)) {
            getSourceCatalogData(url);
            return;
        }
        if (WikisourceApi.isWikisourceUrl(url)) {
            List<String> chapterNameList = new ArrayList<>();
            List<String> chapterUrlList = new ArrayList<>();
            chapterNameList.add(WikisourceApi.titleFromSourceUrl(url));
            chapterUrlList.add(url);
            mPresenter.getCatalogDataSuccess(new CatalogData(chapterNameList, chapterUrlList));
            return;
        }
        OkhttpBuilder builder = new OkhttpBuilder.Builder()
                .setUrl(url)
                .setOkhttpCall(new OkhttpCall() {
                    @Override
                    public void onResponse(String json) {
                        try {
                            CatalogBean catalogBean = mGson.fromJson(json, CatalogBean.class);
                            if (catalogBean.getCode() != 0) {
                                mPresenter.getCatalogDataError(Constant.NOT_FOUND_CATALOG_INFO);
                                return;
                            }
                            List<CatalogBean.ListBean> list = catalogBean.getList();
                            List<String> chapterNameList = new ArrayList<>();
                            List<String> chapterUrlList = new ArrayList<>();
                            for (int i = 0; i < list.size(); i++) {
                                chapterNameList.add(list.get(i).getNum());
                                chapterUrlList.add(list.get(i).getUrl());
                            }
                            mPresenter.getCatalogDataSuccess(new CatalogData(chapterNameList, chapterUrlList));
                        } catch (JsonSyntaxException e) {
                            mPresenter.getCatalogDataError(Constant.JSON_ERROR);
                        }
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        mPresenter.getCatalogDataError(errorMsg);
                    }
                })
                .build();
        OkhttpUtil.getRequest(builder);
    }

    private void getSourceCatalogData(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BookSource source = SourceRepository.getInstance()
                            .findById(SourceBookLink.sourceId(url));
                    if (source == null) {
                        postSourceCatalogError(Constant.NOT_FOUND_CATALOG_INFO);
                        return;
                    }
                    CatalogData data = new BookSourceExecutor().catalog(source, url);
                    if (data == null || data.getChapterUrlList().isEmpty()) {
                        postSourceCatalogError(Constant.NOT_FOUND_CATALOG_INFO);
                        return;
                    }
                    postSourceCatalogSuccess(data);
                } catch (Throwable t) {
                    String sourceId = SourceBookLink.sourceId(url);
                    String detailUrl = SourceBookLink.originalUrl(url);
                    Log.e("CatalogModel",
                            "加载书源目录失败"
                                    + "\nsourceId=" + sourceId
                                    + "\nbookUrl=" + url
                                    + "\ndetailUrl=" + detailUrl
                                    + "\nexception=" + t.getClass().getSimpleName(),
                            t);
                    String message = t.getClass().getSimpleName();
                    if (t.getMessage() != null && t.getMessage().trim().length() > 0) {
                        message += "：" + t.getMessage();
                    }
                    postSourceCatalogError(message);
                }
            }
        }).start();
    }

    private void postSourceCatalogSuccess(final CatalogData data) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mPresenter.getCatalogDataSuccess(data);
            }
        });
    }

    private void postSourceCatalogError(final String error) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                mPresenter.getCatalogDataError(error == null ? "" : error);
            }
        });
    }
}
