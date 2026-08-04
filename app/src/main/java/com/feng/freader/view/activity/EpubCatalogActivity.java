package com.feng.freader.view.activity;

import android.content.Intent;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;

import com.feng.freader.R;
import com.feng.freader.adapter.CatalogAdapter;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.entity.epub.EpubTocItem;
import com.feng.freader.entity.epub.OpfData;
import com.feng.freader.entity.eventbus.EpubCatalogInitEvent;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.reader.EpubChapterIndexResolver;
import com.feng.freader.util.StatusBarUtil;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

public class EpubCatalogActivity extends BaseActivity implements View.OnClickListener {

    private ImageView mBackIv;
    private RecyclerView mListRv;

    private ReadActivity mReadActivity;
    private List<EpubTocItem> mEpubTocItemList = new ArrayList<>();
    private OpfData mOpfData;
    private String mNovelUrl;
    private String mName;
    private String mCover;

    private List<String> mChapterNameList = new ArrayList<>();

    @Override
    protected void doBeforeSetContentView() {
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_epub_catalog;
    }

    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected void initData() {
        for (int i = 0; i < mEpubTocItemList.size(); i++) {
            mChapterNameList.add(mEpubTocItemList.get(i).getTitle());
        }
    }

    @Override
    protected void initView() {
        mBackIv = findViewById(R.id.iv_epub_catalog_back);
        mBackIv.setOnClickListener(this);

        mListRv = findViewById(R.id.rv_epub_catalog_list);
        mListRv.setLayoutManager(new LinearLayoutManager(this));
        CatalogAdapter adapter = new CatalogAdapter(this, mChapterNameList);
        adapter.setOnCatalogListener(new CatalogAdapter.CatalogListener() {
            @Override
            public void clickItem(int position) {
                if (mReadActivity != null) {
                    mReadActivity.finish();
                }
                int chapterIndex = EpubChapterIndexResolver.resolve(mEpubTocItemList, mOpfData, position);
                Intent intent = new Intent(EpubCatalogActivity.this, ReadActivity.class);
                intent.putExtra(ReadActivity.KEY_NOVEL_URL, mNovelUrl);
                intent.putExtra(ReadActivity.KEY_NAME, mName);
                intent.putExtra(ReadActivity.KEY_COVER, mCover);
                intent.putExtra(ReadActivity.KEY_TYPE, 2);
                intent.putExtra(ReadActivity.KEY_CHAPTER_INDEX, chapterIndex);
                startActivity(intent);
                finish();
            }
        });
        mListRv.setAdapter(adapter);
    }

    @Override
    protected void doAfterInit() {
        StatusBarUtil.setLightColorStatusBar(this);
        getWindow().setStatusBarColor(getResources().getColor(R.color.catalog_bg));
    }

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onStickyEventBusCome(Event event) {
        switch (event.getCode()) {
            case EventBusCode.EPUB_CATALOG_INIT:
                if (event.getData() instanceof EpubCatalogInitEvent) {
                    EpubCatalogInitEvent e = (EpubCatalogInitEvent) event.getData();
                    mReadActivity = e.getReadActivity();
                    mEpubTocItemList = e.getTocItemList();
                    mOpfData = e.getOpfData();
                    mNovelUrl = e.getNovelUrl();
                    mName = e.getNovelName();
                    mCover = e.getNovelCover();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_epub_catalog_back:
                finish();
                break;
            default:
                break;
        }
    }
}
