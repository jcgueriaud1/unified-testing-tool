package com.vaadin.testing.unified;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** One Testing Tool's own implementation of a wrapper interface. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolImplementation {
    TestingTool tool();

    /**
     * An interface extending the wrapper interface: it redefines the default methods that need the tool's
     * own API (through the Escape Hatch, e.g. {@code element().unwrap(Rating.class)}); the others are
     * inherited. Used instead of the wrapper interface when the test runs on {@link #tool()}.
     */
    Class<? extends UnifiedWrapper> value();
}
