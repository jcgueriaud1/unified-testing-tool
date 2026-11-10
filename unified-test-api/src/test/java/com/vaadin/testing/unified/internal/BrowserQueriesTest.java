package com.vaadin.testing.unified.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.vaadin.testing.unified.spi.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserQueriesTest {

    private static Filter f(Filter.Kind kind, String name, String value, boolean exact) {
        return new Filter(kind, name, value, exact);
    }

    @Test
    void putsWhatCssCanExpressIntoTheSelector() {
        List<Filter> filters = List.of(f(Filter.Kind.CLASS_NAME, null, "card", true),
                f(Filter.Kind.THEME, null, "primary", true), f(Filter.Kind.ATTRIBUTE, "role", "row", true),
                f(Filter.Kind.TEST_ID, null, "a\"b", true), f(Filter.Kind.TEXT, null, "Save", true));

        assertEquals("*[class~=\"card\"][theme~=\"primary\"][role=\"row\"][data-testid=\"a\\\"b\"]",
                BrowserQueries.selector("*", filters));
        assertEquals(1, BrowserQueries.remaining(filters).size());
        assertEquals("TEXT", BrowserQueries.remaining(filters).get(0).get("kind"));
    }

    @Test
    void leavesTheRestToThePage() {
        List<Filter> filters = List.of(f(Filter.Kind.CLASS_NAME, null, "two words", true),
                f(Filter.Kind.ATTRIBUTE, "on:click", "x", true), f(Filter.Kind.LABEL, null, "Name", false));

        assertEquals("vaadin-text-field", BrowserQueries.selector("vaadin-text-field", filters));
        assertEquals(3, BrowserQueries.remaining(filters).size());
    }

    @Test
    void namesCssPropertiesWithDashes() {
        assertEquals("background-color", BrowserQueries.cssName("backgroundColor"));
        assertEquals("gap", BrowserQueries.cssName("gap"));
        assertEquals("--lumo-space-m", BrowserQueries.cssName("--lumo-space-m"));
    }
}
