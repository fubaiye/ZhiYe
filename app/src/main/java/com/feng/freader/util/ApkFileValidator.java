package com.feng.freader.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public final class ApkFileValidator {
    private static final int MIN_APK_SIZE = 256 * 1024;

    private ApkFileValidator() {
    }

    public static boolean isValidApk(File file, long expectedSize) {
        if (file == null || !file.exists() || !file.isFile()) {
            return false;
        }
        long length = file.length();
        if (length < MIN_APK_SIZE) {
            return false;
        }
        if (expectedSize > 0 && length != expectedSize) {
            return false;
        }
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            byte[] header = new byte[4];
            if (inputStream.read(header) != header.length) {
                return false;
            }
            return header[0] == 'P'
                    && header[1] == 'K'
                    && (header[2] == 3 || header[2] == 5 || header[2] == 7)
                    && (header[3] == 4 || header[3] == 6 || header[3] == 8);
        } catch (IOException ignored) {
            return false;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
    }
}
