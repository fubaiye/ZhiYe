package com.feng.freader.reader;

public class ReaderCurlColorPolicy {
    private static final int OPAQUE_ALPHA = 0xFF000000;
    private static final int BACK_TEXT_ALPHA = 0x33000000;
    private static final int RGB_MASK = 0x00FFFFFF;

    private ReaderCurlColorPolicy() {
    }

    public static int backBackgroundFor(int pageBackgroundColor) {
        return OPAQUE_ALPHA | (pageBackgroundColor & RGB_MASK);
    }

    public static int backTextFor(int pageTextColor) {
        return BACK_TEXT_ALPHA | (pageTextColor & RGB_MASK);
    }
}
