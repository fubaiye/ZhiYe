package com.feng.freader.util;

public class VolumePageTurnPolicy {
    private static final int KEYCODE_VOLUME_UP = 24;
    private static final int KEYCODE_VOLUME_DOWN = 25;

    public enum TurnDirection {
        NONE,
        PREVIOUS,
        NEXT
    }

    private VolumePageTurnPolicy() {
    }

    public static TurnDirection directionForKeyCode(int keyCode) {
        if (keyCode == KEYCODE_VOLUME_DOWN) {
            return TurnDirection.NEXT;
        }
        if (keyCode == KEYCODE_VOLUME_UP) {
            return TurnDirection.PREVIOUS;
        }
        return TurnDirection.NONE;
    }
}
