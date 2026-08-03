package com.feng.freader.source;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class SourceDebugResultTest {
    @Test
    public void displayContainsDebuggerSections() {
        SourceDebugResult result = new SourceDebugResult();
        result.setUrl("https://example.com");
        result.setCssResult("book");
        result.setExecutionTimeMs(12);

        String text = result.toDisplayText();

        assertTrue(text.contains("CSS Result"));
        assertTrue(text.contains("Execution Time"));
        assertTrue(text.contains("HTML/JSON"));
    }
}
