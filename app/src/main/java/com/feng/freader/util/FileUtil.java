package com.feng.freader.util;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;

import com.feng.freader.constant.Constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String uri2FilePath(Activity activity, Uri uri) {
        String filePath;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor actualimagecursor = activity.managedQuery(uri, proj, null, null, null);
        if (actualimagecursor == null) {
            filePath = uri.getPath();
        } else {
            int actualImageColumnIndex =
                    actualimagecursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            actualimagecursor.moveToFirst();
            filePath = actualimagecursor.getString(actualImageColumnIndex);
        }

        return filePath;
    }

    public static File uri2FileQ(Context context, Uri uri) {
        return uriToReadableFile(context, uri);
    }

    public static File uriToReadableFile(Context context, Uri uri) {
        try {
            if (uri == null || uri.getScheme() == null) {
                return null;
            }
            if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                return new File(uri.getPath());
            }
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                return copyUriToCache(context, uri);
            }
        } catch (Throwable t) {
            Log.e(TAG, "uriToReadableFile error", t);
        }
        return null;
    }

    private static File copyUriToCache(Context context, Uri uri) throws IOException {
        ContentResolver contentResolver = context.getContentResolver();
        String displayName = getDisplayName(contentResolver, uri);
        File cacheDir = context.getExternalCacheDir();
        if (cacheDir == null) {
            cacheDir = context.getCacheDir();
        }
        File cache = new File(cacheDir, System.currentTimeMillis() + "_" + displayName);
        InputStream is = null;
        OutputStream os = null;
        try {
            is = contentResolver.openInputStream(uri);
            if (is == null) {
                return null;
            }
            os = new FileOutputStream(cache);
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            return cache;
        } finally {
            if (os != null) {
                os.close();
            }
            if (is != null) {
                is.close();
            }
        }
    }

    private static String getDisplayName(ContentResolver contentResolver, Uri uri) {
        Cursor cursor = null;
        try {
            cursor = contentResolver.query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String displayName = cursor.getString(index);
                    if (displayName != null && !displayName.trim().isEmpty()) {
                        return displayName.replace(File.separatorChar, '_');
                    }
                }
            }
        } catch (Throwable t) {
            Log.e(TAG, "getDisplayName error", t);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null || fallback.trim().isEmpty() ? "book.tmp" : fallback;
    }

    public static double getFileSize(File file) {
        long len = file.length();
        return (double) len / Math.pow(2, 20);
    }

    public static double getFileSize(long len) {
        return (double) len / Math.pow(2, 20);
    }

    public static Bitmap loadLocalPicture(String filePath) {
        FileInputStream fis = null;
        try {
            File file = new File(filePath);
            fis = new FileInputStream(file);
            return BitmapFactory.decodeStream(fis);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (fis != null) {
                    fis.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void writeTxtToLocal(String content) {
        String filePath = "/storage/emulated/0/1/";
        String fileName = "data.txt";
        makeFilePath(filePath, fileName);
        String strFilePath = filePath + fileName;
        String strContent = content + "\r\n";
        try {
            File file = new File(strFilePath);
            if (!file.exists()) {
                Log.d("TestFile", "Create the file:" + strFilePath);
                file.getParentFile().mkdirs();
                file.createNewFile();
            }
            RandomAccessFile raf = new RandomAccessFile(file, "rwd");
            raf.seek(file.length());
            raf.write(strContent.getBytes());
            raf.close();
        } catch (Exception e) {
            Log.e("TestFile", "Error on write File:" + e);
        }
    }

    private static File makeFilePath(String filePath, String fileName) {
        File file = null;
        makeRootDirectory(filePath);
        try {
            file = new File(filePath + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    private static void makeRootDirectory(String filePath) {
        File file;
        try {
            file = new File(filePath);
            if (!file.exists()) {
                file.mkdir();
            }
        } catch (Exception e) {
            Log.i("error:", e + "");
        }
    }

    public static String getLocalCacheSize() {
        File file = new File(Constant.EPUB_SAVE_PATH);
        double len = getFileSize(getTotalSizeOfFiles(file));

        return String.valueOf((int) (len)) + "M";
    }

    private static long getTotalSizeOfFiles(File file) {
        if (file.isFile()) {
            return file.length();
        }
        File[] children = file.listFiles();
        long total = 0;
        if (children != null) {
            for (File child : children) {
                total += getTotalSizeOfFiles(child);
            }
        }
        return total;
    }

    public static void clearLocalCache() {
        File file = new File(Constant.EPUB_SAVE_PATH);
        deleteFile(file);
    }

    public static void deleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        if (file.isFile()) {
            file.delete();
            return;
        }
        File[] files = file.listFiles();
        if (files != null) {
            for (File f : files) {
                deleteFile(f);
            }
        }
        file.delete();
    }
}
