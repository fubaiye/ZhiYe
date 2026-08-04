package com.feng.freader.reader;

import com.feng.freader.entity.epub.EpubTocItem;
import com.feng.freader.entity.epub.OpfData;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class EpubChapterIndexResolverTest {

    @Test
    public void matchesTocPathWithAnchorToSpineChapter() {
        OpfData opfData = new OpfData();
        opfData.setSpine(Arrays.asList(
                "C:\\books\\demo\\OEBPS\\cover.xhtml",
                "C:\\books\\demo\\OEBPS\\Text\\chapter1.xhtml",
                "C:\\books\\demo\\OEBPS\\Text\\chapter2.xhtml"));
        EpubTocItem tocItem = new EpubTocItem("第二章", "C:\\books\\demo\\OEBPS\\Text\\chapter2.xhtml#part-1");

        int index = EpubChapterIndexResolver.resolve(
                Arrays.asList(new EpubTocItem("第一章", "C:\\books\\demo\\OEBPS\\Text\\chapter1.xhtml"),
                        tocItem),
                opfData,
                1);

        assertEquals(2, index);
    }

    @Test
    public void fallsBackToClickedPositionWhenTocPathDoesNotMatchSpine() {
        OpfData opfData = new OpfData();
        opfData.setSpine(Arrays.asList("cover.xhtml", "chapter1.xhtml", "chapter2.xhtml"));

        int index = EpubChapterIndexResolver.resolve(
                Arrays.asList(new EpubTocItem("封面", "missing-cover.xhtml"),
                        new EpubTocItem("第一章", "missing-1.xhtml"),
                        new EpubTocItem("第二章", "missing-2.xhtml")),
                opfData,
                2);

        assertEquals(2, index);
    }
}
