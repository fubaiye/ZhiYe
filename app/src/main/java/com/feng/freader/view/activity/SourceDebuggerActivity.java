package com.feng.freader.view.activity;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.feng.freader.R;
import com.feng.freader.base.BaseActivity;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.source.SourceDebugResult;
import com.feng.freader.source.SourceDebugger;
import com.feng.freader.util.StatusBarUtil;

public class SourceDebuggerActivity extends BaseActivity implements View.OnClickListener {
    private EditText mUrlEt;
    private EditText mCssEt;
    private EditText mXpathEt;
    private EditText mJsonPathEt;
    private TextView mResultTv;

    @Override
    protected void doBeforeSetContentView() {
        StatusBarUtil.setLightColorStatusBar(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_source_debugger;
    }

    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void initView() {
        findViewById(R.id.tv_source_debugger_title).setOnClickListener(this);
        findViewById(R.id.btn_source_debugger_run).setOnClickListener(this);
        mUrlEt = findViewById(R.id.et_source_debugger_url);
        mCssEt = findViewById(R.id.et_source_debugger_css);
        mXpathEt = findViewById(R.id.et_source_debugger_xpath);
        mJsonPathEt = findViewById(R.id.et_source_debugger_jsonpath);
        mResultTv = findViewById(R.id.tv_source_debugger_result);
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
        if (view.getId() == R.id.tv_source_debugger_title) {
            finish();
            return;
        }
        runDebugger();
    }

    private void runDebugger() {
        final String url = mUrlEt.getText().toString().trim();
        if (TextUtils.isEmpty(url)) {
            showShortToast("请输入 URL");
            return;
        }
        mResultTv.setText("正在运行...");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String text;
                try {
                    SourceDebugResult result = new SourceDebugger().run(url,
                            mCssEt.getText().toString(),
                            mXpathEt.getText().toString(),
                            mJsonPathEt.getText().toString());
                    text = result.toDisplayText();
                } catch (Throwable t) {
                    text = t.getMessage() == null ? "调试失败" : t.getMessage();
                }
                final String finalText = text;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mResultTv.setText(finalText);
                    }
                });
            }
        }).start();
    }
}
