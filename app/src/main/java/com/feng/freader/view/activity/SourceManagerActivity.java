package com.feng.freader.view.activity;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.feng.freader.R;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.source.BookSource;
import com.feng.freader.source.BookSourceParser;
import com.feng.freader.source.SourceRepository;
import com.feng.freader.util.StatusBarUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SourceManagerActivity extends BaseActivity implements View.OnClickListener {
    private static final int REQ_QR = 5601;
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private EditText mJsonEt;
    private EditText mUrlEt;
    private LinearLayout mListLl;
    private SourceRepository mRepository;

    @Override
    protected void doBeforeSetContentView() {
        StatusBarUtil.setLightColorStatusBar(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_source_manager;
    }

    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected void initData() {
        mRepository = SourceRepository.getInstance();
    }

    @Override
    protected void initView() {
        findViewById(R.id.tv_source_manager_title).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_clipboard).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_url).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_qr).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_save).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_export).setOnClickListener(this);
        findViewById(R.id.btn_source_manager_debug).setOnClickListener(this);
        mJsonEt = findViewById(R.id.et_source_manager_json);
        mUrlEt = findViewById(R.id.et_source_manager_url);
        mListLl = findViewById(R.id.ll_source_manager_list);
        refreshList();
    }

    @Override
    protected void doAfterInit() {
    }

    @Override
    protected boolean isRegisterEventBus() {
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.tv_source_manager_title:
                finish();
                break;
            case R.id.btn_source_manager_clipboard:
                importFromClipboard();
                break;
            case R.id.btn_source_manager_url:
                importFromUrl();
                break;
            case R.id.btn_source_manager_qr:
                importFromQr();
                break;
            case R.id.btn_source_manager_save:
                importFromEditor();
                break;
            case R.id.btn_source_manager_export:
                exportAll();
                break;
            case R.id.btn_source_manager_debug:
                startActivity(new Intent(this, SourceDebuggerActivity.class));
                break;
            default:
                break;
        }
    }

    private void importFromClipboard() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData data = manager == null ? null : manager.getPrimaryClip();
        if (data == null || data.getItemCount() == 0) {
            showShortToast("剪贴板为空");
            return;
        }
        CharSequence text = data.getItemAt(0).coerceToText(this);
        mJsonEt.setText(text);
        importFromEditor();
    }

    private void importFromUrl() {
        final String url = mUrlEt.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            showShortToast("请输入 URL");
            return;
        }
        showShortToast("正在下载书源");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String error = "";
                String json = "";
                Response response = null;
                try {
                    response = new OkHttpClient().newCall(new Request.Builder().url(url).build()).execute();
                    json = response.body() == null ? "" : response.body().string();
                    if (!response.isSuccessful()) {
                        error = "HTTP " + response.code();
                    }
                } catch (Throwable t) {
                    error = t.getMessage() == null ? "下载失败" : t.getMessage();
                } finally {
                    if (response != null) {
                        response.close();
                    }
                }
                final String finalJson = json;
                final String finalError = error;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        if (TextUtils.isEmpty(finalError)) {
                            mJsonEt.setText(finalJson);
                            importFromEditor();
                        } else {
                            showShortToast(finalError);
                        }
                    }
                });
            }
        }).start();
    }

    private void importFromQr() {
        try {
            Intent intent = new Intent("com.google.zxing.client.android.SCAN");
            intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
            startActivityForResult(intent, REQ_QR);
        } catch (Throwable t) {
            showShortToast("未找到扫码应用，请用剪贴板或 URL 导入");
        }
    }

    private void importFromEditor() {
        String json = mJsonEt.getText().toString().trim();
        int count = mRepository.importJson(json);
        if (count <= 0) {
            showShortToast("未识别到书源");
            return;
        }
        refreshList();
        showShortToast("已导入 " + count + " 个书源");
    }

    private void exportAll() {
        String json = mRepository.exportJson();
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("BookSource JSON", json));
        }
        try {
            File dir = new File(getExternalFilesDir(null), "sources");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, "book_sources.json");
            FileOutputStream outputStream = new FileOutputStream(file);
            try {
                outputStream.write(json.getBytes(UTF_8));
            } finally {
                outputStream.close();
            }
            showShortToast("已导出并复制到剪贴板");
        } catch (Throwable t) {
            showShortToast("已复制到剪贴板");
        }
    }

    private void refreshList() {
        mListLl.removeAllViews();
        List<BookSource> sources = mRepository.getAll();
        for (final BookSource source : sources) {
            mListLl.addView(createSourceRow(source));
        }
    }

    private View createSourceRow(final BookSource source) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView name = new TextView(this);
        name.setText(source.getName() + "\n" + source.getId());
        name.setTextColor(getResources().getColor(R.color.more_text));
        name.setTextSize(15);
        row.addView(name, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        Switch enabled = new Switch(this);
        enabled.setChecked(source.isEnabled());
        enabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mRepository.setEnabled(source.getId(), isChecked);
            }
        });
        row.addView(enabled);

        TextView edit = actionText("编辑");
        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BookSource> one = new ArrayList<>();
                one.add(source);
                mJsonEt.setText(BookSourceParser.toJson(one));
            }
        });
        row.addView(edit);

        TextView delete = actionText("删除");
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRepository.delete(source.getId());
                refreshList();
            }
        });
        row.addView(delete);

        return row;
    }

    private TextView actionText(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setGravity(Gravity.CENTER);
        textView.setTextColor(getResources().getColor(R.color.colorAccent));
        textView.setPadding(14, 8, 0, 8);
        return textView;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_QR && resultCode == Activity.RESULT_OK && data != null) {
            String result = data.getStringExtra("SCAN_RESULT");
            if (!TextUtils.isEmpty(result)) {
                mJsonEt.setText(result);
                importFromEditor();
            }
        }
    }
}
