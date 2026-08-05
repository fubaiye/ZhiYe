package com.feng.freader.adapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

public class HotRankAdapterPolicyTest {

    @Test
    public void itemCountUsesSafeMinimumOfRankTitlesAndDataGroups() {
        assertEquals(1, HotRankAdapter.safeItemCount(
                Arrays.asList("榜一", "榜二"),
                Collections.singletonList(Collections.singletonList("书名"))));
    }
}
