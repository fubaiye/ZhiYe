package com.feng.freader.backup;

import android.content.Context;

import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.source.SourceRepository;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.Charset;

public class AppBackupManager {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private final Context context;
    private final Gson gson = new Gson();

    public AppBackupManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public File createBackup() throws Exception {
        AppBackupData data = new AppBackupData();
        data.setSourcesJson(SourceRepository.getInstance().exportJson());
        data.setBookshelf(DatabaseManager.getInstance().queryAllBookshelfNovel());
        File dir = backupDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(dir, "zhiye_backup.json");
        write(file, gson.toJson(data));
        return file;
    }

    public int restoreLatestBackup() throws Exception {
        File file = new File(backupDir(), "zhiye_backup.json");
        if (!file.exists()) {
            return 0;
        }
        AppBackupData data = gson.fromJson(read(file), AppBackupData.class);
        if (data == null) {
            return 0;
        }
        SourceRepository.getInstance().importJson(data.getSourcesJson());
        DatabaseManager db = DatabaseManager.getInstance();
        int count = 0;
        for (BookshelfNovelDbData book : data.getBookshelf()) {
            if (!db.isExistInBookshelfNovel(book.getNovelUrl())) {
                db.insertBookshelfNovel(book);
                count++;
            }
        }
        return count;
    }

    public File backupDir() {
        return new File(context.getExternalFilesDir(null), "backups");
    }

    private void write(File file, String text) throws Exception {
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(text.getBytes(UTF_8));
        } finally {
            outputStream.close();
        }
    }

    private String read(File file) throws Exception {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            byte[] buffer = new byte[(int) file.length()];
            int length = inputStream.read(buffer);
            return new String(buffer, 0, Math.max(0, length), UTF_8);
        } finally {
            inputStream.close();
        }
    }
}
