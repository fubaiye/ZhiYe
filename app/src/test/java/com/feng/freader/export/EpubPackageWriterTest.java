package com.feng.freader.export;

import org.junit.Test;

import java.io.File;
import java.util.zip.ZipFile;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EpubPackageWriterTest {
    @Test
    public void writeCreatesReadableEpubPackage() throws Exception {
        File file = File.createTempFile("zhiye", ".epub");
        EpubPackageWriter.write(file, "测试书", "知页", "第一章\n内容");

        ZipFile zipFile = new ZipFile(file);
        try {
            assertNotNull(zipFile.getEntry("mimetype"));
            assertNotNull(zipFile.getEntry("META-INF/container.xml"));
            assertNotNull(zipFile.getEntry("OEBPS/content.opf"));
            assertNotNull(zipFile.getEntry("OEBPS/chapter1.xhtml"));
            assertTrue(file.length() > 0);
        } finally {
            zipFile.close();
            file.delete();
        }
    }
}
