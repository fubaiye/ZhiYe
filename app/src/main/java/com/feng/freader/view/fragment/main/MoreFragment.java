package com.feng.freader.view.fragment.main;

import android.view.View;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;

import com.feng.freader.R;
import com.feng.freader.backup.AppBackupManager;
import com.feng.freader.base.BaseFragment;
import com.feng.freader.base.BasePresenter;
import com.feng.freader.constant.Constant;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.model.UpdateChecker;
import com.feng.freader.model.UpdateInfo;
import com.feng.freader.util.FileUtil;
import com.feng.freader.util.NetUtil;
import com.feng.freader.util.UpdateInstaller;
import com.feng.freader.util.VersionUtil;
import com.feng.freader.view.activity.OnlineLibraryActivity;
import com.feng.freader.view.activity.SourceManagerActivity;
import com.feng.freader.widget.TipDialog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * @author Feng Zhaohao
 * Created on 2019/10/20
 */
public class MoreFragment extends BaseFragment implements View.OnClickListener {
    private View mCheckUpdateV;
    private TextView mVersionTv;
    private View mClearV;
    private TextView mCacheSizeTv;
    private View mOnlineLibraryV;
    private View mSourceManagerV;
    private View mBackupV;
    private View mRestoreV;
    private View mAboutV;

    @Override
    protected void doInOnCreate() {
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_more;
    }

    @Override
    protected void initData() {
    }

    @Override
    protected void initView() {
        mCheckUpdateV = getActivity().findViewById(R.id.v_more_check_update);
        mCheckUpdateV.setOnClickListener(this);
        mVersionTv = getActivity().findViewById(R.id.tv_more_version);
        mVersionTv.setText(VersionUtil.getVersionName(getActivity()));

        mClearV = getActivity().findViewById(R.id.v_more_clear);
        mClearV.setOnClickListener(this);
        mCacheSizeTv = getActivity().findViewById(R.id.tv_more_cache_size);
        mCacheSizeTv.setText(FileUtil.getLocalCacheSize());

        mOnlineLibraryV = getActivity().findViewById(R.id.v_more_online_library);
        mOnlineLibraryV.setOnClickListener(this);

        mSourceManagerV = getActivity().findViewById(R.id.v_more_source_manager);
        mSourceManagerV.setOnClickListener(this);

        mBackupV = getActivity().findViewById(R.id.v_more_backup);
        mBackupV.setOnClickListener(this);
        mRestoreV = getActivity().findViewById(R.id.v_more_restore);
        mRestoreV.setOnClickListener(this);

        mAboutV = getActivity().findViewById(R.id.v_more_about);
        mAboutV.setOnClickListener(this);
    }

    @Override
    protected BasePresenter getPresenter() {
        return null;
    }

    @Override
    protected boolean isRegisterEventBus() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventCome(Event event) {
        switch (event.getCode()) {
            case EventBusCode.MORE_INTO:
                mCacheSizeTv.setText(FileUtil.getLocalCacheSize());
                break;
            default:
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.v_more_check_update:
                checkUpdate();
                break;
            case R.id.v_more_clear:
                showClearCacheDialog();
                break;
            case R.id.v_more_online_library:
                jump2Activity(OnlineLibraryActivity.class);
                break;
            case R.id.v_more_source_manager:
                jump2Activity(SourceManagerActivity.class);
                break;
            case R.id.v_more_backup:
                backupData();
                break;
            case R.id.v_more_restore:
                restoreData();
                break;
            case R.id.v_more_about:
                break;
            default:
                break;
        }
    }

    private void checkUpdate() {
        if (!NetUtil.hasInternet(getActivity())) {
            showShortToast("当前无网络，请检查网络后重试");
            return;
        }
        final int currVersionCode = VersionUtil.getVersionCode(getActivity());
        showShortToast("正在检查更新");
        UpdateChecker.check(Constant.UPDATE_RELEASE_API_URL, new UpdateChecker.Callback() {
            @Override
            public void onSuccess(final UpdateInfo updateInfo) {
                if (!updateInfo.isValid()) {
                    showShortToast("没有找到可安装的更新包");
                    return;
                }
                if (!updateInfo.isNewerThan(currVersionCode)) {
                    showShortToast("已经是最新版本");
                    return;
                }
                showUpdateDialog(updateInfo);
            }

            @Override
            public void onError(String errorMsg) {
                showShortToast("更新源暂时不可用");
            }
        });
    }

    private void showClearCacheDialog() {
        TipDialog tipDialog = new TipDialog.Builder(getActivity())
                .setContent("是否清除缓存")
                .setCancel("否")
                .setEnsure("是")
                .setOnClickListener(new TipDialog.OnClickListener() {
                    @Override
                    public void clickEnsure() {
                        FileUtil.clearLocalCache();
                        mCacheSizeTv.setText(FileUtil.getLocalCacheSize());
                    }

                    @Override
                    public void clickCancel() {
                    }
                })
                .build();
        tipDialog.show();
    }

    private void backupData() {
        showShortToast("正在备份");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    message = "备份完成";
                    new AppBackupManager(getActivity()).createBackup();
                } catch (Throwable t) {
                    message = "备份失败";
                }
                final String finalMessage = message;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        showShortToast(finalMessage);
                    }
                });
            }
        }).start();
    }

    private void restoreData() {
        showShortToast("正在恢复");
        new Thread(new Runnable() {
            @Override
            public void run() {
                String message;
                try {
                    int count = new AppBackupManager(getActivity()).restoreLatestBackup();
                    message = count > 0 ? "恢复完成" : "没有可恢复的数据";
                } catch (Throwable t) {
                    message = "恢复失败";
                }
                final String finalMessage = message;
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        showShortToast(finalMessage);
                    }
                });
            }
        }).start();
    }

    private void showUpdateDialog(final UpdateInfo updateInfo) {
        String versionName = updateInfo.getVersionName();
        String content = "检测到新版本";
        if (!versionName.isEmpty()) {
            content += " v" + versionName;
        }
        if (!updateInfo.getReleaseNotes().trim().isEmpty()) {
            content += "\n\n" + updateInfo.getReleaseNotes();
        }
        content += "\n\n是否下载并安装更新？";
        new TipDialog.Builder(getActivity())
                .setContent(content)
                .setEnsure("更新")
                .setCancel("不了")
                .setOnClickListener(new TipDialog.OnClickListener() {
                    @Override
                    public void clickEnsure() {
                        UpdateInstaller.downloadAndInstall(getActivity(), updateInfo);
                    }

                    @Override
                    public void clickCancel() {
                    }
                })
                .build()
                .show();
    }
}
