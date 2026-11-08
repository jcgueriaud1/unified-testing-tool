package com.vaadin.testing.unified.query;

/** Components with visible text content. */
public interface TextFilter<SELF> {
    SELF withText(String text);
    SELF withTextContaining(String part);
}
