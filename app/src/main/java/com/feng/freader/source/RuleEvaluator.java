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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        if (rule == null || rule.trim().length() == 0 || rule.startsWith("jsonpath:")
                || rule.startsWith("xpath:") || rule.startsWith("javascript:")) {
            return new ArrayList<>();
        }
        String rawRule = stripPrefix(rule, "css:");
        if (rawRule.equals(rule) && !rule.startsWith("css:")) {
            rawRule = rule;
        }
        int attrIndex = rawRule.indexOf('@');
        String selector = attrIndex >= 0 ? rawRule.substring(0, attrIndex) : rawRule;
        if (selector.trim().length() == 0) {
            return new ArrayList<>();
        }
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

    public static List<JsonElement> selectJsonElements(String body, String rule) {
        try {
            JsonElement root = new JsonParser().parse(body == null ? "" : body);
            return selectJsonElements(root, stripPrefix(rule, "jsonpath:"));
        } catch (Throwable ignored) {
            return new ArrayList<>();
        }
    }

    public static String evalJsonElement(JsonElement element, String rule) {
        if (element == null || element.isJsonNull()) {
            return "";
        }
        String path = stripPrefix(rule, "jsonpath:");
        List<JsonElement> values = selectJsonElements(element, path);
        if (values.isEmpty()) {
            return "";
        }
        JsonElement value = values.get(0);
        return value == null || value.isJsonNull()
                ? ""
                : (value.isJsonPrimitive() ? value.getAsString() : value.toString());
    }

    public static List<XPathItem> selectXPathItems(String body, String rule) {
        List<XPathItem> items = new ArrayList<>();
        try {
            org.w3c.dom.Document document = parseXPathDocument(body);
            NodeList nodes = (NodeList) XPathFactory.newInstance().newXPath()
                    .evaluate(stripPrefix(rule, "xpath:"), document, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node != null) {
                    items.add(new XPathItem(node));
                }
            }
        } catch (Throwable ignored) {
        }
        return items;
    }

    public static String eval(String body, String rule) {
        if (rule == null || rule.length() == 0) {
            return "";
        }
        if (rule.startsWith("css:")) {
            return evalCss(Jsoup.parse(body == null ? "" : body), stripPrefix(rule, "css:"));
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
            org.w3c.dom.Document document = parseXPathDocument(html);
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

    private static org.w3c.dom.Document parseXPathDocument(String html) throws Exception {
        try {
            return new W3CDom().fromJsoup(Jsoup.parse(html == null ? "" : html));
        } catch (Throwable ignored) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(html)));
        }
    }

    private static String evalJsonPath(String json, String path) {
        try {
            JsonElement current = new JsonParser().parse(json);
            return evalJsonElement(current, path);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static List<JsonElement> selectJsonElements(JsonElement root, String path) {
        List<JsonElement> current = new ArrayList<>();
        current.add(root);
        String body = normalizeJsonPath(path);
        if (body.length() == 0 || "$".equals(body)) {
            return current;
        }
        if (body.startsWith("$..")) {
            List<JsonElement> found = new ArrayList<>();
            findByKey(root, body.substring(3), found);
            return found;
        }
        if (body.startsWith("$.")) {
            body = body.substring(2);
        } else if (body.startsWith(".")) {
            body = body.substring(1);
        }
        String[] parts = body.split("\\.");
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            current = stepJson(current, part);
            if (current.isEmpty()) {
                return current;
            }
        }
        return current;
    }

    private static List<JsonElement> stepJson(List<JsonElement> input, String token) {
        List<JsonElement> output = new ArrayList<>();
        int bracket = token.indexOf('[');
        String key = bracket >= 0 ? token.substring(0, bracket) : token;
        String index = "";
        if (bracket >= 0) {
            int end = token.indexOf(']', bracket);
            index = end > bracket ? token.substring(bracket + 1, end) : "";
        }
        for (JsonElement element : input) {
            List<JsonElement> values = selectKey(element, key);
            if (bracket >= 0) {
                output.addAll(selectArray(values, index));
            } else {
                output.addAll(values);
            }
        }
        return output;
    }

    private static List<JsonElement> selectKey(JsonElement element, String key) {
        List<JsonElement> values = new ArrayList<>();
        if (element == null || element.isJsonNull()) {
            return values;
        }
        if (key.length() == 0) {
            values.add(element);
            return values;
        }
        if (element.isJsonObject()) {
            JsonElement value = element.getAsJsonObject().get(key);
            if (value != null && !value.isJsonNull()) {
                values.add(value);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                values.addAll(selectKey(child, key));
            }
        }
        return values;
    }

    private static List<JsonElement> selectArray(List<JsonElement> input, String index) {
        List<JsonElement> output = new ArrayList<>();
        for (JsonElement element : input) {
            if (element == null || !element.isJsonArray()) {
                continue;
            }
            JsonArray array = element.getAsJsonArray();
            if ("*".equals(index) || index.length() == 0) {
                for (JsonElement child : array) {
                    output.add(child);
                }
            } else {
                try {
                    int position = Integer.parseInt(index);
                    if (position >= 0 && position < array.size()) {
                        output.add(array.get(position));
                    }
                } catch (Throwable ignored) {
                }
            }
        }
        return output;
    }

    private static void findByKey(JsonElement element, String key, List<JsonElement> found) {
        if (element == null || element.isJsonNull() || key.length() == 0) {
            return;
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement value = object.get(key);
            if (value != null && !value.isJsonNull()) {
                found.add(value);
            }
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                findByKey(entry.getValue(), key, found);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                findByKey(child, key, found);
            }
        }
    }

    private static String normalizeJsonPath(String path) {
        String body = path == null ? "" : path.trim();
        if (body.startsWith("jsonpath:")) {
            body = body.substring("jsonpath:".length());
        }
        return body;
    }

    private static String stripPrefix(String value, String prefix) {
        return value != null && value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    public static class XPathItem {
        private final Node node;

        XPathItem(Node node) {
            this.node = node;
        }

        public String eval(String rule) {
            if (rule == null || rule.trim().length() == 0) {
                return text(node);
            }
            String body = stripPrefix(rule.trim(), "xpath:");
            if (body.startsWith("@")) {
                return attr(node, body.substring(1));
            }
            try {
                Object result = XPathFactory.newInstance().newXPath()
                        .evaluate(body, node, XPathConstants.NODESET);
                NodeList nodes = (NodeList) result;
                if (nodes.getLength() > 0) {
                    return text(nodes.item(0));
                }
                String text = XPathFactory.newInstance().newXPath().evaluate(body, node);
                return text == null ? "" : text.trim();
            } catch (Throwable ignored) {
                return "";
            }
        }

        private static String text(Node node) {
            return node == null || node.getTextContent() == null
                    ? ""
                    : node.getTextContent().trim();
        }

        private static String attr(Node node, String name) {
            if (node == null || name == null || name.length() == 0) {
                return "";
            }
            NamedNodeMap attributes = node.getAttributes();
            if (attributes == null) {
                return "";
            }
            Node attr = attributes.getNamedItem(name);
            return text(attr);
        }
    }
}
