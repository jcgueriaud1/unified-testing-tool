package com.vaadin.testing.unified;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** One Testing Tool's own implementation of a wrapper interface. */
@Retention(RetentionPolicy.RUNTIME)
public @interface ToolImplementation {
    TestingTool tool();

    /** Implements the wrapper interface with the tool's own API, through the Escape Hatch. */
    Class<? extends UnifiedWrapper> value();
}
