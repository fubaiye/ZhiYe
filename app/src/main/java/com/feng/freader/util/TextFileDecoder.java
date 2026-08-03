package com.feng.freader.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class TextFileDecoder {
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final Charset GB_18030 = Charset.forName("GB18030");

    private TextFileDecoder() {
    }

    public static String read(File file) throws IOException {
        FileInputStream inputStream = new FileInputStream(file);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            return decode(outputStream.toByteArray());
        } finally {
            inputStream.close();
        }
    }

    public static String decode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (hasUtf8Bom(bytes)) {
            return new String(bytes, 3, bytes.length - 3, UTF_8);
        }
        try {
            return decodeStrict(bytes, UTF_8);
        } catch (CharacterCodingException ignored) {
            return new String(bytes, GB_18030);
        }
    }

    private static boolean hasUtf8Bom(byte[] bytes) {
        return bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF;
    }

    private static String decodeStrict(byte[] bytes, Charset charset)
            throws CharacterCodingException {
        CharsetDecoder decoder = charset.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));
        return charBuffer.toString();
    }
}
