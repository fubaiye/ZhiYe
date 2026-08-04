package com.feng.freader.util;

import com.feng.freader.entity.epub.EpubData;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class EpubUtilsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void extractsTextFromParagraphsWithInlineElements() throws Exception {
        File chapter = temporaryFolder.newFile("chapter.xhtml");
        writeUtf8(chapter, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><body>"
                + "<section><p><span>第一章 若是有来生，伴君天下舞！</span></p>"
                + "<p>第二段<strong>正文</strong></p></section>"
                + "</body></html>");

        List<EpubData> data = EpubUtils.getEpubData(temporaryFolder.getRoot().getAbsolutePath(),
                chapter.getAbsolutePath());

        assertFalse(data.isEmpty());
        assertEquals(EpubData.TYPE.TEXT, data.get(0).getType());
        assertEquals("    第一章 若是有来生，伴君天下舞！\n    第二段正文\n",
                data.get(0).getData());
    }

    private static void writeUtf8(File file, String content) throws Exception {
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            outputStream.write(content.getBytes("UTF-8"));
        } finally {
            outputStream.close();
        }
    }
}
