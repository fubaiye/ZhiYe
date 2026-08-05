package com.feng.freader.source;

import com.feng.freader.entity.data.NovelSourceData;
import com.feng.freader.entity.data.CatalogData;
import com.feng.freader.entity.data.DetailedChapterData;
import com.google.gson.JsonElement;

import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BookSourceExecutor {
    private SourceHttpClient httpClient;

    public BookSourceExecutor() {
    }

    BookSourceExecutor(SourceHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public List<NovelSourceData> search(BookSource source, String keyword) throws IOException {
        List<NovelSourceData> all = new ArrayList<>();
        for (int page = source.getPagination().getStart(); page <= source.getPagination().getMax(); page++) {
            String body = httpClient().execute(source, keyword, page);
            all.addAll(parseSearchPage(source, body));
        }
        return all;
    }

    private SourceHttpClient httpClient() {
        if (httpClient == null) {
            httpClient = new SourceHttpClient();
        }
        return httpClient;
    }

    public List<NovelSourceData> parseSearchPage(BookSource source, String body) {
        BookSource.SourceRules rules = source.getSearchRules();
        List<NovelSourceData> results = new ArrayList<>();
        if (rules.getList().startsWith("jsonpath:")) {
            List<JsonElement> items = RuleEvaluator.selectJsonElements(body, rules.getList());
            for (JsonElement element : items) {
                NovelSourceData item = fromJsonElement(source, element, rules);
                if (!item.getName().isEmpty() && !item.getUrl().isEmpty()) {
                    results.add(item);
                }
            }
            return results;
        }
        List<Element> items = RuleEvaluator.selectElements(body, rules.getList());
        for (Element element : items) {
            NovelSourceData data = new NovelSourceData(
                    transform(RuleEvaluator.evalElement(element, rules.getName()), rules.getJavaScript()),
                    RuleEvaluator.evalElement(element, rules.getAuthor()),
                    RuleEvaluator.evalElement(element, rules.getIntro()),
                    SourceBookLink.encode(source.getId(),
                            absolute(source, RuleEvaluator.evalElement(element, rules.getUrl()))),
                    absolute(source, RuleEvaluator.evalElement(element, rules.getCover())));
            data.setSourceId(source.getId());
            data.setSourceName(source.getName());
            if (!data.getName().isEmpty() && !data.getUrl().isEmpty()) {
                results.add(data);
            }
        }
        return results;
    }

    private NovelSourceData fromBody(BookSource source, String body, BookSource.SourceRules rules) {
        NovelSourceData data = new NovelSourceData(
                transform(RuleEvaluator.eval(body, rules.getName()), rules.getJavaScript()),
                RuleEvaluator.eval(body, rules.getAuthor()),
                RuleEvaluator.eval(body, rules.getIntro()),
                SourceBookLink.encode(source.getId(), absolute(source, RuleEvaluator.eval(body, rules.getUrl()))),
                absolute(source, RuleEvaluator.eval(body, rules.getCover())));
        data.setSourceId(source.getId());
        data.setSourceName(source.getName());
        return data;
    }

    private NovelSourceData fromJsonElement(BookSource source, JsonElement element, BookSource.SourceRules rules) {
        NovelSourceData data = new NovelSourceData(
                transform(RuleEvaluator.evalJsonElement(element, rules.getName()), rules.getJavaScript()),
                RuleEvaluator.evalJsonElement(element, rules.getAuthor()),
                RuleEvaluator.evalJsonElement(element, rules.getIntro()),
                SourceBookLink.encode(source.getId(),
                        absolute(source, RuleEvaluator.evalJsonElement(element, rules.getUrl()))),
                absolute(source, RuleEvaluator.evalJsonElement(element, rules.getCover())));
        data.setSourceId(source.getId());
        data.setSourceName(source.getName());
        return data;
    }

    public CatalogData catalog(BookSource source, String bookUrl) throws IOException {
        String detailUrl = SourceBookLink.originalUrl(bookUrl);
        if (detailUrl == null || detailUrl.trim().isEmpty()) {
            throw new IOException("书籍详情地址为空");
        }
        String detailBody = httpClient().executeUrl(source, detailUrl);
        String catalogBody = detailBody;
        String tocUrlRule = source.getDetailRules().getUrl();
        String tocUrl = "";
        if (tocUrlRule != null && tocUrlRule.trim().length() > 0) {
            tocUrl = absolute(source, RuleEvaluator.eval(detailBody, tocUrlRule));
        }
        if (tocUrl != null && tocUrl.trim().length() > 0 && !tocUrl.equals(detailUrl)) {
            catalogBody = httpClient().executeUrl(source, tocUrl);
        }
        BookSource.SourceRules rules = source.getCatalogRules();
        List<String> chapterNames = new ArrayList<>();
        List<String> chapterUrls = new ArrayList<>();
        String listRule = rules.getList();
        if (listRule.startsWith("jsonpath:")) {
            parseJsonCatalog(source, catalogBody, rules, chapterNames, chapterUrls);
        } else if (listRule.startsWith("xpath:")) {
            parseXPathCatalog(source, catalogBody, rules, chapterNames, chapterUrls);
        } else if (listRule.length() == 0) {
            NovelSourceData one = fromBody(source, catalogBody, rules);
            if (!one.getName().isEmpty() && !one.getUrl().isEmpty()) {
                chapterNames.add(one.getName());
                chapterUrls.add(one.getUrl());
            }
        } else {
            parseCssCatalog(source, catalogBody, rules, chapterNames, chapterUrls);
        }
        if (chapterUrls.isEmpty()) {
            throw new IOException("目录解析为空；规则：" + listRule
                    + "；详情地址：" + detailUrl
                    + "；目录地址：" + tocUrl);
        }
        return new CatalogData(chapterNames, chapterUrls);
    }

    private void parseCssCatalog(BookSource source, String body, BookSource.SourceRules rules,
                                 List<String> names, List<String> urls) {
        List<Element> items = RuleEvaluator.selectElements(body, rules.getList());
        for (Element item : items) {
            addChapter(source,
                    RuleEvaluator.evalElement(item, rules.getName()),
                    absolute(source, RuleEvaluator.evalElement(item, rules.getUrl())),
                    names,
                    urls);
        }
    }

    private void parseJsonCatalog(BookSource source, String body, BookSource.SourceRules rules,
                                  List<String> names, List<String> urls) {
        List<JsonElement> items = RuleEvaluator.selectJsonElements(body, rules.getList());
        for (JsonElement item : items) {
            addChapter(source,
                    RuleEvaluator.evalJsonElement(item, rules.getName()),
                    absolute(source, RuleEvaluator.evalJsonElement(item, rules.getUrl())),
                    names,
                    urls);
        }
    }

    private void parseXPathCatalog(BookSource source, String body, BookSource.SourceRules rules,
                                   List<String> names, List<String> urls) {
        List<RuleEvaluator.XPathItem> items = RuleEvaluator.selectXPathItems(body, rules.getList());
        for (RuleEvaluator.XPathItem item : items) {
            addChapter(source,
                    item.eval(rules.getName()),
                    absolute(source, item.eval(rules.getUrl())),
                    names,
                    urls);
        }
    }

    private void addChapter(BookSource source, String name, String url,
                            List<String> names, List<String> urls) {
        if (name == null || url == null) {
            return;
        }
        name = name.trim();
        url = url.trim();
        if (name.length() == 0 || url.length() == 0) {
            return;
        }
        names.add(name);
        urls.add(SourceBookLink.encode(source.getId(), url));
    }

    public DetailedChapterData content(BookSource source, String chapterUrl) throws IOException {
        String url = SourceBookLink.originalUrl(chapterUrl);
        String body = httpClient().executeUrl(source, url);
        BookSource.SourceRules rules = source.getContentRules();
        String title = RuleEvaluator.eval(body, rules.getName());
        String content = extractContent(body, rules);
        return new DetailedChapterData(title, content);
    }

    private String extractContent(String body, BookSource.SourceRules rules) {
        List<Element> items = RuleEvaluator.selectElements(body, rules.getContent());
        if (!items.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (Element item : items) {
                String text = item.text().trim();
                if (text.length() == 0) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(text);
            }
            return builder.toString();
        }
        return RuleEvaluator.eval(body, rules.getContent());
    }

    private String transform(String value, String javaScript) {
        if (javaScript == null || javaScript.length() == 0) {
            return value == null ? "" : value.trim();
        }
        return RuleEvaluator.applyJavaScript(value, javaScript).trim();
    }

    private String absolute(BookSource source, String value) {
        if (value == null || value.length() == 0 || value.startsWith("http")) {
            return value == null ? "" : value;
        }
        String host = source.getVariables().get("host");
        if (host == null || host.length() == 0) {
            return value;
        }
        if (host.endsWith("/") && value.startsWith("/")) {
            return host.substring(0, host.length() - 1) + value;
        }
        if (!host.endsWith("/") && !value.startsWith("/")) {
            return host + "/" + value;
        }
        return host + value;
    }
}
