package com.feng.freader.source;

import org.jsoup.nodes.Element;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class RuleEvaluatorTest {

    @Test
    public void evaluatesCssAndAttributes() {
        String html = "<div class='book'><a href='/b/1'><span class='name'>剑来</span></a></div>";
        List<Element> items = RuleEvaluator.selectElements(html, "css:.book");
        String name = RuleEvaluator.evalElement(items.get(0), "css:.name");
        String url = RuleEvaluator.evalElement(items.get(0), "css:a@href");

        assertEquals("剑来", name);
        assertEquals("/b/1", url);
    }

    @Test
    public void evaluatesXPath() {
        String html = "<html><body><div class='book'><span>雪中悍刀行</span></div></body></html>";
        String name = RuleEvaluator.eval(html, "xpath://div[@class='book']/span/text()");

        assertEquals("雪中悍刀行", name);
    }

    @Test
    public void evaluatesJsonPath() {
        String json = "{\"data\":{\"books\":[{\"name\":\"庆余年\"}]}}";
        String name = RuleEvaluator.eval(json, "jsonpath:$.data.books[0].name");

        assertEquals("庆余年", name);
    }

    @Test
    public void appliesJavaScriptStyleTextHelpers() {
        String value = RuleEvaluator.applyJavaScript("  《长夜余火》  ", "trim().replace('《','').replace('》','')");

        assertEquals("长夜余火", value);
    }
}
