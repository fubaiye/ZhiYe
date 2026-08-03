package com.feng.freader.view.activity;

import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.feng.freader.R;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.online.BookFormat;
import com.feng.freader.online.BookPage;
import com.feng.freader.online.OnlineBook;
import com.feng.freader.online.OnlineBookDownloadService;
import com.feng.freader.online.OnlineBookSource;
import com.feng.freader.online.OnlineSearchAggregator;
import com.feng.freader.online.OnlineSourceRegistry;
import com.feng.freader.online.OnlineSourceSettings;
import com.feng.freader.util.NetUtil;
import com.feng.freader.util.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

public class OnlineLibraryActivity extends BaseActivity implements View.OnClickListener {
    private EditText searchEt;
    private TextView searchTv;
    private TextView statusTv;
    private TextView sourceManagerTv;
    private Spinner sourceSpinner;
    private LinearLayout resultLayout;
    private OnlineSourceRegistry registry;
    private OnlineSourceSettings sourceSettings;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<OnlineBook> currentBooks = new ArrayList<>();

    @Override
    protected void doBeforeSetContentView() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_online_library;
    }

    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected void initData() {
        registry = OnlineSourceRegistry.createDefault();
        sourceSettings = new OnlineSourceSettings(this);
        sourceSettings.applyTo(registry.getSources());
    }

    @Override
    protected void initView() {
        StatusBarUtil.setLightColorStatusBar(this);
        StatusBarUtil.applyStatusBarTopPadding(this, findViewById(R.id.ll_online_root));
        searchEt = findViewById(R.id.et_online_search);
        configureSearchInput(searchEt);
        searchTv = findViewById(R.id.tv_online_search);
        statusTv = findViewById(R.id.tv_online_status);
        sourceSpinner = findViewById(R.id.sp_online_source);
        sourceManagerTv = findViewById(R.id.tv_online_source_manager);
        resultLayout = findViewById(R.id.ll_online_results);
        searchTv.setOnClickListener(this);
        sourceManagerTv.setOnClickListener(this);
        searchEt.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    search(searchEt.getText().toString());
                    return true;
                }
                return false;
            }
        });

        List<String> names = new ArrayList<>();
        names.add("全部书源");
        for (OnlineBookSource source : registry.getSources()) {
            names.add(source.getName());
        }
        sourceSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
    }

    @Override
    protected void doAfterInit() {
        loadHome();
    }

    @Override
    protected boolean isRegisterEventBus() {
        return false;
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.tv_online_search) {
            search(searchEt.getText().toString());
        } else if (view.getId() == R.id.tv_online_source_manager) {
            showSourceManager();
        }
    }

    private void loadHome() {
        setStatus("正在加载推荐书目...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    OnlineBookSource source = registry.find("gutenberg");
                    final BookPage page = source == null ? BookPage.empty() : source.getHome();
                    showBooks(page.getItems(), "推荐：Project Gutenberg 公版书");
                } catch (final Throwable throwable) {
                    showError("加载失败：" + throwable.getMessage());
                }
            }
        }).start();
    }

    private void configureSearchInput(EditText editText) {
        editText.setSingleLine(true);
        editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        editText.setImeOptions(EditorInfo.IME_ACTION_SEARCH | EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            editText.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        }
    }

    private void search(final String keyword) {
        if (!NetUtil.hasInternet(this)) {
            setStatus("当前无网络，请稍后重试");
            return;
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            setStatus("请输入搜索关键词");
            return;
        }
        setStatus("正在聚合搜索...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int pos = sourceSpinner.getSelectedItemPosition();
                    BookPage page;
                    if (pos <= 0) {
                        page = new OnlineSearchAggregator().search(registry.getSources(), keyword, 1);
                    } else {
                        OnlineBookSource source = registry.getSources().get(pos - 1);
                        if (!source.isEnabled()) {
                            showError("该书源已停用");
                            return;
                        }
                        page = source.search(keyword, 1);
                    }
                    showBooks(page.getItems(), page.getItems().isEmpty()
                            ? "没有找到可公开访问的图书"
                            : "找到 " + page.getItems().size() + " 本开放图书");
                } catch (final Throwable throwable) {
                    showError("搜索失败：" + throwable.getMessage());
                }
            }
        }).start();
    }

    private void showBooks(final List<OnlineBook> books, final String status) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                currentBooks.clear();
                currentBooks.addAll(books);
                resultLayout.removeAllViews();
                setStatus(status);
                if (books.isEmpty()) {
                    addEmptyView("暂无结果。可切换书源或更换关键词。");
                    return;
                }
                for (int i = 0; i < books.size(); i++) {
                    resultLayout.addView(createBookRow(books.get(i), i));
                }
            }
        });
    }

    private View createBookRow(final OnlineBook book, final int index) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(10), dp(10), dp(10), dp(10));
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, 0, 0, dp(10));
        row.setLayoutParams(rowParams);

        ImageView cover = new ImageView(this);
        LinearLayout.LayoutParams coverParams = new LinearLayout.LayoutParams(dp(64), dp(92));
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(cover, coverParams);
        if (!book.getCoverUrl().isEmpty()) {
            Glide.with(this)
                    .load(book.getCoverUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.cover_place_holder)
                            .error(R.drawable.cover_error))
                    .into(cover);
        } else {
            cover.setImageResource(R.drawable.cover_place_holder);
        }

        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(12), 0, 0, 0);
        row.addView(info, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView title = label(book.getTitle(), 16, "#202124", true);
        TextView author = label(book.getAuthorText().isEmpty() ? "未知作者" : book.getAuthorText(),
                13, "#666666", false);
        TextView source = label(book.getSourceName() + "  " + formats(book), 12, "#4B6F55", false);
        TextView desc = label(book.getDescription(), 12, "#888888", false);
        info.addView(title);
        info.addView(author);
        info.addView(source);
        if (!book.getDescription().isEmpty()) {
            info.addView(desc);
        }
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDetail(book);
            }
        });
        return row;
    }

    private void showDetail(final OnlineBook book) {
        final LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(12), dp(8), dp(12), dp(8));
        box.addView(label(book.getTitle(), 18, "#202124", true));
        box.addView(label("作者：" + fallback(book.getAuthorText(), "未知"), 14, "#333333", false));
        box.addView(label("来源：" + book.getSourceName(), 14, "#4B6F55", false));
        box.addView(label("语言：" + fallback(book.getLanguage(), "未标注"), 14, "#333333", false));
        box.addView(label("许可：" + fallback(book.getLicenseNote(), "仅展示开放访问资源"), 13, "#666666", false));
        box.addView(label("简介：" + fallback(book.getDescription(), "暂无简介"), 13, "#666666", false));
        if (book.getFormats().isEmpty()) {
            box.addView(label("暂不支持下载", 14, "#B00020", true));
        } else {
            box.addView(label("可下载格式：" + formats(book), 14, "#333333", true));
            for (final BookFormat format : book.getFormats()) {
                TextView button = label("下载 " + format.getType().toUpperCase(), 15, "#FFFFFF", true);
                button.setGravity(Gravity.CENTER);
                button.setBackgroundColor(Color.rgb(75, 111, 85));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(42));
                params.setMargins(0, dp(8), 0, 0);
                box.addView(button, params);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        OnlineBookDownloadService.enqueue(OnlineLibraryActivity.this, book, format);
                        showShortToast("已加入下载队列，进度请看通知栏");
                    }
                });
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("图书详情")
                .setView(box)
                .setNegativeButton("关闭", null)
                .show();
    }

    private void showSourceManager() {
        final List<OnlineBookSource> sources = registry.getSources();
        String[] names = new String[sources.size()];
        boolean[] checked = new boolean[sources.size()];
        for (int i = 0; i < sources.size(); i++) {
            names[i] = sources.get(i).getName() + "\n" + sources.get(i).getLicenseNote();
            checked[i] = sources.get(i).isEnabled();
        }
        new AlertDialog.Builder(this)
                .setTitle("在线书源")
                .setMultiChoiceItems(names, checked, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which, boolean isChecked) {
                        sources.get(which).setEnabled(isChecked);
                        sourceSettings.save(sources.get(which));
                    }
                })
                .setPositiveButton("测试连接", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int which) {
                        testSources();
                    }
                })
                .setNegativeButton("完成", null)
                .show();
    }

    private void testSources() {
        setStatus("正在测试书源连接...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final StringBuilder result = new StringBuilder();
                for (OnlineBookSource source : registry.getSources()) {
                    if (!source.isEnabled()) {
                        result.append(source.getName()).append("：已停用\n");
                        continue;
                    }
                    try {
                        source.getHome();
                        result.append(source.getName()).append("：可访问\n");
                    } catch (Throwable throwable) {
                        result.append(source.getName()).append("：").append(throwable.getMessage()).append("\n");
                    }
                }
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setStatus(result.toString());
                    }
                });
            }
        }).start();
    }

    private void addEmptyView(String text) {
        TextView empty = label(text, 15, "#888888", false);
        empty.setGravity(Gravity.CENTER);
        resultLayout.addView(empty, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(120)));
    }

    private void showError(final String text) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                setStatus(text);
                resultLayout.removeAllViews();
                addEmptyView("加载失败，可切换书源或稍后重试。");
            }
        });
    }

    private void setStatus(String text) {
        statusTv.setText(text);
    }

    private TextView label(String text, int sp, String color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text == null ? "" : text);
        tv.setTextSize(sp);
        tv.setTextColor(Color.parseColor(color));
        tv.setPadding(0, dp(2), 0, dp(2));
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return tv;
    }

    private String formats(OnlineBook book) {
        if (book.getFormats().isEmpty()) {
            return "暂不支持下载";
        }
        StringBuilder builder = new StringBuilder();
        for (BookFormat format : book.getFormats()) {
            if (builder.length() > 0) {
                builder.append(" / ");
            }
            builder.append(format.getType().toUpperCase());
        }
        return builder.toString();
    }

    private static String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
