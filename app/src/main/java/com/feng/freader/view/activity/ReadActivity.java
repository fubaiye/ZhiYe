package com.feng.freader.view.activity;

import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.feng.freader.R;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.constant.Constant;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.constract.IReadContract;
import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.entity.data.DetailedChapterData;
import com.feng.freader.entity.epub.EpubData;
import com.feng.freader.entity.epub.OpfData;
import com.feng.freader.entity.epub.EpubTocItem;
import com.feng.freader.entity.eventbus.EpubCatalogInitEvent;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.entity.eventbus.HoldReadActivityEvent;
import com.feng.freader.http.UrlObtainer;
import com.feng.freader.presenter.ReadPresenter;
import com.feng.freader.reader.EpubChapterFallbackPolicy;
import com.feng.freader.reader.ReaderChapterTitleFormatter;
import com.feng.freader.reader.ReaderDisplayPolicy;
import com.feng.freader.reader.ReaderProfileManager;
import com.feng.freader.reader.ReaderRecord;
import com.feng.freader.source.SourceBookLink;
import com.feng.freader.util.EpubUtils;
import com.feng.freader.util.ScreenUtil;
import com.feng.freader.util.EventBusUtil;
import com.feng.freader.util.SpUtil;
import com.feng.freader.util.StatusBarUtil;
import com.feng.freader.util.VolumePageTurnPolicy;
import com.feng.freader.widget.PageView;
import com.feng.freader.widget.RealPageView;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 小说阅读界面
 *
 * @author Feng Zhaohao
 * Created on 2019/11/25
 */
public class ReadActivity extends BaseActivity<ReadPresenter>
        implements IReadContract.View, View.OnClickListener {
    private static final String TAG = "ReadActivity";
    private static final String LOADING_TEXT = "正在加载中…";

    public static final String KEY_NOVEL_URL = "read_key_novel_url";
    public static final String KEY_NAME = "read_key_name";
    public static final String KEY_COVER = "read_key_cover";
    public static final String KEY_CHAPTER_INDEX = "read_key_chapter_index";
    public static final String KEY_POSITION = "read_key_position";
    public static final String KEY_IS_REVERSE = "read_key_is_reverse";
    public static final String KEY_TYPE = "read_key_type";
    public static final String KEY_SECOND_POSITION = "read_key_second_position";

    private RealPageView mPageView;
    private TextView mNovelTitleTv;
    private TextView mBarChapterTitleTv;
    private TextView mNovelProgressTv;
    private TextView mStateTv;

    private RelativeLayout mTopSettingBarRv;
    private ConstraintLayout mBottomBarCv;
    private ConstraintLayout mBrightnessBarCv;
    private ConstraintLayout mSettingBarCv;

    private ImageView mBackIv;
    private ImageView mMenuIv;
    private TextView mPreviousChapterTv;
    private SeekBar mNovelProcessSb;
    private TextView mCatalogProgressTv;
    private TextView mNextChapterTv;
    private ImageView mCatalogIv;
    private ImageView mBrightnessIv;
    private ImageView mDayAndNightModeIv;
    private ImageView mSettingIv;
    private TextView mCatalogTv;
    private TextView mBrightnessTv;
    private TextView mDayAndNightModeTv;
    private TextView mSettingTv;

    private SeekBar mBrightnessProcessSb;
    private Switch mSystemBrightnessSw;

    private ImageView mDecreaseFontIv;
    private ImageView mIncreaseFontIv;
    private ImageView mDecreaseRowSpaceIv;
    private ImageView mIncreaseRowSpaceIv;
    private TextView mFontSizeValueTv;
    private TextView mRowSpaceValueTv;
    private TextView mFontFamilyTv;
    private View mTheme0;
    private View mTheme1;
    private View mTheme2;
    private View mTheme3;
    private View mTheme4;
    private TextView mTurnNormalTv;
    private TextView mTurnRealTv;

    // 章节 url 列表（通过网络请求获取）
    private List<String> mChapterUrlList = new ArrayList<>();
    // Epub Opf 文件数据
    private OpfData mOpfData;
    // Epub 文件的目录
    private List<EpubTocItem> mEpubTocList = new ArrayList<>();
    // 图片的父目录，为 opf 文件的父目录
    private String mParentPath = "";
    // 网络小说目录
    private List<String> mNetCatalogList = new ArrayList<>();
    // 当前小说阅读进度（本地 txt 用）
    private float mTxtNovelProgress;
    // 小说内容（本地 txt 用）
    private String mNovelContent;
    // 小说进度（本地 txt 用）
    private String mNovelProgress = "";

    // 以下内容通过 Intent 传入
    private String mNovelUrl;   // 小说 url，本地小说为 filePath
    private String mName;   // 小说名
    private String mCover;  // 小说封面
    private int mType;      // 小说类型，0 为网络小说， 1 为本地 txt 小说, 2 为本地 epub 小说
    private int mChapterIndex;   // 当前阅读的章节索引
    private int mPosition;  // 文本开始读取位置
    private boolean mIsReverse; // 是否需要将章节列表倒序
    private int mSecondPosition; // epub 用

    private DatabaseManager mDbManager;
    private boolean mIsLoadingChapter = false;  // 是否正在加载具体章节
    private boolean mIsShowingOrHidingBar = false;  // 是否正在显示或隐藏上下栏
    private boolean mIsShowBrightnessBar = false;   // 是否正在显示亮度栏
    private boolean mIsSystemBrightness = true;     // 是否为系统亮度
    private boolean mIsShowSettingBar = false;      // 是否正在显示设置栏
    private boolean mIsNeedWrite2Db = true;         // 活动结束时是否需要将书籍信息写入数据库
    private boolean mIsUpdateChapter = false;   // 是否更新章节
    private boolean mJumpToChapterEnd = false;


    // 从 sp 中读取
    private float mTextSize;    // 字体大小
    private float mRowSpace;    // 行距
    private int mTheme;         // 阅读主题
    private float mBrightness;  // 屏幕亮度，为 -1 时表示系统亮度
    private boolean mIsNightMode;           // 是否为夜间模式
    private int mTurnType;      // 翻页模式：0 为正常，1 为仿真
    private int mReaderFont;

    private ReaderProfileManager mReaderProfileManager;
    private long mReadingStartMs;

    private float mMinTextSize = ReaderDisplayPolicy.MIN_FONT_SIZE;
    private float mMaxTextSize = ReaderDisplayPolicy.MAX_FONT_SIZE;
    private float mMinRowSpace = ReaderDisplayPolicy.MIN_ROW_SPACE;
    private float mMaxRowSpace = ReaderDisplayPolicy.MAX_ROW_SPACE;

    // 监听系统亮度的变化
    private ContentObserver mBrightnessObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mIsSystemBrightness) {
                // 屏幕亮度更新为新的系统亮度
                ScreenUtil.setWindowBrightness(ReadActivity.this,
                        (float) ScreenUtil.getSystemBrightness() / ScreenUtil.getBrightnessMax());
            }
        }
    };

    @Override
    protected void doBeforeSetContentView() {
        StatusBarUtil.setLightColorStatusBar(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_read;
    }

    @Override
    protected ReadPresenter getPresenter() {
        return new ReadPresenter();
    }

    @Override
    protected void initData() {
        // 从前一个活动传来
        mNovelUrl = getIntent().getStringExtra(KEY_NOVEL_URL);
        mName = getIntent().getStringExtra(KEY_NAME);
        mCover = getIntent().getStringExtra(KEY_COVER);
        mChapterIndex = getIntent().getIntExtra(KEY_CHAPTER_INDEX, 0);
        mPosition = getIntent().getIntExtra(KEY_POSITION, 0);
        mIsReverse = getIntent().getBooleanExtra(KEY_IS_REVERSE, false);
        mType = getIntent().getIntExtra(KEY_TYPE, 0);
        mSecondPosition = getIntent().getIntExtra(KEY_SECOND_POSITION, 0);

        // 从 SP 得到
        mTextSize = ReaderDisplayPolicy.clampFontSize(SpUtil.getTextSize());
        mRowSpace = ReaderDisplayPolicy.clampRowSpace(SpUtil.getRowSpace());
        mTheme = SpUtil.getTheme();
        mBrightness = SpUtil.getBrightness();
        mIsNightMode = SpUtil.getIsNightMode();
        mTurnType = SpUtil.getTurnType();
        mReaderFont = ReaderDisplayPolicy.normalizeFontIndex(SpUtil.getReaderFont());

        // 其他
        mDbManager = DatabaseManager.getInstance();
        mReaderProfileManager = new ReaderProfileManager(this);
        mReadingStartMs = System.currentTimeMillis();
    }

    @Override
    protected void initView() {
        mTopSettingBarRv = findViewById(R.id.rv_read_top_bar);
        mBottomBarCv = findViewById(R.id.cv_read_bottom_bar);
        mBrightnessBarCv = findViewById(R.id.cv_read_brightness_bar);
        mSettingBarCv = findViewById(R.id.cv_read_setting_bar);

        mPageView = findViewById(R.id.pv_read_page_view);
        mPageView.setPageViewListener(new PageView.PageViewListener() {
            @Override
            public void updateProgress(String progress) {
                mNovelProgress = progress;
                mNovelProgressTv.setText(progress);
            }

            @Override
            public void next() {
                if (mType == 0) {
                    nextNet();
                } else if (mType == 1) {
                    showShortToast("已经到最后了");
                } else if (mType == 2){
                    nextEpub();
                }
            }

            @Override
            public void pre() {
                if (mType == 0) {
                    preNet();
                } else if (mType == 1){
                    showShortToast("已经到最前了");
                } else if (mType == 2){
                    preEpub();
                }
            }

            @Override
            public void nextPage() {
                if (mType == 1) {
                    updateChapterProgress();
                }
            }

            @Override
            public void prePage() {
                if (mType == 1) {
                    updateChapterProgress();
                }
            }

            @Override
            public void showOrHideSettingBar() {
                if (mIsShowingOrHidingBar) {
                    return;
                }
                if (mIsShowBrightnessBar) {
                    hideBrightnessBar();
                    return;
                }
                if (mIsShowSettingBar) {
                    hideSettingBar();
                    return;
                }
                mIsShowingOrHidingBar = true;
                if (mTopSettingBarRv.getVisibility() != View.VISIBLE) {
                    // 显示上下栏
                    showBar();
                } else {
                    // 隐藏上下栏
                    hideBar();
                }
            }
        });

        mNovelTitleTv = findViewById(R.id.tv_read_novel_title);
        mBarChapterTitleTv = findViewById(R.id.tv_read_bar_chapter_title);
        mNovelProgressTv = findViewById(R.id.tv_read_novel_progress);
        mStateTv = findViewById(R.id.tv_read_state);

        mBackIv = findViewById(R.id.iv_read_back);
        mBackIv.setOnClickListener(this);
        mMenuIv = findViewById(R.id.iv_read_menu);
        mMenuIv.setOnClickListener(this);
        mPreviousChapterTv = findViewById(R.id.tv_read_previous_chapter);
        mPreviousChapterTv.setOnClickListener(this);
        mNextChapterTv = findViewById(R.id.tv_read_next_chapter);
        mNextChapterTv.setOnClickListener(this);
        mCatalogIv = findViewById(R.id.iv_read_catalog);
        mCatalogIv.setOnClickListener(this);
        mBrightnessIv = findViewById(R.id.iv_read_brightness);
        mBrightnessIv.setOnClickListener(this);
        mDayAndNightModeIv = findViewById(R.id.iv_read_day_and_night_mode);
        mDayAndNightModeIv.setOnClickListener(this);
        mSettingIv = findViewById(R.id.iv_read_setting);
        mSettingIv.setOnClickListener(this);
        mCatalogTv = findViewById(R.id.tv_read_catalog);
        mCatalogTv.setOnClickListener(this);
        mBrightnessTv = findViewById(R.id.tv_read_brightness);
        mBrightnessTv.setOnClickListener(this);
        mDayAndNightModeTv = findViewById(R.id.tv_read_day_and_night_mode);
        mDayAndNightModeTv.setOnClickListener(this);
        mSettingTv = findViewById(R.id.tv_read_setting);
        mSettingTv.setOnClickListener(this);

        mNovelProcessSb = findViewById(R.id.sb_read_novel_progress);
        mNovelProcessSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                double scale = (double) progress / 100f;
                if (mIsUpdateChapter) {
                    if (mType == 0) {   // 网络小说
                        mChapterIndex = (int) ((mNetCatalogList.size() - 1) * scale);
                        mCatalogProgressTv.setText(mNetCatalogList.get(mChapterIndex));
                    } else if (mType == 1) {    // 本地 txt
                        mTxtNovelProgress = (float) scale;
                        String s = String.valueOf(scale * 100);
                        mCatalogProgressTv.setText(s.substring(0, Math.min(5, s.length())) + "%");
                    } else if (mType == 2) {    // 本地 epub
                        mChapterIndex = (int) ((mEpubTocList.size() - 1) * scale);
                        mCatalogProgressTv.setText(mEpubTocList.get(mChapterIndex).getTitle());
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mIsUpdateChapter = true;
                mCatalogProgressTv.setVisibility(View.VISIBLE);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mIsUpdateChapter = false;
                mCatalogProgressTv.setVisibility(View.GONE);
                if (mType == 0 || mType == 2) {
                    showChapter();
                } else if (mType == 1) {
                    mPageView.jumpWithProgress(mTxtNovelProgress);
                }
            }
        });
        mCatalogProgressTv = findViewById(R.id.tv_read_catalog_progress);

        mBrightnessProcessSb = findViewById(R.id.sb_read_brightness_bar_brightness_progress);
        mBrightnessProcessSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (!mIsSystemBrightness) {
                    // 调整亮度
                    mBrightness = (float) progress / 100;
                    ScreenUtil.setWindowBrightness(ReadActivity.this, mBrightness);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        mSystemBrightnessSw = findViewById(R.id.sw_read_system_brightness_switch);
        mSystemBrightnessSw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 变为系统亮度
                    mIsSystemBrightness = true;
                    mBrightness = -1f;
                    // 将屏幕亮度设置为系统亮度
                    ScreenUtil.setWindowBrightness(ReadActivity.this,
                            (float) ScreenUtil.getSystemBrightness() / ScreenUtil.getBrightnessMax());
                } else {
                    // 变为自定义亮度
                    mIsSystemBrightness = false;
                    // 将屏幕亮度设置为自定义亮度
                    mBrightness = (float) mBrightnessProcessSb.getProgress() / 100;
                    ScreenUtil.setWindowBrightness(ReadActivity.this, mBrightness);
                }
            }
        });

        mDecreaseFontIv = findViewById(R.id.iv_read_decrease_font);
        mDecreaseFontIv.setOnClickListener(this);
        mIncreaseFontIv= findViewById(R.id.iv_read_increase_font);
        mIncreaseFontIv.setOnClickListener(this);
        mDecreaseRowSpaceIv = findViewById(R.id.iv_read_decrease_row_space);
        mDecreaseRowSpaceIv.setOnClickListener(this);
        mIncreaseRowSpaceIv = findViewById(R.id.iv_read_increase_row_space);
        mIncreaseRowSpaceIv.setOnClickListener(this);
        mFontSizeValueTv = findViewById(R.id.tv_read_font_size_value);
        mRowSpaceValueTv = findViewById(R.id.tv_read_row_space_value);
        mFontFamilyTv = findViewById(R.id.tv_read_font_family);
        mFontFamilyTv.setOnClickListener(this);
        applyReaderFont();
        updateReaderSettingLabels();
        mTheme0 = findViewById(R.id.v_read_theme_0);
        mTheme0.setOnClickListener(this);
        mTheme1 = findViewById(R.id.v_read_theme_1);
        mTheme1.setOnClickListener(this);
        mTheme2 = findViewById(R.id.v_read_theme_2);
        mTheme2.setOnClickListener(this);
        mTheme3 = findViewById(R.id.v_read_theme_3);
        mTheme3.setOnClickListener(this);
        mTheme4 = findViewById(R.id.v_read_theme_4);
        mTheme4.setOnClickListener(this);
        mTurnNormalTv = findViewById(R.id.tv_read_turn_normal);
        mTurnNormalTv.setOnClickListener(this);
        mTurnRealTv = findViewById(R.id.tv_read_turn_real);
        mTurnRealTv.setOnClickListener(this);
        switch (mTurnType) {
            case 0:
                mTurnNormalTv.setSelected(true);
                mPageView.setTurnType(PageView.TURN_TYPE.NORMAL);
                break;
            case 1:
                mTurnRealTv.setSelected(true);
                mPageView.setTurnType(PageView.TURN_TYPE.REAL);
                break;
        }
    }

    @Override
    protected void doAfterInit() {
        if (mType == 0) {
            // 先通过小说 url 获取所有章节 url 信息
            mPresenter.getChapterList(catalogRequestUrl(mNovelUrl));
        } else if (mType == 1){
            // 通过 FilePath 读取本地小说
            mPresenter.loadTxt(mNovelUrl);
        } else if (mType == 2) {
            // 先根据 filePath 获得 OpfData
            mPresenter.getOpfData(mNovelUrl);
        }

        if (mBrightness == -1f) {    // 系统亮度
            mSystemBrightnessSw.setChecked(true);
        } else {    // 自定义亮度
            mBrightnessProcessSb.setProgress((int) (100 * mBrightness));
            mSystemBrightnessSw.setChecked(false);
            ScreenUtil.setWindowBrightness(this, mBrightness);
        }

        if (mIsNightMode) { // 夜间模式
            nightMode();
        } else {    // 日间模式
            dayMode();
        }

        // 监听系统亮度的变化
        getContentResolver().registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                true,
                mBrightnessObserver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mReaderProfileManager.addReadingTime(mNovelUrl,
                System.currentTimeMillis() - mReadingStartMs);
        if (mIsNeedWrite2Db) {
            // 将书籍信息存入数据库
            mDbManager.deleteBookshelfNovel(mNovelUrl);
            if (mIsReverse) {   // 如果倒置了目录的话，需要倒置章节索引
                mChapterIndex = mChapterUrlList.size() - 1 - mChapterIndex;
            }
            if (mType == 0 || mType == 1) {
                BookshelfNovelDbData dbData = new BookshelfNovelDbData(mNovelUrl, mName,
                        mCover, mChapterIndex, mPageView.getPosition(), mType);
                mDbManager.insertBookshelfNovel(dbData);
            } else if (mType == 2){
                BookshelfNovelDbData dbData = new BookshelfNovelDbData(mNovelUrl, mName,
                        mCover, mChapterIndex, mPageView.getFirstPos(), mType, mPageView.getSecondPos());
                mDbManager.insertBookshelfNovel(dbData);
            }
        }

        // 更新书架页面数据
        Event event = new Event(EventBusCode.BOOKSHELF_UPDATE_LIST);
        EventBusUtil.sendEvent(event);

        // 将相关数据存入 SP
        SpUtil.saveTextSize(mTextSize);
        SpUtil.saveRowSpace(mRowSpace);
        SpUtil.saveTheme(mTheme);
        SpUtil.saveBrightness(mBrightness);
        SpUtil.saveIsNightMode(mIsNightMode);
        SpUtil.saveTurnType(mTurnType);
        SpUtil.saveReaderFont(mReaderFont);

        // 解除监听
        getContentResolver().unregisterContentObserver(mBrightnessObserver);
    }

    @Override
    protected boolean isRegisterEventBus() {
        return false;
    }

    /**
     * 获取章节目录成功
     */
    @Override
    public void getChapterUrlListSuccess(List<String> chapterUrlList, List<String> chapterNameList) {
        if (chapterUrlList == null || chapterUrlList.isEmpty() ||
                chapterNameList == null || chapterNameList.isEmpty()) {
            mStateTv.setText("获取章节目录信息失败");
            return;
        }
        mChapterUrlList = chapterUrlList;
        mNetCatalogList = chapterNameList;
        if (mIsReverse) {
            Collections.reverse(mChapterUrlList);
            Collections.reverse(mNetCatalogList);
        }
        mChapterIndex = Math.max(0, Math.min(mChapterIndex, mChapterUrlList.size() - 1));
        updateDisplayedChapterTitle(ReaderChapterTitleFormatter.titleAt(mNetCatalogList, mChapterIndex, mName));
        // 获取具体章节信息
        if (mChapterUrlList.get(mChapterIndex) != null) {
            mIsLoadingChapter = true;
            mPresenter.getDetailedChapterData(chapterRequestUrl(mChapterUrlList.get(mChapterIndex)));
        } else {
            mStateTv.setText("获取章节信息失败");
        }
    }

    /**
     * 获取章节目录失败
     */
    @Override
    public void getChapterUrlListError(String errorMsg) {
        mStateTv.setText("获取失败，请检查网络后重新加载");
    }

    /**
     * 获取具体章节信息成功
     */
    @Override
    public void getDetailedChapterDataSuccess(DetailedChapterData data) {
        mIsLoadingChapter = false;
        if (data == null) {
            mStateTv.setText("获取不到相关数据，请查看其他章节");
            return;
        }
        mStateTv.setVisibility(View.GONE);
        mPageView.initDrawText(data.getContent(), mPosition);
        if (mJumpToChapterEnd) {
            mPageView.jumpToLastTextPage();
            mPosition = mPageView.getPosition();
            mJumpToChapterEnd = false;
        }
        updateDisplayedChapterTitle(ReaderChapterTitleFormatter.firstNonEmpty(
                ReaderChapterTitleFormatter.titleAt(mNetCatalogList, mChapterIndex, ""),
                data.getName(), mName));
        updateChapterProgress();
    }

    /**
     * 获取具体章节信息失败
     */
    @Override
    public void getDetailedChapterDataError(String errorMsg) {
        mIsLoadingChapter = false;
        mJumpToChapterEnd = false;
        mStateTv.setVisibility(View.VISIBLE);
        mStateTv.setText("获取失败，请检查网络后重新加载");
    }

    /**
     * 加载本地 txt 成功
     */
    @Override
    public void loadTxtSuccess(String text) {
        mNovelContent = text;
        mStateTv.setVisibility(View.GONE);
        mPageView.initDrawText(text, mPosition);
        updateDisplayedChapterTitle(mName);
        updateChapterProgress();
    }

    /**
     * 加载本地 txt 失败
     */
    @Override
    public void loadTxtError(String errorMsg) {
        if (errorMsg.equals(Constant.NOT_FOUND_FROM_LOCAL)) {
            // 该文件已从本地删除
            mStateTv.setText("该文件已从本地删除");
            mIsNeedWrite2Db = false;
            // 从数据库中删除该条记录
            mDbManager.deleteBookshelfNovel(mNovelUrl);
            // 更新书架页面
            Event event = new Event(EventBusCode.BOOKSHELF_UPDATE_LIST);
            EventBusUtil.sendEvent(event);
            return;
        }
        mStateTv.setText(errorMsg);
    }

    /**
     * 获取 Epub 的 Opf 文件数据成功
     */
    @Override
    public void getOpfDataSuccess(OpfData opfData) {
        if (opfData == null) {
            mStateTv.setText("读取失败");
            return;
        }
        mParentPath = opfData.getParentPath();
        // 解析 ncx 文件，得到小说目录
        try {
            mEpubTocList = EpubUtils.getTocData(opfData.getNcx());
        } catch (XmlPullParserException e) {
            e.printStackTrace();
            mStateTv.setText("读取失败");
            return;
        } catch (IOException e) {
            e.printStackTrace();
            mStateTv.setText("读取失败");
            return;
        }
        mOpfData = opfData;
        // 获取具体章节数据
        mPresenter.getEpubChapterData(mParentPath, mOpfData.getSpine().get(mChapterIndex));
    }

    /**
     * 获取 Epub 的 Opf 文件数据失败
     */
    @Override
    public void getOpfDataError(String errorMsg) {
        mStateTv.setText("读取失败");
    }

    /**
     * 获取 Epub 的章节数据成功
     */
    @Override
    public void getEpubChapterDataSuccess(List<EpubData> dataList) {
        mIsLoadingChapter = false;
        if (dataList == null || dataList.isEmpty()) {
            int nextChapter = mOpfData == null ? -1 :
                    EpubChapterFallbackPolicy.nextCandidate(mOpfData.getSpine(), mChapterIndex);
            if (nextChapter != -1) {
                mChapterIndex = nextChapter;
                mPosition = 0;
                mSecondPosition = 0;
                mIsLoadingChapter = true;
                mStateTv.setVisibility(View.GONE);
                mPresenter.getEpubChapterData(mParentPath, mOpfData.getSpine().get(mChapterIndex));
                updateChapterProgress();
                return;
            }
            mStateTv.setText("本章无数据，请查看其他章节");
            return;
        }
        mStateTv.setVisibility(View.GONE);
        // 通知 PageView 绘制章节数据
        mPageView.initDrawEpub(dataList, mPosition, mSecondPosition);
        if (mJumpToChapterEnd) {
            mPageView.jumpToLastEpubPage();
            mPosition = mPageView.getFirstPos();
            mSecondPosition = mPageView.getSecondPos();
            mJumpToChapterEnd = false;
        }
        // 设置该节的名称
        String title = dataList.get(0).getType() == EpubData.TYPE.TITLE?
                dataList.get(0).getData() : "";
        updateDisplayedChapterTitle(ReaderChapterTitleFormatter.firstNonEmpty(
                title, epubTocTitle(mChapterIndex), mName));
        updateChapterProgress();
    }

    /**
     * 获取 Epub 的章节数据失败
     */
    @Override
    public void getEpubChapterDataError(String errorMsg) {
        mIsLoadingChapter = false;
        mJumpToChapterEnd = false;
        mStateTv.setVisibility(View.VISIBLE);
        mStateTv.setText("读取失败");
    }

    /**
     * 点击上一页/下一页后加载具体章节
     */
    private void showChapter() {
        if (mIsLoadingChapter) {    // 已经在加载了
            return;
        }
        if (mType == 0) {   // 显示网络小说
            if (!mJumpToChapterEnd) {
                mPosition = 0;
            }
            mStateTv.setVisibility(View.GONE);
            mIsLoadingChapter = true;
            if (!mChapterUrlList.isEmpty()) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        updateDisplayedChapterTitle(ReaderChapterTitleFormatter.titleAt(
                                mNetCatalogList, mChapterIndex, mName));
                        mPresenter.getDetailedChapterData(chapterRequestUrl(
                                mChapterUrlList.get(mChapterIndex)));
                    }
                });
            } else {
                mStateTv.setText("加载失败");
                mStateTv.setVisibility(View.VISIBLE);
                mIsLoadingChapter = false;
            }
        } else if (mType == 2) {    // 显示 epub 小说
            if (!mJumpToChapterEnd) {
                mPosition = 0;
                mSecondPosition = 0;
            }
            mStateTv.setVisibility(View.GONE);
            mIsLoadingChapter = true;
            if (mOpfData != null) {
                new Handler().post(new Runnable() {
                    @Override
                    public void run() {
                        mPresenter.getEpubChapterData(mParentPath, mOpfData.getSpine().get(mChapterIndex));
                    }
                });
            } else {
                mStateTv.setText("加载失败");
                mStateTv.setVisibility(View.VISIBLE);
                mIsLoadingChapter = false;
            }
        }
        updateChapterProgress();
    }

    /**
     * 显示上下栏
     */
    private String catalogRequestUrl(String url) {
        return SourceBookLink.isSourceLink(url) ? url : UrlObtainer.getCatalogInfo(url);
    }

    private String chapterRequestUrl(String url) {
        return SourceBookLink.isSourceLink(url) ? url : UrlObtainer.getDetailedChapter(url);
    }

    private void updateDisplayedChapterTitle(String title) {
        String display = ReaderChapterTitleFormatter.firstNonEmpty(title, "", mName);
        if (mNovelTitleTv != null) {
            mNovelTitleTv.setText(display);
        }
        if (mBarChapterTitleTv != null) {
            mBarChapterTitleTv.setText(display);
        }
    }

    private String epubTocTitle(int index) {
        if (mEpubTocList != null && index >= 0 && index < mEpubTocList.size()
                && mEpubTocList.get(index) != null) {
            return mEpubTocList.get(index).getTitle();
        }
        return "";
    }

    private void showBar() {
        Animation topAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_top_enter);
        topAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                StatusBarUtil.setDarkColorStatusBar(ReadActivity.this);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // 结束时重置标记
                mIsShowingOrHidingBar = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        Animation bottomAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_enter);
        mTopSettingBarRv.startAnimation(topAnim);
        mBottomBarCv.startAnimation(bottomAnim);
        mTopSettingBarRv.setVisibility(View.VISIBLE);
        mBottomBarCv.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏上下栏
     */
    private void hideBar() {
        Animation topExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_top_exit);
        topExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mTopSettingBarRv.setVisibility(View.GONE);
                mIsShowingOrHidingBar = false;
                StatusBarUtil.setLightColorStatusBar(ReadActivity.this);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        Animation bottomExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_exit);
        bottomExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBottomBarCv.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mTopSettingBarRv.startAnimation(topExitAnim);
        mBottomBarCv.startAnimation(bottomExitAnim);
    }

    /**
     * 显示亮度栏
     */
    private void showBrightnessBar() {
        mIsShowBrightnessBar = true;
        Animation bottomAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_enter);
        mBrightnessBarCv.startAnimation(bottomAnim);
        mBrightnessBarCv.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏亮度栏
     */
    private void hideBrightnessBar() {
        Animation bottomExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_exit);
        bottomExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mBrightnessBarCv.setVisibility(View.GONE);
                mIsShowBrightnessBar = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mBrightnessBarCv.startAnimation(bottomExitAnim);
    }

    /**
     * 显示设置栏
     */
    private void showSettingBar() {
        mIsShowSettingBar = true;
        Animation bottomAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_enter);
        mSettingBarCv.startAnimation(bottomAnim);
        mSettingBarCv.setVisibility(View.VISIBLE);
    }

    /**
     * 隐藏设置栏
     */
    private void hideSettingBar() {
        Animation bottomExitAnim = AnimationUtils.loadAnimation(
                this, R.anim.read_setting_bottom_exit);
        bottomExitAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                mSettingBarCv.setVisibility(View.GONE);
                mIsShowSettingBar = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        mSettingBarCv.startAnimation(bottomExitAnim);
    }

    private void showReaderMenu() {
        PopupMenu popupMenu = new PopupMenu(this, mMenuIv);
        popupMenu.getMenu().add(0, 1, 0, "加入书签");
        popupMenu.getMenu().add(0, 2, 1, "写笔记");
        popupMenu.getMenu().add(0, 3, 2, "保存高亮");
        popupMenu.getMenu().add(0, 4, 3, "阅读统计");
        popupMenu.getMenu().add(0, 5, 4, "词典查询");
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == 1) {
                    addReaderRecord(ReaderRecord.BOOKMARK, "");
                    showShortToast("已加入书签");
                    return true;
                }
                if (item.getItemId() == 2) {
                    showInputDialog("写笔记", "输入笔记", new InputCallback() {
                        @Override
                        public void onInput(String text) {
                            addReaderRecord(ReaderRecord.NOTE, text);
                            showShortToast("笔记已保存");
                        }
                    });
                    return true;
                }
                if (item.getItemId() == 3) {
                    showInputDialog("保存高亮", "输入摘录内容", new InputCallback() {
                        @Override
                        public void onInput(String text) {
                            addReaderRecord(ReaderRecord.HIGHLIGHT, text);
                            showShortToast("高亮已保存");
                        }
                    });
                    return true;
                }
                if (item.getItemId() == 4) {
                    new AlertDialog.Builder(ReadActivity.this)
                            .setTitle(mName)
                            .setMessage(mReaderProfileManager.buildSummary(mNovelUrl))
                            .setPositiveButton("知道了", null)
                            .show();
                    return true;
                }
                showInputDialog("词典查询", "输入词语", new InputCallback() {
                    @Override
                    public void onInput(String text) {
                        openDictionary(text);
                    }
                });
                return true;
            }
        });
        popupMenu.show();
    }

    private void addReaderRecord(String type, String text) {
        int position = mType == 2 ? mPageView.getFirstPos() : mPageView.getPosition();
        int secondPosition = mType == 2 ? mPageView.getSecondPos() : 0;
        ReaderRecord record = new ReaderRecord(type, mNovelUrl, mName, text, mNovelProgress,
                mChapterIndex, position, secondPosition, System.currentTimeMillis());
        mReaderProfileManager.addRecord(record);
    }

    private void showInputDialog(String title, String hint, final InputCallback callback) {
        final EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setSingleLine(false);
        editText.setMinLines(2);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(editText)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        String text = editText.getText().toString().trim();
                        if (text.length() == 0) {
                            showShortToast("内容不能为空");
                            return;
                        }
                        callback.onInput(text);
                    }
                })
                .show();
    }

    private void openDictionary(String text) {
        try {
            String keyword = URLEncoder.encode(text, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://hanyu.baidu.com/s?wd=" + keyword));
            startActivity(intent);
        } catch (Throwable t) {
            showShortToast("无法打开词典");
        }
    }

    private interface InputCallback {
        void onInput(String text);
    }

    private void updateReaderSettingLabels() {
        if (mFontSizeValueTv != null) {
            mFontSizeValueTv.setText(ReaderDisplayPolicy.fontSizeLabel(mTextSize));
        }
        if (mRowSpaceValueTv != null) {
            mRowSpaceValueTv.setText(ReaderDisplayPolicy.rowSpaceLabel(mRowSpace));
        }
        if (mFontFamilyTv != null) {
            mFontFamilyTv.setText("字体 " + ReaderDisplayPolicy.fontName(mReaderFont));
        }
    }

    private void showFontDialog() {
        new AlertDialog.Builder(this)
                .setTitle("选择字体")
                .setSingleChoiceItems(ReaderDisplayPolicy.FONT_NAMES, mReaderFont,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                mReaderFont = ReaderDisplayPolicy.normalizeFontIndex(which);
                                applyReaderFont();
                                updateReaderSettingLabels();
                                dialog.dismiss();
                            }
                        })
                .setNegativeButton("取消", null)
                .show();
    }

    private void applyReaderFont() {
        mPageView.setTypeface(typefaceFor(mReaderFont));
    }

    private Typeface typefaceFor(int fontIndex) {
        switch (ReaderDisplayPolicy.normalizeFontIndex(fontIndex)) {
            case 1:
                return Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case 2:
                return Typeface.SERIF;
            case 3:
                return Typeface.MONOSPACE;
            default:
                return Typeface.DEFAULT;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_read_back:
                finish();
                break;
            case R.id.iv_read_menu:
                showReaderMenu();
                break;
            case R.id.tv_read_previous_chapter:
                // 加载上一章节
                if (mType == 0) {
                    preNet();
                } else if (mType == 2) {
                    preEpub();
                } else if (mType == 1) {
                    showShortToast("本地 TXT 小说暂不支持该功能");
                }
                break;
            case R.id.tv_read_next_chapter:
                // 加载下一章节
                if (mType == 0) {
                    nextNet();
                } else if (mType == 2) {
                    nextEpub();
                } else if (mType == 1) {
                    showShortToast("本地 TXT 小说暂不支持该功能");
                }
                break;
            case R.id.iv_read_catalog:
            case R.id.tv_read_catalog:
                // 目录
                if (mType == 0) {
                    // 跳转到目录页面，并且将自己的引用传递给它
                    Event<HoldReadActivityEvent> event = new Event<>(EventBusCode.CATALOG_HOLD_READ_ACTIVITY,
                            new HoldReadActivityEvent(ReadActivity.this));
                    EventBusUtil.sendStickyEvent(event);
                    Intent intent = new Intent(ReadActivity.this, CatalogActivity.class);
                    intent.putExtra(CatalogActivity.KEY_URL, mNovelUrl);    // 传递当前小说的 url
                    intent.putExtra(CatalogActivity.KEY_NAME, mName);  // 传递当前小说的名字
                    intent.putExtra(CatalogActivity.KEY_COVER, mCover); // 传递当前小说的封面
                    startActivity(intent);
                } else if (mType == 1) {
                    showShortToast("本地 TXT 小说暂不支持该功能");
                } else if (mType == 2) {
                    // 跳转到 epub 目录界面
                    Event<EpubCatalogInitEvent> event = new Event<>(EventBusCode.EPUB_CATALOG_INIT,
                            new EpubCatalogInitEvent(ReadActivity.this, mEpubTocList,
                                    mOpfData, mNovelUrl, mName, mCover));
                    EventBusUtil.sendStickyEvent(event);
                    jumpToNewActivity(EpubCatalogActivity.class);
                }
                break;
            case R.id.iv_read_brightness:
            case R.id.tv_read_brightness:
                // 隐藏上下栏，并显示亮度栏
                hideBar();
                showBrightnessBar();
                break;
            case R.id.iv_read_day_and_night_mode:
            case R.id.tv_read_day_and_night_mode:
                if (!mIsNightMode) {    // 进入夜间模式
                    nightMode();
                } else {    // 进入日间模式
                    dayMode();
                }
                hideBar();
                break;
            case R.id.iv_read_setting:
            case R.id.tv_read_setting:
                // 隐藏上下栏，并显示设置栏
                hideBar();
                showSettingBar();
                break;
            case R.id.iv_read_decrease_font:
                if (mTextSize <= mMinTextSize) {
                    break;
                }
                mTextSize = ReaderDisplayPolicy.decreaseFontSize(mTextSize);
                mPageView.setTextSize(mTextSize);
                updateReaderSettingLabels();
                break;
            case R.id.iv_read_increase_font:
                if (mTextSize >= mMaxTextSize) {
                    break;
                }
                mTextSize = ReaderDisplayPolicy.increaseFontSize(mTextSize);
                mPageView.setTextSize(mTextSize);
                updateReaderSettingLabels();
                break;
            case R.id.iv_read_decrease_row_space:
                if (mRowSpace <= mMinRowSpace) {
                    break;
                }
                mRowSpace = ReaderDisplayPolicy.decreaseRowSpace(mRowSpace);
                mPageView.setRowSpace(mRowSpace);
                updateReaderSettingLabels();
                break;
            case R.id.iv_read_increase_row_space:
                if (mRowSpace >= mMaxRowSpace) {
                    break;
                }
                mRowSpace = ReaderDisplayPolicy.increaseRowSpace(mRowSpace);
                mPageView.setRowSpace(mRowSpace);
                updateReaderSettingLabels();
                break;
            case R.id.tv_read_font_family:
                showFontDialog();
                break;
            case R.id.v_read_theme_0:
                if (!mIsNightMode && mTheme == 0) {
                    break;
                }
                mTheme = 0;
                updateWithTheme();
                break;
            case R.id.v_read_theme_1:
                if (!mIsNightMode && mTheme == 1) {
                    break;
                }
                mTheme = 1;
                updateWithTheme();
                break;
            case R.id.v_read_theme_2:
                if (!mIsNightMode && mTheme == 2) {
                    break;
                }
                mTheme = 2;
                updateWithTheme();
                break;
            case R.id.v_read_theme_3:
                if (!mIsNightMode && mTheme == 3) {
                    break;
                }
                mTheme = 3;
                updateWithTheme();
                break;
            case R.id.v_read_theme_4:
                if (!mIsNightMode && mTheme == 4) {
                    break;
                }
                mTheme = 4;
                updateWithTheme();
                break;
            case R.id.tv_read_turn_normal:
                if (mTurnType != 0) {
                    mTurnType = 0;
                    mTurnNormalTv.setSelected(true);
                    mTurnRealTv.setSelected(false);
                    mPageView.setTurnType(PageView.TURN_TYPE.NORMAL);
                }
                break;
            case R.id.tv_read_turn_real:
                if (mTurnType != 1) {
                    mTurnType = 1;
                    mTurnRealTv.setSelected(true);
                    mTurnNormalTv.setSelected(false);
                    mPageView.setTurnType(PageView.TURN_TYPE.REAL);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event != null) {
            VolumePageTurnPolicy.TurnDirection direction =
                    VolumePageTurnPolicy.directionForKeyCode(event.getKeyCode());
            if (direction != VolumePageTurnPolicy.TurnDirection.NONE) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    turnPageByVolumeKey(direction);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void turnPageByVolumeKey(VolumePageTurnPolicy.TurnDirection direction) {
        if (mPageView == null) {
            return;
        }
        if (direction == VolumePageTurnPolicy.TurnDirection.NEXT) {
            mPageView.turnToNextPage();
        } else if (direction == VolumePageTurnPolicy.TurnDirection.PREVIOUS) {
            mPageView.turnToPreviousPage();
        }
    }

    /**
     * 进入夜间模式
     */
    private void nightMode() {
        mIsNightMode = true;
        // 取消主题
        mTheme0.setSelected(false);
        mTheme1.setSelected(false);
        mTheme2.setSelected(false);
        mTheme3.setSelected(false);
        mTheme4.setSelected(false);
        // 设置图标和文字
        mDayAndNightModeIv.setImageResource(R.drawable.read_day);
        mDayAndNightModeTv.setText(getResources().getString(R.string.read_day_mode));
        // 设置相关颜色
        mNovelTitleTv.setTextColor(getResources().getColor(R.color.read_night_mode_title));
        mNovelProgressTv.setTextColor(getResources().getColor(R.color.read_night_mode_title));
        mStateTv.setTextColor(getResources().getColor(R.color.read_night_mode_text));
        mPageView.setBgColor(getResources().getColor(R.color.read_night_mode_bg));
        mPageView.setTextColor(getResources().getColor(R.color.read_night_mode_text));
        mPageView.setBackBgColor(getResources().getColor(R.color.read_night_mode_back_bg));
        mPageView.setBackTextColor(getResources().getColor(R.color.read_night_mode_back_text));
        mPageView.post(new Runnable() {
            @Override
            public void run() {
                mPageView.updateBitmap();
            }
        });
    }

    /**
     * 进入白天模式
     */
    private void dayMode() {
        mIsNightMode = false;
        // 设置图标和文字
        mDayAndNightModeIv.setImageResource(R.drawable.read_night);
        mDayAndNightModeTv.setText(getResources().getString(R.string.read_night_mode));
        // 根据主题进行相关设置
        mPageView.post(new Runnable() {
            @Override
            public void run() {
                updateWithTheme();
            }
        });
    }

    /**
     * 根据主题更新阅读界面
     */
    private void updateWithTheme() {
        if (mIsNightMode) {
            // 退出夜间模式
            mDayAndNightModeIv.setImageResource(R.drawable.read_night);
            mDayAndNightModeTv.setText(getResources().getString(R.string.read_night_mode));
            mIsNightMode = false;
        }
        mTheme0.setSelected(false);
        mTheme1.setSelected(false);
        mTheme2.setSelected(false);
        mTheme3.setSelected(false);
        mTheme4.setSelected(false);
        int bgColor = getResources().getColor(R.color.read_theme_0_bg);
        int textColor = getResources().getColor(R.color.read_theme_0_text);
        int backBgColor = getResources().getColor(R.color.read_theme_0_back_bg);
        int backTextColor = getResources().getColor(R.color.read_theme_0_back_text);
        switch (mTheme) {
            case 0:
                mTheme0.setSelected(true);
                bgColor = getResources().getColor(R.color.read_theme_0_bg);
                textColor = getResources().getColor(R.color.read_theme_0_text);
                backBgColor = getResources().getColor(R.color.read_theme_0_back_bg);
                backTextColor = getResources().getColor(R.color.read_theme_0_back_text);
                break;
            case 1:
                mTheme1.setSelected(true);
                bgColor = getResources().getColor(R.color.read_theme_1_bg);
                textColor = getResources().getColor(R.color.read_theme_1_text);
                backBgColor = getResources().getColor(R.color.read_theme_1_back_bg);
                backTextColor = getResources().getColor(R.color.read_theme_1_back_text);
                break;
            case 2:
                mTheme2.setSelected(true);
                bgColor = getResources().getColor(R.color.read_theme_2_bg);
                textColor = getResources().getColor(R.color.read_theme_2_text);
                backBgColor = getResources().getColor(R.color.read_theme_2_back_bg);
                backTextColor = getResources().getColor(R.color.read_theme_2_back_text);
                break;
            case 3:
                mTheme3.setSelected(true);
                bgColor = getResources().getColor(R.color.read_theme_3_bg);
                textColor = getResources().getColor(R.color.read_theme_3_text);
                backBgColor = getResources().getColor(R.color.read_theme_3_back_bg);
                backTextColor = getResources().getColor(R.color.read_theme_3_back_text);
                break;
            case 4:
                mTheme4.setSelected(true);
                bgColor = getResources().getColor(R.color.read_theme_4_bg);
                textColor = getResources().getColor(R.color.read_theme_4_text);
                backBgColor = getResources().getColor(R.color.read_theme_4_back_bg);
                backTextColor = getResources().getColor(R.color.read_theme_4_back_text);
                break;
        }
        // 设置相关颜色
        mNovelTitleTv.setTextColor(textColor);
        mNovelProgressTv.setTextColor(textColor);
        mStateTv.setTextColor(textColor);
        mPageView.setTextColor(textColor);
        mPageView.setBgColor(bgColor);
        mPageView.setBackTextColor(backTextColor);
        mPageView.setBackBgColor(backBgColor);
        mPageView.updateBitmap();
        if (PageView.IS_TEST) {
            mPageView.setBackgroundColor(bgColor);
            mPageView.invalidate();
        }
    }

    /**
     * 网络小说加载上一章节
     */
    private void preNet() {
        if (mChapterIndex == 0) {
            showShortToast("已经到最前了");
            return;
        }
        // 加载上一章节
        mJumpToChapterEnd = true;
        mChapterIndex--;
        showChapter();
    }

    /**
     * Epub 小说加载上一章节
     */
    private void preEpub() {
        if (mChapterIndex == 0) {
            showShortToast("已经到最前了");
            return;
        }
        // 加载上一章节
        mJumpToChapterEnd = true;
        mChapterIndex--;
        showChapter();
    }

    /**
     * 网络小说加载下一章节
     */
    private void nextNet() {
        if (mChapterIndex == mChapterUrlList.size() - 1) {
            showShortToast("已经到最后了");
            return;
        }
        // 加载下一章节
        mJumpToChapterEnd = false;
        mChapterIndex++;
        showChapter();
    }

    /**
     * Epub 小说加载下一章节
     */
    private void nextEpub() {
        if (mChapterIndex == mOpfData.getSpine().size() - 1) {
            showShortToast("已经到最后了");
            return;
        }
        // 加载下一章节
        mJumpToChapterEnd = false;
        mChapterIndex++;
        showChapter();
    }

    /**
     * 更新章节进度条的进度
     */
    private void updateChapterProgress() {
        int progress = 0;
        if (mType == 0) {   // 网络小说
            if (!mNetCatalogList.isEmpty()) {
                progress = (int) (100 * ((float) mChapterIndex / (mNetCatalogList.size() - 1)));
            }
        } else if (mType == 1) {    // 本地 txt
            if (mNovelProgress.equals("")) {
                if (mNovelContent.length() != 0) {
                    progress = (int) (100 * (float)mPosition / (mNovelContent.length() - 1));
                }
            } else {
                try {
                    progress = (int) Float.parseFloat(
                            mNovelProgress.substring(0, mNovelProgress.length()-1));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } else if (mType == 2) {    // 本地 epub
            if (!mEpubTocList.isEmpty()) {
                progress = (int) (100 * ((float)mChapterIndex / (mEpubTocList.size() - 1)));
            }
        }

        mNovelProcessSb.setProgress(progress);
    }
}
