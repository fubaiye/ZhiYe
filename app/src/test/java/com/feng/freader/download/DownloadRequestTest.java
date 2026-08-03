package com.feng.freader.download;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DownloadRequestTest {

    @Test
    public void serializesDownloadRequest() {
        DownloadRequest request = new DownloadRequest("u1", "雪中", "https://example.com/book", "cover");
        request.setState(DownloadState.PAUSED);
        request.setRetryCount(2);

        DownloadRequest copy = DownloadRequest.fromJson(request.toJson());

        assertEquals("u1", copy.getId());
        assertEquals("雪中", copy.getName());
        assertEquals("https://example.com/book", copy.getUrl());
        assertEquals("cover", copy.getCover());
        assertEquals(DownloadState.PAUSED, copy.getState());
        assertEquals(2, copy.getRetryCount());
    }

    @Test
    public void identifiesTerminalState() {
        DownloadRequest request = new DownloadRequest("u1", "雪中", "url", "");

        assertFalse(request.isTerminal());
        request.setState(DownloadState.COMPLETED);
        assertTrue(request.isTerminal());
    }
}
