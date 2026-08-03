package com.feng.freader.util;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;

public class TextFileDecoderTest {

    @Test
    public void decodesUtf8Text() throws Exception {
        byte[] bytes = "第一章 天苍守夜人\n这是中文正文。".getBytes("UTF-8");

        assertEquals("第一章 天苍守夜人\n这是中文正文。", TextFileDecoder.decode(bytes));
    }

    @Test
    public void decodesGb18030Text() throws Exception {
        byte[] bytes = "第一章 天苍守夜人\n这是中文正文。".getBytes(Charset.forName("GB18030"));

        assertEquals("第一章 天苍守夜人\n这是中文正文。", TextFileDecoder.decode(bytes));
    }

    @Test
    public void removesUtf8Bom() throws Exception {
        byte[] bytes = new byte[] {
                (byte) 0xEF, (byte) 0xBB, (byte) 0xBF,
                (byte) 0xE7, (byte) 0xAC, (byte) 0xAC
        };

        assertEquals("第", TextFileDecoder.decode(bytes));
    }
}
