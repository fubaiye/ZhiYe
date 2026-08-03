package com.feng.freader.export;

import android.content.Context;
import android.text.TextUtils;

import com.feng.freader.db.DatabaseManager;
import com.feng.freader.entity.data.BookshelfNovelDbData;
import com.feng.freader.util.TextFileDecoder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class BookExporter {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private BookExporter() {
    }

    public static BookExportResult export(Context context, BookshelfNovelDbData book,
                                          ExportFormat format) throws IOException {
        File exportDir = new File(context.getExternalFilesDir(null), "exports");
        if (!exportDir.exists()) {
            exportDir.mkdirs();
        }
        String title = cleanFileName(book.getName());
        File output = uniqueFile(exportDir, title, format.getSuffix());
        if (format == ExportFormat.TXT) {
            writeTxt(book, output);
        } else {
            writeEpub(book, output);
        }
        boolean imported = importExportedBook(book, output, format);
        return new BookExportResult(output, format, imported);
    }

    private static void writeTxt(BookshelfNovelDbData book, File output) throws IOException {
        String content = readTextContent(book);
        FileOutputStream outputStream = new FileOutputStream(output);
        try {
            outputStream.write(content.getBytes(UTF_8));
        } finally {
            outputStream.close();
        }
    }

    private static void writeEpub(BookshelfNovelDbData book, File output) throws IOException {
        File source = new File(book.getNovelUrl());
        if (book.getType() == 2 && source.exists()) {
            copy(source, output);
            return;
        }
        EpubPackageWriter.write(output, book.getName(), "知页", readTextContent(book));
    }

    private static String readTextContent(BookshelfNovelDbData book) throws IOException {
        File source = new File(book.getNovelUrl());
        if (source.exists() && source.isFile() && book.getType() == 1) {
            return TextFileDecoder.read(source);
        }
        return book.getName() + "\n\n来源：" + book.getNovelUrl();
    }

    private static boolean importExportedBook(BookshelfNovelDbData origin, File output,
                                              ExportFormat format) {
        DatabaseManager db = DatabaseManager.getInstance();
        String path = output.getAbsolutePath();
        if (db.isExistInBookshelfNovel(path)) {
            return false;
        }
        db.insertBookshelfNovel(new BookshelfNovelDbData(path, origin.getName(), origin.getCover(),
                0, 0, format.getBookshelfType()));
        return true;
    }

    private static void copy(File source, File output) throws IOException {
        FileInputStream inputStream = new FileInputStream(source);
        FileOutputStream outputStream = new FileOutputStream(output);
        try {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        } finally {
            outputStream.close();
            inputStream.close();
        }
    }

    private static File uniqueFile(File dir, String title, String suffix) {
        File file = new File(dir, title + suffix);
        int index = 2;
        while (file.exists()) {
            file = new File(dir, title + "_" + index + suffix);
            index++;
        }
        return file;
    }

    private static String cleanFileName(String title) {
        String name = TextUtils.isEmpty(title) ? "知页导出" : title.trim();
        name = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return name.length() > 80 ? name.substring(0, 80) : name;
    }
}
