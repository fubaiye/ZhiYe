package com.feng.freader.model;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateInfo {
    private static final Pattern VERSION_CODE_PATTERN =
            Pattern.compile("(?:code|vc|versionCode)[-_+]?([0-9]+)", Pattern.CASE_INSENSITIVE);

    private int versionCode;
    private String versionName;
    private String apkUrl;
    private String releaseNotes;

    public static UpdateInfo fromJson(String json) {
        try {
            UpdateInfo info = new Gson().fromJson(json, UpdateInfo.class);
            return info == null ? new UpdateInfo() : info;
        } catch (Throwable ignored) {
            return new UpdateInfo();
        }
    }

    public static UpdateInfo fromGitHubReleaseJson(String json) {
        try {
            JsonObject release = new JsonParser().parse(json).getAsJsonObject();
            UpdateInfo info = new UpdateInfo();
            String tagName = getString(release, "tag_name");
            info.versionName = normalizeVersionName(tagName);
            info.versionCode = parseVersionCode(tagName);
            info.releaseNotes = getString(release, "body");

            JsonArray assets = release.getAsJsonArray("assets");
            if (assets != null) {
                for (JsonElement element : assets) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject asset = element.getAsJsonObject();
                    String name = getString(asset, "name");
                    String url = getString(asset, "browser_download_url");
                    if (name.toLowerCase().endsWith(".apk") && url.startsWith("https://")) {
                        info.apkUrl = url;
                        if (info.versionCode <= 0) {
                            info.versionCode = parseVersionCode(name);
                        }
                        break;
                    }
                }
            }
            return info;
        } catch (Throwable ignored) {
            return new UpdateInfo();
        }
    }

    public boolean isValid() {
        return versionCode > 0
                && apkUrl != null
                && apkUrl.trim().startsWith("https://");
    }

    public boolean isNewerThan(int currentVersionCode) {
        return isValid() && versionCode > currentVersionCode;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public String getVersionName() {
        return emptyIfNull(versionName);
    }

    public String getApkUrl() {
        return emptyIfNull(apkUrl);
    }

    public String getReleaseNotes() {
        return emptyIfNull(releaseNotes);
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return "";
        }
        return element.getAsString();
    }

    private static String normalizeVersionName(String tagName) {
        String version = emptyIfNull(tagName).trim();
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        int codeIndex = version.toLowerCase().indexOf("-code");
        if (codeIndex >= 0) {
            version = version.substring(0, codeIndex);
        }
        int metadataIndex = version.indexOf("+");
        if (metadataIndex >= 0) {
            version = version.substring(0, metadataIndex);
        }
        return version;
    }

    private static int parseVersionCode(String text) {
        Matcher matcher = VERSION_CODE_PATTERN.matcher(emptyIfNull(text));
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
