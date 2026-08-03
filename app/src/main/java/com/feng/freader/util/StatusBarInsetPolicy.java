package com.feng.freader.util;

public final class StatusBarInsetPolicy {

    private StatusBarInsetPolicy() {
    }

    public static int topPadding(int originalTopPadding, int statusBarHeight) {
        return originalTopPadding + Math.max(0, statusBarHeight);
    }
}
