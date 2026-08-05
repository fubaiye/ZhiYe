package com.feng.freader.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateApkDownloaderTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void followsRedirectAndReportsDownloadProgress() throws Exception {
        MockWebServer server = new MockWebServer();
        byte[] apkBytes = new byte[128 * 1024];
        Arrays.fill(apkBytes, (byte) 7);

        try {
            server.enqueue(new MockResponse()
                    .setResponseCode(302)
                    .addHeader("Location", "/assets/app.apk"));
            server.enqueue(new MockResponse()
                    .addHeader("Content-Type", "application/vnd.android.package-archive")
                    .setBody(new okio.Buffer().write(apkBytes)));
            server.start();

            File target = folder.newFile("update.apk");
            final List<Long> downloaded = new ArrayList<>();
            final List<Long> totals = new ArrayList<>();

            UpdateApkDownloader downloader = new UpdateApkDownloader(new OkHttpClient());
            downloader.download(server.url("/releases/download/app.apk").toString(), target,
                    new UpdateApkDownloader.ProgressListener() {
                        @Override
                        public void onProgress(long downloadedBytes, long totalBytes) {
                            downloaded.add(downloadedBytes);
                            totals.add(totalBytes);
                        }
                    });

            assertArrayEquals(apkBytes, Files.readAllBytes(target.toPath()));
            assertTrue(downloaded.contains((long) apkBytes.length));
            assertEquals((long) apkBytes.length, totals.get(totals.size() - 1).longValue());

            RecordedRequest first = server.takeRequest();
            RecordedRequest second = server.takeRequest();
            assertEquals("/releases/download/app.apk", first.getPath());
            assertEquals("/assets/app.apk", second.getPath());
            assertEquals("ZhiYe Android Updater", second.getHeader("User-Agent"));
        } finally {
            server.shutdown();
        }
    }
}
