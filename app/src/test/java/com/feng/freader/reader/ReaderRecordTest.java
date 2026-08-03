package com.feng.freader.reader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReaderRecordTest {
    @Test
    public void formatPositionUsesHumanChapterNumber() {
        ReaderRecord record = new ReaderRecord(ReaderRecord.BOOKMARK, "u", "n", "",
                "12.5%", 2, 10, 0, 1L);

        assertEquals("第3章 12.5%", record.formatPosition());
    }
}
