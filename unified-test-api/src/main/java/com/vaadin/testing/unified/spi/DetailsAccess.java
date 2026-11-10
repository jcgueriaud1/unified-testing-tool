package com.vaadin.testing.unified.spi;

import com.vaadin.testing.unified.UnifiedElement;

/** Adapter access to a Details, reached by the core with {@code element.unwrap(DetailsAccess.class)}. */
public interface DetailsAccess {

    String summaryText();

    boolean isOpen();

    /** Clicks the summary, as a user would. */
    void toggle();

    /** The content, below the summary. */
    UnifiedElement content();
}
