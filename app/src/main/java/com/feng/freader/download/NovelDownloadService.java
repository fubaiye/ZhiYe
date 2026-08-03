package com.feng.freader.download;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.feng.freader.R;
import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.http.WikisourceApi;
import com.feng.freader.source.SourceHttpClient;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NovelDownloadService extends Service {
    public static final String ACTION_ENQUEUE = "com.feng.freader.download.ENQUEUE";
    public static final String ACTION_PAUSE = "com.feng.freader.download.PAUSE";
    public static final String ACTION_RESUME = "com.feng.freader.download.RESUME";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_COVER = "cover";
    private static final String CHANNEL_ID = "novel_download";
    private static final int NOTIFICATION_ID = 2024;
    private static final int MAX_RETRY = 3;

    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private DownloadQueueStore store;

    public static void enqueue(Context context, String name, String url, String cover) {
        Intent intent = new Intent(context, NovelDownloadService.class);
        intent.setAction(ACTION_ENQUEUE);
        intent.putExtra(EXTRA_ID, UUID.randomUUID().toString());
        intent.putExtra(EXTRA_NAME, name);
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_COVER, cover);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        store = DownloadQueueStore.getInstance();
        createChannel();
        startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.zhiye_icon)
                .setContentTitle("知页下载器")
                .setContentText("下载队列正在运行")
                .setOngoing(true)
                .build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || intent.getAction() == null) {
            return START_STICKY;
        }
        if (ACTION_ENQUEUE.equals(intent.getAction())) {
            DownloadRequest request = new DownloadRequest(
                    intent.getStringExtra(EXTRA_ID),
                    intent.getStringExtra(EXTRA_NAME),
                    intent.getStringExtra(EXTRA_URL),
                    intent.getStringExtra(EXTRA_COVER));
            store.upsert(request);
            runDownload(request);
        } else if (ACTION_PAUSE.equals(intent.getAction())) {
            setState(intent.getStringExtra(EXTRA_ID), DownloadState.PAUSED);
        } else if (ACTION_RESUME.equals(intent.getAction())) {
            DownloadRequest request = store.find(intent.getStringExtra(EXTRA_ID));
            if (request != null) {
                request.setState(DownloadState.QUEUED);
                store.upsert(request);
                runDownload(request);
            }
        }
        return START_STICKY;
    }

    private void runDownload(final DownloadRequest request) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                downloadWithRetry(request);
            }
        });
    }

    private void downloadWithRetry(DownloadRequest request) {
        while (request.getRetryCount() < MAX_RETRY) {
            DownloadRequest latest = store.find(request.getId());
            if (latest != null && latest.getState() == DownloadState.PAUSED) {
                return;
            }
            try {
                request.setState(DownloadState.RUNNING);
                request.setProgress(10);
                store.upsert(request);
                String content = fetchContent(request);
                request.setProgress(80);
                store.upsert(request);
                File file = OfflineBookCache.writeTxt(request.getName(), content);
                DatabaseManager db = DatabaseManager.getInstance();
                if (!db.isExistInBookshelfNovel(file.getAbsolutePath())) {
                    db.insertBookshelfNovel(new BookshelfNovelDbData(file.getAbsolutePath(),
                            request.getName(), request.getCover(), 0, 0, 1));
                }
                request.setOutputPath(file.getAbsolutePath());
                request.setState(DownloadState.COMPLETED);
                request.setProgress(100);
                store.upsert(request);
                return;
            } catch (Throwable throwable) {
                request.setRetryCount(request.getRetryCount() + 1);
                request.setError(throwable.getMessage());
                request.setState(request.getRetryCount() >= MAX_RETRY
                        ? DownloadState.FAILED
                        : DownloadState.QUEUED);
                store.upsert(request);
            }
        }
    }

    private String fetchContent(DownloadRequest request) throws Exception {
        if (WikisourceApi.isWikisourceUrl(request.getUrl())) {
            String apiUrl = WikisourceApi.buildPageTextUrl(WikisourceApi.titleFromSourceUrl(request.getUrl()));
            com.feng.freader.entity.data.DetailedChapterData data =
                    WikisourceApi.parsePageText(new SourceHttpClient().execute(simpleSource(apiUrl), "", 1));
            return data == null ? "" : data.getName() + "\n\n" + data.getContent();
        }
        if (WikisourceApi.isWikisourcePageTextUrl(request.getUrl())) {
            com.feng.freader.entity.data.DetailedChapterData data =
                    WikisourceApi.parsePageText(new SourceHttpClient().execute(simpleSource(request.getUrl()), "", 1));
            return data == null ? "" : data.getName() + "\n\n" + data.getContent();
        }
        return new SourceHttpClient().execute(simpleSource(request.getUrl()), "", 1);
    }

    private com.feng.freader.source.BookSource simpleSource(String url) {
        com.feng.freader.source.BookSource source = new com.feng.freader.source.BookSource();
        source.setSearchUrl(url);
        source.setSearchMethod("GET");
        return source;
    }

    private void setState(String id, DownloadState state) {
        DownloadRequest request = store.find(id);
        if (request != null) {
            request.setState(state);
            store.upsert(request);
        }
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.createNotificationChannel(new NotificationChannel(CHANNEL_ID,
                    "小说下载", NotificationManager.IMPORTANCE_LOW));
        }
    }

    @Override
    public void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
