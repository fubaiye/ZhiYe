package com.feng.freader.online;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class OpdsParser {
    public BookPage parse(String feedUrl, String xml, String sourceId, String sourceName) throws Exception {
        if (xml == null || xml.trim().isEmpty() || xml.trim().toLowerCase().startsWith("<html")) {
            throw new IllegalArgumentException("Invalid OPDS feed");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Throwable ignored) {
        }
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        Element root = document.getDocumentElement();
        if (root == null || !"feed".equals(root.getLocalName())) {
            throw new IllegalArgumentException("Invalid OPDS feed");
        }

        List<OnlineBook> books = new ArrayList<>();
        NodeList entries = root.getElementsByTagNameNS("*", "entry");
        for (int i = 0; i < entries.getLength(); i++) {
            Element entry = (Element) entries.item(i);
            OnlineBook book = parseEntry(feedUrl, entry, sourceId, sourceName);
            book.setLicenseNote("免费、公版或开放许可资源");
            books.add(book);
        }
        String nextUrl = findFeedLink(feedUrl, root, "next");
        return new BookPage(books, 1, !nextUrl.isEmpty(), nextUrl);
    }

    private OnlineBook parseEntry(String feedUrl, Element entry, String sourceId, String sourceName) throws Exception {
        String title = text(entry, "title");
        String id = text(entry, "id");
        OnlineBook book = new OnlineBook(id.isEmpty() ? title : id, sourceId, sourceName, title);
        NodeList authors = entry.getElementsByTagNameNS("*", "author");
        for (int i = 0; i < authors.getLength(); i++) {
            if (authors.item(i) instanceof Element) {
                book.addAuthor(text((Element) authors.item(i), "name"));
            }
        }
        book.setDescription(firstText(entry, "summary", "content"));
        book.setLanguage(firstText(entry, "language"));
        book.setPublisher(firstText(entry, "publisher"));
        book.setPublishedAt(firstText(entry, "issued", "published", "updated"));
        NodeList categories = entry.getElementsByTagNameNS("*", "category");
        for (int i = 0; i < categories.getLength(); i++) {
            Node node = categories.item(i);
            if (node instanceof Element) {
                Element category = (Element) node;
                String term = category.getAttribute("term");
                book.addSubject(term.isEmpty() ? category.getAttribute("label") : term);
            }
        }
        NodeList links = entry.getElementsByTagNameNS("*", "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String rel = link.getAttribute("rel");
            String href = resolve(feedUrl, link.getAttribute("href"));
            String type = link.getAttribute("type");
            if (href.isEmpty()) {
                continue;
            }
            if (rel.contains("image") || rel.contains("thumbnail") || type.startsWith("image/")) {
                if (book.getCoverUrl().isEmpty() || rel.contains("thumbnail")) {
                    book.setCoverUrl(href);
                }
            }
            if (rel.contains("acquisition") || OnlineDownloadValidator.isAllowed(href, type)) {
                String format = BookFormat.fromMimeAndUrl(type, href);
                if (!BookFormat.TYPE_OTHER.equals(format)) {
                    book.addFormat(new BookFormat(format, type, href, 0));
                }
            }
            if ("alternate".equals(rel) && book.getDetailUrl().isEmpty()) {
                book.setDetailUrl(href);
            }
        }
        return book;
    }

    public String findSearchUrl(String feedUrl, String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        return findFeedLink(feedUrl, document.getDocumentElement(), "search");
    }

    private String findFeedLink(String feedUrl, Element root, String relName) throws Exception {
        NodeList links = root.getElementsByTagNameNS("*", "link");
        for (int i = 0; i < links.getLength(); i++) {
            Element link = (Element) links.item(i);
            String rel = link.getAttribute("rel");
            if (relName.equals(rel) || rel.contains(relName)) {
                return resolve(feedUrl, link.getAttribute("href"));
            }
        }
        return "";
    }

    private String firstText(Element parent, String... names) {
        for (String name : names) {
            String value = text(parent, name);
            if (!value.isEmpty()) {
                return value;
            }
        }
        return "";
    }

    private String text(Element parent, String localName) {
        NodeList nodes = parent.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        String text = nodes.item(0).getTextContent();
        return text == null ? "" : text.trim();
    }

    static String resolve(String base, String href) throws Exception {
        if (href == null || href.trim().isEmpty()) {
            return "";
        }
        URI uri = new URI(href.trim());
        if (uri.isAbsolute()) {
            return uri.toString();
        }
        return new URI(base).resolve(uri).toString();
    }
}
