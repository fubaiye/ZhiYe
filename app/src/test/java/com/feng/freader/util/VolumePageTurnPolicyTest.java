package com.feng.freader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VolumePageTurnPolicyTest {

    @Test
    public void volumeDownTurnsToNextPage() {
        assertEquals(VolumePageTurnPolicy.TurnDirection.NEXT,
                VolumePageTurnPolicy.directionForKeyCode(25));
    }

    @Test
    public void volumeUpTurnsToPreviousPage() {
        assertEquals(VolumePageTurnPolicy.TurnDirection.PREVIOUS,
                VolumePageTurnPolicy.directionForKeyCode(24));
    }

    @Test
    public void unrelatedKeyIsIgnored() {
        assertEquals(VolumePageTurnPolicy.TurnDirection.NONE,
                VolumePageTurnPolicy.directionForKeyCode(4));
    }
}
