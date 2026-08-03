package com.feng.freader.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class EpubPackageWriter {
    private static final Charset UTF_8 = Charset.forName("UTF-8");

    private EpubPackageWriter() {
    }

    public static void write(File output, String title, String author, String content)
            throws IOException {
        if (output.getParentFile() != null && !output.getParentFile().exists()) {
            output.getParentFile().mkdirs();
        }
        ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(output));
        try {
            writeStored(zip, "mimetype", "application/epub+zip");
            write(zip, "META-INF/container.xml", containerXml());
            write(zip, "OEBPS/content.opf", contentOpf(title, author));
            write(zip, "OEBPS/toc.ncx", tocNcx(title));
            write(zip, "OEBPS/chapter1.xhtml", chapter(title, content));
        } finally {
            zip.close();
        }
    }

    private static void writeStored(ZipOutputStream zip, String name, String data)
            throws IOException {
        byte[] bytes = data.getBytes(UTF_8);
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(bytes.length);
        entry.setCompressedSize(bytes.length);
        entry.setCrc(crc32.getValue());
        zip.putNextEntry(entry);
        zip.write(bytes);
        zip.closeEntry();
    }

    private static void write(ZipOutputStream zip, String name, String data)
            throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(data.getBytes(UTF_8));
        zip.closeEntry();
    }

    private static String containerXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">"
                + "<rootfiles><rootfile full-path=\"OEBPS/content.opf\" "
                + "media-type=\"application/oebps-package+xml\"/></rootfiles></container>";
    }

    private static String contentOpf(String title, String author) {
        String id = UUID.nameUUIDFromBytes((safe(title) + safe(author)).getBytes(UTF_8)).toString();
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<package xmlns=\"http://www.idpf.org/2007/opf\" unique-identifier=\"bookid\" version=\"2.0\">"
                + "<metadata xmlns:dc=\"http://purl.org/dc/elements/1.1/\">"
                + "<dc:title>" + escapeXml(title) + "</dc:title>"
                + "<dc:creator>" + escapeXml(author) + "</dc:creator>"
                + "<dc:language>zh-CN</dc:language>"
                + "<dc:identifier id=\"bookid\">urn:uuid:" + id + "</dc:identifier>"
                + "</metadata><manifest>"
                + "<item id=\"ncx\" href=\"toc.ncx\" media-type=\"application/x-dtbncx+xml\"/>"
                + "<item id=\"chapter1\" href=\"chapter1.xhtml\" media-type=\"application/xhtml+xml\"/>"
                + "</manifest><spine toc=\"ncx\"><itemref idref=\"chapter1\"/></spine></package>";
    }

    private static String tocNcx(String title) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\" version=\"2005-1\">"
                + "<head><meta name=\"dtb:uid\" content=\"知页\"/></head>"
                + "<docTitle><text>" + escapeXml(title) + "</text></docTitle>"
                + "<navMap><navPoint id=\"chapter1\" playOrder=\"1\"><navLabel><text>"
                + escapeXml(title) + "</text></navLabel><content src=\"chapter1.xhtml\"/>"
                + "</navPoint></navMap></ncx>";
    }

    private static String chapter(String title, String content) {
        String[] paragraphs = safe(content).split("\\r?\\n");
        StringBuilder body = new StringBuilder();
        body.append("<h1>").append(escapeXml(title)).append("</h1>");
        for (String paragraph : paragraphs) {
            String text = paragraph.trim();
            if (!text.isEmpty()) {
                body.append("<p>").append(escapeXml(text)).append("</p>");
            }
        }
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>"
                + escapeXml(title)
                + "</title><style>body{font-family:serif;line-height:1.75;}p{text-indent:2em;}</style>"
                + "</head><body>" + body + "</body></html>";
    }

    static String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
