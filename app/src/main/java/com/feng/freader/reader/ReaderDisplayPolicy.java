package com.feng.freader.reader;

public final class ReaderDisplayPolicy {
    public static final float MIN_FONT_SIZE = 32f;
    public static final float MAX_FONT_SIZE = 96f;
    public static final float FONT_STEP = 2f;
    public static final float MIN_ROW_SPACE = 0f;
    public static final float MAX_ROW_SPACE = 72f;
    public static final float ROW_SPACE_STEP = 4f;

    public static final String[] FONT_NAMES = new String[]{
            "系统", "黑体", "宋体", "等宽"
    };

    private ReaderDisplayPolicy() {
    }

    public static float increaseFontSize(float current) {
        return clampFontSize(current + FONT_STEP);
    }

    public static float decreaseFontSize(float current) {
        return clampFontSize(current - FONT_STEP);
    }

    public static float increaseRowSpace(float current) {
        return clampRowSpace(current + ROW_SPACE_STEP);
    }

    public static float decreaseRowSpace(float current) {
        return clampRowSpace(current - ROW_SPACE_STEP);
    }

    public static float clampFontSize(float value) {
        return clamp(value, MIN_FONT_SIZE, MAX_FONT_SIZE);
    }

    public static float clampRowSpace(float value) {
        return clamp(value, MIN_ROW_SPACE, MAX_ROW_SPACE);
    }

    public static String fontSizeLabel(float value) {
        return "字号 " + Math.round(value) + "sp";
    }

    public static String rowSpaceLabel(float value) {
        return "行距 " + Math.round(value) + "px";
    }

    public static String fontName(int index) {
        return FONT_NAMES[normalizeFontIndex(index)];
    }

    public static int normalizeFontIndex(int index) {
        return index >= 0 && index < FONT_NAMES.length ? index : 0;
    }

    private static float clamp(float value, float min, float max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
