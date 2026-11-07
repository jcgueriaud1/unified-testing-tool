package com.vaadin.testing.unified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** The test uses a tool-specific Escape Hatch. Skipped on the other Testing Tools. */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresTestingTool {
    TestingTool[] value();
}
