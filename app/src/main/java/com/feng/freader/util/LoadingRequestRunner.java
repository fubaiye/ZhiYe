package com.feng.freader.util;

public class LoadingRequestRunner {

    private LoadingRequestRunner() {
    }

    public static void run(Runnable showLoading, Runnable request) {
        if (showLoading != null) {
            showLoading.run();
        }
        if (request != null) {
            request.run();
        }
    }
}
