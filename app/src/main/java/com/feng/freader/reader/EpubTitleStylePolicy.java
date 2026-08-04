package com.feng.freader.reader;

public final class EpubTitleStylePolicy {

    private EpubTitleStylePolicy() {
    }

    public static float titleTextSize(float bodyTextSize) {
        return bodyTextSize;
    }

    public static float titleLineStep(float bodyTextSize, float rowSpace) {
        return bodyTextSize + rowSpace;
    }
}
