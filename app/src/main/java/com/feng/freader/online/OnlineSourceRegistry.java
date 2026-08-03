package com.feng.freader.online;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OnlineSourceRegistry {
    private final List<OnlineBookSource> sources = new ArrayList<>();

    public static OnlineSourceRegistry createDefault() {
        OnlineSourceRegistry registry = new OnlineSourceRegistry();
        registry.add(new OpdsBookSource("gutenberg", "Project Gutenberg",
                "https://www.gutenberg.org",
                "https://www.gutenberg.org/ebooks/search.opds/",
                "https://www.gutenberg.org/ebooks/search.opds/?query={searchTerms}&start_index={page}",
                "免费公版开放资源，来自 Project Gutenberg"));
        registry.add(new StandardEbooksSource());
        registry.add(new InternetArchiveSource());
        registry.add(new WikisourceOnlineSource());
        registry.add(new OapenSource());
        return registry;
    }

    public void add(OnlineBookSource source) {
        sources.add(source);
    }

    public List<OnlineBookSource> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public OnlineBookSource find(String id) {
        for (OnlineBookSource source : sources) {
            if (source.getId().equals(id)) {
                return source;
            }
        }
        return null;
    }
}
