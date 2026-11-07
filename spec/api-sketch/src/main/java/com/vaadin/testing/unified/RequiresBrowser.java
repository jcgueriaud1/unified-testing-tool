package com.vaadin.testing.unified;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Capability: the test needs a real browser (CSS, layout, client-side JavaScript, real focus and
 * keyboard, screenshots). Skipped on Testing Tools without one.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresBrowser {}
