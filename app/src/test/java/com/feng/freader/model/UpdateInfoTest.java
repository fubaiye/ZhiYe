package com.feng.freader.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpdateInfoTest {

    @Test
    public void parsesManifestJson() {
        String json = "{\"versionCode\":2,\"versionName\":\"1.1\","
                + "\"apkUrl\":\"https://example.com/FReader.apk\","
                + "\"releaseNotes\":\"修复阅读体验\"}";

        UpdateInfo info = UpdateInfo.fromJson(json);

        assertEquals(2, info.getVersionCode());
        assertEquals("1.1", info.getVersionName());
        assertEquals("https://example.com/FReader.apk", info.getApkUrl());
        assertTrue(info.isNewerThan(1));
        assertTrue(info.isValid());
    }

    @Test
    public void invalidManifestIsNotNewer() {
        UpdateInfo info = UpdateInfo.fromJson("{}");

        assertFalse(info.isValid());
        assertFalse(info.isNewerThan(1));
    }

    @Test
    public void parsesGitHubReleaseAsset() {
        String json = "{"
                + "\"tag_name\":\"v1.3-code4\","
                + "\"body\":\"修复更新体验\","
                + "\"assets\":["
                + "{\"name\":\"notes.txt\",\"browser_download_url\":\"https://example.com/notes.txt\"},"
                + "{\"name\":\"ZhiYe-v1.3-code4.apk\","
                + "\"browser_download_url\":\"https://github.com/fubaiye/ZhiYe/releases/download/v1.3-code4/ZhiYe.apk\"}"
                + "]}";

        UpdateInfo info = UpdateInfo.fromGitHubReleaseJson(json);

        assertEquals(4, info.getVersionCode());
        assertEquals("1.3", info.getVersionName());
        assertEquals("修复更新体验", info.getReleaseNotes());
        assertEquals("https://github.com/fubaiye/ZhiYe/releases/download/v1.3-code4/ZhiYe.apk",
                info.getApkUrl());
        assertTrue(info.isNewerThan(3));
    }
}
