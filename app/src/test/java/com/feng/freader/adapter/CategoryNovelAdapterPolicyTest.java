package com.feng.freader.adapter;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CategoryNovelAdapterPolicyTest {

    @Test
    public void itemCountUsesNamesEvenWhenCoversAreShorter() {
        assertEquals(2, CategoryNovelAdapter.safeItemCount(
                Arrays.asList("a", "b"), Collections.singletonList("cover")));
    }

    @Test
    public void detectsBlankCoversForPlaceholderRendering() {
        assertTrue(CategoryNovelAdapter.isBlankCover(""));
        assertTrue(CategoryNovelAdapter.isBlankCover("   "));
        assertFalse(CategoryNovelAdapter.isBlankCover("https://example.com/a.jpg"));
    }
}
