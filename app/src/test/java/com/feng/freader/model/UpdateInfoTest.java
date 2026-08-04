package com.feng.freader.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UpdateInfoTest {

    @Test
    public void parsesGitHubReleaseApiAsset() {
        String json = "{"
                + "\"tag_name\":\"v1.7-code8\","
                + "\"body\":\"auto build\","
                + "\"assets\":[{\"name\":\"ZhiYe-v1.7-code8.apk\","
                + "\"browser_download_url\":\"https://github.com/fubaiye/ZhiYe/releases/download/v1.7-code8/ZhiYe-v1.7-code8.apk\"}]"
                + "}";

        UpdateInfo info = UpdateInfo.fromGitHubReleaseJson(json);

        assertTrue(info.isValid());
        assertEquals(8, info.getVersionCode());
        assertEquals("1.7", info.getVersionName());
    }

    @Test
    public void parsesGitHubLatestReleasePageWhenApiIsBlocked() {
        String html = "<html><a href=\"/fubaiye/ZhiYe/releases/download/v1.7-code8/ZhiYe-v1.7-code8.apk\">APK</a></html>";

        UpdateInfo info = UpdateInfo.fromGitHubReleasePage(html);

        assertTrue(info.isValid());
        assertEquals(8, info.getVersionCode());
        assertEquals("1.7", info.getVersionName());
        assertEquals("https://github.com/fubaiye/ZhiYe/releases/download/v1.7-code8/ZhiYe-v1.7-code8.apk",
                info.getApkUrl());
    }

    @Test
    public void parsesGitHubReleaseAtomWhenJsonApiIsBlocked() {
        String atom = "<feed><entry><link href=\"https://github.com/fubaiye/ZhiYe/releases/tag/v1.7-code8\"/>"
                + "<content>&lt;a href=&quot;/fubaiye/ZhiYe/releases/download/v1.7-code8/ZhiYe-v1.7-code8.apk&quot;&gt;APK&lt;/a&gt;</content>"
                + "</entry></feed>";

        UpdateInfo info = UpdateInfo.fromGitHubReleaseAtom(atom);

        assertTrue(info.isValid());
        assertEquals(8, info.getVersionCode());
        assertEquals("1.7", info.getVersionName());
    }
}
