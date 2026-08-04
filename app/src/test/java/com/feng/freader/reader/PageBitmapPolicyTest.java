package com.feng.freader.reader;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PageBitmapPolicyTest {

    @Test
    public void rejectsZeroSizedReaderViewBeforeLayout() {
        assertFalse(PageBitmapPolicy.canCreateBitmap(0, 2400));
        assertFalse(PageBitmapPolicy.canCreateBitmap(1080, 0));
        assertFalse(PageBitmapPolicy.canCreateBitmap(0, 0));
    }

    @Test
    public void acceptsLaidOutReaderView() {
        assertTrue(PageBitmapPolicy.canCreateBitmap(1080, 2400));
    }
}
