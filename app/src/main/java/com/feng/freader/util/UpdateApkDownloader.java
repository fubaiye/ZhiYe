package com.feng.freader.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class UpdateApkDownloader {
    private static final String APK_MIME_TYPE = "application/vnd.android.package-archive";
    private static final int BUFFER_SIZE = 32 * 1024;

    private final OkHttpClient client;

    public UpdateApkDownloader(OkHttpClient client) {
        this.client = client;
    }

    public void download(String apkUrl, File targetFile, ProgressListener listener)
            throws IOException {
        Request request = new Request.Builder()
                .url(apkUrl)
                .header("User-Agent", "ZhiYe Android Updater")
                .header("Accept", APK_MIME_TYPE + ",application/octet-stream,*/*")
                .build();

        Response response = client.newCall(request).execute();
        try {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected http status " + response.code()
                        + " for " + response.request().url());
            }
            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("Empty update response body");
            }
            streamToFile(body, targetFile, listener);
        } finally {
            response.close();
        }
    }

    private void streamToFile(ResponseBody body, File targetFile, ProgressListener listener)
            throws IOException {
        long totalBytes = body.contentLength();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(body.byteStream());
            outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int count;
            long downloadedBytes = 0L;
            notifyProgress(listener, downloadedBytes, totalBytes);
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
                downloadedBytes += count;
                notifyProgress(listener, downloadedBytes, totalBytes);
            }
            outputStream.flush();
            notifyProgress(listener, downloadedBytes, totalBytes);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private void notifyProgress(ProgressListener listener, long downloadedBytes, long totalBytes) {
        if (listener != null) {
            listener.onProgress(downloadedBytes, totalBytes);
        }
    }

    public interface ProgressListener {
        void onProgress(long downloadedBytes, long totalBytes);
    }
}
