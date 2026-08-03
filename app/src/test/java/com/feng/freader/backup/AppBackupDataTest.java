package com.feng.freader.backup;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppBackupDataTest {
    @Test
    public void defaultSourcesJsonIsSafe() {
        AppBackupData data = new AppBackupData();

        assertEquals("", data.getSourcesJson());
    }
}
