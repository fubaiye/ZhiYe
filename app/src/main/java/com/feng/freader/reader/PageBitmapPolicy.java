package com.feng.freader.reader;

public final class PageBitmapPolicy {

    private PageBitmapPolicy() {
    }

    public static boolean canCreateBitmap(int width, int height) {
        return width > 0 && height > 0;
    }
}
