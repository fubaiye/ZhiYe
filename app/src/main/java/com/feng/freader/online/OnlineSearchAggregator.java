package com.feng.freader.online;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class OnlineSearchAggregator {
    public BookPage search(List<OnlineBookSource> sources, final String keyword, final int page) throws Exception {
        if (keyword == null || keyword.trim().isEmpty()) {
            return BookPage.empty();
        }
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(4, Math.max(1, sources.size())));
        List<Future<BookPage>> futures = new ArrayList<>();
        for (final OnlineBookSource source : sources) {
            if (!source.isEnabled()) {
                continue;
            }
            futures.add(executor.submit(new Callable<BookPage>() {
                @Override
                public BookPage call() throws Exception {
                    return source.search(keyword, page);
                }
            }));
        }
        List<OnlineBook> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Future<BookPage> future : futures) {
            try {
                for (OnlineBook book : future.get().getItems()) {
                    String key = book.getTitle().trim().toLowerCase() + "|" + book.getAuthorText().trim().toLowerCase();
                    if (seen.add(key)) {
                        merged.add(book);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        executor.shutdownNow();
        return new BookPage(merged, page, false, "");
    }
}
