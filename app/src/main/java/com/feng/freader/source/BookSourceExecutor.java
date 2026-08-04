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
        String detailBody = httpClient().executeUrl(source, detailUrl);
        String catalogBody = detailBody;
        String tocUrl = absolute(source, RuleEvaluator.eval(detailBody, source.getDetailRules().getUrl()));
        if (tocUrl.length() > 0 && !tocUrl.equals(detailUrl)) {
            catalogBody = httpClient().executeUrl(source, tocUrl);
        }
        BookSource.SourceRules rules = source.getCatalogRules();
        List<String> chapterNames = new ArrayList<>();
        List<String> chapterUrls = new ArrayList<>();
        List<Element> items = RuleEvaluator.selectElements(catalogBody, rules.getList());
        if (items.isEmpty() && rules.getList().length() == 0) {
            NovelSourceData one = fromBody(source, catalogBody, rules);
            if (!one.getName().isEmpty() && !one.getUrl().isEmpty()) {
                chapterNames.add(one.getName());
                chapterUrls.add(one.getUrl());
            }
        } else {
            for (Element item : items) {
                String name = RuleEvaluator.evalElement(item, rules.getName());
                String url = absolute(source, RuleEvaluator.evalElement(item, rules.getUrl()));
                if (name.length() > 0 && url.length() > 0) {
                    chapterNames.add(name);
                    chapterUrls.add(SourceBookLink.encode(source.getId(), url));
                }
            }
        }
        return new CatalogData(chapterNames, chapterUrls);
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
