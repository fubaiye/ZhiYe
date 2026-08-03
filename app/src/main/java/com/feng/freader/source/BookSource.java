package com.feng.freader.source;

import java.util.LinkedHashMap;
import java.util.Map;

public class BookSource {
    private String id = "";
    private String name = "";
    private boolean enabled = true;
    private String searchUrl = "";
    private String searchMethod = "GET";
    private String searchBody = "";
    private Map<String, String> headers = new LinkedHashMap<>();
    private Map<String, String> cookies = new LinkedHashMap<>();
    private Map<String, String> variables = new LinkedHashMap<>();
    private Pagination pagination = new Pagination();
    private SourceRules searchRules = new SourceRules();
    private SourceRules detailRules = new SourceRules();
    private SourceRules catalogRules = new SourceRules();
    private SourceRules contentRules = new SourceRules();

    public String getId() {
        return empty(id);
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return empty(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getSearchUrl() {
        return empty(searchUrl);
    }

    public void setSearchUrl(String searchUrl) {
        this.searchUrl = searchUrl;
    }

    public String getSearchMethod() {
        return empty(searchMethod).toUpperCase();
    }

    public void setSearchMethod(String searchMethod) {
        this.searchMethod = searchMethod;
    }

    public String getSearchBody() {
        return empty(searchBody);
    }

    public void setSearchBody(String searchBody) {
        this.searchBody = searchBody;
    }

    public Map<String, String> getHeaders() {
        return headers == null ? new LinkedHashMap<String, String>() : headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getCookies() {
        return cookies == null ? new LinkedHashMap<String, String>() : cookies;
    }

    public void setCookies(Map<String, String> cookies) {
        this.cookies = cookies;
    }

    public Map<String, String> getVariables() {
        return variables == null ? new LinkedHashMap<String, String>() : variables;
    }

    public void setVariables(Map<String, String> variables) {
        this.variables = variables;
    }

    public Pagination getPagination() {
        return pagination == null ? new Pagination() : pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }

    public SourceRules getSearchRules() {
        return searchRules == null ? new SourceRules() : searchRules;
    }

    public void setSearchRules(SourceRules searchRules) {
        this.searchRules = searchRules;
    }

    public SourceRules getDetailRules() {
        return detailRules == null ? new SourceRules() : detailRules;
    }

    public void setDetailRules(SourceRules detailRules) {
        this.detailRules = detailRules;
    }

    public SourceRules getCatalogRules() {
        return catalogRules == null ? new SourceRules() : catalogRules;
    }

    public void setCatalogRules(SourceRules catalogRules) {
        this.catalogRules = catalogRules;
    }

    public SourceRules getContentRules() {
        return contentRules == null ? new SourceRules() : contentRules;
    }

    public void setContentRules(SourceRules contentRules) {
        this.contentRules = contentRules;
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }

    public static class Pagination {
        private int start = 1;
        private int max = 1;

        public int getStart() {
            return Math.max(1, start);
        }

        public void setStart(int start) {
            this.start = start;
        }

        public int getMax() {
            return Math.max(getStart(), max);
        }

        public void setMax(int max) {
            this.max = max;
        }
    }

    public static class SourceRules {
        private String list = "";
        private String name = "";
        private String author = "";
        private String intro = "";
        private String url = "";
        private String cover = "";
        private String content = "";
        private String nextPage = "";
        private String javaScript = "";

        public String getList() {
            return empty(list);
        }

        public void setList(String list) {
            this.list = list;
        }

        public String getName() {
            return empty(name);
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAuthor() {
            return empty(author);
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getIntro() {
            return empty(intro);
        }

        public void setIntro(String intro) {
            this.intro = intro;
        }

        public String getUrl() {
            return empty(url);
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getCover() {
            return empty(cover);
        }

        public void setCover(String cover) {
            this.cover = cover;
        }

        public String getContent() {
            return empty(content);
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getNextPage() {
            return empty(nextPage);
        }

        public void setNextPage(String nextPage) {
            this.nextPage = nextPage;
        }

        public String getJavaScript() {
            return empty(javaScript);
        }

        public void setJavaScript(String javaScript) {
            this.javaScript = javaScript;
        }
    }
}
