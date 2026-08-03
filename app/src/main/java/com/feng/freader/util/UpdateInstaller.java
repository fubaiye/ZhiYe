package com.feng.freader.util;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.feng.freader.model.UpdateInfo;

import java.io.File;

public class UpdateInstaller {
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String UPDATE_FILE_NAME = "ZhiYe-update.apk";

    private UpdateInstaller() {
    }

    public static void downloadAndInstall(Context context, UpdateInfo updateInfo) {
        if (context == null || updateInfo == null || !updateInfo.isValid()) {
            return;
        }
        Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !appContext.getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + appContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
            Toast.makeText(appContext, "请允许知页安装未知来源应用后再次点击更新", Toast.LENGTH_LONG).show();
            return;
        }

        File updateDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (updateDir == null) {
            Toast.makeText(appContext, "无法创建更新下载目录", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            Toast.makeText(appContext, "无法创建更新下载目录", Toast.LENGTH_SHORT).show();
            return;
        }
        final File apkFile = new File(updateDir, UPDATE_FILE_NAME);
        if (apkFile.exists() && !apkFile.delete()) {
            Toast.makeText(appContext, "无法清理旧更新包", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager downloadManager =
                (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            Toast.makeText(appContext, "系统下载服务不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(updateInfo.getApkUrl()))
                .setTitle("知页更新包")
                .setDescription("正在下载知页 " + updateInfo.getVersionName())
                .setMimeType(APK_MIME_TYPE)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(apkFile));
        final long downloadId = downloadManager.enqueue(request);
        Toast.makeText(appContext, "更新包开始下载", Toast.LENGTH_SHORT).show();

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context receiverContext, Intent intent) {
                long completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
                if (completedId != downloadId) {
                    return;
                }
                receiverContext.getApplicationContext().unregisterReceiver(this);
                if (isDownloadSuccessful(receiverContext, downloadId)) {
                    openInstaller(receiverContext, apkFile);
                } else {
                    Toast.makeText(receiverContext, "更新包下载失败，请稍后重试", Toast.LENGTH_SHORT).show();
                }
            }
        };
        appContext.registerReceiver(receiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private static boolean isDownloadSuccessful(Context context, long downloadId) {
        DownloadManager downloadManager =
                (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (downloadManager == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));
            if (cursor != null && cursor.moveToFirst()) {
                int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                return cursor.getInt(statusIndex) == DownloadManager.STATUS_SUCCESSFUL;
            }
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return false;
    }

    private static void openInstaller(Context context, File apkFile) {
        Uri apkUri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                apkFile);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(apkUri, APK_MIME_TYPE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
