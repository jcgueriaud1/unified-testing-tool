package com.vaadin.testing.unified.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vaadin.testing.unified.spi.Filter;
import java.util.List;
import org.junit.jupiter.api.Test;

class WrapperSelectorTest {

    @Test
    void turnsTheSelectorIntoFilters() {
        assertEquals(List.of(new Filter(Filter.Kind.CLASS_NAME, null, "metric-card", true),
                new Filter(Filter.Kind.ID, null, "total", true),
                new Filter(Filter.Kind.ATTRIBUTE, "data-kind", "a \"b\"", true),
                new Filter(Filter.Kind.THEME, null, "compact", true)),
                WrapperSelector.filters(".metric-card#total[data-kind=\"a \\\"b\\\"\"][theme~=\"compact\"]", "W"));
    }

    @Test
    void refusesWhatSomeToolCantApply() {
        assertThrows(IllegalArgumentException.class, () -> WrapperSelector.filters(".card span", "W"));
        assertThrows(IllegalArgumentException.class, () -> WrapperSelector.filters(":hover", "W"));
        assertThrows(IllegalArgumentException.class, () -> WrapperSelector.filters("[class~=\"x\"]", "W"));
    }
}
