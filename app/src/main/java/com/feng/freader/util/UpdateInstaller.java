package com.feng.freader.util;

import android.app.Activity;
import android.app.ProgressDialog;
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
    private static final long PROGRESS_NOTIFY_BYTES = 512L * 1024L;
    private static final long PROGRESS_NOTIFY_INTERVAL_MS = 800L;

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
        final DownloadProgressReporter progressReporter =
                new DownloadProgressReporter(context, expectedSize);
        progressReporter.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (!deleteIfExists(apkFile) || !deleteIfExists(tempFile)) {
                    progressReporter.dismiss();
                    postToast(appContext, "无法清理旧更新包");
                    return;
                }
                try {
                    downloadWithRetry(apkUrl, tempFile, expectedSize, progressReporter);
                    progressReporter.update(tempFile.length(), expectedSize, true);
                    if (!tempFile.renameTo(apkFile)) {
                        throw new IOException("Unable to move update apk");
                    }
                    if (!ApkFileValidator.isValidApk(apkFile, expectedSize)) {
                        deleteIfExists(apkFile);
                        progressReporter.dismiss();
                        postToast(appContext, "更新包校验失败，请稍后重试");
                        return;
                    }
                    progressReporter.dismiss();
                    postOpenInstaller(appContext, apkFile);
                } catch (Throwable throwable) {
                    deleteIfExists(apkFile);
                    deleteIfExists(tempFile);
                    progressReporter.dismiss();
                    postToast(appContext, "更新包下载失败，请切换网络后重试");
                }
            }
        }).start();
    }

    private static void downloadWithRetry(String apkUrl, File tempFile, long expectedSize,
                                          DownloadProgressReporter progressReporter)
            throws IOException {
        IOException lastException = null;
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            try {
                if (i > 0) {
                    progressReporter.setMessage("正在重试下载更新包（第 " + (i + 1) + " 次）");
                }
                download(apkUrl, tempFile, expectedSize, progressReporter);
                return;
            } catch (IOException e) {
                lastException = e;
                deleteIfExists(tempFile);
                sleepQuietly((i + 1) * 1500L);
            }
        }
        throw lastException == null ? new IOException("Download failed") : lastException;
    }

    private static void download(String apkUrl, File apkFile, long expectedSize,
                                 DownloadProgressReporter progressReporter)
            throws IOException {
        HttpURLConnection connection = openConnection(apkUrl, 0);
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            long contentLength = contentLength(connection);
            long totalBytes = contentLength > 0 ? contentLength : expectedSize;
            inputStream = new BufferedInputStream(connection.getInputStream());
            outputStream = new FileOutputStream(apkFile);
            byte[] buffer = new byte[32 * 1024];
            int count;
            long downloadedBytes = 0L;
            long lastNotifiedBytes = 0L;
            long lastNotifiedAt = 0L;
            progressReporter.update(0L, totalBytes, true);
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
                downloadedBytes += count;
                long now = System.currentTimeMillis();
                if (downloadedBytes - lastNotifiedBytes >= PROGRESS_NOTIFY_BYTES
                        || now - lastNotifiedAt >= PROGRESS_NOTIFY_INTERVAL_MS) {
                    progressReporter.update(downloadedBytes, totalBytes, false);
                    lastNotifiedBytes = downloadedBytes;
                    lastNotifiedAt = now;
                }
            }
            outputStream.flush();
            progressReporter.update(downloadedBytes, totalBytes, true);
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

    private static long contentLength(HttpURLConnection connection) {
        if (connection == null) {
            return -1L;
        }
        String value = connection.getHeaderField("Content-Length");
        if (value == null || value.trim().length() == 0) {
            return -1L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Throwable ignored) {
            return -1L;
        }
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

    private static class DownloadProgressReporter {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final Context appContext;
        private final Activity activity;
        private final long expectedSize;
        private ProgressDialog dialog;

        DownloadProgressReporter(Context context, long expectedSize) {
            this.appContext = context.getApplicationContext();
            this.activity = context instanceof Activity ? (Activity) context : null;
            this.expectedSize = expectedSize;
        }

        void show() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (activity == null || activity.isFinishing()) {
                        showToast(appContext, "正在下载更新包："
                                + UpdateDownloadProgressFormatter.format(0L, expectedSize),
                                Toast.LENGTH_LONG);
                        return;
                    }
                    dialog = new ProgressDialog(activity);
                    dialog.setTitle("正在下载更新包");
                    dialog.setMessage(UpdateDownloadProgressFormatter.format(0L, expectedSize));
                    dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                    dialog.setIndeterminate(expectedSize <= 0L);
                    dialog.setCancelable(false);
                    if (expectedSize > 0L) {
                        dialog.setMax(100);
                        dialog.setProgress(0);
                    }
                    dialog.show();
                }
            });
        }

        void setMessage(final String message) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.setMessage(message);
                    } else {
                        showToast(appContext, message, Toast.LENGTH_SHORT);
                    }
                }
            });
        }

        void update(final long downloadedBytes, final long totalBytes, final boolean forceToast) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    String message = UpdateDownloadProgressFormatter.format(downloadedBytes, totalBytes);
                    if (dialog != null && dialog.isShowing()) {
                        dialog.setMessage(message);
                        if (totalBytes > 0L) {
                            dialog.setIndeterminate(false);
                            dialog.setMax(100);
                            dialog.setProgress((int) Math.min(100L,
                                    downloadedBytes * 100L / totalBytes));
                        } else {
                            dialog.setIndeterminate(true);
                        }
                    } else if (forceToast) {
                        showToast(appContext, "正在下载更新包：" + message, Toast.LENGTH_LONG);
                    }
                }
            });
        }

        void dismiss() {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                    dialog = null;
                }
            });
        }
    }
}
