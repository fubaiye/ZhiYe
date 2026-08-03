package com.feng.freader.download;

import android.content.Context;

import com.feng.freader.app.App;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class OfflineBookCache {
    private static final String DIR = "offline_books";

    private OfflineBookCache() {
    }

    public static File bookFile(String name) {
        Context context = App.getContext();
        File dir = new File(context.getFilesDir(), DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, safeFileName(name) + ".txt");
    }

    public static File writeTxt(String name, String content) throws IOException {
        File file = bookFile(name);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file, false);
            outputStream.write((content == null ? "" : content).getBytes("UTF-8"));
            outputStream.flush();
            return file;
        } finally {
            if (outputStream != null) {
                outputStream.close();
            }
        }
    }

    private static String safeFileName(String name) {
        String value = name == null || name.trim().length() == 0 ? "book" : name.trim();
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
