package com.feng.freader.online;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.feng.freader.R;
import com.feng.freader.constant.EventBusCode;
import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.entity.eventbus.Event;
import com.feng.freader.http.NetworkClientFactory;
import com.feng.freader.util.EventBusUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

import okhttp3.Request;
import okhttp3.Response;

public class OnlineBookDownloadService extends Service {
    public static final String ACTION_DOWNLOAD = "com.feng.freader.online.DOWNLOAD";
    public static final String EXTRA_SOURCE_ID = "source_id";
    public static final String EXTRA_SOURCE_NAME = "source_name";
    public static final String EXTRA_BOOK_ID = "book_id";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_COVER = "cover";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_MIME = "mime";
    public static final String EXTRA_URL = "url";
    private static final String CHANNEL_ID = "online_book_download";
    private static final int NOTIFICATION_ID = 3030;

    public static void enqueue(Context context, OnlineBook book, BookFormat format) {
        Intent intent = new Intent(context, OnlineBookDownloadService.class);
        intent.setAction(ACTION_DOWNLOAD);
        intent.putExtra(EXTRA_SOURCE_ID, book.getSourceId());
        intent.putExtra(EXTRA_SOURCE_NAME, book.getSourceName());
        intent.putExtra(EXTRA_BOOK_ID, book.getId());
        intent.putExtra(EXTRA_TITLE, book.getTitle());
        intent.putExtra(EXTRA_COVER, book.getCoverUrl());
        intent.putExtra(EXTRA_TYPE, format.getType());
        intent.putExtra(EXTRA_MIME, format.getMimeType());
        intent.putExtra(EXTRA_URL, format.getDownloadUrl());
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, notification("在线书库下载", "等待下载任务", 0));
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        if (intent == null || !ACTION_DOWNLOAD.equals(intent.getAction())) {
            return START_NOT_STICKY;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                download(intent);
                stopSelf();
            }
        }).start();
        return START_NOT_STICKY;
    }

    private void download(Intent intent) {
        String title = safe(intent.getStringExtra(EXTRA_TITLE));
        String sourceId = safe(intent.getStringExtra(EXTRA_SOURCE_ID));
        String sourceName = safe(intent.getStringExtra(EXTRA_SOURCE_NAME));
        String bookId = safe(intent.getStringExtra(EXTRA_BOOK_ID));
        String cover = safe(intent.getStringExtra(EXTRA_COVER));
        String type = safe(intent.getStringExtra(EXTRA_TYPE));
        String mime = safe(intent.getStringExtra(EXTRA_MIME));
        String url = safe(intent.getStringExtra(EXTRA_URL));
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        try {
            if (!OnlineDownloadValidator.isAllowed(url, mime)) {
                throw new IOException("下载格式不受支持或来源不明确");
            }
            if (DatabaseManager.getInstance().isExistInBookshelfNovel(url)) {
                throw new IOException("该图书已经下载过");
            }
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "ZhiYe/1.4")
                    .build();
            Response response = NetworkClientFactory.shared().newCall(request).execute();
            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }
            String responseType = response.header("Content-Type", mime);
            if (!OnlineDownloadValidator.isAllowed(url, responseType)) {
                throw new IOException("服务器返回的文件类型不受支持");
            }
            File output = writeFile(title, type, response, manager);
            String hash = sha256(output);
            addToBookshelf(output, title, cover, type, sourceName, url);
            new OnlineDownloadRecordStore().add(new OnlineDownloadRecord(sourceId, bookId,
                    sourceName, url, output.getAbsolutePath(), hash));
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification("下载完成", title, 100));
            }
            EventBusUtil.sendEvent(new Event(EventBusCode.BOOKSHELF_UPDATE_LIST, null));
        } catch (Throwable throwable) {
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification("下载失败",
                        title + "：" + throwable.getMessage(), 0));
            }
        }
    }

    private File writeFile(String title, String type, Response response, NotificationManager manager) throws IOException {
        File dir = new File(getExternalFilesDir(null), "onlineBooks");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String ext = extension(type);
        File file = uniqueFile(dir, sanitize(title), ext);
        InputStream input = null;
        FileOutputStream output = null;
        try {
            long total = response.body() == null ? -1 : response.body().contentLength();
            input = response.body().byteStream();
            output = new FileOutputStream(file);
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
                downloaded += len;
                if (total > 0 && manager != null) {
                    int progress = (int) Math.min(99, downloaded * 100 / total);
                    manager.notify(NOTIFICATION_ID, notification("正在下载", title, progress));
                }
            }
        } finally {
            if (output != null) {
                output.close();
            }
            if (input != null) {
                input.close();
            }
        }
        if (BookFormat.TYPE_PDF.equals(type)) {
            File note = uniqueFile(dir, sanitize(title) + "_PDF", ".txt");
            FileOutputStream noteOut = new FileOutputStream(note);
            noteOut.write(("PDF 已下载到：\n" + file.getAbsolutePath()
                    + "\n\n知页当前内置阅读器主要支持 TXT/EPUB，PDF 文件请使用系统 PDF 阅读器打开。").getBytes("UTF-8"));
            noteOut.close();
            return note;
        }
        return file;
    }

    private void addToBookshelf(File file, String title, String cover, String type,
                                String sourceName, String url) {
        int readerType = BookFormat.TYPE_EPUB.equals(type) ? 2 : 1;
        String name = title.isEmpty() ? file.getName() : title;
        if (BookFormat.TYPE_PDF.equals(type)) {
            name = name + " PDF";
        }
        DatabaseManager db = DatabaseManager.getInstance();
        if (!db.isExistInBookshelfNovel(file.getAbsolutePath())) {
            db.insertBookshelfNovel(new BookshelfNovelDbData(file.getAbsolutePath(), name,
                    cover, 0, 0, readerType));
        }
    }

    private Notification notification(String title, String text, int progress) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.zhiye_icon)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true);
        if (progress > 0 && progress < 100) {
            builder.setProgress(100, progress, false);
        }
        return builder.build();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    "在线书库下载", NotificationManager.IMPORTANCE_LOW));
        }
    }

    private static String extension(String type) {
        if (BookFormat.TYPE_EPUB.equals(type)) {
            return ".epub";
        }
        if (BookFormat.TYPE_PDF.equals(type)) {
            return ".pdf";
        }
        if (BookFormat.TYPE_HTML.equals(type)) {
            return ".html";
        }
        return ".txt";
    }

    private static File uniqueFile(File dir, String title, String ext) {
        File file = new File(dir, title + ext);
        int index = 1;
        while (file.exists()) {
            file = new File(dir, title + "_" + index + ext);
            index++;
        }
        return file;
    }

    private static String sanitize(String value) {
        String safe = value == null || value.trim().isEmpty() ? "online_book" : value.trim();
        return safe.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        InputStream input = new java.io.FileInputStream(file);
        try {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = input.read(buffer)) != -1) {
                digest.update(buffer, 0, len);
            }
        } finally {
            input.close();
        }
        byte[] bytes = digest.digest();
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
