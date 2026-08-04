package com.feng.freader.util;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApkFileValidatorTest {

    @Test
    public void rejectsHtmlDownloadedAsApk() throws Exception {
        File file = File.createTempFile("update", ".apk");
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write("<html>not apk</html>".getBytes("UTF-8"));
        outputStream.close();

        assertFalse(ApkFileValidator.isValidApk(file, 0));
    }

    @Test
    public void acceptsZipApkWithMatchingSize() throws Exception {
        File file = File.createTempFile("update", ".apk");
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(new byte[]{'P', 'K', 3, 4});
        byte[] padding = new byte[300 * 1024];
        outputStream.write(padding);
        outputStream.close();

        assertTrue(ApkFileValidator.isValidApk(file, file.length()));
    }

    @Test
    public void rejectsUnexpectedSize() throws Exception {
        File file = File.createTempFile("update", ".apk");
        FileOutputStream outputStream = new FileOutputStream(file);
        outputStream.write(new byte[]{'P', 'K', 3, 4});
        byte[] padding = new byte[300 * 1024];
        outputStream.write(padding);
        outputStream.close();

        assertFalse(ApkFileValidator.isValidApk(file, file.length() + 1));
    }
}
