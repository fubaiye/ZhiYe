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
    private static final Pattern SEMVER_PATTERN =
            Pattern.compile("v?([0-9]+)(?:\\.([0-9]+))?(?:\\.([0-9]+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_DOWNLOAD_PATTERN = Pattern.compile(
            "(https://github\\.com)?(/[^/\"'<>\\s]+/[^/\"'<>\\s]+/releases/download/([^/\"'<>\\s]+)/([^/\"'<>\\s]+\\.apk))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RELEASE_TAG_PATTERN = Pattern.compile(
            "/[^/\"'<>\\s]+/[^/\"'<>\\s]+/releases/tag/([^\"'<>\\s]+)",
            Pattern.CASE_INSENSITIVE);

    private int versionCode;
    private String versionName;
    private String apkUrl;
    private long apkSize;
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
                        info.apkSize = getLong(asset, "size");
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

    public static UpdateInfo fromGitHubReleaseAtom(String atom) {
        return fromGitHubReleasePage(unescapeHtml(atom));
    }

    public static UpdateInfo fromGitHubReleasePage(String html) {
        UpdateInfo info = new UpdateInfo();
        String page = unescapeHtml(html);
        Matcher downloadMatcher = RELEASE_DOWNLOAD_PATTERN.matcher(page);
        if (downloadMatcher.find()) {
            String path = downloadMatcher.group(2);
            String tagName = downloadMatcher.group(3);
            String assetName = downloadMatcher.group(4);
            info.apkUrl = "https://github.com" + path;
            info.versionName = normalizeVersionName(tagName);
            info.versionCode = parseVersionCode(tagName);
            if (info.versionCode <= 0) {
                info.versionCode = parseVersionCode(assetName);
            }
        }
        if (info.versionCode <= 0) {
            Matcher tagMatcher = RELEASE_TAG_PATTERN.matcher(page);
            if (tagMatcher.find()) {
                String tagName = tagMatcher.group(1);
                info.versionName = normalizeVersionName(tagName);
                info.versionCode = parseVersionCode(tagName);
            }
        }
        return info;
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

    public long getApkSize() {
        return apkSize;
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

    private static long getLong(JsonObject object, String key) {
        try {
            JsonElement element = object.get(key);
            if (element == null || element.isJsonNull()) {
                return 0L;
            }
            return element.getAsLong();
        } catch (Throwable ignored) {
            return 0L;
        }
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

    private static String unescapeHtml(String text) {
        return emptyIfNull(text)
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }

    private static int parseVersionCode(String text) {
        Matcher matcher = VERSION_CODE_PATTERN.matcher(emptyIfNull(text));
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        Matcher semver = SEMVER_PATTERN.matcher(emptyIfNull(text));
        if (!semver.find()) {
            return 0;
        }
        try {
            int major = parsePart(semver.group(1));
            int minor = parsePart(semver.group(2));
            int patch = parsePart(semver.group(3));
            if (major == 1 && minor > 0 && patch == 0) {
                return minor;
            }
            if (minor == 0 && patch == 0) {
                return major;
            }
            return major * 10000 + minor * 100 + patch;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int parsePart(String value) {
        if (value == null || value.length() == 0) {
            return 0;
        }
        return Integer.parseInt(value);
    }
}
