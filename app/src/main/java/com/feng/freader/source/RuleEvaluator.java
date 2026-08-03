package com.feng.freader.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.InputSource;

public class RuleEvaluator {
    private static final Pattern REPLACE_PATTERN =
            Pattern.compile("replace\\('([^']*)','([^']*)'\\)");

    private RuleEvaluator() {
    }

    public static List<Element> selectElements(String body, String rule) {
        String rawRule = stripPrefix(rule, "css:");
        if (rawRule.equals(rule) && !rule.startsWith("css:")) {
            rawRule = rule;
        }
        int attrIndex = rawRule.indexOf('@');
        String selector = attrIndex >= 0 ? rawRule.substring(0, attrIndex) : rawRule;
        Elements elements = Jsoup.parse(body == null ? "" : body).select(selector);
        return new ArrayList<>(elements);
    }

    public static String evalElement(Element element, String rule) {
        if (element == null) {
            return "";
        }
        if (rule == null || rule.length() == 0) {
            return element.text();
        }
        if (rule.startsWith("css:")) {
            return evalCss(element, stripPrefix(rule, "css:"));
        }
        if (rule.startsWith("javascript:")) {
            return applyJavaScript(element.text(), stripPrefix(rule, "javascript:"));
        }
        return evalCss(element, rule);
    }

    public static String eval(String body, String rule) {
        if (rule == null || rule.length() == 0) {
            return "";
        }
        if (rule.startsWith("css:")) {
            List<Element> elements = selectElements(body, rule);
            return elements.isEmpty() ? "" : evalElement(elements.get(0), rule);
        }
        if (rule.startsWith("xpath:")) {
            return evalXPath(body, stripPrefix(rule, "xpath:"));
        }
        if (rule.startsWith("jsonpath:")) {
            return evalJsonPath(body, stripPrefix(rule, "jsonpath:"));
        }
        if (rule.startsWith("javascript:")) {
            return applyJavaScript(body, stripPrefix(rule, "javascript:"));
        }
        return body == null ? "" : body;
    }

    public static String applyJavaScript(String value, String script) {
        String result = value == null ? "" : value;
        String code = script == null ? "" : script;
        if (code.contains("trim()")) {
            result = result.trim();
        }
        Matcher matcher = REPLACE_PATTERN.matcher(code);
        while (matcher.find()) {
            result = result.replace(matcher.group(1), matcher.group(2));
        }
        return result;
    }

    private static String evalCss(Element root, String rule) {
        int attrIndex = rule.indexOf('@');
        String selector = attrIndex >= 0 ? rule.substring(0, attrIndex) : rule;
        String attr = attrIndex >= 0 ? rule.substring(attrIndex + 1) : "";
        Element target = selector.length() == 0 ? root : root.selectFirst(selector);
        if (target == null) {
            return "";
        }
        if (attr.length() > 0) {
            return target.attr(attr).trim();
        }
        return target.text().trim();
    }

    private static String evalXPath(String html, String xpath) {
        try {
            org.w3c.dom.Document document;
            try {
                document = new W3CDom().fromJsoup(Jsoup.parse(html == null ? "" : html));
            } catch (Throwable ignored) {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(html)));
            }
            Object result = XPathFactory.newInstance().newXPath()
                    .evaluate(xpath, document, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            if (nodes.getLength() == 0) {
                String text = XPathFactory.newInstance().newXPath().evaluate(xpath, document);
                return text == null ? "" : text.trim();
            }
            Node node = nodes.item(0);
            return node == null ? "" : node.getTextContent().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String evalJsonPath(String json, String path) {
        try {
            JsonElement current = new JsonParser().parse(json);
            String body = path.startsWith("$.") ? path.substring(2) : path;
            String[] parts = body.split("\\.");
            for (String part : parts) {
                if (part.length() == 0) {
                    continue;
                }
                int arrayIndex = part.indexOf('[');
                String key = arrayIndex >= 0 ? part.substring(0, arrayIndex) : part;
                if (key.length() > 0) {
                    JsonObject object = current.getAsJsonObject();
                    current = object.get(key);
                }
                if (arrayIndex >= 0) {
                    int end = part.indexOf(']', arrayIndex);
                    int index = Integer.parseInt(part.substring(arrayIndex + 1, end));
                    JsonArray array = current.getAsJsonArray();
                    current = array.get(index);
                }
                if (current == null || current.isJsonNull()) {
                    return "";
                }
            }
            return current.isJsonPrimitive() ? current.getAsString() : current.toString();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String stripPrefix(String value, String prefix) {
        return value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }
}
