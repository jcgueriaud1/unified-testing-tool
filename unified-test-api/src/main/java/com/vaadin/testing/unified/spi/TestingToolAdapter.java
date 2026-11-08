package com.vaadin.testing.unified.spi;

import com.vaadin.testing.unified.TestingTool;

/** A Testing Tool adapter, registered with {@code ServiceLoader}. Loads nothing of its tool until {@link #open}. */
public interface TestingToolAdapter {
    TestingTool tool();

    /** Starts one test: one fresh page. */
    AdapterSession open(AdapterContext context);
}
