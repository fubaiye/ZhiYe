package com.feng.freader.util;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LoadingRequestRunnerTest {

    @Test
    public void keepsLoadingHiddenWhenRequestCompletesSynchronously() {
        final boolean[] loadingVisible = {false};

        LoadingRequestRunner.run(
                new Runnable() {
                    @Override
                    public void run() {
                        loadingVisible[0] = true;
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        assertTrue(loadingVisible[0]);
                        loadingVisible[0] = false;
                    }
                });

        assertFalse(loadingVisible[0]);
    }
}
