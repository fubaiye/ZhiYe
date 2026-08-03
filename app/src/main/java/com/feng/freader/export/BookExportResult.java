package com.feng.freader.export;

import java.io.File;

public class BookExportResult {
    private final File file;
    private final ExportFormat format;
    private final boolean imported;

    public BookExportResult(File file, ExportFormat format, boolean imported) {
        this.file = file;
        this.format = format;
        this.imported = imported;
    }

    public File getFile() {
        return file;
    }

    public ExportFormat getFormat() {
        return format;
    }

    public boolean isImported() {
        return imported;
    }
}
