package com.feng.freader.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.feng.freader.model.UpdateInfo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class UpdateInstaller {
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final String UPDATE_FILE_NAME = "ZhiYe-update.apk";
    private static final String TEMP_UPDATE_FILE_NAME = UPDATE_FILE_NAME + ".download";
    private static final int CONNECT_TIMEOUT_MS = 30000;
    private static final int READ_TIMEOUT_MS = 120000;
    private static final int MAX_REDIRECTS = 8;
    private static final int MAX_RETRY_COUNT = 3;

    private UpdateInstaller() {
    }

    public static void downloadAndInstall(Context context, UpdateInfo updateInfo) {
        if (context == null || updateInfo == null || !updateInfo.isValid()) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !appContext.getPackageManager().canRequestPackageInstalls()) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + appContext.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appContext.startActivity(intent);
            showToast(appContext, "请允许知页安装未知来源应用后再次点击更新", Toast.LENGTH_LONG);
            return;
        }

        final File updateDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (updateDir == null) {
            showToast(appContext, "无法创建更新下载目录", Toast.LENGTH_SHORT);
            return;
        }
        if (!updateDir.exists() && !updateDir.mkdirs()) {
            showToast(appContext, "无法创建更新下载目录", Toast.LENGTH_SHORT);
            return;
        }
        final File apkFile = new File(updateDir, UPDATE_FILE_NAME);
        final File tempFile = new File(updateDir, TEMP_UPDATE_FILE_NAME);
        final String apkUrl = updateInfo.getApkUrl();
        final long expectedSize = updateInfo.getApkSize();
        showToast(appContext, "正在下载更新包", Toast.LENGTH_SHORT);

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!deleteIfExists(apkFile) || !deleteIfExists(tempFile)) {
                    postToast(appContext, "无法清理旧更新包");
                    return;
                }
                try {
                    downloadWithRetry(apkUrl, tempFile);
                    if (!tempFile.renameTo(apkFile)) {
                        throw new IOException("Unable to move update apk");
                    }
                    if (!ApkFileValidator.isValidApk(apkFile, expectedSize)) {
                        deleteIfExists(apkFile);
                        postToast(appContext, "更新包校验失败，请稍后重试");
                        return;
                    }
                    postOpenInstaller(appContext, apkFile);
                } catch (Throwable throwable) {
                    deleteIfExists(apkFile);
                    deleteIfExists(tempFile);
                    postToast(appContext, "更新包下载失败，请切换网络后重试");
                }
            }
        }).start();
    }

    private static void downloadWithRetry(String apkUrl, File tempFile) throws IOException {
        IOException lastException = null;
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            try {
                download(apkUrl, tempFile);
                return;
            } catch (IOException e) {
                lastException = e;
                deleteIfExists(tempFile);
                sleepQuietly((i + 1) * 1500L);
            }
        }
        throw lastException == null ? new IOException("Download failed") : lastException;
    }

    private static void download(String apkUrl, File apkFile) throws IOException {
        HttpURLConnection connection = openConnection(apkUrl, 0);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(connection.getInputStream());
            outputStream = new FileOutputStream(apkFile);
            byte[] buffer = new byte[32 * 1024];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            outputStream.flush();
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            connection.disconnect();
        }
    }

    private static HttpURLConnection openConnection(String url, int redirectCount) throws IOException {
        if (redirectCount > MAX_REDIRECTS) {
            throw new IOException("Too many redirects");
        }
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent", "ZhiYe Android Updater");
        connection.setRequestProperty("Accept", APK_MIME_TYPE + ",application/octet-stream,*/*");
        int code = connection.getResponseCode();
        if (code == HttpURLConnection.HTTP_MOVED_PERM
                || code == HttpURLConnection.HTTP_MOVED_TEMP
                || code == HttpURLConnection.HTTP_SEE_OTHER
                || code == 307
                || code == 308) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null || location.length() == 0) {
                throw new IOException("Empty redirect");
            }
            URL next = new URL(new URL(url), location);
            return openConnection(next.toString(), redirectCount + 1);
        }
        if (code < HttpURLConnection.HTTP_OK || code >= HttpURLConnection.HTTP_MULT_CHOICE) {
            connection.disconnect();
            throw new IOException("Unexpected http status " + code);
        }
        return connection;
    }

    private static boolean deleteIfExists(File file) {
        return file == null || !file.exists() || file.delete();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void postOpenInstaller(final Context context, final File apkFile) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                openInstaller(context, apkFile);
            }
        });
    }

    private static void postToast(final Context context, final String text) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                showToast(context, text, Toast.LENGTH_SHORT);
            }
        });
    }

    private static void showToast(Context context, String text, int length) {
        Toast.makeText(context, text, length).show();
    }

    private static void openInstaller(Context context, File apkFile) {
        Uri apkUri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                apkFile);
        Intent intent = new Intent(Intent.ACTION_INSTALL_PACKAGE);
        intent.setDataAndType(apkUri, APK_MIME_TYPE);
        intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        intent.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}
